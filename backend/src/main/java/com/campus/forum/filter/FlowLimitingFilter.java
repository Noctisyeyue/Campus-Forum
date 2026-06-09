package com.campus.forum.filter;

import com.campus.forum.entity.RestBean;
import com.campus.forum.utils.Const;
import com.campus.forum.utils.FlowUtils;
import jakarta.annotation.Resource;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.PrintWriter;

/**
 * IP 限流过滤器，基于 Redis 实现，防止同一 IP 高频请求刷接口
 */
@Slf4j
@Component
@Order(Const.ORDER_FLOW_LIMIT)
public class FlowLimitingFilter extends HttpFilter {

    /** Redis 操作模板 */
    @Resource
    StringRedisTemplate template;

    /** 时间周期内允许的最大请求次数 500 */
    @Value("${spring.web.flow.limit}")
    int limit;
    /** 计数时间周期 3（秒） */
    @Value("${spring.web.flow.period}")
    int period;
    /** 超限后的封禁时间 10（秒） */
    @Value("${spring.web.flow.block}")
    int block;

    /** 限流工具 */
    @Resource
    FlowUtils utils;

    /**
     * 过滤逻辑：非 OPTIONS 请求才做限流检查，超限则返回 429
     *
     * @param request  HTTP 请求
     * @param response HTTP 响应
     * @param chain    过滤器链
     */
    @Override
    protected void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {
        String address = request.getRemoteAddr();
        // 不是预检请求 && 超过请求次数限制
        if (!"OPTIONS".equals(request.getMethod()) && !tryCount(address))
            this.writeBlockMessage(response);
        else
        // 放行，交给下一个过滤器
            chain.doFilter(request, response);
    }

    /**
     * 对指定 IP 进行请求计数与封禁检查
     * <p>
     * 先检查是否已被封禁，再对请求计数，超限则写入封禁标记
     *
     * @param address 客户端 IP 地址
     * @return true=允许请求，false=已被封禁或超限
     */
    private boolean tryCount(String address) {
        synchronized (address.intern()) {
            // 检查这个 IP 是否已经被封禁
            if (Boolean.TRUE.equals(template.hasKey(Const.FLOW_LIMIT_BLOCK + address)))
                return false;
            String counterKey = Const.FLOW_LIMIT_COUNTER + address;
            String blockKey = Const.FLOW_LIMIT_BLOCK + address;
            return utils.limitPeriodCheck(counterKey, blockKey, block, limit, period);
        }
    }

    /**
     * 向客户端写入 429 限流响应
     *
     * @param response HTTP 响应
     */
    private void writeBlockMessage(HttpServletResponse response) throws IOException {
        response.setStatus(429);
        response.setContentType("application/json;charset=utf-8");
        PrintWriter writer = response.getWriter();
        writer.write(RestBean.failure(429, "请求频率过快，请稍后再试").asJsonString());
    }
}
