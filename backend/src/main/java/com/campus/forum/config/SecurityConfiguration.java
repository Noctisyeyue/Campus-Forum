package com.campus.forum.config;

import com.campus.forum.entity.RestBean;
import com.campus.forum.entity.dto.Account;
import com.campus.forum.entity.vo.response.AuthorizeVO;
import com.campus.forum.filter.JwtAuthenticationFilter;
import com.campus.forum.filter.RequestLogFilter;
import com.campus.forum.service.AccountService;
import com.campus.forum.utils.Const;
import com.campus.forum.utils.JwtUtils;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;
import java.io.PrintWriter;

/**
 * Spring Security 安全配置，定义路径权限、登录/登出处理、过滤器链
 */
@Configuration
public class SecurityConfiguration {

    /** JWT 认证过滤器，校验每个请求的 Token */
    @Resource
    JwtAuthenticationFilter jwtAuthenticationFilter;

    /** 请求日志过滤器 */
    @Resource
    RequestLogFilter requestLogFilter;

    /** JWT 工具类 */
    @Resource
    JwtUtils utils;

    /** 用户账户服务 */
    @Resource
    AccountService service;

    /**
     * 安全过滤器链配置
     *
     * @param http HttpSecurity 构建器
     * @return 构建完成的 SecurityFilterChain
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                // 路径权限：公开接口、管理员接口、用户接口
                .authorizeHttpRequests(conf -> conf
                        .requestMatchers("/api/auth/**", "/error").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/images/**").permitAll()
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/api/admin/**").hasRole(Const.ROLE_ADMIN)
                        .anyRequest().hasAnyRole(Const.ROLE_DEFAULT, Const.ROLE_ADMIN)
                )
                // 表单登录：Spring Security 自动处理 /api/auth/login 的 POST 请求
                .formLogin(conf -> conf
                        .loginProcessingUrl("/api/auth/login")
                        .failureHandler(this::handleProcess)
                        .successHandler(this::handleProcess)
                        .permitAll()
                )
                // 退出登录：将 Token 加入 Redis 黑名单
                .logout(conf -> conf
                        .logoutUrl("/api/auth/logout")
                        .logoutSuccessHandler(this::onLogoutSuccess)
                )
                // 异常处理：401 未登录、403 无权限
                .exceptionHandling(conf -> conf
                        .accessDeniedHandler(this::handleProcess)
                        .authenticationEntryPoint(this::handleProcess)
                )
                // 关闭 CSRF（前后端分离不需要）
                .csrf(AbstractHttpConfigurer::disable)
                // 无状态 Session，不使用 HttpSession，全靠 JWT
                .sessionManagement(conf -> conf
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // 自定义过滤器，执行顺序：JwtAuthenticationFilter → RequestLogFilter → Spring Security 默认
                .addFilterBefore(requestLogFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthenticationFilter, RequestLogFilter.class)
                .build();
    }

    /**
     * 统一处理器：登录成功生成 JWT、登录失败返回错误、未登录返回 401、无权限返回 403
     * <p>
     * Spring Security 把四种场景的参数统一传给这一个方法，通过类型判断区分：
     * - Authentication → 登录成功
     * - AccessDeniedException → 无权限（403）
     * - 其他 Exception → 未登录（401）/ 登录失败
     *
     * @param request  HTTP 请求
     * @param response HTTP 响应
     * @param exceptionOrAuthentication 异常对象或认证对象，由 Spring Security 传入
     */
    private void handleProcess(HttpServletRequest request,
                               HttpServletResponse response,
                               Object exceptionOrAuthentication) throws IOException {
        response.setContentType("application/json;charset=utf-8");
        PrintWriter writer = response.getWriter();
        // "你是 AccessDeniedException 吗？是的话赋值给 exception 变量"
        if (exceptionOrAuthentication instanceof AccessDeniedException exception) {
            // 403：已登录但角色不够（普通用户访问管理端）
            writer.write(RestBean
                    .forbidden(exception.getMessage()).asJsonString());
        } else if (exceptionOrAuthentication instanceof Exception exception) {
            // 401：未登录或登录失败
            writer.write(RestBean
                    .unauthorized(exception.getMessage()).asJsonString());
        } else if (exceptionOrAuthentication instanceof Authentication authentication) {
            // 登录成功：查用户 → 检查禁用 → 生成 JWT → 返回
            User user = (User) authentication.getPrincipal();
            Account account = service.findAccountByNameOrEmail(user.getUsername());
            if (account == null || "disabled".equals(account.getStatus())) {
                writer.write(RestBean.forbidden("登录失败，账号已被禁用").asJsonString());
                return;
            }
            String jwt = utils.createJwt(user, account.getUsername(), account.getId());
            if (jwt == null) {
                writer.write(RestBean.forbidden("登录验证频繁，请稍后再试").asJsonString());
            } else {
                AuthorizeVO vo = account.asViewObject(AuthorizeVO.class, o -> o.setToken(jwt));
                vo.setExpire(utils.expireTime());
                writer.write(RestBean.success(vo).asJsonString());
            }
        }
    }

    /**
     * 退出登录成功回调，将 JWT 令牌加入 Redis 黑名单使其失效
     *
     * @param request  HTTP 请求，用于获取 Authorization 头中的 Token
     * @param response HTTP 响应
     * @param authentication 认证信息
     */
    private void onLogoutSuccess(HttpServletRequest request,
                                 HttpServletResponse response,
                                 Authentication authentication) throws IOException {
        response.setContentType("application/json;charset=utf-8");
        PrintWriter writer = response.getWriter();
        String authorization = request.getHeader("Authorization");
        if (utils.invalidateJwt(authorization)) {
            writer.write(RestBean.success("退出登录成功").asJsonString());
            return;
        }
        writer.write(RestBean.failure(400, "退出登录失败").asJsonString());
    }
}
