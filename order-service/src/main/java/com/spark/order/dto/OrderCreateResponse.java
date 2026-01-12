package com.spark.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 订单创建响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreateResponse {
    /**
     * 订单ID
     */
    private Long orderId;

    /**
     * 订单状态
     */
    private String status;

    /**
     * 时间戳
     */
    private Long timestamp;
}
