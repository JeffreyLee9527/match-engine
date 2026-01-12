package com.spark.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.spark.common.enums.OrderStatus;
import com.spark.order.model.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 订单Mapper
 */
@Mapper
public interface OrderMapper extends BaseMapper<Order> {
    /**
     * 根据用户ID、交易对ID、状态查询订单
     */
    List<Order> selectByUserIdAndSymbol(
            @Param("userId") Long userId,
            @Param("symbolId") Integer symbolId,
            @Param("status") OrderStatus status
    );
}
