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
 * 跨域过滤器，添加 CORS 响应头
 */
@Component
@Order(Const.ORDER_CORS)
public class CorsFilter extends HttpFilter {

    @Value("${spring.web.cors.origin}")
    String origin;          // 允许的来源
    @Value("${spring.web.cors.credentials}")
    boolean credentials;    // 是否允许携带凭证
    @Value("${spring.web.cors.methods}")
    String methods;         // 允许的请求方法

    @Override
    protected void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {
        this.addCorsHeader(request, response);
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            response.setStatus(HttpServletResponse.SC_OK);
            return;
        }
        chain.doFilter(request, response);
    }

    // 添加所有跨域相关响应头
    private void addCorsHeader(HttpServletRequest request, HttpServletResponse response) {
        response.setHeader("Access-Control-Allow-Origin", this.resolveOrigin(request));
        response.setHeader("Access-Control-Allow-Methods", this.resolveMethod());
        response.setHeader("Access-Control-Allow-Headers", "Authorization, Content-Type");
        if (credentials) {
            response.setHeader("Access-Control-Allow-Credentials", "true");
        }
    }

    // 解析允许的请求方法，* 表示全部
    private String resolveMethod() {
        return methods.equals("*") ? "GET, HEAD, POST, PUT, DELETE, OPTIONS, TRACE, PATCH" : methods;
    }

    // 解析允许的来源，* 表示取请求中的 Origin
    private String resolveOrigin(HttpServletRequest request) {
        return origin.equals("*") ? request.getHeader("Origin") : origin;
    }
}
