package com.campus.forum.entity.vo.response;

import lombok.Data;

import java.util.Date;

@Data
public class AdminReportVO {
    Integer id;
    Integer uid;
    String reporterName;
    String targetType;
    Integer targetId;
    Integer topicId;
    String targetSummary;
    String reason;
    String detail;
    String status;
    String adminNote;
    Date time;
    Date resolveTime;
}
