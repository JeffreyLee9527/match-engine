package com.spark.match.orderbook;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.spark.common.enums.OrderSide;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * 订单簿核心类
 * - 买单簿：TreeMap降序（价格从高到低）
 * - 卖单簿：TreeMap升序（价格从低到高）
 * - 订单索引：HashMap（O(1)撤单）
 */
@Slf4j
@Data
public class OrderBook {
    /**
     * 交易对ID
     */
    private final Integer symbolId;

    /**
     * 买单簿：TreeMap<价格, PriceLevel>，降序
     */
    private final TreeMap<Long, PriceLevel> buyBook;

    /**
     * 卖单簿：TreeMap<价格, PriceLevel>，升序
     */
    private final TreeMap<Long, PriceLevel> sellBook;

    /**
     * 订单索引：HashMap<订单ID, Order>，O(1)撤单
     */
    private final Map<Long, Order> orderIndex;

    /**
     * 最后应用的WAL序列号
     * 用于Snapshot恢复时确定从哪个WAL序列号开始重放
     */
    private volatile long lastAppliedWalSeq = 0;

    /**
     * 构造函数（用于正常创建）
     */
    public OrderBook(Integer symbolId) {
        this.symbolId = symbolId;
        // 买单降序：从高到低
        this.buyBook = new TreeMap<>(Collections.reverseOrder());
        // 卖单升序：从低到高
        this.sellBook = new TreeMap<>();
        this.orderIndex = new HashMap<>();
    }

    /**
     * Jackson反序列化构造函数
     * 用于从Snapshot恢复OrderBook
     */
    @JsonCreator
    public OrderBook(
            @JsonProperty("symbolId") Integer symbolId,
            @JsonProperty("buyBook") TreeMap<Long, PriceLevel> buyBook,
            @JsonProperty("sellBook") TreeMap<Long, PriceLevel> sellBook,
            @JsonProperty("orderIndex") Map<Long, Order> orderIndex,
            @JsonProperty("lastAppliedWalSeq") long lastAppliedWalSeq) {
        this.symbolId = symbolId;
        
        // 重新创建buyBook，确保使用降序比较器（从JSON反序列化的TreeMap可能没有正确的比较器）
        this.buyBook = new TreeMap<>(Collections.reverseOrder());
        if (buyBook != null) {
            this.buyBook.putAll(buyBook);
        }
        
        // 重新创建sellBook，确保使用升序比较器
        this.sellBook = new TreeMap<>();
        if (sellBook != null) {
            this.sellBook.putAll(sellBook);
        }
        
        // 如果orderIndex为null，创建新的HashMap
        this.orderIndex = orderIndex != null ? new HashMap<>(orderIndex) : new HashMap<>();
        this.lastAppliedWalSeq = lastAppliedWalSeq;
    }

    /**
     * 添加订单
     */
    public void addOrder(Order order) {
        TreeMap<Long, PriceLevel> book = order.getOrderSide() == OrderSide.BUY ? buyBook : sellBook;

        PriceLevel priceLevel = book.computeIfAbsent(order.getPrice(), PriceLevel::new);
        priceLevel.addOrder(order);
        orderIndex.put(order.getOrderId(), order);

        log.info("订单添加到订单簿: orderId={}, symbolId={}, side={}, price={}, quantity={}", order.getOrderId(), symbolId, order.getOrderSide(), order.getPrice(), order.getQuantity());
    }

    /**
     * 移除订单
     */
    public void removeOrder(Long orderId) {
        Order order = orderIndex.remove(orderId);
        if (order == null) {
            log.info("订单不存在: orderId={}", orderId);
            return;
        }

        TreeMap<Long, PriceLevel> book = order.getOrderSide() == OrderSide.BUY ? buyBook : sellBook;
        PriceLevel priceLevel = book.get(order.getPrice());
        if (priceLevel != null) {
            priceLevel.removeOrder(order);
            if (priceLevel.isEmpty()) {
                book.remove(order.getPrice());
            }
        }

        log.info("订单从订单簿移除: orderId={}, symbolId={}", orderId, symbolId);
    }

    /**
     * 获取订单
     */
    public Order getOrder(Long orderId) {
        return orderIndex.get(orderId);
    }

    /**
     * 获取买单簿
     */
    public TreeMap<Long, PriceLevel> getBuyBook() {
        return buyBook;
    }

    /**
     * 获取卖单簿
     */
    public TreeMap<Long, PriceLevel> getSellBook() {
        return sellBook;
    }

