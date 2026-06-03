package com.campus.forum.entity.vo.response;

import lombok.Data;

import java.util.Date;

/**
 * 评论响应
 */
@Data
public class CommentVO {
    int id;             // 评论ID
    String content;     // 评论内容
    Date time;          // 评论时间
    String quote;       // 引用内容
    User user;          // 评论用户信息

    /**
     * 评论用户信息
     */
    @Data
    public static class User {
        Integer id;         // 用户ID
        String username;    // 用户名
        String avatar;      // 头像
        boolean gender;     // 性别
        String qq;          // QQ号
        String wx;          // 微信号
        String phone;       // 手机号
        String email;       // 邮箱
    }
}
