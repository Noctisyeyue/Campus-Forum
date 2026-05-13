package com.campus.forum.entity.dto;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 举报记录表
 */
@Data
@TableName("db_report")
public class Report {
    @TableId(type = IdType.AUTO)
    Integer id;             // 举报ID，主键自增
    Integer uid;            // 举报人ID
    String targetType;      // 举报目标类型：topic=帖子, comment=评论
    Integer targetId;       // 举报目标ID
    String reason;          // 举报原因
    String detail;          // 举报详情
    String status;          // 处理状态：pending=待处理, resolved=已处理, dismissed=已驳回
    Integer adminId;        // 处理管理员ID
    String adminNote;       // 管理员处理备注
    Date resolveTime;       // 处理时间
    Date time;              // 举报时间
}