    /**
     * 获取订单簿深度
     *
     * @param depth 深度（档位数）
     * @return 订单簿深度
     */
    public OrderBookDepth getDepth(int depth) {
        List<PriceQuantity> bids = new ArrayList<>();
        List<PriceQuantity> asks = new ArrayList<>();

        // 买单：从高到低取N档
        int bidCount = 0;
        for (Map.Entry<Long, PriceLevel> entry : buyBook.entrySet()) {
            if (bidCount >= depth) {
                break;
            }
            PriceLevel priceLevel = entry.getValue();
            if (!priceLevel.isEmpty()) {
                bids.add(new PriceQuantity(entry.getKey(), priceLevel.getTotalQuantity()));
                bidCount++;
            }
        }

        // 卖单：从低到高取N档
        int askCount = 0;
        for (Map.Entry<Long, PriceLevel> entry : sellBook.entrySet()) {
            if (askCount >= depth) {
                break;
            }
            PriceLevel priceLevel = entry.getValue();
            if (!priceLevel.isEmpty()) {
                asks.add(new PriceQuantity(entry.getKey(), priceLevel.getTotalQuantity()));
                askCount++;
            }
        }

        return new OrderBookDepth(symbolId, bids, asks, System.currentTimeMillis());
    }

    /**
     * 价格数量对
     */
    @Data
    @lombok.AllArgsConstructor
    public static class PriceQuantity {
        private Long price;
        private Long quantity;
    }

    /**
     * 订单簿深度
     */
    @Data
    @lombok.AllArgsConstructor
    public static class OrderBookDepth {
        private Integer symbolId;
        private List<PriceQuantity> bids;
        private List<PriceQuantity> asks;
        private Long timestamp;
    }

    /**
     * 深拷贝OrderBook（用于Snapshot创建）
     * 创建OrderBook的完整副本，避免Snapshot创建时OrderBook被修改
     *
     * @return OrderBook的深拷贝
     */
    public OrderBook deepCopy() {
        OrderBook copy = new OrderBook(this.symbolId);
        copy.lastAppliedWalSeq = this.lastAppliedWalSeq;

        // 深拷贝买单簿
        for (Map.Entry<Long, PriceLevel> entry : this.buyBook.entrySet()) {
            PriceLevel originalLevel = entry.getValue();
            PriceLevel copyLevel = new PriceLevel(originalLevel.getPrice());
            copyLevel.setTotalQuantity(originalLevel.getTotalQuantity());

            // 深拷贝订单队列
            for (Order order : originalLevel.getOrders()) {
                Order orderCopy = Order.builder()
                        .orderId(order.getOrderId())
                        .userId(order.getUserId())
                        .symbolId(order.getSymbolId())
                        .orderType(order.getOrderType())
                        .orderSide(order.getOrderSide())
                        .price(order.getPrice())
                        .quantity(order.getQuantity())
                        .filledQuantity(order.getFilledQuantity())
                        .tifType(order.getTifType())
                        .createTime(order.getCreateTime())
                        .build();
                copyLevel.getOrders().offer(orderCopy);
                copy.orderIndex.put(orderCopy.getOrderId(), orderCopy);
            }

            copy.buyBook.put(entry.getKey(), copyLevel);
        }

        // 深拷贝卖单簿
        for (Map.Entry<Long, PriceLevel> entry : this.sellBook.entrySet()) {
            PriceLevel originalLevel = entry.getValue();
            PriceLevel copyLevel = new PriceLevel(originalLevel.getPrice());
            copyLevel.setTotalQuantity(originalLevel.getTotalQuantity());

            // 深拷贝订单队列
            for (Order order : originalLevel.getOrders()) {
                Order orderCopy = Order.builder()
                        .orderId(order.getOrderId())
                        .userId(order.getUserId())
                        .symbolId(order.getSymbolId())
                        .orderType(order.getOrderType())
                        .orderSide(order.getOrderSide())
                        .price(order.getPrice())
                        .quantity(order.getQuantity())
                        .filledQuantity(order.getFilledQuantity())
                        .tifType(order.getTifType())
                        .createTime(order.getCreateTime())
                        .build();
                copyLevel.getOrders().offer(orderCopy);
                copy.orderIndex.put(orderCopy.getOrderId(), orderCopy);
            }

            copy.sellBook.put(entry.getKey(), copyLevel);
        }

        return copy;
    }
}
