package com.campus.forum.entity.vo.response;

import lombok.Data;

/**
 * 帖子分类响应
 */
@Data
public class TopicTypeVO {
    int id;             // 分类ID
    String name;        // 分类名称
    String desc;        // 分类描述
    String color;       // 标签颜色
    String systemKey;   // 系统分类标识
}
