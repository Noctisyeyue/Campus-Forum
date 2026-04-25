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
 * JWT 认证过滤器，校验请求头中的令牌并设置 SecurityContext
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Resource
    JwtUtils utils;

    @Resource
    AccountService accountService;

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authorization = request.getHeader("Authorization");
        DecodedJWT jwt = utils.resolveJwt(authorization);
        if (jwt != null) {
            int userId = utils.toId(jwt);
            // 检查用户是否已被禁用
            var account = accountService.findAccountById(userId);
            if (account == null || "disabled".equals(account.getStatus())) {
                response.setContentType("application/json;charset=utf-8");
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.getWriter().write(mapper.writeValueAsString(
                        RestBean.forbidden("账号已被禁用，请联系管理员")));
                return;
            }
            UserDetails user = utils.toUser(jwt);
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
            request.setAttribute(Const.ATTR_USER_ID, userId);
        }
        filterChain.doFilter(request, response);
    }
}
