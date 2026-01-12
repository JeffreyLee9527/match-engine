package com.spark.common.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 订单簿更新消息模型（Kafka消息）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderBookUpdateMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 交易对ID
     */
    private Integer symbolId;

    /**
     * 买单深度（价格从高到低）
     */
    private List<PriceQuantity> bids;

    /**
     * 卖单深度（价格从低到高）
     */
    private List<PriceQuantity> asks;

    /**
     * 时间戳
     */
    private Long timestamp;
}
