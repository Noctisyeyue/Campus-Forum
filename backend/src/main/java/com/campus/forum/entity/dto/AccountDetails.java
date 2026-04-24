package com.campus.forum.entity.dto;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.campus.forum.entity.BaseData;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户详情表
 */
@Data
@TableName("db_account_details")
@AllArgsConstructor
@NoArgsConstructor
public class AccountDetails implements BaseData {
    @TableId
    Integer id;             // 用户ID，与 db_account.id 一致
    int gender;             // 性别：0=未设置, 1=男, 2=女
    String phone;           // 手机号码
    String qq;              // QQ号
    String wx;              // 微信号
    @TableField("`desc`")
    String desc;            // 个人简介
}
