package com.spark.match.matcher;

import com.spark.common.util.SnowflakeIdGenerator;
import com.spark.match.orderbook.Order;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 成交生成器
 */
@Slf4j
@Component
public class TradeGenerator {
    private final SnowflakeIdGenerator idGenerator = SnowflakeIdGenerator.getDefault();

    /**
     * 生成成交记录
     *
     * @param maker    Maker订单
     * @param taker    Taker订单
     * @param price    成交价格
     * @param quantity 成交数量
     * @return 成交记录
     */
    public Trade generateTrade(Order maker, Order taker, Long price, Long quantity) {
        Long tradeId = idGenerator.nextId();

        Trade trade = Trade.builder()
                .tradeId(tradeId)
                .symbolId(maker.getSymbolId())
                .makerOrderId(maker.getOrderId())
                .takerOrderId(taker.getOrderId())
                .makerUserId(maker.getUserId())
                .takerUserId(taker.getUserId())
                .price(price)
                .quantity(quantity)
                .tradeTime(System.currentTimeMillis())
                .build();

        log.info("生成成交记录: tradeId={}, makerOrderId={}, takerOrderId={}, price={}, quantity={}",
                tradeId, maker.getOrderId(), taker.getOrderId(), price, quantity);

        return trade;
    }
}
