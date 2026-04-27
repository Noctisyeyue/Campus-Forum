package com.campus.forum.entity.dto;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("db_report")
public class Report {
    @TableId(type = IdType.AUTO)
    Integer id;
    Integer uid;
    String targetType;
    Integer targetId;
    String reason;
    String detail;
    String status;
    Integer adminId;
    String adminNote;
    Date resolveTime;
    Date time;
}
