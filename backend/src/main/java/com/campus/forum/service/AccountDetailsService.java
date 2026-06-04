package com.campus.forum.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.campus.forum.entity.dto.AccountDetails;
import com.campus.forum.entity.vo.request.DetailsSaveVO;

/**
 * 用户详情服务
 */
public interface AccountDetailsService extends IService<AccountDetails> {

    /**
     * 根据用户ID查询详情
     *
     * @param id 用户ID
     * @return 用户详情，不存在返回 null
     */
    AccountDetails findAccountDetailsById(int id);

    /**
     * 保存用户详情信息
     *
     * @param id 用户ID
     * @param vo 详情保存参数
     * @return true=保存成功，false=用户名已被占用
     */
    boolean saveAccountDetails(int id, DetailsSaveVO vo);
}
