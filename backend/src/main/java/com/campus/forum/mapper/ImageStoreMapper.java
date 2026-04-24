package com.campus.forum.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.forum.entity.dto.StoreImage;
import org.apache.ibatis.annotations.Mapper;

/**
 * 图片存储记录 Mapper
 */
@Mapper
public interface ImageStoreMapper extends BaseMapper<StoreImage> {
}
