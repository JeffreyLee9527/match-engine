package com.spark.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.spark.common.enums.ErrorCode;
import com.spark.common.enums.OrderStatus;
import com.spark.common.exception.OrderException;
import com.spark.common.util.SnowflakeIdGenerator;
import com.spark.common.util.SymbolIdMapper;
import com.spark.order.dto.*;
import com.spark.order.mapper.OrderMapper;
import com.spark.order.model.Order;
import com.spark.order.producer.KafkaOrderProducer;
import com.spark.order.service.MockAssetService;
import com.spark.order.service.MockRiskControlService;
import com.spark.order.service.MockUserService;
import com.spark.order.service.OrderService;
import com.spark.order.service.OrderValidationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 订单服务实现
 */
@Slf4j
@Service
public class OrderServiceImpl implements OrderService {
    private final SnowflakeIdGenerator idGenerator = SnowflakeIdGenerator.getDefault();
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private OrderValidationService orderValidationService;
    @Autowired
    private MockUserService mockUserService;
    @Autowired
    private MockRiskControlService mockRiskControlService;
    @Autowired
    private MockAssetService mockAssetService;
    @Autowired
    private KafkaOrderProducer kafkaOrderProducer;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderCreateResponse createOrder(Long userId, OrderCreateRequest request) {
        long startTime = System.currentTimeMillis();
        long stepStartTime;
        
        // 1. 参数校验
        stepStartTime = System.currentTimeMillis();
        orderValidationService.validateOrderCreateRequest(request);
        log.debug("[下单链路耗时] 参数校验耗时: {}ms", System.currentTimeMillis() - stepStartTime);

        // 2. 验证交易对
        stepStartTime = System.currentTimeMillis();
        Integer symbolId = orderValidationService.validateTradingPair(request.getSymbol());
        log.debug("[下单链路耗时] 交易对验证耗时: {}ms", System.currentTimeMillis() - stepStartTime);

        // 3. 验证价格和数量
        stepStartTime = System.currentTimeMillis();
        orderValidationService.validatePriceQuantity(
                symbolId,
                request.getPrice(),
                request.getQuantity(),
                request.getOrderType()
        );
        log.debug("[下单链路耗时] 价格数量验证耗时: {}ms", System.currentTimeMillis() - stepStartTime);

        // 4. 业务规则校验（用户状态）
        stepStartTime = System.currentTimeMillis();
        if (!mockUserService.checkUserStatus(userId)) {
            throw new OrderException(ErrorCode.USER_STATUS_ERROR);
        }
        log.debug("[下单链路耗时] 用户状态校验耗时: {}ms", System.currentTimeMillis() - stepStartTime);

        // 5. 风控校验
        stepStartTime = System.currentTimeMillis();
        if (!mockRiskControlService.checkRiskControl(userId, request)) {
            throw new OrderException(ErrorCode.RISK_CHECK_FAILED, "风控检查失败");
        }
        log.debug("[下单链路耗时] 风控校验耗时: {}ms", System.currentTimeMillis() - stepStartTime);

        // 6. 冻结资金
        stepStartTime = System.currentTimeMillis();
        String freezeId = mockAssetService.freezeAsset(userId, request);
        log.debug("[下单链路耗时] 冻结资金耗时: {}ms", System.currentTimeMillis() - stepStartTime);

        // 7. 生成订单ID
        Long orderId = idGenerator.nextId();

        // 8. 创建订单实体
        Order order = new Order();
        order.setOrderId(orderId);
        order.setUserId(userId);
        order.setSymbolId(symbolId);
        order.setOrderType(request.getOrderType());
        order.setOrderSide(request.getOrderSide());
        order.setPrice(request.getPrice());
        order.setQuantity(request.getQuantity());
        order.setFilledQuantity(0L);
        order.setTifType(request.getTifType());
        order.setStatus(OrderStatus.PENDING);
        long now = System.currentTimeMillis();
        order.setCreateTime(now);
        order.setUpdateTime(now);

        try {
            // 9. 持久化到MySQL
            stepStartTime = System.currentTimeMillis();
            orderMapper.insert(order);
            long dbDuration = System.currentTimeMillis() - stepStartTime;
            log.debug("[下单链路耗时] MySQL持久化耗时: {}ms", dbDuration);

            // 10. 发送到Kafka
            stepStartTime = System.currentTimeMillis();
            kafkaOrderProducer.sendOrderCreateMessage(order);
            long kafkaDuration = System.currentTimeMillis() - stepStartTime;
            log.debug("[下单链路耗时] Kafka发送耗时: {}ms", kafkaDuration);

            long totalDuration = System.currentTimeMillis() - startTime;
            log.info("订单创建成功: orderId={}, userId={}, symbol={}, freezeId={}, 总耗时={}ms, DB耗时={}ms, Kafka耗时={}ms", 
                    orderId, userId, request.getSymbol(), freezeId, totalDuration, dbDuration, kafkaDuration);
        } catch (Exception e) {
            // 如果订单创建失败，需要解冻资金
            long totalDuration = System.currentTimeMillis() - startTime;
            log.error("订单创建失败，解冻资金: orderId={}, freezeId={}, 总耗时={}ms", orderId, freezeId, totalDuration, e);
            try {
                mockAssetService.unfreezeAsset(userId, freezeId, request.getOrderSide(), symbolId, request.getQuantity(), 0L);
            } catch (Exception ex) {
                log.error("解冻资金失败: freezeId={}", freezeId, ex);
            }
            throw e;
        }

        return OrderCreateResponse.builder()
                .orderId(orderId)
                .status(order.getStatus().name())
                .timestamp(now)
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderCreateResponse cancelOrder(Long userId, Long orderId) {
        long startTime = System.currentTimeMillis();
        long stepStartTime;
        
        // 1. 查询订单
        stepStartTime = System.currentTimeMillis();
        Order order = orderMapper.selectOne(
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getOrderId, orderId)
        );
        long queryDuration = System.currentTimeMillis() - stepStartTime;
        log.debug("[撤单链路耗时] 查询订单耗时: {}ms", queryDuration);

        if (order == null) {
            throw new OrderException(ErrorCode.ORDER_NOT_FOUND);
        }

        // 2. 校验订单归属
        if (!order.getUserId().equals(userId)) {
            throw new OrderException(ErrorCode.FORBIDDEN, "无权操作此订单");
        }

        // 3. 校验订单状态
        if (order.getStatus() != OrderStatus.PENDING && order.getStatus() != OrderStatus.PARTIAL_FILLED) {
            throw new OrderException(ErrorCode.ORDER_CANNOT_CANCEL);
        }

        // 4. 更新订单状态为CANCELLING
        stepStartTime = System.currentTimeMillis();
        order.setStatus(OrderStatus.CANCELLING);
        order.setUpdateTime(System.currentTimeMillis());
        orderMapper.updateById(order);
        long updateDuration = System.currentTimeMillis() - stepStartTime;
        log.debug("[撤单链路耗时] 更新订单状态耗时: {}ms", updateDuration);

        // 5. 发送取消消息到Kafka
        stepStartTime = System.currentTimeMillis();
        kafkaOrderProducer.sendOrderCancelMessage(orderId, userId, order.getSymbolId());
        long kafkaDuration = System.currentTimeMillis() - stepStartTime;
        log.debug("[撤单链路耗时] Kafka发送耗时: {}ms", kafkaDuration);

        long totalDuration = System.currentTimeMillis() - startTime;
        log.info("订单取消成功: orderId={}, userId={}, 总耗时={}ms, 查询耗时={}ms, 更新耗时={}ms, Kafka耗时={}ms", 
                orderId, userId, totalDuration, queryDuration, updateDuration, kafkaDuration);

        return OrderCreateResponse.builder()
                .orderId(orderId)
                .status(OrderStatus.CANCELLING.name())
                .timestamp(System.currentTimeMillis())
                .build();
    }

