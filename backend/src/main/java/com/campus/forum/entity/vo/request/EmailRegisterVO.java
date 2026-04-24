package com.campus.forum.entity.vo.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

/**
 * 邮箱注册请求
 */
@Data
public class EmailRegisterVO {
    @Email
    String email;       // 邮箱
    @Length(max = 6, min = 6)
    String code;        // 验证码
    @Pattern(regexp = "^[a-zA-Z0-9\\u4e00-\\u9fa5]+$")
    @Length(min = 1, max = 10)
    String username;    // 用户名
    @Length(min = 6, max = 20)
    String password;    // 密码
}
