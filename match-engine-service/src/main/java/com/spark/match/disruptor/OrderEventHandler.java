package com.spark.match.disruptor;

import com.lmax.disruptor.EventHandler;
import com.spark.common.enums.MessageType;
import com.spark.match.matcher.Matcher;
import com.spark.match.matcher.MatcherFactory;
import com.spark.match.matcher.Trade;
import com.spark.match.orderbook.Order;
import com.spark.match.orderbook.OrderBook;
import com.spark.match.orderbook.OrderBookManager;
import com.spark.match.producer.OrderBookUpdateProducer;
import com.spark.match.producer.TradeNotificationProducer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 单线程事件处理器
 * 处理流程：
 * 1. 订单消息格式校验
 * 2. 获取订单簿
 * 3. 执行撮合
 * 4. 发送通知
 */
@Slf4j
@Component
public class OrderEventHandler implements EventHandler<OrderEvent> {
    @Autowired
    private OrderBookManager orderBookManager;
    @Autowired
    private MatcherFactory matcherFactory;
    @Autowired
    private TradeNotificationProducer tradeNotificationProducer;
    @Autowired
    private OrderBookUpdateProducer orderBookUpdateProducer;

    @Value("${match-engine.orderbook.update-depth:5}")
    private int orderBookUpdateDepth;

    @Override
    public void onEvent(OrderEvent event, long sequence, boolean endOfBatch) {
        long startTime = System.currentTimeMillis();
        try {
            // 1. 订单消息格式校验（撮合引擎职责）
            validateOrderMessage(event);

            // 2. 获取订单簿
            OrderBook orderBook = orderBookManager.getOrderBook(event.getSymbolId());
            if (orderBook == null) {
                throw new IllegalStateException("订单簿不存在: symbolId=" + event.getSymbolId() + 
                        "，请检查数据库配置是否正确");
            }

            // 3. 执行撮合
            boolean orderBookChanged = false;
            byte messageType = event.getMessageType();
            if (messageType == MessageType.ORDER_CANCEL.getCode()) {
                // 取消订单
                long cancelStartTime = System.currentTimeMillis();
                Order order = orderBook.getOrder(event.getOrderId());
                if (order != null) {
                    orderBook.removeOrder(event.getOrderId());
                    orderBookChanged = true;
                    long cancelDuration = System.currentTimeMillis() - cancelStartTime;
                    log.info("[撮合链路耗时] 订单取消耗时: {}ms, orderId={}, symbolId={}", 
                            cancelDuration, event.getOrderId(), event.getSymbolId());
                }
            } else if (messageType == MessageType.ORDER_CREATE.getCode()) {
                // 创建订单并撮合
                Order order = convertToOrder(event);
                Matcher matcher = matcherFactory.getMatcher(order.getOrderType());
                long matchStartTime = System.currentTimeMillis();
                List<Trade> trades = matcher.match(orderBook, order);
                long matchDuration = System.currentTimeMillis() - matchStartTime;
                log.info("[撮合链路耗时] 撮合引擎耗时: {}ms, orderId={}, symbolId={}, tradeCount={}", 
                        matchDuration, event.getOrderId(), event.getSymbolId(), trades.size());

                // 4. 发送成交通知
                for (Trade trade : trades) {
                    tradeNotificationProducer.sendTradeNotification(trade);
                }

                // 判断订单簿是否改变
                // 订单簿改变的情况：
                // 1. 有成交（trades不为空）- 对手单被成交
                // 2. 订单被添加到订单簿（GTC未成交）
                // 订单簿不改变的情况：
                // 1. FOK订单被拒绝（trades为空，订单未添加）
                // 2. IOC订单完全未成交（trades为空，订单未添加）
                // 3. 市价单完全未成交（trades为空，订单未添加）
                if (!trades.isEmpty()) {
                    // 有成交，订单簿一定改变（对手单被成交）
                    orderBookChanged = true;
                } else {
                    // 没有成交，检查订单是否被添加到订单簿
                    // 如果订单在订单簿中，说明订单簿改变了（订单被添加）
                    // 如果订单不在订单簿中，说明订单簿未改变（订单被拒绝或未添加）
                    orderBookChanged = orderBook.getOrder(order.getOrderId()) != null;
                }

                log.info("订单撮合完成: orderId={}, symbolId={}, trades={}, orderBookChanged={}", event.getOrderId(), event.getSymbolId(), trades.size(), orderBookChanged);
            }

            // 5. 发送订单簿更新消息（如果订单簿发生变更）
            if (orderBookChanged) {
                long updateStartTime = System.currentTimeMillis();
                orderBookUpdateProducer.sendOrderBookUpdate(orderBook, orderBookUpdateDepth);
                log.debug("[撮合链路耗时] 订单簿更新消息发送耗时: {}ms, orderId={}", 
                        System.currentTimeMillis() - updateStartTime, event.getOrderId());
            }

            // 6. 【关键】更新订单簿的最后应用的WAL序列号
            // 用于Snapshot创建时确定每个订单簿应用到了哪个WAL序列号
            if (event.getWalSeq() > 0) {
                orderBook.setLastAppliedWalSeq(event.getWalSeq());
            }
            
            long totalDuration = System.currentTimeMillis() - startTime;
            log.info("[撮合链路耗时] Disruptor事件处理总耗时: {}ms, orderId={}, symbolId={}, walSeq={}", 
                    totalDuration, event.getOrderId(), event.getSymbolId(), event.getWalSeq());
        } catch (Exception e) {
            long totalDuration = System.currentTimeMillis() - startTime;
            log.error("[撮合链路耗时] 处理订单事件失败: 总耗时={}ms, event={}", totalDuration, event, e);
            // 不抛出异常，避免影响Disruptor处理
        } finally {
            // 清理事件对象（重用）
            event.clear();
        }
    }

    /**
     * 验证订单消息格式
     */
    private void validateOrderMessage(OrderEvent event) {
        // 撮合引擎职责：只做消息格式和撮合规则校验
        // - 校验订单消息格式（消息完整性、必填字段等）
        // - 校验交易对是否存在（订单簿相关）
        // - 校验价格、数量格式（价格精度、数量精度等撮合规则）

        if (event.getOrderId() <= 0) {
            throw new IllegalArgumentException("订单ID不能为空或无效");
        }
        if (event.getSymbolId() <= 0) {
            throw new IllegalArgumentException("交易对ID不能为空或无效");
        }
        if (event.getMessageType() == MessageType.ORDER_CREATE.getCode()) {
            if (event.getQuantity() == null || event.getQuantity() <= 0) {
                throw new IllegalArgumentException("订单数量必须大于0");
            }
        }
    }

    /**
     * 转换为订单簿订单对象
     */
    private Order convertToOrder(OrderEvent event) {
        return Order.builder()
                .orderId(event.getOrderId())
                .userId(event.getUserId())
                .symbolId(event.getSymbolId())
                .orderType(event.getOrderType())
                .orderSide(event.getOrderSide())
                .price(event.getPrice())
                .quantity(event.getQuantity())
                .filledQuantity(0L)
                .tifType(event.getTifType())
                .createTime(event.getTimestamp())
                .build();
    }
}
