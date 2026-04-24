package com.campus.forum.entity.dto;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Date;

/**
 * 图片存储记录表
 */
@Data
@TableName("db_image_store")
@AllArgsConstructor
public class StoreImage {
    Integer uid;            // 上传用户ID
    String name;            // 图片在MinIO中的存储路径
    Date time;              // 上传时间
}
