package com.spark.common.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 交易对配置实体类
 * 对应数据库表 symbol_config
 */
@Data
@TableName("symbol_config")
public class SymbolConfig {
    /**
     * 主键自增ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 交易对ID（int类型，内存中使用）
     */
    private Integer symbolId;

    /**
     * 交易对名称（如BTC/USDT）
     */
    private String symbol;

    /**
     * 基础资产（如BTC）
     */
    private String baseAsset;

    /**
     * 计价资产（如USDT）
     */
    private String quoteAsset;

    /**
     * 最小价格变动单位（以最小单位存储）
     */
    private Long tickSize;

    /**
     * 最小下单数量（以最小单位存储）
     */
    private Long minQuantity;

    /**
     * 最大下单数量（以最小单位存储）
     */
    private Long maxQuantity;

    /**
     * 价格精度（小数点后位数）
     */
    private Integer pricePrecision;

    /**
     * 数量精度（小数点后位数）
     */
    private Integer quantityPrecision;

    /**
     * Maker手续费率（基点，1基点=0.01%，如10表示0.1%）
     */
    private Integer makerFee;

    /**
     * Taker手续费率（基点，1基点=0.01%，如10表示0.1%）
     */
    private Integer takerFee;

    /**
     * Kafka Topic名称，如果不配置则使用默认topic
     */
    private String topic;

    /**
     * Kafka分片/分区号，如果不配置则使用默认分片0
     */
    private Integer partition;

    /**
     * 是否启用：0=禁用，1=启用
     */
    private Integer enabled;

    /**
     * 优先级：0=低，1=中，2=高
     */
    private Integer priority;

    /**
     * 撮合模式：0=专用实例，1=共享实例
     */
    private Integer matchMode;

    /**
     * 创建时间（毫秒时间戳）
     */
    private Long createTime;

    /**
     * 更新时间（毫秒时间戳）
     */
    private Long updateTime;
}
