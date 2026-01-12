package com.spark.order.producer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.spark.common.config.ConfigService;
import com.spark.common.config.RoutingConfig;
import com.spark.common.constant.KafkaTopic;
import com.spark.common.enums.MessageType;
import com.spark.common.model.OrderMessage;
import com.spark.order.model.Order;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Kafka订单消息生产者
 */
@Slf4j
@Component
public class KafkaOrderProducer {
    /**
     * 默认分片号（当数据库中没有配置时使用）
     */
    private static final int DEFAULT_PARTITION = 0;
    /**
     * 默认Topic（当数据库中没有配置时使用）
     */
    private static final String DEFAULT_TOPIC = KafkaTopic.ORDER_INPUT;
    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;
    @Autowired
    private ConfigService configService;
    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 发送订单创建消息
     * 优先从数据库获取topic和分片配置，如果没有配置则使用默认值
     */
    public void sendOrderCreateMessage(Order order) {
        try {
            OrderMessage message = OrderMessage.builder()
                    .messageId(UUID.randomUUID().toString())
                    .messageType(MessageType.ORDER_CREATE)
                    .orderId(order.getOrderId())
                    .userId(order.getUserId())
                    .symbolId(order.getSymbolId())
                    .orderType(order.getOrderType())
                    .orderSide(order.getOrderSide())
                    .price(order.getPrice())
                    .quantity(order.getQuantity())
                    .tifType(order.getTifType())
                    .timestamp(System.currentTimeMillis())
                    .build();

            String messageJson = objectMapper.writeValueAsString(message);
            String key = String.valueOf(order.getSymbolId());

            // 从数据库获取路由配置（topic和partition）：优先从数据库获取，没有则使用默认值
            RoutingConfig routingConfig = configService.getRoutingConfig(order.getSymbolId(), DEFAULT_TOPIC, DEFAULT_PARTITION);
            String topic = routingConfig.getTopic();
            int partition = routingConfig.getPartition();

            // 发送消息到指定的topic和分片
            kafkaTemplate.send(topic, partition, key, messageJson);
            log.info("发送订单创建消息: orderId={}, symbolId={}, topic={}, partition={}", order.getOrderId(), order.getSymbolId(), topic, partition);
        } catch (JsonProcessingException e) {
            log.error("订单消息序列化失败: orderId={}", order.getOrderId(), e);
            throw new RuntimeException("订单消息序列化失败", e);
        }
    }

    /**
     * 发送订单取消消息
     * 优先从数据库获取topic和分片配置，如果没有配置则使用默认值
     */
    public void sendOrderCancelMessage(Long orderId, Long userId, Integer symbolId) {
        try {
            OrderMessage message = OrderMessage.builder()
                    .messageId(UUID.randomUUID().toString())
                    .messageType(MessageType.ORDER_CANCEL)
                    .orderId(orderId)
                    .userId(userId)
                    .symbolId(symbolId)
                    .timestamp(System.currentTimeMillis())
                    .build();

            String messageJson = objectMapper.writeValueAsString(message);
            String key = String.valueOf(symbolId);

            // 从数据库获取路由配置（topic和partition）：优先从数据库获取，没有则使用默认值
            RoutingConfig routingConfig = configService.getRoutingConfig(symbolId, DEFAULT_TOPIC, DEFAULT_PARTITION);
            String topic = routingConfig.getTopic();
            int partition = routingConfig.getPartition();

            // 发送消息到指定的topic和分片
            kafkaTemplate.send(topic, partition, key, messageJson);
            log.info("发送订单取消消息: orderId={}, symbolId={}, topic={}, partition={}", orderId, symbolId, topic, partition);
        } catch (JsonProcessingException e) {
            log.error("订单取消消息序列化失败: orderId={}", orderId, e);
            throw new RuntimeException("订单取消消息序列化失败", e);
        }
    }
}
