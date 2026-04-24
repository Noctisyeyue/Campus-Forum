package com.campus.forum.entity.vo.request;

import jakarta.validation.constraints.Email;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

/**
 * 修改邮箱请求
 */
@Data
public class ModifyEmailVO {
    @Email
    String email;       // 新邮箱
    @Length(max = 6, min = 6)
    String code;        // 验证码
}
