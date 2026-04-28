package com.campus.forum.entity;

import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONWriter;
import org.slf4j.MDC;

import java.util.Optional;

/**
 * 统一 REST 响应封装，所有接口返回给前端的数据都用这个格式
 *
 * @param id      请求 ID（来自 MDC，用于链路追踪）
 * @param code    状态码（200=成功，401=未登录，403=无权限）
 * @param data    响应数据（成功时携带，失败时为 null）
 * @param message 响应消息（成功/失败的描述文字）
 */
public record RestBean<T>(long id, int code, T data, String message) {

    /**
     * 创建成功响应（携带数据）
     *
     * @param data 响应数据
     * @return RestBean(code=200, data=data, message="请求成功")
     */
    public static <T> RestBean<T> success(T data) {
        return new RestBean<>(requestId(), 200, data, "请求成功");
    }

    /**
     * 创建成功响应（无数据）
     *
     * @return RestBean(code=200, data=null, message="请求成功")
     */
    public static <T> RestBean<T> success() {
        return success(null);
    }

    /**
     * 创建 403 禁止访问响应（已登录但角色不够）
     *
     * @param message 错误描述
     * @return RestBean(code=403, data=null)
     */
    public static <T> RestBean<T> forbidden(String message) {
        return failure(403, message);
    }

    /**
     * 创建 401 未认证响应（未登录或登录失败）
     *
     * @param message 错误描述
     * @return RestBean(code=401, data=null)
     */
    public static <T> RestBean<T> unauthorized(String message) {
        return failure(401, message);
    }

    /**
     * 创建自定义失败响应
     *
     * @param code    状态码
     * @param message 错误描述
     * @return RestBean(code=code, data=null, message=message)
     */
    public static <T> RestBean<T> failure(int code, String message) {
        return new RestBean<>(requestId(), code, null, message);
    }

    /**
     * 将当前对象转换为 JSON 字符串（用于 writer.write() 直接写回前端）
     *
     * @return JSON 格式的字符串
     */
    public String asJsonString() {
        return JSONObject.toJSONString(this, JSONWriter.Feature.WriteNulls);
    }

    /**
     * 从 MDC 获取当前请求 ID，用于日志链路追踪
     *
     * @return 请求 ID，无则返回 0
     */
    private static long requestId() {
        String requestId = Optional.ofNullable(MDC.get("reqId")).orElse("0");
        return Long.parseLong(requestId);
    }
}
