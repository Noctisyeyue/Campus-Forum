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

    @Value("${spring.security.jwt.key}")
    private String key;              // JWT 签名密钥
    @Value("${spring.security.jwt.expire}")
    private int expire;              // 令牌过期时间（小时）
    @Value("${spring.security.jwt.limit.base}")
    private int limit_base;          // 令牌生成基础冷却时间（秒）
    @Value("${spring.security.jwt.limit.upgrade}")
    private int limit_upgrade;       // 令牌生成升级封禁时间（秒）
    @Value("${spring.security.jwt.limit.frequency}")
    private int limit_frequency;     // 恶意刷令牌的请求次数阈值

    @Resource
    StringRedisTemplate template;

    @Resource
    FlowUtils utils;

    /**
     * 使指定令牌失效（加入 Redis 黑名单）
     */
    public boolean invalidateJwt(String headerToken) {
        String token = this.convertToken(headerToken);
        Algorithm algorithm = Algorithm.HMAC256(key);
        JWTVerifier jwtVerifier = JWT.require(algorithm).build();
        try {
            DecodedJWT verify = jwtVerifier.verify(token);
            return deleteToken(verify.getId(), verify.getExpiresAt());
        } catch (JWTVerificationException e) {
            return false;
        }
    }

    /**
     * 计算令牌过期时间
     */
    public Date expireTime() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.HOUR, expire);
        return calendar.getTime();
    }

    /**
     * 为已认证用户创建 JWT 令牌，频率限制触发时返回 null
     */
    public String createJwt(UserDetails user, String username, int userId) {
        if (this.frequencyCheck(userId)) {
            Algorithm algorithm = Algorithm.HMAC256(key);
            Date expire = this.expireTime();
            return JWT.create()
                    .withJWTId(UUID.randomUUID().toString())
                    .withClaim("id", userId)
                    .withClaim("name", username)
                    .withClaim("authorities", user.getAuthorities()
                            .stream()
                            .map(GrantedAuthority::getAuthority).toList())
                    .withExpiresAt(expire)
                    .withIssuedAt(new Date())
                    .sign(algorithm);
        } else {
            return null;
        }
    }

    /**
     * 解析并校验 JWT 令牌，无效返回 null
     */
    public DecodedJWT resolveJwt(String headerToken) {
        String token = this.convertToken(headerToken);
        if (token == null) return null;
        Algorithm algorithm = Algorithm.HMAC256(key);
        JWTVerifier jwtVerifier = JWT.require(algorithm).build();
        try {
            DecodedJWT verify = jwtVerifier.verify(token);
            if (this.isInvalidToken(verify.getId())) return null;
            Map<String, Claim> claims = verify.getClaims();
            return new Date().after(claims.get("exp").asDate()) ? null : verify;
        } catch (JWTVerificationException e) {
            return null;
        }
    }

    /** 将 JWT 信息转换为 Spring Security UserDetails */
    public UserDetails toUser(DecodedJWT jwt) {
        Map<String, Claim> claims = jwt.getClaims();
        return User
                .withUsername(claims.get("name").asString())
                .password("******")
                .authorities(claims.get("authorities").asArray(String.class))
                .build();
    }

    /** 从 JWT 中提取用户 ID */
    public Integer toId(DecodedJWT jwt) {
        Map<String, Claim> claims = jwt.getClaims();
        return claims.get("id").asInt();
    }

    // JWT 生成频率检测，防止高频刷令牌
    private boolean frequencyCheck(int userId) {
        String key = Const.JWT_FREQUENCY + userId;
        return utils.limitOnceUpgradeCheck(key, limit_frequency, limit_base, limit_upgrade);
    }

    // 从请求头提取 Token 字符串
    private String convertToken(String headerToken) {
        if (headerToken == null || !headerToken.startsWith("Bearer "))
            return null;
        return headerToken.substring(7);
    }

    // 将令牌加入 Redis 黑名单
    private boolean deleteToken(String uuid, Date time) {
        if (this.isInvalidToken(uuid))
            return false;
        Date now = new Date();
        long expire = Math.max(time.getTime() - now.getTime(), 0);
        template.opsForValue().set(Const.JWT_BLACK_LIST + uuid, "", expire, TimeUnit.MILLISECONDS);
        return true;
    }

    // 检查令牌是否已在黑名单中
    private boolean isInvalidToken(String uuid) {
        return Boolean.TRUE.equals(template.hasKey(Const.JWT_BLACK_LIST + uuid));
    }
}
