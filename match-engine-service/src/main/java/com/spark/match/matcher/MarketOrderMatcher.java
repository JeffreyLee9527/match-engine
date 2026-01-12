package com.spark.match.matcher;

import com.spark.common.enums.OrderSide;
import com.spark.common.enums.TIFType;
import com.spark.match.orderbook.Order;
import com.spark.match.orderbook.OrderBook;
import com.spark.match.orderbook.PriceLevel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 市价单撮合器
 * <p>
 * 注意：市价单不支持TIF（Time In Force）
 * - 市价单没有价格，无法保留在订单簿中，因此不支持任何TIF类型
 */
@Slf4j
@Component
public class MarketOrderMatcher implements Matcher {
    @Autowired
    private TradeGenerator tradeGenerator;

    @Override
    public List<Trade> match(OrderBook orderBook, Order order) {
        long startTime = System.currentTimeMillis();
        
        // 市价单不支持TIF，如果包含TIF则忽略（校验在order服务完成）
        ignoreMarketOrderTIF(order);

        List<Trade> trades = new ArrayList<>();

        long matchStartTime = System.currentTimeMillis();
        if (order.getOrderSide() == OrderSide.BUY) {
            // 市价买单：从卖单簿最低价开始撮合
            matchMarketBuyOrder(orderBook, order, trades);
        } else {
            // 市价卖单：从买单簿最高价开始撮合
            matchMarketSellOrder(orderBook, order, trades);
        }
        long matchDuration = System.currentTimeMillis() - matchStartTime;
        log.debug("[撮合引擎耗时] 市价单核心撮合逻辑耗时: {}ms, orderId={}, tradeCount={}", 
                matchDuration, order.getOrderId(), trades.size());

        // 市价单不支持TIF，撮合后不保留未成交部分
        // 如果部分成交，已成交部分保留，未成交部分直接丢弃
        if (!order.isFilled() && order.getFilledQuantity() > 0) {
            // 部分成交的市价单，已成交部分已处理，未成交部分丢弃
            // 不需要添加到订单簿（市价单没有价格，无法保留）
        }

        long totalDuration = System.currentTimeMillis() - startTime;
        log.info("[撮合引擎耗时] 市价单撮合总耗时: {}ms, orderId={}, symbolId={}, tradeCount={}, 核心撮合耗时={}ms", 
                totalDuration, order.getOrderId(), order.getSymbolId(), trades.size(), matchDuration);
        return trades;
    }

    /**
     * 忽略市价单的TIF类型
     * 市价单没有价格，无法保留在订单簿中，因此不支持任何TIF类型
     * 撮合引擎不做校验抛错，只做忽略处理（校验在order服务完成）
     *
     * @param order 订单
     */
    private void ignoreMarketOrderTIF(Order order) {
        TIFType tifType = order.getTifType();

        // 市价单不支持任何TIF类型，如果包含TIF则忽略
        if (tifType != null) {
            log.info("市价单不支持TIF类型，已忽略: orderId={}, tifType={}", order.getOrderId(), tifType);
            // 清除TIF类型，避免后续处理逻辑受到影响
            order.setTifType(null);
        }
    }

    /**
     * 撮合市价买单
     */
    private void matchMarketBuyOrder(OrderBook orderBook, Order buyOrder, List<Trade> trades) {
        // 从卖单簿最低价开始撮合（没有价格限制）
        for (Map.Entry<Long, PriceLevel> entry : orderBook.getSellBook().entrySet()) {
            Long sellPrice = entry.getKey();
            PriceLevel sellPriceLevel = entry.getValue();

            // 撮合该价格级别的订单
            while (!sellPriceLevel.isEmpty() && !buyOrder.isFilled()) {
                Order sellOrder = sellPriceLevel.getFirstOrder();

                // 计算成交数量
                long tradeQuantity = Math.min(
                        buyOrder.getRemainingQuantity(),
                        sellOrder.getRemainingQuantity()
                );

                // 保存成交前的remainingQuantity（用于更新PriceLevel的totalQuantity）
                long oldSellRemainingQuantity = sellOrder.getRemainingQuantity();

                // 生成成交记录
                Trade trade = tradeGenerator.generateTrade(sellOrder, buyOrder, sellPrice, tradeQuantity);
                trades.add(trade);

                // 更新订单状态
                buyOrder.setFilledQuantity(buyOrder.getFilledQuantity() + tradeQuantity);
                sellOrder.setFilledQuantity(sellOrder.getFilledQuantity() + tradeQuantity);

                // 计算成交后的remainingQuantity
                long newSellRemainingQuantity = sellOrder.getRemainingQuantity();

                // 更新价格级别数量（使用成交前后的remainingQuantity）
                sellPriceLevel.updateQuantity(sellOrder, oldSellRemainingQuantity, newSellRemainingQuantity);

                // 如果卖单完全成交，从订单簿移除
                if (sellOrder.isFilled()) {
                    sellPriceLevel.removeOrder(sellOrder);
                    orderBook.removeOrder(sellOrder.getOrderId());
                }
            }

            // 如果价格级别为空，从订单簿移除
            if (sellPriceLevel.isEmpty()) {
                orderBook.getSellBook().remove(sellPrice);
            }

            // 如果买单完全成交，停止撮合
            if (buyOrder.isFilled()) {
                break;
            }
        }
    }

    /**
     * 撮合市价卖单
     */
    private void matchMarketSellOrder(OrderBook orderBook, Order sellOrder, List<Trade> trades) {
        // 从买单簿最高价开始撮合（没有价格限制）
        for (Map.Entry<Long, PriceLevel> entry : orderBook.getBuyBook().entrySet()) {
            Long buyPrice = entry.getKey();
            PriceLevel buyPriceLevel = entry.getValue();

            // 撮合该价格级别的订单
            while (!buyPriceLevel.isEmpty() && !sellOrder.isFilled()) {
                Order buyOrder = buyPriceLevel.getFirstOrder();

                // 计算成交数量
                long tradeQuantity = Math.min(
                        sellOrder.getRemainingQuantity(),
                        buyOrder.getRemainingQuantity()
                );

                // 保存成交前的remainingQuantity（用于更新PriceLevel的totalQuantity）
                long oldBuyRemainingQuantity = buyOrder.getRemainingQuantity();

                // 生成成交记录
                Trade trade = tradeGenerator.generateTrade(buyOrder, sellOrder, buyPrice, tradeQuantity);
                trades.add(trade);

                // 更新订单状态
                sellOrder.setFilledQuantity(sellOrder.getFilledQuantity() + tradeQuantity);
                buyOrder.setFilledQuantity(buyOrder.getFilledQuantity() + tradeQuantity);

                // 计算成交后的remainingQuantity
                long newBuyRemainingQuantity = buyOrder.getRemainingQuantity();

                // 更新价格级别数量（使用成交前后的remainingQuantity）
                buyPriceLevel.updateQuantity(buyOrder, oldBuyRemainingQuantity, newBuyRemainingQuantity);

                // 如果买单完全成交，从订单簿移除
                if (buyOrder.isFilled()) {
                    buyPriceLevel.removeOrder(buyOrder);
                    orderBook.removeOrder(buyOrder.getOrderId());
                }
            }

            // 如果价格级别为空，从订单簿移除
            if (buyPriceLevel.isEmpty()) {
                orderBook.getBuyBook().remove(buyPrice);
            }

            // 如果卖单完全成交，停止撮合
            if (sellOrder.isFilled()) {
                break;
            }
        }
    }
}
