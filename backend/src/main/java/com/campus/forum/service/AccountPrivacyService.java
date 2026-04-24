package com.campus.forum.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.campus.forum.entity.dto.AccountPrivacy;
import com.campus.forum.entity.vo.request.PrivacySaveVO;

/**
 * 用户隐私设置服务
 */
public interface AccountPrivacyService extends IService<AccountPrivacy> {
    void savePrivacy(int id, PrivacySaveVO vo);
    AccountPrivacy accountPrivacy(int id);
}
