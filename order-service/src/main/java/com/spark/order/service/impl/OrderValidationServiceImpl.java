package com.spark.order.service.impl;

import com.spark.common.config.ConfigService;
import com.spark.common.model.SymbolConfig;
import com.spark.common.enums.ErrorCode;
import com.spark.common.enums.OrderType;
import com.spark.common.exception.OrderException;
import com.spark.common.util.SymbolIdMapper;
import com.spark.order.dto.OrderCreateRequest;
import com.spark.order.service.OrderValidationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 订单验证服务实现
 */
@Slf4j
@Service
public class OrderValidationServiceImpl implements OrderValidationService {
    @Autowired
    private ConfigService configService;

    @Override
    public void validateOrderCreateRequest(OrderCreateRequest request) {
        if (request == null) {
            throw new OrderException(ErrorCode.INVALID_PARAMETER, "订单创建请求不能为空");
        }

        // 限价单必须指定价格
        if (request.getOrderType() == OrderType.LIMIT && request.getPrice() == null) {
            throw new OrderException(ErrorCode.PRICE_REQUIRED);
        }

        // 市价单不需要价格
        if (request.getOrderType() == OrderType.MARKET && request.getPrice() != null) {
            throw new OrderException(ErrorCode.PRICE_NOT_ALLOWED);
        }
    }

    @Override
    public Integer validateTradingPair(String symbol) {
        if (symbol == null || symbol.trim().isEmpty()) {
            throw new OrderException(ErrorCode.BAD_REQUEST, "交易对不能为空");
        }

        // 从数据库获取交易对配置
        SymbolConfig config = configService.getTradingPairConfig(symbol);
        if (config == null) {
            throw new OrderException(ErrorCode.TRADING_PAIR_NOT_FOUND);
        }

        if (config.getEnabled() == null || config.getEnabled() != 1) {
            throw new OrderException(ErrorCode.TRADING_PAIR_DISABLED);
        }

        // 注册交易对映射
        if (config.getSymbolId() != null) {
            SymbolIdMapper.register(symbol, config.getSymbolId());
            return config.getSymbolId();
        }

        throw new OrderException(ErrorCode.TRADING_PAIR_NOT_FOUND);
    }

    @Override
    public void validatePriceQuantity(Integer symbolId, Long price, Long quantity, OrderType orderType) {
        // 获取交易对配置
        SymbolConfig config = getTradingPairConfigBySymbolId(symbolId);
        if (config == null) {
            throw new OrderException(ErrorCode.TRADING_PAIR_NOT_FOUND);
        }

        // 验证数量
        if (quantity == null || quantity <= 0) {
            throw new OrderException(ErrorCode.INVALID_QUANTITY);
        }

        if (config.getMinQuantity() != null && quantity < config.getMinQuantity()) {
            throw new OrderException(ErrorCode.INVALID_QUANTITY, "数量小于最小下单数量");
        }

        if (config.getMaxQuantity() != null && quantity > config.getMaxQuantity()) {
            throw new OrderException(ErrorCode.INVALID_QUANTITY, "数量大于最大下单数量");
        }

        // 验证价格（限价单）
        if (orderType == OrderType.LIMIT) {
            if (price == null || price <= 0) {
                throw new OrderException(ErrorCode.PRICE_REQUIRED);
            }

            // 验证价格是否满足tick_size
            if (config.getTickSize() != null && price % config.getTickSize() != 0) {
                throw new OrderException(ErrorCode.INVALID_PRICE, "价格不满足最小变动单位");
            }
        }
    }

    /**
     * 根据symbolId获取交易对配置
     */
    private SymbolConfig getTradingPairConfigBySymbolId(Integer symbolId) {
        String symbol = SymbolIdMapper.idToSymbol(symbolId);
        if (symbol != null) {
            return configService.getTradingPairConfig(symbol);
        }
        return null;
    }
}
