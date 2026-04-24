package com.campus.forum.entity.vo.request;

import com.alibaba.fastjson2.JSONObject;
import jakarta.validation.constraints.Min;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

/**
 * 更新帖子请求
 */
@Data
public class TopicUpdateVO {
    @Min(0)
    int id;                 // 帖子ID
    @Min(1)
    int type;               // 分类ID
    @Length(min = 1, max = 30)
    String title;           // 帖子标题
    JSONObject content;     // 帖子内容（Quill Delta JSON）
}
