package com.spark.order.dto;

import com.spark.common.enums.OrderSide;
import com.spark.common.enums.OrderStatus;
import com.spark.common.enums.OrderType;
import com.spark.common.enums.TIFType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 订单响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {
    /**
     * 订单ID
     */
    private Long orderId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 交易对
     */
    private String symbol;

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
     * 已成交数量（最小单位）
     */
    private Long filledQuantity;

    /**
     * TIF类型
     */
    private TIFType tifType;

    /**
     * 订单状态
     */
    private OrderStatus status;

    /**
     * 创建时间
     */
    private Long createTime;

    /**
     * 更新时间
     */
    private Long updateTime;
}
