package com.spark.common.enums;

import lombok.Getter;

/**
 * 消息类型枚举
 * 使用 byte 类型以节省内存和提高性能
 */
@Getter
public enum MessageType {
    /**
     * 创建订单
     */
    ORDER_CREATE((byte) 0, "ORDER_CREATE"),

    /**
     * 取消订单
     */
    ORDER_CANCEL((byte) 1, "ORDER_CANCEL");

    private final byte code;
    private final String name;

    MessageType(byte code, String name) {
        this.code = code;
        this.name = name;
    }

    /**
     * 根据code获取枚举
     */
    public static MessageType fromCode(byte code) {
        for (MessageType type : values()) {
            if (type.code == code) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid MessageType code: " + code);
    }

    /**
     * 根据name获取枚举（用于从JSON反序列化）
     */
    public static MessageType fromName(String name) {
        if (name == null) {
            return null;
        }
        for (MessageType type : values()) {
            if (type.name.equalsIgnoreCase(name)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid MessageType name: " + name);
    }
}
