package com.spark.match.controller;

import com.spark.common.enums.ErrorCode;
import com.spark.common.model.Response;
import com.spark.common.util.SymbolIdMapper;
import com.spark.match.orderbook.OrderBook;
import com.spark.match.orderbook.OrderBookManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 订单簿查询控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/orderbook")
public class OrderBookController {
    @Autowired
    private OrderBookManager orderBookManager;

    /**
     * 查询订单簿深度
     */
    @GetMapping("/{symbol}")
    public Response<OrderBook.OrderBookDepth> getOrderBook(
            @PathVariable String symbol,
            @RequestParam(defaultValue = "10") int depth) {
        log.info("查询订单簿: symbol={}, depth={}", symbol, depth);

        Integer symbolId = SymbolIdMapper.symbolToId(symbol);
        if (symbolId == null) {
            log.warn("交易对未注册: symbol={}", symbol);
            return Response.error(ErrorCode.SYMBOL_NOT_FOUND);
        }

        OrderBook orderBook = orderBookManager.getOrderBook(symbolId);
        if (orderBook == null) {
            log.warn("订单簿不存在: symbolId={}", symbolId);
            return Response.error(ErrorCode.SYMBOL_NOT_FOUND);
        }

        OrderBook.OrderBookDepth orderBookDepth = orderBook.getDepth(depth);
        log.info("订单簿查询成功: symbol={}, bids={}, asks={}",
                 symbol, orderBookDepth.getBids().size(), orderBookDepth.getAsks().size());

        return Response.success(orderBookDepth);
    }
}
