package com.campus.forum.entity.vo.response;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Date;

/**
 * 帖子详情响应
 */
@Data
public class TopicDetailVO {
    Integer id;             // 帖子ID
    String title;           // 帖子标题
    String content;         // 帖子内容
    Integer type;           // 分类ID
    Date time;              // 创建时间
    Boolean allowComment;   // 是否允许评论
    String status;          // 帖子状态
    String reviewReason;    // 审核理由
    String hideReason;      // 下架理由
    Date activityTime;      // 活动时间
    String location;        // 活动地点
    String organizer;       // 主办方
    Date signupDeadline;    // 报名截止时间
    User user;              // 发帖用户信息
    Interact interact;      // 当前用户互动状态（点赞/收藏）
    Long comments;          // 评论数量

    @Data
    @AllArgsConstructor
    public static class Interact {
        Boolean like;       // 是否已点赞
        Boolean collect;    // 是否已收藏
    }

    @Data
    public static class User {
        Integer id;         // 用户ID
        String username;    // 用户名
        String avatar;      // 头像
        String desc;        // 个人简介
        Integer gender;     // 性别
        String qq;          // QQ号
        String wx;          // 微信号
        String phone;       // 手机号
        String email;       // 邮箱
    }
}
