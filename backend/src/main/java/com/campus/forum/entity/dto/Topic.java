package com.campus.forum.entity.dto;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 帖子表，核心业务表
 */
@Data
@TableName("db_topic")
public class Topic {
    @TableId(type = IdType.AUTO)
    Integer id;                 // 帖子ID，主键自增
    String title;               // 帖子标题
    String content;             // 帖子内容（JSON格式，Quill Delta数据）
    Integer uid;                // 发帖用户ID
    Integer type;               // 分类ID
    Date time;                  // 创建时间
    Integer top;                // 是否置顶：0=否, 1=是
    String status;              // 帖子状态：pending_review/published/rejected/hidden/deleted
    Date reviewTime;            // 最近一次审核时间
    Integer reviewBy;           // 审核人ID
    String reviewReason;        // 审核理由
    String hideReason;          // 下架理由（管理员下架时填写）
    Date lastSubmitTime;        // 最后提交审核时间
    Date deletedTime;           // 删除时间（软删除）
    Integer deletedBy;          // 删除人ID
}
