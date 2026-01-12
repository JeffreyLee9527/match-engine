package com.spark.common.exception;

import com.spark.common.enums.ErrorCode;

/**
 * 撮合引擎异常
 */
public class MatchEngineException extends BaseException {
    public MatchEngineException(ErrorCode errorCode) {
        super(errorCode);
    }

    public MatchEngineException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public MatchEngineException(ErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
}
