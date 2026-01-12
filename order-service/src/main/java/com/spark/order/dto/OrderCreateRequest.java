package com.spark.order.dto;

import com.spark.common.enums.OrderSide;
import com.spark.common.enums.OrderType;
import com.spark.common.enums.TIFType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

/**
 * 订单创建请求
 */
@Data
public class OrderCreateRequest {
    /**
     * 交易对（如 "BTC/USDT"）
     */
    @NotNull(message = "交易对不能为空")
    private String symbol;

    /**
     * 订单类型
     */
    @NotNull(message = "订单类型不能为空")
    private OrderType orderType;

    /**
     * 订单方向
     */
    @NotNull(message = "订单方向不能为空")
    private OrderSide orderSide;

    /**
     * 价格（最小单位，限价单必填）
     */
    private Long price;

    /**
     * 数量（最小单位）
     */
    @NotNull(message = "数量不能为空")
    @Positive(message = "数量必须大于0")
    private Long quantity;

    /**
     * TIF类型
     */
    @NotNull(message = "TIF类型不能为空")
    private TIFType tifType;
}
