package com.campus.forum.entity.vo.response;

import lombok.Data;

import java.util.Date;

/**
 * 用户信息响应
 */
@Data
public class AccountVO {
    int id;                 // 用户ID
    String username;        // 用户名
    String email;           // 邮箱
    String role;            // 角色
    String avatar;          // 头像路径
    Date registerTime;      // 注册时间
}
