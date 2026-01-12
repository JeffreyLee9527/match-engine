package com.spark.common.exception;

import com.spark.common.enums.ErrorCode;

/**
 * 订单异常
 */
public class OrderException extends BaseException {
    public OrderException(ErrorCode errorCode) {
        super(errorCode);
    }

    public OrderException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public OrderException(ErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
}
