package com.spark.match.orderbook;

import com.spark.common.enums.OrderSide;
import com.spark.common.enums.OrderType;
import com.spark.common.enums.TIFType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 订单簿中的订单对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Order {
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
     * 订单类型 0:限价 1:市价
     */
    private OrderType orderType;

    /**
     * 订单方向 0:买 1:卖
     */
    private OrderSide orderSide;

    /**
     * 价格
     */
    private Long price;

    /**
     * 数量
     */
    private Long quantity;

    /**
     * 已成交数量
     */
    private Long filledQuantity;

    /**
     * TIF类型
     */
    private TIFType tifType;

    /**
     * 创建时间
     */
    private Long createTime;

    /**
     * 获取剩余数量
     */
    public long getRemainingQuantity() {
        return quantity - filledQuantity;
    }

    /**
     * 判断是否完全成交
     */
    public boolean isFilled() {
        return filledQuantity >= quantity;
    }
}
