package com.campus.forum.entity.vo.response;

import lombok.Data;

import java.util.Date;

/**
 * 论坛公告响应
 */
@Data
public class ForumNoticeVO {
    Integer id;             // 公告ID
    String content;         // 公告正文
    Date updateTime;        // 更新时间
    Integer updateBy;       // 更新人ID
    String updateByName;    // 更新人名称
}
