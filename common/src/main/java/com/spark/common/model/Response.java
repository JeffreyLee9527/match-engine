package com.spark.common.model;

import com.spark.common.enums.ErrorCode;
import lombok.Data;

/**
 * 统一响应模型
 */
@Data
public class Response<T> {
    /**
     * 响应码
     */
    private int code;

    /**
     * 响应消息
     */
    private String message;

    /**
     * 响应数据
     */
    private T data;

    /**
     * 时间戳
     */
    private long timestamp;

    public Response() {
        this.timestamp = System.currentTimeMillis();
    }

    public Response(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * 成功响应
     */
    public static <T> Response<T> success(T data) {
        return new Response<>(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMessage(), data);
    }

    /**
     * 成功响应（无数据）
     */
    public static <T> Response<T> success() {
        return success(null);
    }

    /**
     * 成功响应（自定义消息）
     */
    public static <T> Response<T> success(String message, T data) {
        return new Response<>(ErrorCode.SUCCESS.getCode(), message, data);
    }

    /**
     * 失败响应
     */
    public static <T> Response<T> error(ErrorCode errorCode) {
        return new Response<>(errorCode.getCode(), errorCode.getMessage(), null);
    }

    /**
     * 失败响应（自定义消息）
     */
    public static <T> Response<T> error(ErrorCode errorCode, String message) {
        return new Response<>(errorCode.getCode(), message, null);
    }

    /**
     * 失败响应（自定义码和消息）
     */
    public static <T> Response<T> error(int code, String message) {
        return new Response<>(code, message, null);
    }

    /**
     * 判断是否成功
     */
    public boolean isSuccess() {
        return this.code == ErrorCode.SUCCESS.getCode();
    }
}
