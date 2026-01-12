package com.spark.order.dto;

import com.spark.common.enums.OrderStatus;
import lombok.Data;

/**
 * 订单查询请求
 */
@Data
public class OrderQueryRequest {
    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 交易对
     */
    private String symbol;

    /**
     * 订单状态
     */
    private OrderStatus status;

    /**
     * 页码（从1开始）
     */
    private Integer page = 1;

    /**
     * 每页大小
     */
    private Integer size = 20;
}
