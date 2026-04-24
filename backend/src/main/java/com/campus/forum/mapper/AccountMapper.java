package com.campus.forum.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.forum.entity.dto.Account;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户账户 Mapper
 */
@Mapper
public interface AccountMapper extends BaseMapper<Account> {
}
