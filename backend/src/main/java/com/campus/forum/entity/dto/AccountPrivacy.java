package com.campus.forum.entity.dto;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.campus.forum.entity.BaseData;
import lombok.Data;

import java.lang.reflect.Field;
import java.util.LinkedList;
import java.util.List;

/**
 * 用户隐私设置表
 */
@Data
@TableName("db_account_privacy")
public class AccountPrivacy implements BaseData {
    @TableId(type = IdType.AUTO)
    final Integer id;
    boolean phone = true;   // 手机号是否公开
    boolean email = true;   // 邮箱是否公开
    boolean wx = true;      // 微信是否公开
    boolean qq = true;      // QQ是否公开
    boolean gender = true;  // 性别是否公开

    /** 返回需要隐藏的字段名数组 */
    public String[] hiddenFields() {
        List<String> strings = new LinkedList<>();
        Field[] fields = this.getClass().getDeclaredFields();  // 反射：拿到这个类的所有字段
        for (Field field : fields) {
            try {
                if (field.getType().equals(boolean.class) && !field.getBoolean(this))    // 字段类型是 boolean 并且值是 false
                    strings.add(field.getName());    // 记录这个字段名
            } catch (Exception ignored) {
            }
        }
        return strings.toArray(String[]::new);   // 转成数组返回
    }
}
