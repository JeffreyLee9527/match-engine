package com.spark.match.matcher;

import com.spark.match.orderbook.Order;
import com.spark.match.orderbook.OrderBook;

import java.util.List;

/**
 * 撮合器接口
 */
public interface Matcher {
    /**
     * 执行撮合
     *
     * @param orderBook 订单簿
     * @param order     订单
     * @return 成交列表
     */
    List<Trade> match(OrderBook orderBook, Order order);
}
