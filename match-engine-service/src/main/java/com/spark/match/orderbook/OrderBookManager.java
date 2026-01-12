package com.spark.match.orderbook;

import com.spark.common.config.ConfigService;
import com.spark.common.model.SymbolConfig;
import com.spark.common.util.SymbolIdMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 订单簿管理器
 * 管理多个交易对的订单簿
 */
@Slf4j
@Component
public class OrderBookManager {
    /**
     * 订单簿映射：Map<SymbolId, OrderBook>
     */
    private final Map<Integer, OrderBook> orderBooks = new ConcurrentHashMap<>();
    
    @Autowired
    private ConfigService configService;

    /**
     * 初始化订单簿管理器
     */
    @PostConstruct
    public void init() {
        log.info("初始化订单簿管理器");
        
        // 从数据库加载交易对配置
        loadTradingPairsFromDatabase();
        
        // 注意：订单簿的恢复由OrderBookRecoveryService负责，在应用启动后通过CommandLineRunner执行
        // 这里只负责从数据库加载交易对配置并创建空的订单簿
    }

    /**
     * 获取订单簿
     * 注意：如果订单簿不存在，返回null（不自动创建）
     *
     * @param symbolId 交易对ID
     * @return 订单簿，如果不存在则返回null
     */
    public OrderBook getOrderBook(Integer symbolId) {
        return orderBooks.get(symbolId);
    }

    /**
     * 创建订单簿
     *
     * @param symbolId 交易对ID
     * @return 订单簿
     */
    public OrderBook createOrderBook(Integer symbolId) {
        OrderBook orderBook = new OrderBook(symbolId);
        orderBooks.put(symbolId, orderBook);
        log.info("创建订单簿: symbolId={}", symbolId);
        return orderBook;
    }

    /**
     * 移除订单簿
     *
     * @param symbolId 交易对ID
     */
    public void removeOrderBook(Integer symbolId) {
        OrderBook removed = orderBooks.remove(symbolId);
        if (removed != null) {
            log.info("移除订单簿: symbolId={}", symbolId);
        }
    }

    /**
     * 获取所有订单簿
     */
    public Map<Integer, OrderBook> getAllOrderBooks() {
        return orderBooks;
    }

    /**
     * 注册订单簿（用于从Snapshot恢复）
     * 将已恢复的OrderBook注册到管理器
     *
     * @param symbolId  交易对ID
     * @param orderBook 订单簿
     */
    public void registerOrderBook(Integer symbolId, OrderBook orderBook) {
        if (orderBook == null) {
            throw new IllegalArgumentException("OrderBook不能为null: symbolId=" + symbolId);
        }
        if (orderBooks.containsKey(symbolId)) {
            log.info("订单簿已存在，将被覆盖: symbolId={}", symbolId);
        }
        orderBooks.put(symbolId, orderBook);
        log.info("注册订单簿: symbolId={}, lastAppliedWalSeq={}", symbolId, orderBook.getLastAppliedWalSeq());
    }

    /**
     * 从数据库加载交易对配置
     */
    private void loadTradingPairsFromDatabase() {
        try {
            List<String> enabledPairs = configService.getEnabledTradingPairs();
            log.info("从数据库加载交易对配置: count={}", enabledPairs.size());

            for (String symbol : enabledPairs) {
                SymbolConfig config = configService.getTradingPairConfig(symbol);
                if (config != null && config.getSymbolId() != null) {
                    // 注册交易对映射
                    SymbolIdMapper.register(symbol, config.getSymbolId());

                    // 创建订单簿（如果不存在）
                    if (!orderBooks.containsKey(config.getSymbolId())) {
                        createOrderBook(config.getSymbolId());
                    }
                }
            }
        } catch (Exception e) {
            log.error("从数据库加载交易对配置失败", e);
        }
    }

    /**
     * 刷新交易对配置
     * 从数据库重新加载交易对配置，并更新订单簿
     * 注意：此方法需要手动调用，通常在配置变更后调用
     */
    public void refreshTradingPairs() {
        log.info("开始刷新交易对配置");
        
        try {
            // 刷新配置缓存
            configService.refreshConfigCache();
            
            // 获取所有启用的交易对
            List<String> enabledPairs = configService.getEnabledTradingPairs();
            log.info("从数据库加载交易对配置: count={}", enabledPairs.size());

            // 获取当前已存在的订单簿的 symbolId 集合
            java.util.Set<Integer> currentSymbolIds = new java.util.HashSet<>(orderBooks.keySet());

            // 处理启用的交易对
            for (String symbol : enabledPairs) {
                SymbolConfig config = configService.getTradingPairConfig(symbol);
                if (config != null && config.getSymbolId() != null) {
                    Integer symbolId = config.getSymbolId();
                    // 从当前集合中移除，表示这个交易对仍然存在
                    currentSymbolIds.remove(symbolId);
                    
                    // 注册交易对映射
                    SymbolIdMapper.register(symbol, symbolId);
                    
                    // 创建订单簿（如果不存在）
                    if (!orderBooks.containsKey(symbolId)) {
                        createOrderBook(symbolId);
                        log.info("启用交易对: symbol={}, symbolId={}", symbol, symbolId);
                    }
                }
            }

            // 移除已禁用的交易对的订单簿
            for (Integer symbolId : currentSymbolIds) {
                removeOrderBook(symbolId);
                log.info("禁用交易对: symbolId={}", symbolId);
            }
            
            log.info("交易对配置刷新完成: enabled={}, disabled={}", 
                    enabledPairs.size(), currentSymbolIds.size());
        } catch (Exception e) {
            log.error("刷新交易对配置失败", e);
            throw new RuntimeException("刷新交易对配置失败", e);
        }
    }
}
