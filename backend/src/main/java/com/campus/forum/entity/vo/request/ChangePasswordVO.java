package com.campus.forum.entity.vo.request;

import lombok.Data;
import org.hibernate.validator.constraints.Length;

/**
 * 修改密码请求
 */
@Data
public class ChangePasswordVO {
    @Length(min = 6, max = 20)
    String password;        // 原密码
    @Length(min = 6, max = 20)
    String new_password;    // 新密码
}
