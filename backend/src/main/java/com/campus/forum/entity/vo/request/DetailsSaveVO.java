package com.campus.forum.entity.vo.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

/**
 * 保存用户详情请求
 */
@Data
public class DetailsSaveVO {
    @Pattern(regexp = "^[a-zA-Z0-9\\u4e00-\\u9fa5]+$")
    @Length(min = 1, max = 10)
    String username;    // 用户名
    @Min(0)
    @Max(1)
    int gender;         // 性别
    @Length(max = 11)
    String phone;       // 手机号
    @Length(max = 13)
    String qq;          // QQ号
    @Length(max = 20)
    String wx;          // 微信号
    @Length(max = 200)
    String desc;        // 个人简介
}
