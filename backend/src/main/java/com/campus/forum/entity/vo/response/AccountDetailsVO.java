package com.campus.forum.entity.vo.response;

import lombok.Data;

/**
 * 用户详情响应
 */
@Data
public class AccountDetailsVO {
    int gender;         // 性别
    String phone;       // 手机号
    String qq;          // QQ号
    String wx;          // 微信号
    String desc;        // 个人简介
}
