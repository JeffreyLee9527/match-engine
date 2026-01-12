package com.spark.common.enums;

import lombok.Getter;

/**
 * 错误码枚举
 */
@Getter
public enum ErrorCode {
    // 通用错误
    SUCCESS(200, "成功"),
    BAD_REQUEST(400, "参数错误"),
    UNAUTHORIZED(401, "未授权"),
    FORBIDDEN(403, "禁止访问"),
    NOT_FOUND(404, "资源不存在"),
    INTERNAL_SERVER_ERROR(500, "服务器错误"),
    INTERNAL_ERROR(500, "服务器内部错误"),
    INVALID_PARAMETER(400, "参数无效"),

    // 订单创建错误（1001-1099）
    PRICE_REQUIRED(1001, "限价单必须指定价格"),
    PRICE_NOT_ALLOWED(1002, "市价单不能指定价格"),
    INVALID_PRICE(1003, "价格无效"),
    INVALID_QUANTITY(1004, "数量无效"),
    TRADING_PAIR_NOT_FOUND(1005, "交易对不存在"),
    TRADING_PAIR_DISABLED(1006, "交易对已禁用"),
    SYMBOL_NOT_FOUND(1007, "交易对符号不存在"),
    USER_NOT_FOUND(1008, "用户不存在"),
    USER_INACTIVE(1009, "用户状态异常"),
    USER_STATUS_ERROR(1010, "用户状态错误"),
    RISK_CHECK_FAILED(1011, "风控检查失败"),
    TIF_TYPE_REQUIRED(1012, "TIF类型不能为空"),
    INVALID_TIF_TYPE(1013, "TIF类型无效"),

    // 订单取消错误（2001-2099）
    ORDER_NOT_FOUND(2001, "订单不存在"),
    ORDER_NOT_BELONG_TO_USER(2002, "订单不属于当前用户"),
    ORDER_STATUS_NOT_ALLOW_CANCEL(2003, "订单状态不允许取消"),
    ORDER_CANNOT_CANCEL(2004, "订单不能取消"),

    // 状态转换错误（3001-3099）
    INVALID_STATUS_TRANSITION(3001, "不允许的状态转换");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    /**
     * 根据code获取枚举
     */
    public static ErrorCode fromCode(int code) {
        for (ErrorCode errorCode : values()) {
            if (errorCode.code == code) {
                return errorCode;
            }
        }
        return INTERNAL_ERROR;
    }
}
