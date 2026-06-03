package com.campus.forum.entity.vo.response;

import lombok.Data;

import java.util.Date;

/**
 * 管理端举报详情响应
 */
@Data
public class AdminReportVO {
    Integer id;             // 举报ID
    Integer uid;            // 举报者用户ID
    String reporterName;    // 举报者用户名
    String targetType;      // 举报目标类型：topic=帖子, comment=评论
    Integer targetId;       // 举报目标ID
    Integer topicId;        // 关联帖子ID（评论举报时也关联到帖子）
    String targetSummary;   // 被举报内容的摘要文本
    String reason;          // 举报原因
    String detail;          // 举报补充说明
    String status;          // 举报状态：pending=待处理, resolved=已处理, dismissed=已驳回
    String adminNote;       // 管理员处理备注
    Date time;              // 举报时间
    Date resolveTime;       // 处理时间
}
