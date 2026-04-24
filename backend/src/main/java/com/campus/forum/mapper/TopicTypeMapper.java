package com.campus.forum.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.forum.entity.dto.TopicType;
import org.apache.ibatis.annotations.Mapper;

/**
 * 帖子分类 Mapper
 */
@Mapper
public interface TopicTypeMapper extends BaseMapper<TopicType> {
}
