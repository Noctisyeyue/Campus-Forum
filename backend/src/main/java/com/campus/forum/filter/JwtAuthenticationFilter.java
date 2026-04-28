package com.campus.forum.filter;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.campus.forum.entity.RestBean;
import com.campus.forum.service.AccountService;
import com.campus.forum.utils.Const;
import com.campus.forum.utils.JwtUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT 认证过滤器，从请求头中提取并校验令牌，将用户信息注入 Spring Security 上下文
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    /** JWT 工具类，负责令牌解析与验证 */
    @Resource
    JwtUtils utils;

    /** 用户账户服务，用于查询账号状态 */
    @Resource
    AccountService accountService;

    /** JSON 序列化器，用于将错误响应转为 JSON 字符串 */
    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * 对每个请求执行 JWT 认证：提取令牌 → 校验有效性 → 检查账号状态 → 注入安全上下文
     *
     * @param request     HTTP 请求
     * @param response    HTTP 响应
     * @param filterChain  过滤器链，用于将请求传递给下一个过滤器
     * @throws ServletException Servlet 异常
     * @throws IOException      IO 异常
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // authorization = "Bearer eyJhbGciOiJIUzI1NiJ9.abc123" 
        String authorization = request.getHeader("Authorization");
        // 取出请求头里的 Token
        DecodedJWT jwt = utils.resolveJwt(authorization);
        if (jwt != null) {
            // 从 JWT 中提取用户 ID
            int userId = utils.toId(jwt);
            var account = accountService.findAccountById(userId);
            if (account == null || "disabled".equals(account.getStatus())) {
                // 账号不存在 或 被禁用 → 返回 403 错误，拒绝请求
                response.setContentType("application/json;charset=utf-8");
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.getWriter().write(mapper.writeValueAsString(
                        RestBean.forbidden("账号已被禁用，请联系管理员")));
                return;
            }
            // 把用户信息注入 Spring Security
            UserDetails user = utils.toUser(jwt);
            // 创建一个 Spring Security 的认证凭证
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(user,// 参数1：用户信息（UserDetails 对象）
                                                            null,// 参数2：密码（已经验证过了，不需要，填 null）
                                                            user.getAuthorities());// 参数3：权限列表（["ROLE_USER"] 或 ["ROLE_ADMIN"]）
            // 给认证凭证附加请求的详细信息   请求的远程 IP 地址和会话 ID                                              
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            // 把认证信息存到 Spring Security 的"全局记录本"里
            SecurityContextHolder  // Spring Security 的全局容器
                .getContext()      // 取出当前的上下文（记录本）
                .setAuthentication(authentication);// 把认证凭证写进去
            // 把用户 ID 存到 request 属性里
            request.setAttribute(Const.ATTR_USER_ID, userId);
        }
        filterChain.doFilter(request, response);
    }
}
