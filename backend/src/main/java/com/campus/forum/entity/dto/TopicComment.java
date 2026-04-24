package com.campus.forum.entity.dto;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 评论表
 */
@Data
@TableName("db_topic_comment")
public class TopicComment {
    @TableId(type = IdType.AUTO)
    Integer id;             // 评论ID，主键自增
    Integer uid;            // 评论用户ID
    Integer tid;            // 所属帖子ID
    String content;         // 评论内容（JSON格式，Quill Delta数据）
    Date time;              // 评论时间
    Integer quote;          // 引用的评论ID：-1=顶级评论, 其他=回复的评论ID
    String status;          // 评论状态：normal=正常, deleted=已删除
}
