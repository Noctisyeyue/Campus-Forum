package com.campus.forum.filter;

import com.alibaba.fastjson2.JSONObject;
import com.campus.forum.utils.Const;
import com.campus.forum.utils.SnowflakeIdGenerator;
import jakarta.annotation.Resource;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.util.Set;

/**
 * 请求日志过滤器，记录每个请求的 URL、方法、IP、用户身份、参数及响应结果
 */
@Slf4j
@Component
public class RequestLogFilter extends OncePerRequestFilter {

    /** 雪花算法 ID 生成器，为每个请求分配唯一 reqId */
    @Resource
    SnowflakeIdGenerator generator;

    /** 不打印日志的路径前缀集合（静态资源、Swagger 文档等） */
    private final Set<String> ignores = Set.of("/swagger-ui", "/v3/api-docs", "/images");

    /**
     * 过滤器核心方法，忽略的路径直接放行，其余路径记录请求开始和结束日志
     *
     * @param request     HTTP 请求
     * @param response    HTTP 响应
     * @param filterChain 过滤器链
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        // 忽略的路径：直接放行，不记录日志
        if (this.isIgnoreUrl(request.getServletPath())) {
            filterChain.doFilter(request, response);
        } else {
            long startTime = System.currentTimeMillis();    // 记录开始时间
            this.logRequestStart(request);                  // 打印请求开始日志
            ContentCachingResponseWrapper wrapper = new ContentCachingResponseWrapper(response); // 响应包装器  把原来的 response 包装成 wrapper，后续所有代码用 wrapper 代替 response  原始 response 的数据是即时发送的，无法再读取
            filterChain.doFilter(request, wrapper);         // 继续执行后续过滤器和Controller
            this.logRequestEnd(wrapper, startTime);         // 打印请求结束日志
            wrapper.copyBodyToResponse();                   // 把响应内容写回给浏览器
        }
    }

    /**
     * 判断请求路径是否需要忽略日志
     *
     * @param url 请求路径
     * @return true=忽略，false=记录日志
     */
    private boolean isIgnoreUrl(String url) {
        for (String ignore : ignores) {
            if (url.startsWith(ignore)) return true;
        }
        return false;
    }

    /**
     * 请求结束日志：输出处理耗时和响应结果
     *
     * @param wrapper   响应包装器，用于读取响应体内容
     * @param startTime 请求开始时间戳（毫秒）
     */
    public void logRequestEnd(ContentCachingResponseWrapper wrapper, long startTime) {
        long time = System.currentTimeMillis() - startTime;
        int status = wrapper.getStatus();
        String content = status != 200 ?
                status + " 错误" : new String(wrapper.getContentAsByteArray());
        log.info("请求处理耗时: {}ms | 响应结果: {}", time, content);
    }

    /**
     * 请求开始日志：输出 URL、HTTP 方法、远程 IP、用户身份、角色、请求参数
     *
     * @param request HTTP 请求
     */
    public void logRequestStart(HttpServletRequest request) {
        long reqId = generator.nextId();                         // 生成唯一请求ID
        MDC.put("reqId", String.valueOf(reqId));                 // 放进日志上下文（可配合日志格式输出）
        JSONObject object = new JSONObject();
        // 把所有请求参数收集到一个 JSON 对象中
        request.getParameterMap().forEach((k, v) -> object.put(k, v.length > 0 ? v[0] : null));
        Object id = request.getAttribute(Const.ATTR_USER_ID);    // JWT过滤器放入的用户ID
        if (id != null) {
            // 已登录用户：输出用户名、UID、角色
            User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            log.info("请求URL: \"{}\" ({}) | 远程IP地址: {} │ 身份: {} (UID: {}) | 角色: {} | 请求参数列表: {}",
                    request.getServletPath(), request.getMethod(), request.getRemoteAddr(),
                    user.getUsername(), id, user.getAuthorities(), object);
        } else {
            log.info("请求URL: \"{}\" ({}) | 远程IP地址: {} │ 身份: 未验证 | 请求参数列表: {}",
                    request.getServletPath(), request.getMethod(), request.getRemoteAddr(), object);
        }
    }
}
