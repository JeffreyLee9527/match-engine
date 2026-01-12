package com.spark.match.producer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.spark.common.constant.KafkaTopic;
import com.spark.common.model.TradeMessage;
import com.spark.match.matcher.Trade;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * 成交通知生产者
 */
@Slf4j
@Component
public class TradeNotificationProducer {
    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;
    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 发送成交通知
     */
    public void sendTradeNotification(Trade trade) {
        try {
            TradeMessage message = TradeMessage.builder()
                    .tradeId(trade.getTradeId())
                    .symbolId(trade.getSymbolId())
                    .makerOrderId(trade.getMakerOrderId())
                    .takerOrderId(trade.getTakerOrderId())
                    .makerUserId(trade.getMakerUserId())
                    .takerUserId(trade.getTakerUserId())
                    .price(trade.getPrice())
                    .quantity(trade.getQuantity())
                    .tradeTime(trade.getTradeTime())
                    .build();

            String messageJson = objectMapper.writeValueAsString(message);
            String key = String.valueOf(trade.getSymbolId());

            kafkaTemplate.send(KafkaTopic.TRADE_NOTIFICATION, key, messageJson);

            log.info("发送成交通知: tradeId={}, symbolId={}", trade.getTradeId(), trade.getSymbolId());
        } catch (JsonProcessingException e) {
            log.error("成交通知序列化失败: tradeId={}", trade.getTradeId(), e);
            throw new RuntimeException("成交通知序列化失败", e);
        }
    }
}
