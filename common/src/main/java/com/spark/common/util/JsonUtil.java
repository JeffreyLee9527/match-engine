package com.spark.common.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;

/**
 * JSON工具类
 * 提供统一的JSON序列化和反序列化方法
 * 
 * 使用方式：
 * 1. 在Spring Bean中，优先注入ObjectMapper Bean使用
 * 2. 在非Spring环境中，使用静态方法（内部使用共享的ObjectMapper实例）
 */
@Slf4j
public class JsonUtil {
    /**
     * 共享的ObjectMapper实例（线程安全）
     * 用于非Spring环境或静态方法调用
     */
    private static final ObjectMapper SHARED_MAPPER = new ObjectMapper();

    private JsonUtil() {
        // 工具类，禁止实例化
    }

    /**
     * 将对象序列化为JSON字符串
     *
     * @param obj 要序列化的对象
     * @return JSON字符串
     * @throws RuntimeException 序列化失败时抛出
     */
    public static String toJson(Object obj) {
        try {
            return SHARED_MAPPER.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("JSON序列化失败: {}", obj, e);
            throw new RuntimeException("JSON序列化失败", e);
        }
    }

    /**
     * 将对象序列化为JSON字符串（使用指定的ObjectMapper）
     *
     * @param mapper ObjectMapper实例
     * @param obj    要序列化的对象
     * @return JSON字符串
     * @throws RuntimeException 序列化失败时抛出
     */
    public static String toJson(ObjectMapper mapper, Object obj) {
        try {
            return mapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("JSON序列化失败: {}", obj, e);
            throw new RuntimeException("JSON序列化失败", e);
        }
    }

    /**
     * 将JSON字符串反序列化为对象
     *
     * @param json  JSON字符串
     * @param clazz 目标类型
     * @param <T>   目标类型
     * @return 反序列化后的对象
     * @throws RuntimeException 反序列化失败时抛出
     */
    public static <T> T fromJson(String json, Class<T> clazz) {
        try {
            return SHARED_MAPPER.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            log.error("JSON反序列化失败: json={}, class={}", json, clazz.getName(), e);
            throw new RuntimeException("JSON反序列化失败", e);
        }
    }

    /**
     * 将JSON字符串反序列化为对象（使用指定的ObjectMapper）
     *
     * @param mapper ObjectMapper实例
     * @param json   JSON字符串
     * @param clazz  目标类型
     * @param <T>    目标类型
     * @return 反序列化后的对象
     * @throws RuntimeException 反序列化失败时抛出
     */
    public static <T> T fromJson(ObjectMapper mapper, String json, Class<T> clazz) {
        try {
            return mapper.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            log.error("JSON反序列化失败: json={}, class={}", json, clazz.getName(), e);
            throw new RuntimeException("JSON反序列化失败", e);
        }
    }

    /**
     * 将JSON字符串反序列化为对象（支持泛型）
     *
     * @param json          JSON字符串
     * @param typeReference 类型引用
     * @param <T>           目标类型
     * @return 反序列化后的对象
     * @throws RuntimeException 反序列化失败时抛出
     */
    public static <T> T fromJson(String json, TypeReference<T> typeReference) {
        try {
            return SHARED_MAPPER.readValue(json, typeReference);
        } catch (JsonProcessingException e) {
            log.error("JSON反序列化失败: json={}, type={}", json, typeReference.getType(), e);
            throw new RuntimeException("JSON反序列化失败", e);
        }
    }

    /**
     * 从输入流读取JSON并反序列化为对象
     *
     * @param inputStream 输入流
     * @param clazz       目标类型
     * @param <T>         目标类型
     * @return 反序列化后的对象
     * @throws RuntimeException 反序列化失败时抛出
     */
    public static <T> T fromJson(InputStream inputStream, Class<T> clazz) {
        try {
            return SHARED_MAPPER.readValue(inputStream, clazz);
        } catch (IOException e) {
            log.error("从输入流读取JSON失败: class={}", clazz.getName(), e);
            throw new RuntimeException("从输入流读取JSON失败", e);
        }
    }

    /**
     * 获取共享的ObjectMapper实例
     * 注意：如果需要在Spring环境中使用，建议注入Spring管理的ObjectMapper Bean
     *
     * @return ObjectMapper实例
     */
    public static ObjectMapper getMapper() {
        return SHARED_MAPPER;
    }
}
