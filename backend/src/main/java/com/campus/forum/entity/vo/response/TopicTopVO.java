package com.campus.forum.entity.vo.response;

import lombok.Data;

import java.util.Date;

/**
 * 置顶帖子响应
 */
@Data
public class TopicTopVO {
    int id;             // 帖子ID
    String title;       // 帖子标题
    Date time;          // 创建时间
}
