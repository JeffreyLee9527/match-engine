package com.spark.match.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lmax.disruptor.RingBuffer;
import com.spark.common.constant.KafkaTopic;
import com.spark.common.enums.MessageType;
import com.spark.common.model.OrderMessage;
import com.spark.match.disruptor.OrderEvent;
import com.spark.match.wal.WALWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

/**
 * 订单消息消费者
 * 关键流程：
 * 1. 消费Kafka消息
 * 2. 追加写WAL（同步fsync）
 * 3. 提交Kafka offset（WAL成功后立即提交）
 * 4. 发布到Disruptor（使用BlockingWaitStrategy阻塞等待，保证不丢消息）
 * <p>
 * 设计原则：
 * - WAL是唯一事实源
 * - Kafka offset是WAL序列号的外部进度映射
 * - Disruptor是执行队列，不承担可靠性职责
 */
@Slf4j
@Component
public class OrderConsumer {
    @Autowired
    private WALWriter walWriter;
    @Autowired
    private RingBuffer<OrderEvent> ringBuffer;
    @Autowired
    private ObjectMapper objectMapper;

    @KafkaListener(topics = KafkaTopic.ORDER_INPUT, groupId = "match-engine-group", concurrency = "1") // 单线程消费，保证顺序
    public void onMessage(String message, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic, @Header(KafkaHeaders.RECEIVED_PARTITION) int partition, @Header(KafkaHeaders.OFFSET) long offset, Acknowledgment ack) {
        long startTime = System.currentTimeMillis();
        Long walSeq = null;
        long parseDuration = 0;
        long walDuration = 0;
        long disruptorDuration = 0;
        Long orderId = null;
        try {
            log.info("收到订单消息: topic={}, partition={}, offset={}", topic, partition, offset);

            // 1. 解析消息
            long parseStartTime = System.currentTimeMillis();
            OrderMessage orderMessage = objectMapper.readValue(message, OrderMessage.class);
            parseDuration = System.currentTimeMillis() - parseStartTime;
            orderId = orderMessage.getOrderId();
            log.debug("[撮合链路耗时] 消息解析耗时: {}ms, orderId={}", parseDuration, orderId);

            // 2. 【关键】追加写WAL（同步fsync）
            long walStartTime = System.currentTimeMillis();
            walSeq = walWriter.append(orderMessage);
            walDuration = System.currentTimeMillis() - walStartTime;
            log.info("[撮合链路耗时] WAL写入耗时: {}ms, walSeq={}, orderId={}", walDuration, walSeq, orderId);

            // 3. 【关键】提交Kafka offset（WAL成功后立即提交）
            ack.acknowledge();
            log.info("Kafka offset提交成功: topic={}, partition={}, offset={}, walSeq={}", topic, partition, offset, walSeq);

            // 4. 发布到 Disruptor（使用BlockingWaitStrategy阻塞等待，保证不丢消息）
            // 注意：此时Kafka offset已提交，即使RingBuffer满阻塞也不会丢数据（WAL已写入）
            long disruptorStartTime = System.currentTimeMillis();
            long sequence = ringBuffer.next(); // BlockingWaitStrategy会阻塞等待，直到有空间
            try {
                OrderEvent event = ringBuffer.get(sequence);
                // 设置原始类型字段（使用 0 作为无效值）
                event.setWalSeq(walSeq != null ? walSeq : 0);
                // 将枚举类型的 messageType 转换为 byte
                MessageType msgType = orderMessage.getMessageType();
                event.setMessageType(msgType != null ? msgType.getCode() : (byte) 0);
                event.setOrderId(orderMessage.getOrderId() != null ? orderMessage.getOrderId() : 0);
                event.setSymbolId(orderMessage.getSymbolId() != null ? orderMessage.getSymbolId() : 0);
                event.setUserId(orderMessage.getUserId() != null ? orderMessage.getUserId() : 0);
                event.setOrderType(orderMessage.getOrderType());
                event.setOrderSide(orderMessage.getOrderSide());
                // 保留包装类型字段（可能为 null）
                event.setPrice(orderMessage.getPrice());
                event.setQuantity(orderMessage.getQuantity());
                event.setTifType(orderMessage.getTifType());
                event.setTimestamp(orderMessage.getTimestamp() != null ? orderMessage.getTimestamp() : 0);
            } finally {
                ringBuffer.publish(sequence);
            }
            disruptorDuration = System.currentTimeMillis() - disruptorStartTime;
            log.debug("[撮合链路耗时] Disruptor发布耗时: {}ms, orderId={}", disruptorDuration, orderId);
            
            long totalDuration = System.currentTimeMillis() - startTime;
            log.info("[撮合链路耗时] Kafka消费总耗时: {}ms, orderId={}, walSeq={}, 解析耗时={}ms, WAL耗时={}ms, Disruptor耗时={}ms", 
                    totalDuration, orderId, walSeq, parseDuration, walDuration, disruptorDuration);

        } catch (JsonProcessingException e) {
            log.error("订单消息反序列化失败: topic={}, partition={}, offset={}, message={}", topic, partition, offset, message, e);
            // 反序列化失败，WAL未写入，可以安全重试
            throw new RuntimeException("订单消息反序列化失败", e);
        } catch (Exception e) {
            long totalDuration = System.currentTimeMillis() - startTime;
            log.error("[撮合链路耗时] 处理订单消息失败: 总耗时={}ms, topic={}, partition={}, offset={}, orderId={}, 解析耗时={}ms, WAL耗时={}ms, Disruptor耗时={}ms", 
                    totalDuration, topic, partition, offset, orderId, parseDuration, walDuration, disruptorDuration, e);
            // 如果WAL已写入，不应该抛出异常导致重试（会导致重复处理）
            // 应该记录错误并继续，后续通过幂等性检查处理重复
            if (walSeq != null) {
                log.error("WAL已写入但处理失败，可能导致数据不一致: walSeq={}, topic={}, partition={}, offset={}", walSeq, topic, partition, offset);
                // WAL已写入，offset已提交，消息已进入Disruptor，不应该重试
                // 后续处理会通过幂等性检查避免重复处理
            } else {
                // WAL未写入，可以安全重试
                throw new RuntimeException("处理订单消息失败", e);
            }
        }
    }
}
