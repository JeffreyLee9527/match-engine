package com.spark.order.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.spark.common.enums.OrderSide;
import com.spark.common.enums.OrderStatus;
import com.spark.common.enums.OrderType;
import com.spark.common.enums.TIFType;
import lombok.Data;

/**
 * 订单实体类
 */
@Data
@TableName("`order`")
public class Order {
    /**
     * 主键自增ID（MySQL内部优化）
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 订单ID（雪花算法生成，业务主键）
     */
    private Long orderId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 交易对ID
     */
    private Integer symbolId;

    /**
     * 订单类型
     */
    private OrderType orderType;

    /**
     * 订单方向
     */
    private OrderSide orderSide;

    /**
     * 价格（最小单位）
     */
    private Long price;

    /**
     * 数量（最小单位）
     */
    private Long quantity;

    /**
     * 已成交数量（最小单位）
     */
    private Long filledQuantity;

    /**
     * TIF类型
     */
    private TIFType tifType;

    /**
     * 订单状态
     */
    private OrderStatus status;

    /**
     * 创建时间（毫秒时间戳）
     */
    private Long createTime;

    /**
     * 更新时间（毫秒时间戳）
     */
    private Long updateTime;
}
