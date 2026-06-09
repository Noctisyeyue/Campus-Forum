package com.campus.forum.entity.vo.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 保存论坛公告请求
 */
@Data
public class ForumNoticeSaveVO {
    @NotBlank
    @Size(max = 2000, message = "公告内容不能超过2000个字符")
    String content;         // 公告正文
}
