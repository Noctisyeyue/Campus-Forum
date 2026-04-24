package com.campus.forum.entity.vo.response;

import lombok.Data;

import java.util.Date;

/**
 * 管理员端评论列表响应
 */
@Data
public class AdminCommentVO {
    Integer id;             // 评论ID
    Integer uid;            // 评论用户ID
    String username;        // 评论用户名
    Integer tid;            // 所属帖子ID
    String topicTitle;      // 所属帖子标题
    String content;         // 评论内容
    Date time;              // 评论时间
    String status;          // 评论状态
}
