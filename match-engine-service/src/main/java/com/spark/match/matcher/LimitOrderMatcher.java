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
 * 限价单撮合器
 * 价格优先、时间优先撮合
 */
@Slf4j
@Component
public class LimitOrderMatcher implements Matcher {
    @Autowired
    private TradeGenerator tradeGenerator;
    @Autowired
    private TIFHandler tifHandler;

    @Override
    public List<Trade> match(OrderBook orderBook, Order order) {
        long startTime = System.currentTimeMillis();
        List<Trade> trades = new ArrayList<>();

        // FOK订单：撮合前检查是否能完全成交，如果不能则拒绝订单（不修改订单簿）
        if (order.getTifType() == TIFType.FOK) {
            long fokCheckStartTime = System.currentTimeMillis();
            if (!canFullyFill(orderBook, order)) {
                long fokCheckDuration = System.currentTimeMillis() - fokCheckStartTime;
                log.info("FOK订单无法完全成交，拒绝订单: orderId={}, requiredQuantity={}, availableQuantity={}, FOK检查耗时={}ms", 
                        order.getOrderId(), order.getQuantity(), calculateAvailableQuantity(orderBook, order), fokCheckDuration);
                // FOK订单无法完全成交，直接返回空成交列表，不修改订单簿
                return trades;
            }
            long fokCheckDuration = System.currentTimeMillis() - fokCheckStartTime;
            log.debug("[撮合引擎耗时] FOK检查耗时: {}ms, orderId={}", fokCheckDuration, order.getOrderId());
        }

        long matchStartTime = System.currentTimeMillis();
        if (order.getOrderSide() == OrderSide.BUY) {
            // 买单：从卖单簿最低价开始撮合
            matchBuyOrder(orderBook, order, trades);
        } else {
            // 卖单：从买单簿最高价开始撮合
            matchSellOrder(orderBook, order, trades);
        }
        long matchDuration = System.currentTimeMillis() - matchStartTime;
        log.debug("[撮合引擎耗时] 核心撮合逻辑耗时: {}ms, orderId={}, tradeCount={}", 
                matchDuration, order.getOrderId(), trades.size());

        // TIF处理
        long tifStartTime = System.currentTimeMillis();
        boolean shouldKeep = tifHandler.handleTIF(order, trades);
        long tifDuration = System.currentTimeMillis() - tifStartTime;
        log.debug("[撮合引擎耗时] TIF处理耗时: {}ms, orderId={}", tifDuration, order.getOrderId());
        
        if (!shouldKeep && !order.isFilled()) {
            // 不保留订单（IOC部分成交或FOK未完全成交）
            // 如果订单已在订单簿中（如GTC订单之前已部分成交），需要移除
            if (orderBook.getOrder(order.getOrderId()) != null) {
                orderBook.removeOrder(order.getOrderId());
            }
        } else if (shouldKeep && !order.isFilled()) {
            // 保留未成交部分到订单簿（GTC订单）
            orderBook.addOrder(order);
        }

        long totalDuration = System.currentTimeMillis() - startTime;
        log.info("[撮合引擎耗时] 撮合总耗时: {}ms, orderId={}, symbolId={}, tradeCount={}, 核心撮合耗时={}ms, TIF处理耗时={}ms", 
                totalDuration, order.getOrderId(), order.getSymbolId(), trades.size(), matchDuration, tifDuration);
        return trades;
    }

    /**
     * 撮合买单
     */
    private void matchBuyOrder(OrderBook orderBook, Order buyOrder, List<Trade> trades) {
        // 从卖单簿最低价开始撮合
        for (Map.Entry<Long, PriceLevel> entry : orderBook.getSellBook().entrySet()) {
            Long sellPrice = entry.getKey();
            PriceLevel sellPriceLevel = entry.getValue();

            // 价格匹配：买单价格 >= 卖单价格
            if (buyOrder.getPrice() < sellPrice) {
                break; // 价格不匹配，停止撮合
            }

            // 撮合该价格级别的订单
            while (!sellPriceLevel.isEmpty() && !buyOrder.isFilled()) {
                Order sellOrder = sellPriceLevel.getFirstOrder();

                // 计算成交数量
                long tradeQuantity = Math.min(buyOrder.getRemainingQuantity(), sellOrder.getRemainingQuantity());

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
     * 撮合卖单
     */
    private void matchSellOrder(OrderBook orderBook, Order sellOrder, List<Trade> trades) {
        // 从买单簿最高价开始撮合
        for (Map.Entry<Long, PriceLevel> entry : orderBook.getBuyBook().entrySet()) {
            Long buyPrice = entry.getKey();
            PriceLevel buyPriceLevel = entry.getValue();

            // 价格匹配：卖单价格 <= 买单价格
            if (sellOrder.getPrice() > buyPrice) {
                break; // 价格不匹配，停止撮合
            }

            // 撮合该价格级别的订单
            while (!buyPriceLevel.isEmpty() && !sellOrder.isFilled()) {
                Order buyOrder = buyPriceLevel.getFirstOrder();

                // 计算成交数量
                long tradeQuantity = Math.min(sellOrder.getRemainingQuantity(), buyOrder.getRemainingQuantity());

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

    /**
     * 检查FOK订单是否能完全成交
     *
     * @param orderBook 订单簿
     * @param order     订单
     * @return 是否能完全成交
     */
    private boolean canFullyFill(OrderBook orderBook, Order order) {
        long availableQuantity = calculateAvailableQuantity(orderBook, order);
        return availableQuantity >= order.getQuantity();
    }

    /**
     * 计算可用数量（用于FOK订单检查）
     *
     * @param orderBook 订单簿
     * @param order     订单
     * @return 可用数量
     */
    private long calculateAvailableQuantity(OrderBook orderBook, Order order) {
        long totalQuantity = 0;
        long limitPrice = order.getPrice();

        if (order.getOrderSide() == OrderSide.BUY) {
            // 买单：计算卖单簿中价格<=限价的总数量
            for (Map.Entry<Long, PriceLevel> entry : orderBook.getSellBook().entrySet()) {
                Long sellPrice = entry.getKey();
                if (sellPrice > limitPrice) {
                    break; // 价格超过限价，停止计算
                }
                totalQuantity += entry.getValue().getTotalQuantity();
            }
        } else {
            // 卖单：计算买单簿中价格>=限价的总数量
            for (Map.Entry<Long, PriceLevel> entry : orderBook.getBuyBook().entrySet()) {
                Long buyPrice = entry.getKey();
                if (buyPrice < limitPrice) {
                    break; // 价格低于限价，停止计算
                }
                totalQuantity += entry.getValue().getTotalQuantity();
            }
        }

        return totalQuantity;
    }
}
