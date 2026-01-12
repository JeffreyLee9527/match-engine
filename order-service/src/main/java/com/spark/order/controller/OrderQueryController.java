package com.spark.order.controller;

import com.spark.common.enums.OrderStatus;
import com.spark.common.model.Response;
import com.spark.order.dto.OrderQueryRequest;
import com.spark.order.dto.OrderResponse;
import com.spark.order.dto.PageResult;
import com.spark.order.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 订单查询控制器
 */
@RestController
@RequestMapping("/api/orders")
public class OrderQueryController {
    @Autowired
    private OrderService orderService;

    /**
     * 查询单个订单
     */
    @GetMapping("/{orderId}")
    public Response<OrderResponse> getOrder(@PathVariable Long orderId) {
        OrderResponse response = orderService.getOrderById(orderId);
        return Response.success(response);
    }

    /**
     * 查询订单列表
     */
    @GetMapping
    public Response<PageResult<OrderResponse>> getOrders(
            @RequestHeader(value = "userId", required = false) Long userId,
            @RequestParam(required = false) String symbol,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        OrderQueryRequest request = new OrderQueryRequest();
        request.setUserId(userId);
        request.setSymbol(symbol);
        if (status != null) {
            try {
                request.setStatus(OrderStatus.fromName(status));
            } catch (IllegalArgumentException e) {
                // 忽略无效状态
            }
        }
        request.setPage(page);
        request.setSize(size);

        PageResult<OrderResponse> response = orderService.getOrders(request);
        return Response.success(response);
    }
}
