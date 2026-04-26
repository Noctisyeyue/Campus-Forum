package com.campus.forum.entity.vo.request;

import com.alibaba.fastjson2.JSONObject;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

/**
 * 发布教务通知请求
 */
@Data
public class PublishNoticeTopicVO {
    @Length(min = 1, max = 30)
    String title;           // 通知标题
    @NotNull
    JSONObject content;     // 通知正文
}
