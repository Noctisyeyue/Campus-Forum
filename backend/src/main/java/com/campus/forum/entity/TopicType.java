package com.campus.forum.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("db_topic_type")
public class TopicType {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private String name;

    private String desc;

    private String color;
}
