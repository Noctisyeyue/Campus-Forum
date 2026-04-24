package com.campus.forum.entity;

import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONWriter;
import org.slf4j.MDC;

import java.util.Optional;

/**
 * 统一 REST 响应封装
 *
 * @param id      请求 ID（来自 MDC，用于链路追踪）
 * @param code    状态码
 * @param data    响应数据
 * @param message 响应消息
 */
public record RestBean<T>(long id, int code, T data, String message) {

    public static <T> RestBean<T> success(T data) {
        return new RestBean<>(requestId(), 200, data, "请求成功");
    }

    public static <T> RestBean<T> success() {
        return success(null);
    }

    public static <T> RestBean<T> forbidden(String message) {
        return failure(403, message);
    }

    public static <T> RestBean<T> unauthorized(String message) {
        return failure(401, message);
    }

    public static <T> RestBean<T> failure(int code, String message) {
        return new RestBean<>(requestId(), code, null, message);
    }

    /** 将当前实体转换为 JSON 字符串 */
    public String asJsonString() {
        return JSONObject.toJSONString(this, JSONWriter.Feature.WriteNulls);
    }

    // 从 MDC 获取当前请求 ID
    private static long requestId() {
        String requestId = Optional.ofNullable(MDC.get("reqId")).orElse("0");
        return Long.parseLong(requestId);
    }
}
