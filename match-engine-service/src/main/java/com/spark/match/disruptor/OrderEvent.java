package com.spark.match.disruptor;

import com.spark.common.enums.OrderSide;
import com.spark.common.enums.OrderType;
import com.spark.common.enums.TIFType;
import lombok.Data;

/**
 * 订单事件（Disruptor事件对象）
 * 可重用对象，由EventFactory创建
 * 
 * 优化说明：
 * - 移除了冗余的 OrderMessage 字段（所有字段已展开，且无使用场景）
 * - messageType 从 String 改为 byte 类型，节省内存并提高比较性能
 * - 将总是有值的字段改为原始类型（long/int），节省内存并提高性能：
 *   - walSeq: Long → long（0 表示无效）
 *   - orderId: Long → long（0 表示无效）
 *   - userId: Long → long
 *   - symbolId: Integer → int（0 表示无效）
 *   - timestamp: Long → long
 * - 保留可能为 null 的字段为包装类型：
 *   - price: Long（市价单可能没有价格）
 *   - quantity: Long（取消订单时可能没有数量）
 */
@Data
public class OrderEvent {
    /**
     * WAL序列号（0 表示无效）
     */
    private long walSeq;

    /**
     * 消息类型（使用 byte 类型以节省内存和提高性能）
     */
    private byte messageType;

    /**
     * 订单ID（0 表示无效）
     */
    private long orderId;

    /**
     * 用户ID
     */
    private long userId;

    /**
     * 交易对ID（0 表示无效）
     */
    private int symbolId;

    /**
     * 订单类型
     */
    private OrderType orderType;

    /**
     * 订单方向
     */
    private OrderSide orderSide;

    /**
     * 价格
     */
    private Long price;

    /**
     * 数量
     */
    private Long quantity;

    /**
     * TIF类型
     */
    private TIFType tifType;

    /**
     * 时间戳
     */
    private long timestamp;

    /**
     * 清理事件（重用前调用）
     */
    public void clear() {
        this.walSeq = 0; // long 类型使用 0 作为默认值
        this.messageType = 0; // byte 类型使用 0 作为默认值
        this.orderId = 0; // long 类型使用 0 作为默认值
        this.userId = 0; // long 类型使用 0 作为默认值
        this.symbolId = 0; // int 类型使用 0 作为默认值
        this.orderType = null;
        this.orderSide = null;
        this.price = null;
        this.quantity = null;
        this.tifType = null;
        this.timestamp = 0; // long 类型使用 0 作为默认值
    }
}
