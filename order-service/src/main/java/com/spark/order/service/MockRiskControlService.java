package com.spark.order.service;

import com.spark.order.dto.OrderCreateRequest;

/**
 * Mock风控服务（V1.0）
 */
public interface MockRiskControlService {
    /**
     * 风控校验
     *
     * @param userId  用户ID
     * @param request 订单创建请求
     * @return 是否通过风控校验
     */
    boolean checkRiskControl(Long userId, OrderCreateRequest request);
}
