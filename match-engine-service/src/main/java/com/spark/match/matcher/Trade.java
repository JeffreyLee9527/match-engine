package com.spark.match.matcher;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 成交对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Trade {
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
     * 成交价格
     */
    private Long price;

    /**
     * 成交数量
     */
    private Long quantity;

    /**
     * 成交时间
     */
    private Long tradeTime;
}
