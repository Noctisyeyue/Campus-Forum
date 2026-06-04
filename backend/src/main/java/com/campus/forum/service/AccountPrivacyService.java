package com.campus.forum.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.campus.forum.entity.dto.AccountPrivacy;
import com.campus.forum.entity.vo.request.PrivacySaveVO;

/**
 * 用户隐私设置服务
 */
public interface AccountPrivacyService extends IService<AccountPrivacy> {

    /**
     * 保存用户隐私设置
     *
     * @param id 用户ID
     * @param vo 隐私设置参数
     */
    void savePrivacy(int id, PrivacySaveVO vo);

    /**
     * 获取用户隐私设置
     *
     * @param id 用户ID
     * @return 隐私设置，不存在返回 null
     */
    AccountPrivacy accountPrivacy(int id);
}
