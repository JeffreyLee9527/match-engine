package com.spark.match.producer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.spark.common.constant.KafkaTopic;
import com.spark.common.model.OrderBookUpdateMessage;
import com.spark.common.model.PriceQuantity;
import com.spark.match.orderbook.OrderBook;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 订单簿更新生产者
 * 发送订单簿深度更新到Kafka
 */
@Slf4j
@Component
public class OrderBookUpdateProducer {
    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;
    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 发送订单簿更新消息
     *
     * @param orderBook 订单簿
     * @param depth     深度（档位数）
     */
    public void sendOrderBookUpdate(OrderBook orderBook, int depth) {
        try {
            OrderBook.OrderBookDepth orderBookDepth = orderBook.getDepth(depth);

            // 转换为OrderBookUpdateMessage
            OrderBookUpdateMessage message = OrderBookUpdateMessage.builder()
                    .symbolId(orderBookDepth.getSymbolId())
                    .bids(convertToPriceQuantityList(orderBookDepth.getBids()))
                    .asks(convertToPriceQuantityList(orderBookDepth.getAsks()))
                    .timestamp(orderBookDepth.getTimestamp())
                    .build();

            String messageJson = objectMapper.writeValueAsString(message);
            String key = orderBookDepth.getSymbolId() != null 
                    ? String.valueOf(orderBookDepth.getSymbolId()) 
                    : "unknown";

            kafkaTemplate.send(KafkaTopic.ORDERBOOK_UPDATE, key, messageJson);

            log.info("发送订单簿更新: symbolId={}, bids={}, asks={}",
                    orderBookDepth.getSymbolId(),
                    orderBookDepth.getBids().size(),
                    orderBookDepth.getAsks().size());
        } catch (JsonProcessingException e) {
            log.error("订单簿更新序列化失败: symbolId={}", orderBook.getSymbolId(), e);
            // 不抛出异常，避免影响撮合流程
        }
    }

    /**
     * 转换PriceQuantity列表
     */
    private List<PriceQuantity> convertToPriceQuantityList(List<OrderBook.PriceQuantity> priceQuantities) {
        return priceQuantities.stream()
                .map(pq -> PriceQuantity.builder()
                        .price(pq.getPrice())
                        .quantity(pq.getQuantity())
                        .build())
                .collect(Collectors.toList());
    }
}
