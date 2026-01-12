package com.spark.common.enums;

import lombok.Getter;

/**
 * 订单状态枚举
 */
@Getter
public enum OrderStatus {
    /**
     * 待处理
     */
    PENDING(0, "PENDING"),

    /**
     * 部分成交
     */
    PARTIAL_FILLED(1, "PARTIAL_FILLED"),

    /**
     * 完全成交
     */
    FILLED(2, "FILLED"),

    /**
     * 取消中
     */
    CANCELLING(3, "CANCELLING"),

    /**
     * 已取消
     */
    CANCELLED(4, "CANCELLED"),

    /**
     * 已拒绝
     */
    REJECTED(5, "REJECTED"),

    /**
     * 已过期
     */
    EXPIRED(6, "EXPIRED");

    private final int code;
    private final String name;

    OrderStatus(int code, String name) {
        this.code = code;
        this.name = name;
    }

    /**
     * 根据code获取枚举
     */
    public static OrderStatus fromCode(int code) {
        for (OrderStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        throw new IllegalArgumentException("Invalid OrderStatus code: " + code);
    }

    /**
     * 根据name获取枚举
     */
    public static OrderStatus fromName(String name) {
        for (OrderStatus status : values()) {
            if (status.name.equalsIgnoreCase(name)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Invalid OrderStatus name: " + name);
    }
}
