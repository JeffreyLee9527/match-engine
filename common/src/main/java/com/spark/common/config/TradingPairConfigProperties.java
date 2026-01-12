package com.spark.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 交易对配置属性
 * 注意：此配置类已废弃，配置现在从 MySQL 数据库读取
 * 保留此类仅用于向后兼容，实际配置通过 ConfigService 从数据库读取
 * 
 * @deprecated 使用 ConfigService 替代
 */
@Data
@Component
@Deprecated
@ConfigurationProperties(prefix = "trading")
public class TradingPairConfigProperties {
    /**
     * 交易对配置映射
     * Key: 交易对符号（如 "BTC/USDT"）
     * Value: 交易对配置
     * 
     * @deprecated 配置现在从数据库读取，此字段不再使用
     */
    @Deprecated
    private Map<String, com.spark.common.model.SymbolConfig> pairs;
}
