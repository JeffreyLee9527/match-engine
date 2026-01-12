package com.spark.order.consumer;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.spark.common.constant.KafkaTopic;
import com.spark.common.enums.OrderStatus;
import com.spark.common.model.TradeMessage;
import com.spark.common.util.SnowflakeIdGenerator;
import com.spark.order.mapper.OrderMapper;
import com.spark.order.mapper.TradeMapper;
import com.spark.order.model.Order;
import com.spark.order.model.Trade;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * 成交通知消费者
 */
@Slf4j
@Component
public class TradeNotificationConsumer {
    private final SnowflakeIdGenerator idGenerator = SnowflakeIdGenerator.getDefault();
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private TradeMapper tradeMapper;
    @Autowired
    private ObjectMapper objectMapper;

    @KafkaListener(topics = KafkaTopic.TRADE_NOTIFICATION, groupId = "order-service-group")
    public void onMessage(String message, Acknowledgment ack) {
        try {
            TradeMessage tradeMessage = objectMapper.readValue(message, TradeMessage.class);
            log.info("收到成交通知: tradeId={}, makerOrderId={}, takerOrderId={}", tradeMessage.getTradeId(), tradeMessage.getMakerOrderId(), tradeMessage.getTakerOrderId());

            // 1. 更新订单状态
            updateOrderStatus(tradeMessage.getMakerOrderId(), tradeMessage.getQuantity());
            updateOrderStatus(tradeMessage.getTakerOrderId(), tradeMessage.getQuantity());

            // 2. 持久化成交记录
            persistTrade(tradeMessage);

            // 3. 手动确认
            ack.acknowledge();
        } catch (JsonProcessingException e) {
            log.error("成交通知反序列化失败: message={}", message, e);
            throw new RuntimeException("成交通知反序列化失败", e);
        } catch (Exception e) {
            log.error("处理成交通知失败: message={}", message, e);
            throw new RuntimeException("处理成交通知失败", e);
        }
    }

    private void updateOrderStatus(Long orderId, Long tradeQuantity) {
        Order order = orderMapper.selectOne(
                new LambdaUpdateWrapper<Order>()
                        .eq(Order::getOrderId, orderId)
        );

        if (order == null) {
            log.info("订单不存在: orderId={}", orderId);
            return;
        }

        long newFilledQuantity = order.getFilledQuantity() + tradeQuantity;
        OrderStatus newStatus;

        if (newFilledQuantity >= order.getQuantity()) {
            newStatus = OrderStatus.FILLED;
        } else {
            newStatus = OrderStatus.PARTIAL_FILLED;
        }

        orderMapper.update(null, new LambdaUpdateWrapper<Order>()
                .eq(Order::getOrderId, orderId)
                .set(Order::getFilledQuantity, newFilledQuantity)
                .set(Order::getStatus, newStatus)
                .set(Order::getUpdateTime, System.currentTimeMillis())
        );

        log.info("更新订单状态: orderId={}, filledQuantity={}, status={}", orderId, newFilledQuantity, newStatus);
    }

    private void persistTrade(TradeMessage tradeMessage) {
        Trade trade = new Trade();
        trade.setTradeId(tradeMessage.getTradeId());
        trade.setSymbolId(tradeMessage.getSymbolId());
        trade.setMakerOrderId(tradeMessage.getMakerOrderId());
        trade.setTakerOrderId(tradeMessage.getTakerOrderId());
        trade.setMakerUserId(tradeMessage.getMakerUserId());
        trade.setTakerUserId(tradeMessage.getTakerUserId());
        trade.setPrice(tradeMessage.getPrice());
        trade.setQuantity(tradeMessage.getQuantity());
        trade.setTradeTime(tradeMessage.getTradeTime());
        trade.setCreateTime(System.currentTimeMillis());

        tradeMapper.insert(trade);
        log.info("持久化成交记录: tradeId={}", trade.getTradeId());
    }
}
