package com.campus.forum.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.forum.entity.dto.ForumNotice;
import org.apache.ibatis.annotations.Mapper;

/**
 * 论坛公告 Mapper
 */
@Mapper
public interface ForumNoticeMapper extends BaseMapper<ForumNotice> {
}
