package com.spark.order.controller;

import com.spark.common.model.Response;
import com.spark.order.dto.OrderCreateRequest;
import com.spark.order.dto.OrderCreateResponse;
import com.spark.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 订单控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/orders")
public class OrderController {
    @Autowired
    private OrderService orderService;

    /**
     * 创建订单
     */
    @PostMapping
    public Response<OrderCreateResponse> createOrder(
            @RequestHeader("userId") Long userId,
            @Valid @RequestBody OrderCreateRequest request) {
        long startTime = System.currentTimeMillis();
        try {
            OrderCreateResponse response = orderService.createOrder(userId, request);
            long duration = System.currentTimeMillis() - startTime;
            log.info("[下单链路耗时] 总耗时: {}ms, orderId={}, userId={}, symbol={}", 
                    duration, response.getOrderId(), userId, request.getSymbol());
            return Response.success("订单创建成功", response);
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("[下单链路耗时] 失败总耗时: {}ms, userId={}, symbol={}", 
                    duration, userId, request.getSymbol(), e);
            throw e;
        }
    }

    /**
     * 取消订单
     */
    @DeleteMapping("/{orderId}")
    public Response<OrderCreateResponse> cancelOrder(
            @RequestHeader("userId") Long userId,
            @PathVariable Long orderId) {
        long startTime = System.currentTimeMillis();
        try {
            OrderCreateResponse response = orderService.cancelOrder(userId, orderId);
            long duration = System.currentTimeMillis() - startTime;
            log.info("[撤单链路耗时] 总耗时: {}ms, orderId={}, userId={}", 
                    duration, orderId, userId);
            return Response.success("订单取消成功", response);
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("[撤单链路耗时] 失败总耗时: {}ms, orderId={}, userId={}", 
                    duration, orderId, userId, e);
            throw e;
        }
    }
}
