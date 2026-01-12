package com.spark.common.enums;

import lombok.Getter;

/**
 * 订单方向枚举
 */
@Getter
public enum OrderSide {
    /**
     * 买单
     */
    BUY(0, "BUY"),

    /**
     * 卖单
     */
    SELL(1, "SELL");

    private final int code;
    private final String name;

    OrderSide(int code, String name) {
        this.code = code;
        this.name = name;
    }

    /**
     * 根据code获取枚举
     */
    public static OrderSide fromCode(int code) {
        for (OrderSide side : values()) {
            if (side.code == code) {
                return side;
            }
        }
        throw new IllegalArgumentException("Invalid OrderSide code: " + code);
    }

    /**
     * 根据name获取枚举
     */
    public static OrderSide fromName(String name) {
        for (OrderSide side : values()) {
            if (side.name.equalsIgnoreCase(name)) {
                return side;
            }
        }
        throw new IllegalArgumentException("Invalid OrderSide name: " + name);
    }
}
