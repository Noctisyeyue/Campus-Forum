package com.campus.forum.entity.vo.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 创建举报请求
 */
@Data
public class ReportCreateVO {
    @NotBlank(message = "举报目标类型不能为空")
    @Pattern(regexp = "(topic|comment)", message = "举报目标类型无效")
    String targetType;      // 举报目标类型：topic/comment

    @NotNull(message = "举报目标ID不能为空")
    Integer targetId;       // 举报目标ID

    @NotBlank(message = "举报原因不能为空")
    String reason;          // 举报原因

    @Size(max = 500, message = "举报详情不能超过500个字符")
    String detail;          // 举报详情（可选）
}
