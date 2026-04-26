package com.campus.forum.entity.dto;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 论坛公告表
 */
@Data
@TableName("db_forum_notice")
public class ForumNotice {
    @TableId(type = IdType.INPUT)
    Integer id;             // 公告ID
    String content;         // 公告正文
    Date updateTime;        // 更新时间
    Integer updateBy;       // 更新人ID
}
