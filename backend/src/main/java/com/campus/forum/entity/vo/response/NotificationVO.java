package com.campus.forum.entity.vo.response;

import lombok.Data;

/**
 * 通知响应
 */
@Data
public class NotificationVO {
    int id;             // 通知ID
    String title;       // 通知标题
    String content;     // 通知内容
    String type;        // 通知类型
    String url;         // 跳转链接
    String time;        // 通知时间
}
