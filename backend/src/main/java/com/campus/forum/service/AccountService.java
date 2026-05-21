package com.campus.forum.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.campus.forum.entity.dto.Account;
import com.campus.forum.entity.vo.request.*;
import com.campus.forum.entity.vo.response.AdminUserVO;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.List;

/**
 * 用户账户服务，提供账户查询、注册、密码重置及管理端用户管理能力
 */
public interface AccountService extends IService<Account>, UserDetailsService {

    /**
     * 根据用户名或邮箱查询账户
     *
     * @param text 用户名或邮箱
     * @return 匹配的账户，未找到返回 null
     */
    Account findAccountByNameOrEmail(String text);

    /**
     * 根据用户 ID 查询账户
     *
     * @param id 用户 ID
     * @return 匹配的账户，未找到返回 null
     */
    Account findAccountById(int id);

    /**
     * 发送邮箱验证码并缓存到 Redis
     *
     * @param type    业务类型（register / reset）
     * @param email   目标邮箱
     * @param address 请求方 IP 地址，用于限流
     * @return null 表示发送成功，否则返回错误信息
     */
    String registerEmailVerifyCode(String type, String email, String address);

    /**
     * 使用邮箱验证码完成注册
     *
     * @param info 注册信息（用户名、密码、邮箱、验证码）
     * @return null 表示注册成功，否则返回错误信息
     */
    String registerEmailAccount(EmailRegisterVO info);

    /**
     * 使用邮箱验证码重置密码
     *
     * @param info 重置信息（邮箱、新密码、验证码）
     * @return null 表示重置成功，否则返回错误信息
     */
    String resetEmailAccountPassword(EmailResetVO info);

    /**
     * 验证邮箱重置验证码是否正确
     *
     * @param info 验证信息（邮箱、验证码）
     * @return null 表示验证通过，否则返回错误信息
     */
    String resetConfirm(ConfirmResetVO info);

    /**
     * 修改用户邮箱
     *
     * @param id 用户 ID
     * @param vo 新邮箱及验证码
     * @return null 表示修改成功，否则返回错误信息
     */
    String modifyEmail(int id, ModifyEmailVO vo);

    /**
     * 修改用户密码
     *
     * @param id 用户 ID
     * @param vo 旧密码与新密码
     * @return null 表示修改成功，否则返回错误信息
     */
    String changePassword(int id, ChangePasswordVO vo);

    /**
     * 管理员分页查询用户列表，支持按用户名或邮箱搜索
     *
     * @param page   页码
     * @param search 搜索关键词，为空时返回全部
     * @param status 用户状态筛选，为空时返回全部
     * @return 用户列表
     */
    List<AdminUserVO> adminListUsers(int page, String search, String status);

    /**
     * 管理员禁用用户
     *
     * @param id 用户 ID
     */
    void adminDisableUser(int id);

    /**
     * 管理员启用用户
     *
     * @param id 用户 ID
     */
    void adminEnableUser(int id);

    /**
     * 管理员重置用户密码为默认密码
     *
     * @param id 用户 ID
     */
    void adminResetPassword(int id);
}
