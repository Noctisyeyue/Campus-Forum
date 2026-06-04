package com.campus.forum.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.forum.entity.dto.Account;
import com.campus.forum.entity.dto.AccountDetails;
import com.campus.forum.entity.vo.request.DetailsSaveVO;
import com.campus.forum.mapper.AccountDetailsMapper;
import com.campus.forum.service.AccountDetailsService;
import com.campus.forum.service.AccountService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 用户详情服务实现
 */
@Service
public class AccountDetailsServiceImpl extends ServiceImpl<AccountDetailsMapper, AccountDetails> implements AccountDetailsService {

    /** 用户账号服务 */
    @Resource
    AccountService service;

    /**
     * 根据用户ID查询用户详情
     *
     * @param id 用户ID
     * @return 用户详情实体，不存在时返回 null
     */
    @Override
    public AccountDetails findAccountDetailsById(int id) {
        return this.getById(id);
    }

    /**
     * 保存用户详情，同时更新用户名（需检查用户名唯一性）
     *
     * @param id 用户ID
     * @param vo 用户详情保存参数
     * @return 保存成功返回 true，用户名已被占用返回 false
     */
    @Override
    @Transactional
    public synchronized boolean saveAccountDetails(int id, DetailsSaveVO vo) {
        Account account = service.findAccountByNameOrEmail(vo.getUsername());
        if (account == null || account.getId() == id) {
            service.update()
                    .eq("id", id)
                    .set("username", vo.getUsername())
                    .update();
            this.saveOrUpdate(new AccountDetails(
                    id, vo.getGender(), vo.getPhone(),
                    vo.getQq(), vo.getWx(), vo.getDesc()
            ));
            return true;
        }
        return false;
    }
}
