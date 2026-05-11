package com.campus.forum.filter;

import com.campus.forum.utils.Const;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 跨域过滤器，为响应添加 CORS 头以允许前端跨域访问
 */
@Component
@Order(Const.ORDER_CORS)
public class CorsFilter extends HttpFilter {

    /** 允许的请求来源，* 表示所有来源 */
    @Value("${spring.web.cors.origin}")
    String origin;

    /** 是否允许携带凭证（Cookie 等） */
    @Value("${spring.web.cors.credentials}")
    boolean credentials;

    /** 允许的 HTTP 请求方法，* 表示全部 */
    @Value("${spring.web.cors.methods}")
    String methods;

    /**
     * 对每个请求添加 CORS 响应头，OPTIONS 预检请求直接返回 200
     *
     * @param request  HTTP 请求
     * @param response HTTP 响应
     * @param chain    过滤器链
     * @throws IOException      IO 异常
     * @throws ServletException Servlet 异常
     */
    @Override
    protected void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {
        this.addCorsHeader(request, response);
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            response.setStatus(HttpServletResponse.SC_OK);
            return;
        }
        chain.doFilter(request, response);
    }

    /**
     * 为响应添加所有跨域相关的响应头
     *
     * @param request  HTTP 请求，用于获取 Origin 头
     * @param response HTTP 响应，用于设置 CORS 头
     */
    private void addCorsHeader(HttpServletRequest request, HttpServletResponse response) {
        // 允许哪些来源访问
        response.setHeader("Access-Control-Allow-Origin", this.resolveOrigin(request));
        // 允许哪些 HTTP 方法
        response.setHeader("Access-Control-Allow-Methods", this.resolveMethod());
        // 允许携带哪些请求头
        response.setHeader("Access-Control-Allow-Headers", "Authorization, Content-Type");
        if (credentials) {
            response.setHeader("Access-Control-Allow-Credentials", "true");
        }
    }

    /**
     * 解析允许的请求方法，* 表示全部
     *
     * @return 允许的 HTTP 方法字符串
     */
    private String resolveMethod() {
        return methods.equals("*") ? "GET, HEAD, POST, PUT, DELETE, OPTIONS, TRACE, PATCH" : methods;
    }

    /**
     * 解析允许的来源，* 时直接取请求中的 Origin 头
     *
     * @param request HTTP 请求
     * @return 允许的来源地址
     */
    private String resolveOrigin(HttpServletRequest request) {
        return origin.equals("*") ? request.getHeader("Origin") : origin;
    }
}
