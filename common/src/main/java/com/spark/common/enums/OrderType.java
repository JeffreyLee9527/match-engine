package com.spark.common.enums;

import lombok.Getter;

/**
 * 订单类型枚举
 */
@Getter
public enum OrderType {
    /**
     * 限价单
     */
    LIMIT(0, "LIMIT"),

    /**
     * 市价单
     */
    MARKET(1, "MARKET");

    private final int code;
    private final String name;

    OrderType(int code, String name) {
        this.code = code;
        this.name = name;
    }

    /**
     * 根据code获取枚举
     */
    public static OrderType fromCode(int code) {
        for (OrderType type : values()) {
            if (type.code == code) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid OrderType code: " + code);
    }

    /**
     * 根据name获取枚举
     */
    public static OrderType fromName(String name) {
        for (OrderType type : values()) {
            if (type.name.equalsIgnoreCase(name)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid OrderType name: " + name);
    }
}
