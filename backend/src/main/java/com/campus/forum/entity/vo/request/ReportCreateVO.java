package com.campus.forum.entity.vo.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class ReportCreateVO {
    @NotBlank(message = "举报目标类型不能为空")
    @Pattern(regexp = "(topic|comment)", message = "举报目标类型无效")
    String targetType;

    @NotNull(message = "举报目标ID不能为空")
    Integer targetId;

    @NotBlank(message = "举报原因不能为空")
    String reason;

    String detail;
}
