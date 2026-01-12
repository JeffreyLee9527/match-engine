package com.spark.order.exception;

import com.spark.common.enums.ErrorCode;
import com.spark.common.exception.BaseException;
import com.spark.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(BaseException.class)
    @ResponseStatus(HttpStatus.OK)
    public Response<?> handleBaseException(BaseException e) {
        log.error("业务异常: code={}, message={}", e.getErrorCode().getCode(), e.getMessage(), e);
        return Response.error(e.getErrorCode(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Response<?> handleValidationException(MethodArgumentNotValidException e) {
        FieldError fieldError = e.getBindingResult().getFieldError();
        String message = fieldError != null ? fieldError.getDefaultMessage() : "参数校验失败";
        log.error("参数校验异常: {}", message, e);
        return Response.error(ErrorCode.INVALID_PARAMETER, message);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Response<?> handleException(Exception e) {
        log.error("系统异常", e);
        return Response.error(ErrorCode.INTERNAL_ERROR);
    }
}
