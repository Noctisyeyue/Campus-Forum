package com.campus.forum.entity.vo.response;

import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * 帖子列表预览响应
 */
@Data
public class TopicPreviewVO {
    int id;                 // 帖子ID
    int type;               // 分类ID
    String title;           // 帖子标题
    String text;            // 帖子纯文本摘要
    List<String> images;    // 帖子图片列表
    Date time;              // 创建时间
    Integer uid;            // 发帖用户ID
    String username;        // 发帖用户名
    String avatar;          // 发帖用户头像
    int like;               // 点赞数
    int collect;            // 收藏数
    String status;          // 帖子状态
    String reviewReason;    // 审核拒绝原因
    String hideReason;      // 下架原因
    Date activityTime;      // 活动时间
    String location;        // 活动地点
}
