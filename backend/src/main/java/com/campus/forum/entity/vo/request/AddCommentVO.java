package com.campus.forum.entity.vo.request;

import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 添加评论请求
 */
@Data
public class AddCommentVO {
    @Min(1)
    int tid;            // 所属帖子ID
    String content;     // 评论内容
    @Min(-1)
    int quote;          // 引用评论ID：-1=顶级评论
}
