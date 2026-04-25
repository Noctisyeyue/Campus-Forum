package com.campus.forum.entity.vo.response;

import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * 我的帖子列表响应
 */
@Data
public class UserTopicVO {
    int id;                 // 帖子ID
    int type;               // 分类ID
    String title;           // 帖子标题
    String text;            // 帖子纯文本摘要
    List<String> images;    // 帖子图片列表
    Date time;              // 创建时间
    int like;               // 点赞数
    int collect;            // 收藏数
    String status;          // 帖子状态
    String reviewReason;    // 审核拒绝原因
    String hideReason;      // 下架原因
}
