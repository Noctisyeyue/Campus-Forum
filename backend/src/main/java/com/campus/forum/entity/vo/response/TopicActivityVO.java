package com.campus.forum.entity.vo.response;

import lombok.Data;

import java.util.Date;

/**
 * 校园活动响应
 */
@Data
public class TopicActivityVO {
    Integer tid;                // 帖子ID
    Date activityTime;          // 活动时间
    String location;            // 活动地点
    String organizer;           // 主办方
    Date signupDeadline;        // 报名截止时间
}
