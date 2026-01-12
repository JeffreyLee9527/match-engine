package com.spark.match.matcher;

import com.spark.match.orderbook.Order;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * TIF处理器
 * 处理GTC/IOC/FOK订单
 */
@Slf4j
@Component
public class TIFHandler {
    /**
     * 处理TIF订单
     *
     * @param order  订单
     * @param trades 成交列表
     * @return 是否保留订单（未成交部分）
     */
    public boolean handleTIF(Order order, List<Trade> trades) {
        if (order.getTifType() == null) {
            return true; // 默认GTC
        }

        switch (order.getTifType()) {
            case GTC:
                return handleGTC(order, trades);
            case IOC:
                return handleIOC(order, trades);
            case FOK:
                return handleFOK(order, trades);
            default:
                return true;
        }
    }

    /**
     * GTC订单处理：未成交部分保留在订单簿
     */
    private boolean handleGTC(Order order, List<Trade> trades) {
        // GTC订单：未成交部分保留在订单簿
        return !order.isFilled();
    }

    /**
     * IOC订单处理：部分成交后，立即取消剩余部分
     */
    private boolean handleIOC(Order order, List<Trade> trades) {
        // IOC订单：部分成交后，立即取消剩余部分
        if (trades.isEmpty() || order.isFilled()) {
            return false; // 不保留
        }
        log.info("IOC订单部分成交，取消剩余部分: orderId={}, filledQuantity={}, quantity={}", order.getOrderId(), order.getFilledQuantity(), order.getQuantity());
        return false; // 不保留剩余部分
    }

    /**
     * FOK订单处理：如果不能完全成交，整个订单拒绝
     */
    private boolean handleFOK(Order order, List<Trade> trades) {
        // FOK订单：如果不能完全成交，整个订单拒绝
        if (!order.isFilled()) {
            log.info("FOK订单未完全成交，拒绝订单: orderId={}, filledQuantity={}, quantity={}",
                    order.getOrderId(), order.getFilledQuantity(), order.getQuantity());
            return false; // 不保留，拒绝订单
        }
        return false; // 完全成交，不保留
    }
}