    @Override
    public OrderResponse getOrderById(Long orderId) {
        Order order = orderMapper.selectOne(
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getOrderId, orderId)
        );

        if (order == null) {
            throw new OrderException(ErrorCode.ORDER_NOT_FOUND);
        }

        return convertToResponse(order);
    }

    @Override
    public PageResult<OrderResponse> getOrders(OrderQueryRequest request) {
        LambdaQueryWrapper<Order> queryWrapper = new LambdaQueryWrapper<>();

        if (request.getUserId() != null) {
            queryWrapper.eq(Order::getUserId, request.getUserId());
        }
        if (request.getSymbol() != null) {
            Integer symbolId = SymbolIdMapper.symbolToId(request.getSymbol());
            if (symbolId != null) {
                queryWrapper.eq(Order::getSymbolId, symbolId);
            }
        }
        if (request.getStatus() != null) {
            queryWrapper.eq(Order::getStatus, request.getStatus());
        }

        queryWrapper.orderByDesc(Order::getCreateTime);

        Page<Order> page = new Page<>(request.getPage(), request.getSize());
        Page<Order> result = orderMapper.selectPage(page, queryWrapper);

        List<OrderResponse> records = result.getRecords().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());

        return PageResult.<OrderResponse>builder()
                .records(records)
                .total(result.getTotal())
                .page(request.getPage())
                .size(request.getSize())
                .pages((int) result.getPages())
                .build();
    }

    private OrderResponse convertToResponse(Order order) {
        return OrderResponse.builder()
                .orderId(order.getOrderId())
                .userId(order.getUserId())
                .symbol(SymbolIdMapper.idToSymbol(order.getSymbolId()))
                .orderType(order.getOrderType())
                .orderSide(order.getOrderSide())
                .price(order.getPrice())
                .quantity(order.getQuantity())
                .filledQuantity(order.getFilledQuantity())
                .tifType(order.getTifType())
                .status(order.getStatus())
                .createTime(order.getCreateTime())
                .updateTime(order.getUpdateTime())
                .build();
    }
}
