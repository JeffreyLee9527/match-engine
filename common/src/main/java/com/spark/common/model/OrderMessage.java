package com.spark.common.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.spark.common.enums.MessageType;
import com.spark.common.enums.OrderSide;
import com.spark.common.enums.OrderType;
import com.spark.common.enums.TIFType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 订单消息模型（Kafka消息）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 消息ID（唯一标识）
     */
    private String messageId;

    /**
     * 消息类型
     */
    private MessageType messageType;

    /**
     * 订单ID
     */
    private Long orderId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 交易对ID
     */
    private Integer symbolId;

    /**
     * 订单类型
     */
    private OrderType orderType;

    /**
     * 订单方向
     */
    private OrderSide orderSide;

    /**
     * 价格（最小单位）
     */
    private Long price;

    /**
     * 数量（最小单位）
     */
    private Long quantity;

    /**
     * TIF类型
     */
    private TIFType tifType;

    /**
     * 时间戳
     */
    private Long timestamp;
}
