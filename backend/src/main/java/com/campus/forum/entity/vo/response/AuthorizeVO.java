package com.campus.forum.entity.vo.response;

import lombok.Data;

import java.util.Date;

/**
 * 登录成功响应
 */
@Data
public class AuthorizeVO {
    String username;    // 用户名
    String role;        // 角色
    String token;       // JWT令牌
    Date expire;        // 过期时间
}
