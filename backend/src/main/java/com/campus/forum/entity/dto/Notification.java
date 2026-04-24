package com.campus.forum.entity.dto;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.campus.forum.entity.BaseData;
import lombok.Data;

/**
 * 通知表
 */
@Data
@TableName("db_notification")
public class Notification implements BaseData {
    @TableId(type = IdType.AUTO)
    Integer id;             // 通知ID，主键自增
    Integer uid;            // 接收通知的用户ID
    String title;           // 通知标题
    String content;         // 通知内容
    String type;            // 通知类型
    String url;             // 跳转链接
    String time;            // 通知时间
}
