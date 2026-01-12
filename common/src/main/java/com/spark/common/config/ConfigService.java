package com.spark.common.config;

import com.spark.common.mapper.SymbolConfigMapper;
import com.spark.common.model.SymbolConfig;
import com.spark.common.util.SymbolIdMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 配置服务
 * 从 MySQL 数据库读取交易对配置
 */
@Slf4j
@Service
public class ConfigService {
    @Autowired(required = false)
    private SymbolConfigMapper symbolConfigMapper;

    /**
     * 配置缓存：Map<Symbol, SymbolConfig>
     */
    private final Map<String, SymbolConfig> configCache = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        try {
            if (symbolConfigMapper == null) {
                log.warn("SymbolConfigMapper 未注入，配置服务将无法从数据库读取配置");
                return;
            }
            refreshConfigCache();
            log.info("配置服务初始化完成: count={}", configCache.size());
        } catch (Exception e) {
            log.error("配置服务初始化失败", e);
        }
    }

    /**
     * 刷新配置缓存
     * 从数据库重新加载所有启用的交易对配置到缓存
     * 
     * @throws RuntimeException 如果刷新失败
     */
    public void refreshConfigCache() {
        if (symbolConfigMapper == null) {
            log.warn("SymbolConfigMapper 未注入，无法刷新配置缓存");
            throw new IllegalStateException("SymbolConfigMapper 未注入，无法刷新配置缓存");
        }

        try {
            List<SymbolConfig> configs = symbolConfigMapper.selectEnabledConfigs();
            configCache.clear();

            for (SymbolConfig config : configs) {
                if (config != null && config.getSymbol() != null) {
                    configCache.put(config.getSymbol(), config);
                    // 注册交易对映射
                    if (config.getSymbolId() != null) {
                        SymbolIdMapper.register(config.getSymbol(), config.getSymbolId());
                    }
                }
            }

            log.info("配置缓存刷新完成: count={}", configCache.size());
        } catch (Exception e) {
            log.error("刷新配置缓存失败", e);
            throw new RuntimeException("刷新配置缓存失败", e);
        }
    }

    /**
     * 获取交易对配置
     */
    public SymbolConfig getTradingPairConfig(String symbol) {
        if (symbol == null) {
            return null;
        }

        // 先从缓存获取
        SymbolConfig config = configCache.get(symbol);
        if (config != null) {
            return config;
        }

        // 缓存未命中，从数据库查询
        if (symbolConfigMapper != null) {
            try {
                SymbolConfig dbConfig = symbolConfigMapper.selectBySymbol(symbol);
                if (dbConfig != null) {
                    configCache.put(symbol, dbConfig);
                    // 注册交易对映射
                    if (dbConfig.getSymbolId() != null) {
                        SymbolIdMapper.register(symbol, dbConfig.getSymbolId());
                    }
                    return dbConfig;
                }
            } catch (Exception e) {
                log.error("从数据库查询交易对配置失败: symbol={}", symbol, e);
            }
        }

        return null;
    }

    /**
     * 获取所有启用的交易对
     */
    public List<String> getEnabledTradingPairs() {
        // 如果缓存为空，尝试刷新缓存（失败不影响，返回空列表）
        if (configCache.isEmpty() && symbolConfigMapper != null) {
            try {
                refreshConfigCache();
            } catch (Exception e) {
                log.warn("自动刷新配置缓存失败，返回空列表", e);
            }
        }
        
        List<String> enabledPairs = new ArrayList<>();
        for (Map.Entry<String, SymbolConfig> entry : configCache.entrySet()) {
            SymbolConfig config = entry.getValue();
            if (config.getEnabled() != null && config.getEnabled() == 1) {
                enabledPairs.add(entry.getKey());
            }
        }
        return enabledPairs;
    }

    /**
     * 检查交易对是否存在且启用
     */
    public boolean isTradingPairEnabled(String symbol) {
        SymbolConfig config = getTradingPairConfig(symbol);
        return config != null && config.getEnabled() != null && config.getEnabled() == 1;
    }

    /**
     * 获取路由配置（topic和partition）
     *
     * @param symbolId         交易对ID
     * @param defaultTopic     默认Topic（当数据库中没有配置时使用）
     * @param defaultPartition 默认分片号（当数据库中没有配置时使用）
     * @return 路由配置，如果没有配置则返回默认值
     */
    public RoutingConfig getRoutingConfig(Integer symbolId, String defaultTopic, int defaultPartition) {
        if (symbolId == null) {
            return new RoutingConfig(defaultTopic, defaultPartition);
        }

        try {
            // 通过symbolId获取symbol
            String symbol = SymbolIdMapper.idToSymbol(symbolId);
            if (symbol == null) {
                log.info("无法通过symbolId获取symbol: symbolId={}, 使用默认路由配置", symbolId);
                return new RoutingConfig(defaultTopic, defaultPartition);
            }

            // 从缓存或数据库获取交易对配置
            SymbolConfig config = getTradingPairConfig(symbol);
            if (config != null) {
                // 获取topic：优先使用配置的topic，没有则使用默认topic
                String topic = (config.getTopic() != null && !config.getTopic().trim().isEmpty())
                        ? config.getTopic()
                        : defaultTopic;

                // 获取partition：优先使用配置的partition，没有则使用默认partition
                int partition = (config.getPartition() != null)
                        ? config.getPartition()
                        : defaultPartition;

                log.info("从数据库获取路由配置: symbol={}, symbolId={}, topic={}, partition={}",
                        symbol, symbolId, topic, partition);
                return new RoutingConfig(topic, partition);
            } else {
                log.info("数据库中未找到配置: symbol={}, symbolId={}, 使用默认路由配置", symbol, symbolId);
                return new RoutingConfig(defaultTopic, defaultPartition);
            }
        } catch (Exception e) {
            log.info("获取路由配置失败: symbolId={}, 使用默认路由配置, error={}",
                    symbolId, e.getMessage());
            return new RoutingConfig(defaultTopic, defaultPartition);
        }
    }

}
