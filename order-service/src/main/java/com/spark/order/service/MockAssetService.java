package com.spark.order.service;

import com.spark.common.enums.OrderSide;
import com.spark.order.dto.OrderCreateRequest;

/**
 * Mock资产服务（V1.0）
 */
public interface MockAssetService {
    /**
     * 冻结资金
     *
     * @param userId  用户ID
     * @param request 订单创建请求
     * @return 冻结记录ID（Mock）
     */
    String freezeAsset(Long userId, OrderCreateRequest request);

    /**
     * 解冻资金
     *
     * @param userId     用户ID
     * @param freezeId   冻结记录ID
     * @param orderSide  订单方向
     * @param symbolId   交易对ID
     * @param quantity   数量
     * @param filledQty  已成交数量
     */
    void unfreezeAsset(Long userId, String freezeId, OrderSide orderSide, Integer symbolId, Long quantity, Long filledQty);

    /**
     * 根据订单信息解冻资金（用于取消订单，简化实现）
     *
     * @param userId     用户ID
     * @param orderSide  订单方向
     * @param symbolId   交易对ID
     * @param quantity   数量
     * @param filledQty  已成交数量
     * @param price      价格（限价单）
     */
    void unfreezeAssetByOrder(Long userId, OrderSide orderSide, Integer symbolId, Long quantity, Long filledQty, Long price);
}
