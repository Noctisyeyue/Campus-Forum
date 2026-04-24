package com.campus.forum.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.forum.entity.dto.Account;
import com.campus.forum.entity.dto.AccountDetails;
import com.campus.forum.entity.dto.AccountPrivacy;
import com.campus.forum.entity.vo.request.*;
import com.campus.forum.entity.vo.response.AdminUserVO;
import com.campus.forum.mapper.AccountDetailsMapper;
import com.campus.forum.mapper.AccountMapper;
import com.campus.forum.mapper.AccountPrivacyMapper;
import com.campus.forum.service.AccountService;
import com.campus.forum.utils.Const;
import com.campus.forum.utils.FlowUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.BeanUtils;
import jakarta.annotation.Resource;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * 用户账户服务实现
 */
@Service
public class AccountServiceImpl extends ServiceImpl<AccountMapper, Account> implements AccountService {

    @Value("${spring.web.verify.mail-limit}")
    int verifyLimit;        // 验证邮件发送冷却时间（秒）

    @Resource
    AmqpTemplate rabbitTemplate;

    @Resource
    StringRedisTemplate stringRedisTemplate;

    @Resource
    PasswordEncoder passwordEncoder;

    @Resource
    AccountPrivacyMapper privacyMapper;

    @Resource
    AccountDetailsMapper detailsMapper;

    @Resource
    FlowUtils flow;

    /**
     * Spring Security 登录认证：通过用户名或邮箱查找用户
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Account account = this.findAccountByNameOrEmail(username);
        if (account == null)
            throw new UsernameNotFoundException("用户名或密码错误");
        return User
                .withUsername(username)
                .password(account.getPassword())
                .roles(account.getRole())
                .build();
    }

    /**
     * 发送邮箱验证码，存入 Redis 并通过 RabbitMQ 异步发送邮件
     */
    public String registerEmailVerifyCode(String type, String email, String address) {
        synchronized (address.intern()) {
            if (!this.verifyLimit(address))
                return "请求频繁，请稍后再试";
            Random random = new Random();
            int code = random.nextInt(899999) + 100000;
            Map<String, Object> data = Map.of("type", type, "email", email, "code", code);
            rabbitTemplate.convertAndSend(Const.MQ_MAIL, data);
            stringRedisTemplate.opsForValue()
                    .set(Const.VERIFY_EMAIL_DATA + email, String.valueOf(code), 3, TimeUnit.MINUTES);
            return null;
        }
    }

    /**
     * 邮箱注册账号
     */
    public String registerEmailAccount(EmailRegisterVO info) {
        String email = info.getEmail();
        String code = this.getEmailVerifyCode(email);
        if (code == null) return "请先获取验证码";
        if (!code.equals(info.getCode())) return "验证码错误，请重新输入";
        if (this.existsAccountByEmail(email)) return "该邮件地址已被注册";
        String username = info.getUsername();
        if (this.existsAccountByUsername(username)) return "该用户名已被他人使用，请重新更换";
        String password = passwordEncoder.encode(info.getPassword());
        Account account = new Account(null, info.getUsername(),
                password, email, Const.ROLE_DEFAULT, "active", null, new Date());
        if (!this.save(account)) {
            return "内部错误，注册失败";
        } else {
            this.deleteEmailVerifyCode(email);
            privacyMapper.insert(new AccountPrivacy(account.getId()));
            AccountDetails details = new AccountDetails();
            details.setId(account.getId());
            detailsMapper.insert(details);
            return null;
        }
    }

    /**
     * 邮箱验证码重置密码
     */
    @Override
    public String resetEmailAccountPassword(EmailResetVO info) {
        String verify = resetConfirm(new ConfirmResetVO(info.getEmail(), info.getCode()));
        if (verify != null) return verify;
        String email = info.getEmail();
        String password = passwordEncoder.encode(info.getPassword());
        boolean update = this.update().eq("email", email).set("password", password).update();
        if (update) {
            this.deleteEmailVerifyCode(email);
        }
        return update ? null : "更新失败，请联系管理员";
    }

