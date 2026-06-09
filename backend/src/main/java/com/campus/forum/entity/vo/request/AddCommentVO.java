package com.campus.forum.entity.vo.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 添加评论请求
 */
@Data
public class AddCommentVO {
    @Min(1)
    int tid;            // 所属帖子ID
    @NotBlank(message = "评论内容不能为空")
    String content;     // 评论内容
    @Min(-1)
    int quote;          // 引用评论ID：-1=顶级评论
}
