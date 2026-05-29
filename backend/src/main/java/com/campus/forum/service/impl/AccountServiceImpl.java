package com.campus.forum.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.forum.entity.dto.Account;
import com.campus.forum.entity.dto.AccountDetails;
import com.campus.forum.entity.dto.AccountPrivacy;
import com.campus.forum.entity.vo.request.*;
import com.campus.forum.entity.vo.response.AdminUserVO;
import com.campus.forum.entity.vo.response.PageResult;
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

    /** 验证邮件发送冷却时间（秒） */
    @Value("${spring.web.verify.mail-limit}")
    int verifyLimit;

    /** RabbitMQ 消息模板，用于异步发送邮件 */
    @Resource
    AmqpTemplate rabbitTemplate;

    /** Redis 操作模板，用于缓存验证码 */
    @Resource
    StringRedisTemplate stringRedisTemplate;

    /** 密码加密器 */
    @Resource
    PasswordEncoder passwordEncoder;

    /** 用户隐私设置 Mapper */
    @Resource
    AccountPrivacyMapper privacyMapper;

    /** 用户详情 Mapper */
    @Resource
    AccountDetailsMapper detailsMapper;

    /** 限流工具 */
    @Resource
    FlowUtils flow;

    /**
     * Spring Security 登录认证：通过用户名或邮箱查找用户并构建 UserDetails
     *
     * @param username 用户名或邮箱
     * @return Spring Security 用户对象
     * @throws UsernameNotFoundException 用户不存在时抛出
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
     *
     * @param type    业务类型（register / reset）
     * @param email   目标邮箱
     * @param address 请求方 IP 地址，用于限流
     * @return null 表示发送成功，否则返回错误信息
     */
    @Override
    public String registerEmailVerifyCode(String type, String email, String address) {
        // intern() 相同内容的字符串返回同一个对象
        // 同一个 IP 的请求排队执行，防止重复发送验证码；不同 IP 互不影响。
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
     * 邮箱注册账号，验证码校验通过后创建账户及关联的隐私、详情记录
     *
     * @param info 注册信息（用户名、密码、邮箱、验证码）
     * @return null 表示注册成功，否则返回错误信息
     */
    @Override
    public String registerEmailAccount(EmailRegisterVO info) {
        String email = info.getEmail();
        String code = this.getEmailVerifyCode(email);
        if (code == null) return "请先获取验证码";
        if (!code.equals(info.getCode())) return "验证码错误，请重新输入";
        if (this.existsAccountByEmail(email)) return "该邮件地址已被注册";
        String username = info.getUsername();
        if (this.existsAccountByUsername(username)) return "该用户名已被他人使用，请重新更换";
        String password = passwordEncoder.encode(info.getPassword());
        Account account = new Account(null,
                                         info.getUsername(),
                                         password,
                                         email,
                                         Const.ROLE_DEFAULT,
                                   "active",
                                   null,
                                         new Date());
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
     * 邮箱验证码重置密码，验证通过后更新数据库中的密码
     *
     * @param info 重置信息（邮箱、新密码、验证码）
     * @return null 表示重置成功，否则返回错误信息
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
     * 验证邮箱重置验证码是否正确
     *
     * @param info 验证信息（邮箱、验证码）
     * @return null 表示验证通过，否则返回错误信息
     */
    @Override
    public String resetConfirm(ConfirmResetVO info) {
        String email = info.getEmail();
        String code = this.getEmailVerifyCode(email);
        if (code == null) return "请先获取验证码";
        if (!code.equals(info.getCode())) return "验证码错误，请重新输入";
        return null;
    }

    /**
     * 修改用户邮箱，验证码校验通过后更新数据库
     *
     * @param id 用户 ID
     * @param vo 新邮箱及验证码
     * @return null 表示修改成功，否则返回错误信息
     */
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

    /**
     * 修改用户密码，校验旧密码后更新为新密码
     *
     * @param id 用户 ID
     * @param vo 旧密码与新密码
     * @return null 表示修改成功，否则返回错误信息
     */
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

    /**
     * 删除 Redis 中的邮件验证码
     *
     * @param email 邮箱地址
     */
    private void deleteEmailVerifyCode(String email) {
        String key = Const.VERIFY_EMAIL_DATA + email;
        stringRedisTemplate.delete(key);
    }

    /**
     * 获取 Redis 中缓存的邮件验证码
     *
     * @param email 邮箱地址
     * @return 验证码字符串，已过期或不存在返回 null
     */
    private String getEmailVerifyCode(String email) {
        String key = Const.VERIFY_EMAIL_DATA + email;
        return stringRedisTemplate.opsForValue().get(key);
    }

    /**
     * IP 级别邮件验证码发送限流
     *
     * @param address 请求方 IP 地址
     * @return true=允许发送，false=触发限流
     */
    private boolean verifyLimit(String address) {
        String key = Const.VERIFY_EMAIL_LIMIT + address;
        return flow.limitOnceCheck(key, verifyLimit);
    }

    /**
     * 通过用户名或邮箱查找用户
     *
     * @param text 用户名或邮箱
     * @return 匹配的账户，未找到返回 null
     */
    @Override
    public Account findAccountByNameOrEmail(String text) {
        return this.query()
                .eq("username", text).or()
                .eq("email", text)
                .one();
    }

    /**
     * 根据用户 ID 查询账户
     *
     * @param id 用户 ID
     * @return 匹配的账户，未找到返回 null
     */
    @Override
    public Account findAccountById(int id) {
        return this.query().eq("id", id).one();
    }

    /**
     * 检查邮箱是否已被注册
     *
     * @param email 邮箱地址
     * @return true=已存在
     */
    private boolean existsAccountByEmail(String email) {
        return this.baseMapper.exists(Wrappers.<Account>query().eq("email", email));
    }

    /**
     * 检查用户名是否已存在
     *
     * @param username 用户名
     * @return true=已存在
     */
    private boolean existsAccountByUsername(String username) {
        return this.baseMapper.exists(Wrappers.<Account>query().eq("username", username));
    }

    /**
     * 管理员分页查询用户列表，支持按用户名或邮箱搜索
     *
     * @param page   页码
     * @param search 搜索关键词，为空时返回全部
     * @param status 用户状态筛选，为空时返回全部
     * @return 用户列表
     */
    @Override
    public PageResult<AdminUserVO> adminListUsers(int page, int pageSize, String search, String status) {
        Page<Account> p = Page.of(page, pageSize);
        var wrapper = Wrappers.<Account>query();
        if (search != null && !search.isBlank()) {
            wrapper.and(query -> query.like("username", search).or().like("email", search));
        }
        if (status != null && !status.isBlank()) {
            wrapper.eq("status", status);
        }
        wrapper.orderByDesc("register_time");
        this.page(p, wrapper);
        List<AdminUserVO> list = p.getRecords().stream().map(account -> {
            AdminUserVO vo = new AdminUserVO();
            BeanUtils.copyProperties(account, vo);
            return vo;
        }).toList();
        return new PageResult<>(list, p.getTotal());
    }

    /**
     * 管理员禁用用户，将 status 设为 disabled
     *
     * @param id 用户 ID
     */
    @Override
    public void adminDisableUser(int id) {
        this.update(Wrappers.<Account>update().eq("id", id).set("status", "disabled"));
    }

    /**
     * 管理员启用用户，将 status 设为 active
     *
     * @param id 用户 ID
     */
    @Override
    public void adminEnableUser(int id) {
        this.update(Wrappers.<Account>update().eq("id", id).set("status", "active"));
    }

    /**
     * 管理员重置用户密码为默认密码 123456
     *
     * @param id 用户 ID
     */
    @Override
    public void adminResetPassword(int id) {
        String encoded = passwordEncoder.encode("123456");
        this.update(Wrappers.<Account>update().eq("id", id).set("password", encoded));
    }
}