    /**
     * 重置密码确认操作
     */
    @Override
    public String resetConfirm(ConfirmResetVO info) {
        String email = info.getEmail();
        String code = this.getEmailVerifyCode(email);
        if (code == null) return "请先获取验证码";
        if (!code.equals(info.getCode())) return "验证码错误，请重新输入";
        return null;
    }

    @Override
    public String modifyEmail(int id, ModifyEmailVO vo) {
        String email = vo.getEmail();
        String code = getEmailVerifyCode(email);
        if (code == null) return "请先获取验证码！";
        if (!code.equals(vo.getCode())) return "验证码错误，请重新输入";
        this.deleteEmailVerifyCode(email);
        Account account = this.findAccountByNameOrEmail(email);
        if (account != null && account.getId() != id)
            return "该电子邮件已经被其他账号绑定，无法完成此操作！";
        this.update()
                .set("email", email)
                .eq("id", id)
                .update();
        return null;
    }

    @Override
    public String changePassword(int id, ChangePasswordVO vo) {
        String password = this.query().eq("id", id).one().getPassword();
        if (!passwordEncoder.matches(vo.getPassword(), password))
            return "原密码错误，请重新输入！";
        boolean success = this.update()
                .eq("id", id)
                .set("password", passwordEncoder.encode(vo.getNew_password()))
                .update();
        return success ? null : "未知错误，请联系管理员";
    }

    // 删除 Redis 中的邮件验证码
    private void deleteEmailVerifyCode(String email) {
        String key = Const.VERIFY_EMAIL_DATA + email;
        stringRedisTemplate.delete(key);
    }

    // 获取 Redis 中的邮件验证码
    private String getEmailVerifyCode(String email) {
        String key = Const.VERIFY_EMAIL_DATA + email;
        return stringRedisTemplate.opsForValue().get(key);
    }

    // IP 级别邮件验证码限流
    private boolean verifyLimit(String address) {
        String key = Const.VERIFY_EMAIL_LIMIT + address;
        return flow.limitOnceCheck(key, verifyLimit);
    }

    /**
     * 通过用户名或邮箱查找用户
     */
    public Account findAccountByNameOrEmail(String text) {
        return this.query()
                .eq("username", text).or()
                .eq("email", text)
                .one();
    }

    @Override
    public Account findAccountById(int id) {
        return this.query().eq("id", id).one();
    }

    // 检查邮箱是否已被注册
    private boolean existsAccountByEmail(String email) {
        return this.baseMapper.exists(Wrappers.<Account>query().eq("email", email));
    }

    // 检查用户名是否已存在
    private boolean existsAccountByUsername(String username) {
        return this.baseMapper.exists(Wrappers.<Account>query().eq("username", username));
    }

    // ==================== 管理员方法 ====================

    /**
     * 分页查询用户列表，支持按用户名或邮箱搜索
     * @param page 页码
     * @param search 搜索关键词（可选）
     * @return 用户列表
     */
    @Override
    public List<AdminUserVO> adminListUsers(int page, String search) {
        Page<Account> p = Page.of(page, 10);
        if (search != null && !search.isBlank()) {
            this.page(p, Wrappers.<Account>query()
                    .like("username", search).or()
                    .like("email", search)
                    .orderByDesc("register_time"));
        } else {
            this.page(p, Wrappers.<Account>query().orderByDesc("register_time"));
        }
        return p.getRecords().stream().map(account -> {
            AdminUserVO vo = new AdminUserVO();
            BeanUtils.copyProperties(account, vo);
            return vo;
        }).toList();
    }

    /**
     * 禁用用户
     * @param id 用户ID
     */
    @Override
    public void adminDisableUser(int id) {
        this.update(Wrappers.<Account>update().eq("id", id).set("status", "disabled"));
    }

    /**
     * 启用用户
     * @param id 用户ID
     */
    @Override
    public void adminEnableUser(int id) {
        this.update(Wrappers.<Account>update().eq("id", id).set("status", "active"));
    }

    /**
     * 重置用户密码为默认密码 123456
     * @param id 用户ID
     */
    @Override
    public void adminResetPassword(int id) {
        String encoded = passwordEncoder.encode("123456");
        this.update(Wrappers.<Account>update().eq("id", id).set("password", encoded));
    }
}
