package com.campus.forum.utils;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * JWT 令牌工具类，负责令牌的创建、解析、校验和失效处理
 */
@Component
public class JwtUtils {

    /** JWT 签名密钥 */
    @Value("${spring.security.jwt.key}")
    private String key;
    /** 令牌过期时间（小时） */
    @Value("${spring.security.jwt.expire}")
    private int expire;
    /** 令牌生成基础冷却时间（秒） */
    @Value("${spring.security.jwt.limit.base}")
    private int limit_base;
    /** 令牌生成升级封禁时间（秒） */
    @Value("${spring.security.jwt.limit.upgrade}")
    private int limit_upgrade;
    /** 恶意刷令牌的请求次数阈值 */
    @Value("${spring.security.jwt.limit.frequency}")
    private int limit_frequency;

    @Resource
    StringRedisTemplate template;

    @Resource
    FlowUtils utils;

    /**
     * 使指定令牌失效（加入 Redis 黑名单）
     *
     * @param headerToken 请求头中的 Bearer Token
     * @return true=成功失效，false=令牌无效或已失效
     */
    public boolean invalidateJwt(String headerToken) {
        String token = this.convertToken(headerToken);
        // 选择加密算法
        Algorithm algorithm = Algorithm.HMAC256(key);
        // 创建验证器
        JWTVerifier jwtVerifier = JWT.require(algorithm).build();
        try {
            //验证通过后，把 Token 拆开，存到 verify 变量里
            DecodedJWT verify = jwtVerifier.verify(token);
            return deleteToken(verify.getId(), verify.getExpiresAt());
        } catch (JWTVerificationException e) {
            return false;
        }
    }

    /**
     * 计算令牌过期时间
     *
     * @return 基于当前时间 + expire 小时后的 Date
     */
    public Date expireTime() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.HOUR, expire);
        return calendar.getTime();
    }

    /**
     * 为已认证用户创建 JWT 令牌，频率限制触发时返回 null
     *
     * @param user     Spring Security UserDetails
     * @param username 用户名
     * @param userId   用户 ID
     * @return 签名后的 JWT 字符串，频率限制触发时返回 null
     */
    public String createJwt(UserDetails user, String username, int userId) {
        if (this.frequencyCheck(userId)) {
            Algorithm algorithm = Algorithm.HMAC256(key);
            Date expire = this.expireTime();
            return JWT.create()
                    .withJWTId(UUID.randomUUID().toString())               // 唯一 ID
                    .withClaim("id", userId)                               // 存入用户 ID
                    .withClaim("name", username)                           // 存入用户名
                    .withClaim("authorities", user.getAuthorities()        // 存入ROLE_USER || ROLE_ADMIN
                            .stream()
                            .map(GrantedAuthority::getAuthority).toList())
                    .withExpiresAt(expire)                                 // 设置过期时间
                    .withIssuedAt(new Date())                              // 设置签发时间为当前时间
                    .sign(algorithm);                                      // 对整个内容进行签名
        } else {
            return null;
        }
    }

    /**
     * 解析并校验 JWT 令牌
     *
     * @param headerToken 请求头中的 Bearer Token
     * @return 解析成功返回 DecodedJWT，无效或已失效返回 null
     */
    public DecodedJWT resolveJwt(String headerToken) {
        // 提取纯 Token
        String token = this.convertToken(headerToken);
        if (token == null) return null;
        // 创建验证器
        Algorithm algorithm = Algorithm.HMAC256(key);
        JWTVerifier jwtVerifier = JWT.require(algorithm).build();
        try {
            // 验证签名，不合法会抛异常
            DecodedJWT verify = jwtVerifier.verify(token);
            // 检查黑名单
            if (this.isInvalidToken(verify.getId())) return null;
            // 再次检查过期时间
            // 取出 Token 里的所有字段
            Map<String, Claim> claims = verify.getClaims();
            return new Date().after(claims.get("exp").asDate()) ? null : verify;
        } catch (JWTVerificationException e) {
            return null;
        }
    }

    /**
     * 将 JWT 信息转换为 Spring Security UserDetails
     *
     * @param jwt 已校验的 DecodedJWT
     * @return 包含用户名、权限的 UserDetails
     */
    public UserDetails toUser(DecodedJWT jwt) {
        Map<String, Claim> claims = jwt.getClaims();
        return User
                .withUsername(claims.get("name").asString())
                .password("******")
                .authorities(claims.get("authorities").asArray(String.class))
                .build();
    }

    /**
     * 从 JWT 中提取用户 ID
     *
     * @param jwt 已校验的 DecodedJWT
     * @return 用户 ID
     */
    public Integer toId(DecodedJWT jwt) {
        Map<String, Claim> claims = jwt.getClaims();
        return claims.get("id").asInt();
    }

    /**
     * JWT 生成频率检测，防止高频刷令牌
     *
     * @param userId 用户 ID
     * @return true=允许生成，false=触发频率限制
     */
    private boolean frequencyCheck(int userId) {
        String key = Const.JWT_FREQUENCY + userId;
        return utils.limitOnceUpgradeCheck(key, limit_frequency, limit_base, limit_upgrade);
    }

    /**
     * 从请求头提取 Token 字符串
     *
     * @param headerToken 请求头中的 Bearer Token
     * @return 提取出的 Token 字符串，格式不正确返回 null
     */
    private String convertToken(String headerToken) {
        if (headerToken == null || !headerToken.startsWith("Bearer "))
            return null;
        return headerToken.substring(7);
    }

    /**
     * 将令牌加入 Redis 黑名单，过期时间与令牌本身一致
     *
     * @param uuid 令牌的唯一 ID
     * @param time 令牌的过期时间
     * @return true=成功加入黑名单，false=已在黑名单中
     */
    private boolean deleteToken(String uuid, Date time) {
        if (this.isInvalidToken(uuid))
            return false;  //已经在黑名单中
        Date now = new Date();
        // 计算剩余有效时间
        long expire = Math.max(time.getTime() - now.getTime(), 0);
        // 存入 Redis 黑名单
        template.opsForValue().set(Const.JWT_BLACK_LIST + uuid, "", expire, TimeUnit.MILLISECONDS);
        return true;
    }

    /**
     * 检查令牌是否已在黑名单中
     *
     * @param uuid 令牌的唯一 ID
     * @return true=已失效（在黑名单中），false=有效
     */
    private boolean isInvalidToken(String uuid) {
        return Boolean.TRUE.equals(template.hasKey(Const.JWT_BLACK_LIST + uuid));
    }
}
