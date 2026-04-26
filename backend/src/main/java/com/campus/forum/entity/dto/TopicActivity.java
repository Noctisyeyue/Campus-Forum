package com.campus.forum.entity.dto;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 校园活动扩展表
 */
@Data
@TableName("db_topic_activity")
public class TopicActivity {
    @TableId
    Integer tid;                // 关联帖子ID
    Date activityTime;          // 活动时间
    String location;            // 活动地点
    String organizer;           // 主办方
    Date signupDeadline;        // 报名截止时间
}
