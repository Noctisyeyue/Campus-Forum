package com.campus.forum.entity.vo.request;

import com.alibaba.fastjson2.JSONObject;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import java.util.Date;

/**
 * 发布校园活动请求
 */
@Data
public class PublishActivityVO {
    @Length(min = 1, max = 30)
    String title;               // 帖子标题
    @NotNull
    JSONObject content;         // 帖子正文
    @NotNull
    Date activityTime;          // 活动时间
    @NotBlank
    @Length(max = 100)
    String location;            // 活动地点
    @NotBlank
    @Length(max = 100)
    String organizer;           // 主办方
    Date signupDeadline;        // 报名截止时间
}
