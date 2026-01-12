package com.spark.match.matcher;

import com.spark.common.enums.OrderType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 撮合器工厂
 */
@Component
public class MatcherFactory {
    @Autowired
    private LimitOrderMatcher limitOrderMatcher;
    @Autowired
    private MarketOrderMatcher marketOrderMatcher;

    /**
     * 根据订单类型获取撮合器
     */
    public Matcher getMatcher(OrderType orderType) {
        if (orderType == OrderType.LIMIT) {
            return limitOrderMatcher;
        } else if (orderType == OrderType.MARKET) {
            return marketOrderMatcher;
        } else {
            throw new IllegalArgumentException("不支持的订单类型: " + orderType);
        }
    }
}
