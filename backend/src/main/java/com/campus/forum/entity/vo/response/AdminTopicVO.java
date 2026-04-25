package com.campus.forum.entity.vo.response;

import lombok.Data;

import java.util.Date;

/**
 * 管理员端帖子列表响应
 */
@Data
public class AdminTopicVO {
    Integer id;             // 帖子ID
    String title;           // 帖子标题
    Integer uid;            // 发帖用户ID
    String username;        // 发帖用户名
    Integer type;           // 分类ID
    String typeName;        // 分类名称
    String status;          // 帖子状态
    Integer top;            // 是否置顶
    Date time;              // 创建时间
    Date lastSubmitTime;    // 最后提交审核时间
    Date reviewTime;        // 最近审核时间
    String reviewReason;    // 审核理由
    String hideReason;      // 下架理由
    Long commentCount;      // 评论数
}
