package com.campus.forum.entity.vo.request;

import jakarta.validation.constraints.Email;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

/**
 * 邮箱重置密码请求
 */
@Data
public class EmailResetVO {
    @Email
    String email;       // 邮箱
    @Length(max = 6, min = 6)
    String code;        // 验证码
    @Length(min = 6, max = 20)
    String password;    // 新密码
}
