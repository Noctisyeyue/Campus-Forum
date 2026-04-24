package com.campus.forum.entity.dto;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.campus.forum.entity.BaseData;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 用户账户表
 */
@Data
@TableName("db_account")
@AllArgsConstructor
@NoArgsConstructor
public class Account implements BaseData {
    @TableId(type = IdType.AUTO)
    Integer id;             // 用户ID，主键自增
    String username;        // 用户名
    String password;        // 密码（BCrypt加密存储）
    String email;           // 邮箱地址
    String role;            // 角色：user=普通用户, admin=管理员
    String status;          // 账号状态：active=正常, disabled=禁用
    String avatar;          // 头像路径（MinIO存储地址）
    Date registerTime;      // 注册时间
}
