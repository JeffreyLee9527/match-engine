package com.spark.match.orderbook;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.LinkedList;
import java.util.Queue;

/**
 * 价格级别
 * 同一价格级别的订单使用FIFO队列管理
 */
@Slf4j
@Data
public class PriceLevel {
    /**
     * 价格
     */
    private final Long price;

    /**
     * 订单队列（FIFO）
     */
    private final Queue<Order> orders;

    /**
     * 总数量
     */
    private long totalQuantity;

    public PriceLevel(Long price) {
        this.price = price;
        this.orders = new LinkedList<>();
        this.totalQuantity = 0;
    }

    /**
     * 添加订单
     */
    public void addOrder(Order order) {
        orders.offer(order);
        totalQuantity += order.getRemainingQuantity();
    }

    /**
     * 移除订单
     */
    public void removeOrder(Order order) {
        if (orders.remove(order)) {
            totalQuantity -= order.getRemainingQuantity();
        } else {
            log.info("订单不在队列中，无法移除: orderId={}, price={}", 
                order.getOrderId(), price);
        }
    }

    /**
     * 获取队列第一个订单
     */
    public Order getFirstOrder() {
        return orders.peek();
    }

    /**
     * 判断是否为空
     */
    public boolean isEmpty() {
        return orders.isEmpty();
    }

    /**
     * 更新订单数量（部分成交后）
     */
    public void updateQuantity(Order order, long oldQuantity, long newQuantity) {
        if (!orders.contains(order)) {
            log.info("订单不在队列中，无法更新数量: orderId={}, price={}", 
                order.getOrderId(), price);
            return;
        }
        totalQuantity = totalQuantity - oldQuantity + newQuantity;
    }
}
