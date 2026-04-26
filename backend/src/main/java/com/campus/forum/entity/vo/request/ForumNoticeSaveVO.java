package com.campus.forum.entity.vo.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 保存论坛公告请求
 */
@Data
public class ForumNoticeSaveVO {
    @NotBlank
    String content;         // 公告正文
}
