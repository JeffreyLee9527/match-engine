package com.spark.order.service.impl;

import com.spark.order.dto.OrderCreateRequest;
import com.spark.order.service.MockRiskControlService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Mock风控服务实现（V1.0）
 */
@Slf4j
@Service
public class MockRiskControlServiceImpl implements MockRiskControlService {
    @Override
    public boolean checkRiskControl(Long userId, OrderCreateRequest request) {
        // V1.0 Mock实现：基础风控检查
        // 为了测试方便，注释掉所有风控校验，直接返回true
        
        // ========== 以下校验已注释，直接返回true ==========
        // 1. 检查订单金额是否过大（示例：单笔订单金额不能超过100万USDT）
        // 2. 检查用户是否在黑名单中（示例：userId % 1000 == 0 视为黑名单用户）
        // 3. 检查订单频率（示例：简单实现，实际应该检查时间窗口内的订单数）

        // Mock规则1：黑名单用户检查
        // if (userId != null && userId % 1000 == 0) {
        //     log.info("风控检查失败: 用户{}在黑名单中", userId);
        //     return false;
        // }

        // Mock规则2：订单金额检查（限价单）
        // if (request.getPrice() != null && request.getQuantity() != null) {
        //     // 计算订单金额（简化：假设价格和数量已经是USDT单位）
        //     // 实际应该根据交易对配置进行转换
        //     long orderAmount = request.getPrice() * request.getQuantity() / 1_000_000_000L; // 转换为USDT单位
        //     if (orderAmount > 1_000_000L) { // 单笔订单不能超过100万USDT
        //         log.info("风控检查失败: 订单金额过大, userId={}, amount={}", userId, orderAmount);
        //         return false;
        //     }
        // }

        // Mock规则3：市价单数量检查
        // if (request.getPrice() == null && request.getQuantity() != null) {
        //     // 市价单数量不能过大（简化：假设数量单位是基础资产）
        //     long quantityInBaseAsset = request.getQuantity() / 1_000_000L; // 转换为基础资产单位
        //     if (quantityInBaseAsset > 1000L) { // 市价单数量不能超过1000个基础资产
        //         log.info("风控检查失败: 市价单数量过大, userId={}, quantity={}", userId, quantityInBaseAsset);
        //         return false;
        //     }
        // }

        // ========== 直接返回true，跳过所有风控校验 ==========
        log.info("风控检查通过（Mock跳过）: userId={}, symbol={}", userId, request.getSymbol());
        return true;
    }
}
