package com.campus.forum.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.forum.entity.dto.AccountPrivacy;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户隐私设置 Mapper
 */
@Mapper
public interface AccountPrivacyMapper extends BaseMapper<AccountPrivacy> {
}
