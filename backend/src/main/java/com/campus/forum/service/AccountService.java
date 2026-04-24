package com.campus.forum.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.campus.forum.entity.dto.Account;
import com.campus.forum.entity.vo.request.*;
import com.campus.forum.entity.vo.response.AdminUserVO;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.List;

/**
 * 用户账户服务
 */
public interface AccountService extends IService<Account>, UserDetailsService {
    Account findAccountByNameOrEmail(String text);
    Account findAccountById(int id);
    String registerEmailVerifyCode(String type, String email, String address);
    String registerEmailAccount(EmailRegisterVO info);
    String resetEmailAccountPassword(EmailResetVO info);
    String resetConfirm(ConfirmResetVO info);
    String modifyEmail(int id, ModifyEmailVO vo);
    String changePassword(int id, ChangePasswordVO vo);

    // 管理员方法：分页查询用户列表（支持搜索）
    List<AdminUserVO> adminListUsers(int page, String search);
    // 管理员方法：禁用用户
    void adminDisableUser(int id);
    // 管理员方法：启用用户
    void adminEnableUser(int id);
    // 管理员方法：重置用户密码为 123456
    void adminResetPassword(int id);
}
