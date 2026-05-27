package com.campus.forum.utils;

import com.campus.forum.entity.RestBean;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

/**
 * Controller 层通用工具，统一包装 Service 层返回结果
 */
@Component
public class ControllerUtils {

    /**
     * 统一消息处理：Service 返回 null 表示成功，返回字符串表示错误消息
     *
     * @param action 业务操作
     * @return 成功返回 success，失败返回 failure(400, message)
     */
    public <T> RestBean<T> messageHandle(Supplier<String> action) {
        String message = action.get();  // 执行业务逻辑，拿到返回值
        if (message == null)
            return RestBean.success();  // 返回 null = 成功，返回 200
        else
            return RestBean.failure(400, message);  // 返回字符串 = 失败，返回 400 + 错误信息
    }
}
