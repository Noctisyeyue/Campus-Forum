package com.campus.forum.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.forum.entity.dto.AccountPrivacy;
import com.campus.forum.entity.vo.request.PrivacySaveVO;
import com.campus.forum.mapper.AccountPrivacyMapper;
import com.campus.forum.service.AccountPrivacyService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * 用户隐私设置服务实现
 */
@Service
public class AccountPrivacyServiceImpl extends ServiceImpl<AccountPrivacyMapper, AccountPrivacy> implements AccountPrivacyService {

    /**
     * 切换指定隐私字段的公开/隐藏状态
     *
     * @param id 用户ID
     * @param vo 隐私设置参数（含字段类型和目标状态）
     */
    @Override
    @Transactional
    public void savePrivacy(int id, PrivacySaveVO vo) {
        AccountPrivacy privacy = Optional.ofNullable(this.getById(id)).orElse(new AccountPrivacy(id));
        boolean status = vo.isStatus();
        switch (vo.getType()) {
            case "phone" -> privacy.setPhone(status);
            case "email" -> privacy.setEmail(status);
            case "gender" -> privacy.setGender(status);
            case "wx" -> privacy.setWx(status);
            case "qq" -> privacy.setQq(status);
        }
        this.saveOrUpdate(privacy);
    }

    /**
     * 查询用户隐私设置，不存在时返回默认值
     *
     * @param id 用户ID
     * @return 用户隐私设置实体
     */
    public AccountPrivacy accountPrivacy(int id) {
        return Optional.ofNullable(this.getById(id)).orElse(new AccountPrivacy(id));
    }
}
