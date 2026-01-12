package com.spark.common.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 成交消息模型（Kafka消息）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TradeMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 成交ID
     */
    private Long tradeId;

    /**
     * 交易对ID
     */
    private Integer symbolId;

    /**
     * Maker订单ID
     */
    private Long makerOrderId;

    /**
     * Taker订单ID
     */
    private Long takerOrderId;

    /**
     * Maker用户ID
     */
    private Long makerUserId;

    /**
     * Taker用户ID
     */
    private Long takerUserId;

    /**
     * 成交价格（最小单位）
     */
    private Long price;

    /**
     * 成交数量（最小单位）
     */
    private Long quantity;

    /**
     * 成交时间
     */
    private Long tradeTime;
}
