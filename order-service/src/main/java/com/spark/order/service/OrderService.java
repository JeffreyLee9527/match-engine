package com.spark.order.service;

import com.spark.order.dto.OrderCreateRequest;
import com.spark.order.dto.OrderCreateResponse;
import com.spark.order.dto.OrderQueryRequest;
import com.spark.order.dto.OrderResponse;
import com.spark.order.dto.PageResult;

/**
 * 订单服务接口
 */
public interface OrderService {
    /**
     * 创建订单
     *
     * @param userId  用户ID
     * @param request 订单创建请求
     * @return 订单创建响应
     */
    OrderCreateResponse createOrder(Long userId, OrderCreateRequest request);

    /**
     * 取消订单
     *
     * @param userId  用户ID
     * @param orderId 订单ID
     * @return 订单取消响应
     */
    OrderCreateResponse cancelOrder(Long userId, Long orderId);

    /**
     * 根据订单ID查询订单
     *
     * @param orderId 订单ID
     * @return 订单响应
     */
    OrderResponse getOrderById(Long orderId);

    /**
     * 查询订单列表
     *
     * @param request 查询请求
     * @return 分页结果
     */
    PageResult<OrderResponse> getOrders(OrderQueryRequest request);
}
