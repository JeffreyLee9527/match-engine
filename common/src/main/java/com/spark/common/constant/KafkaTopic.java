package com.spark.common.constant;

/**
 * Kafka Topic常量
 */
public class KafkaTopic {
    /**
     * 订单输入Topic
     */
    public static final String ORDER_INPUT = "order-input";

    /**
     * 成交通知Topic
     */
    public static final String TRADE_NOTIFICATION = "trade-notification";

    /**
     * 订单簿更新Topic
     */
    public static final String ORDERBOOK_UPDATE = "orderbook-update";

    private KafkaTopic() {
        // 工具类，禁止实例化
    }
}
