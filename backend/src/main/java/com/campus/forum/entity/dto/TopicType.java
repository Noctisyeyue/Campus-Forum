package com.campus.forum.entity.dto;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.campus.forum.entity.BaseData;
import lombok.Data;

/**
 * 帖子分类表
 */
@Data
@TableName("db_topic_type")
public class TopicType implements BaseData {
    @TableId(type = IdType.AUTO)
    Integer id;             // 分类ID
    String name;            // 分类名称
    @TableField("`desc`")
    String desc;            // 分类描述
    String color;           // 标签颜色（十六进制色值）
}
