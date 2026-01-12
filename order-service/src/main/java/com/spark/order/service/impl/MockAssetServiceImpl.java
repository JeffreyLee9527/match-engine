package com.spark.order.service.impl;

import com.spark.common.enums.OrderSide;
import com.spark.order.dto.OrderCreateRequest;
import com.spark.order.service.MockAssetService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Mock资产服务实现（V1.0）
 */
@Slf4j
@Service
public class MockAssetServiceImpl implements MockAssetService {
    // Mock冻结记录存储（实际应该存储在数据库或Redis中）
    private final ConcurrentMap<String, FreezeRecord> freezeRecords = new ConcurrentHashMap<>();

    @Override
    public String freezeAsset(Long userId, OrderCreateRequest request) {
        // V1.0 Mock实现：模拟资金冻结
        // 1. 检查用户资产是否充足（Mock：默认充足）
        // 2. 冻结对应资产（买单冻结USDT，卖单冻结基础资产）
        // 3. 返回冻结记录ID

        String freezeId = UUID.randomUUID().toString();

        // 计算需要冻结的金额
        long freezeAmount = calculateFreezeAmount(request);

        // 创建冻结记录
        FreezeRecord record = new FreezeRecord();
        record.setFreezeId(freezeId);
        record.setUserId(userId);
        record.setOrderSide(request.getOrderSide());
        record.setSymbol(request.getSymbol());
        record.setFreezeAmount(freezeAmount);
        record.setQuantity(request.getQuantity());
        record.setPrice(request.getPrice());
        record.setTimestamp(System.currentTimeMillis());

        freezeRecords.put(freezeId, record);

        log.info("资金冻结成功: userId={}, freezeId={}, orderSide={}, symbol={}, amount={}",
                userId, freezeId, request.getOrderSide(), request.getSymbol(), freezeAmount);

        return freezeId;
    }

    @Override
    public void unfreezeAsset(Long userId, String freezeId, OrderSide orderSide, Integer symbolId, Long quantity, Long filledQty) {
        // V1.0 Mock实现：模拟资金解冻
        // 1. 查找冻结记录
        // 2. 计算需要解冻的金额（未成交部分）
        // 3. 解冻资产

        FreezeRecord record = freezeRecords.get(freezeId);
        if (record == null) {
            log.info("冻结记录不存在: freezeId={}", freezeId);
            return;
        }

        // 计算未成交数量
        long unfilledQty = quantity - filledQty;
        if (unfilledQty <= 0) {
            // 全部成交，无需解冻
            freezeRecords.remove(freezeId);
            log.info("订单全部成交，无需解冻: freezeId={}, quantity={}, filledQty={}", freezeId, quantity, filledQty);
            return;
        }

        // 计算解冻金额（按比例）
        long unfreezeAmount = record.getFreezeAmount() * unfilledQty / quantity;
        freezeRecords.remove(freezeId);

        log.info("资金解冻成功: userId={}, freezeId={}, orderSide={}, unfreezeAmount={}, unfilledQty={}",
                userId, freezeId, orderSide, unfreezeAmount, unfilledQty);
    }

    @Override
    public void unfreezeAssetByOrder(Long userId, OrderSide orderSide, Integer symbolId, Long quantity, Long filledQty, Long price) {

    }

    /**
     * 计算需要冻结的金额
     */
    private long calculateFreezeAmount(OrderCreateRequest request) {
        if (request.getOrderSide() == OrderSide.BUY) {
            // 买单：冻结USDT（计价货币）
            if (request.getOrderType() == com.spark.common.enums.OrderType.LIMIT && request.getPrice() != null) {
                // 限价买单：冻结 price * quantity
                return request.getPrice() * request.getQuantity() / 1_000_000_000L; // 转换为USDT单位
            } else {
                // 市价买单：冻结最大可能金额（简化：使用当前市价的1.1倍）
                // Mock实现：假设市价买单冻结数量 * 50000（假设BTC价格）
                return request.getQuantity() * 50000L / 1_000_000_000L;
            }
        } else {
            // 卖单：冻结基础资产（数量）
            return request.getQuantity();
        }
    }

    /**
     * 冻结记录（Mock）
     */
    @lombok.Data
    private static class FreezeRecord {
        private String freezeId;
        private Long userId;
        private OrderSide orderSide;
        private String symbol;
        private Long freezeAmount;
        private Long quantity;
        private Long price;
        private Long timestamp;
    }
}
