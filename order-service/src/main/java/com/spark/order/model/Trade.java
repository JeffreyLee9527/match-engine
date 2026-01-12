package com.spark.order.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 成交实体类
 */
@Data
@TableName("trade")
public class Trade {
    /**
     * 主键自增ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 成交ID（雪花算法生成，业务主键）
     */
    private Long tradeId;

    /**
     * 交易对ID
     */
    private Integer symbolId;

    /**
     * Maker订单ID
     */
    private Long makerOrderId;

    /**
     * Taker订单ID
     */
    private Long takerOrderId;

    /**
     * Maker用户ID
     */
    private Long makerUserId;

    /**
     * Taker用户ID
     */
    private Long takerUserId;

    /**
     * 成交价格（最小单位）
     */
    private Long price;

    /**
     * 成交数量（最小单位）
     */
    private Long quantity;

    /**
     * 成交时间（毫秒时间戳）
     */
    private Long tradeTime;

    /**
     * 记录创建时间（毫秒时间戳）
     */
    private Long createTime;
}
