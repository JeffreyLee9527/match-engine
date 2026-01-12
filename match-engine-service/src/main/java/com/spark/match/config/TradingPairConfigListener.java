package com.spark.match.config;

import com.spark.common.config.ConfigService;
import com.spark.common.model.SymbolConfig;
import com.spark.common.util.SymbolIdMapper;
import com.spark.match.orderbook.OrderBookManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 交易对配置初始化器
 * 在启动时从数据库加载交易对配置并创建订单簿
 */
@Slf4j
@Component
public class TradingPairConfigListener implements CommandLineRunner {
    @Autowired
    private ConfigService configService;
    @Autowired
    private OrderBookManager orderBookManager;

    @Override
    public void run(String... args) {
        // 初始化时加载交易对配置
        loadTradingPairsFromDatabase();
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
                    if (orderBookManager.getOrderBook(config.getSymbolId()) == null) {
                        orderBookManager.createOrderBook(config.getSymbolId());
                        log.info("创建订单簿: symbol={}, symbolId={}", symbol, config.getSymbolId());
                    }
                }
            }
        } catch (Exception e) {
            log.error("加载交易对配置失败", e);
        }
    }

}
