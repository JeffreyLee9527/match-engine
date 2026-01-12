package com.spark.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.spark.order.model.Trade;
import org.apache.ibatis.annotations.Mapper;

/**
 * 成交Mapper
 */
@Mapper
public interface TradeMapper extends BaseMapper<Trade> {
}
