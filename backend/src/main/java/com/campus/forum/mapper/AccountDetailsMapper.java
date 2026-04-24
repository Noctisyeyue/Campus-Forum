package com.campus.forum.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.forum.entity.dto.AccountDetails;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户详情 Mapper
 */
@Mapper
public interface AccountDetailsMapper extends BaseMapper<AccountDetails> {
}
