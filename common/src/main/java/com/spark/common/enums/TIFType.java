package com.spark.common.enums;

import lombok.Getter;

/**
 * TIF (Time In Force) 订单属性枚举
 */
@Getter
public enum TIFType {
    /**
     * Good Till Cancelled - 订单一直有效直到被取消或完全成交
     */
    GTC(0, "GTC"),

    /**
     * Immediate Or Cancel - 立即成交，未成交部分立即取消
     */
    IOC(1, "IOC"),

    /**
     * Fill Or Kill - 立即全部成交，否则整个订单取消
     */
    FOK(2, "FOK");

    private final int code;
    private final String name;

    TIFType(int code, String name) {
        this.code = code;
        this.name = name;
    }

    /**
     * 根据code获取枚举
     */
    public static TIFType fromCode(int code) {
        for (TIFType type : values()) {
            if (type.code == code) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid TIFType code: " + code);
    }

    /**
     * 根据name获取枚举
     */
    public static TIFType fromName(String name) {
        for (TIFType type : values()) {
            if (type.name.equalsIgnoreCase(name)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid TIFType name: " + name);
    }
}
