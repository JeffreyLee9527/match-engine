package com.spark.order.service;

import com.spark.order.dto.OrderCreateRequest;

/**
 * 订单验证服务
 */
public interface OrderValidationService {
    /**
     * 验证订单创建请求
     *
     * @param request 订单创建请求
     */
    void validateOrderCreateRequest(OrderCreateRequest request);

    /**
     * 验证交易对配置
     *
     * @param symbol 交易对符号
     * @return 交易对ID
     */
    Integer validateTradingPair(String symbol);

    /**
     * 验证价格和数量
     *
     * @param symbolId 交易对ID
     * @param price    价格
     * @param quantity 数量
     * @param orderType 订单类型
     */
    void validatePriceQuantity(Integer symbolId, Long price, Long quantity, com.spark.common.enums.OrderType orderType);
}
