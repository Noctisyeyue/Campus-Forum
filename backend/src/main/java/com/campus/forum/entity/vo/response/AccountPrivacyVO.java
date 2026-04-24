package com.campus.forum.entity.vo.response;

import lombok.Data;

/**
 * 用户隐私设置响应
 */
@Data
public class AccountPrivacyVO {
    boolean phone;      // 手机号是否公开
    boolean email;      // 邮箱是否公开
    boolean wx;         // 微信是否公开
    boolean qq;         // QQ是否公开
    boolean gender;     // 性别是否公开
}
