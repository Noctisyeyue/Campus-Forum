package com.campus.forum.controller;

import com.campus.forum.entity.RestBean;
import com.campus.forum.entity.dto.Account;
import com.campus.forum.entity.dto.AccountDetails;
import com.campus.forum.entity.vo.request.ChangePasswordVO;
import com.campus.forum.entity.vo.request.DetailsSaveVO;
import com.campus.forum.entity.vo.request.ModifyEmailVO;
import com.campus.forum.entity.vo.request.PrivacySaveVO;
import com.campus.forum.entity.vo.response.AccountDetailsVO;
import com.campus.forum.entity.vo.response.AccountPrivacyVO;
import com.campus.forum.entity.vo.response.AccountVO;
import com.campus.forum.service.AccountDetailsService;
import com.campus.forum.service.AccountPrivacyService;
import com.campus.forum.service.AccountService;
import com.campus.forum.utils.Const;
import com.campus.forum.utils.ControllerUtils;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

/**
 * 用户账户信息控制器，处理个人资料、密码、邮箱、隐私设置
 */
@RestController
@RequestMapping("/api/user")
public class AccountController {

    /** 用户账户服务 */
    @Resource
    AccountService service;

    /** 用户详情服务 */
    @Resource
    AccountDetailsService detailsService;

    /** 用户隐私设置服务 */
    @Resource
    AccountPrivacyService privacyService;

    /** 通用控制器工具 */
    @Resource
    ControllerUtils utils;

    /**
     * 获取当前用户基本信息
     *
     * @param id 当前用户ID
     * @return 用户基本信息
     */
    @GetMapping("/info")
    public RestBean<AccountVO> info(@RequestAttribute(Const.ATTR_USER_ID) int id){
        Account account = service.findAccountById(id);
        return RestBean.success(account.asViewObject(AccountVO.class));
    }

    /**
     * 获取当前用户详情（不存在则返回空对象）
     *
     * @param id 当前用户ID
     * @return 用户详情
     */
    @GetMapping("/details")
    public RestBean<AccountDetailsVO> details(@RequestAttribute(Const.ATTR_USER_ID) int id){
        AccountDetails details = Optional
                .ofNullable(detailsService.findAccountDetailsById(id))
                .orElseGet(AccountDetails::new);
        return RestBean.success(details.asViewObject(AccountDetailsVO.class));
    }

    /**
     * 保存用户详情信息
     *
     * @param id 当前用户ID
     * @param vo 详情保存参数
     * @return 操作结果
     */
    @PostMapping("/save-details")
    public RestBean<Void> saveDetails(@RequestAttribute(Const.ATTR_USER_ID) int id,
                                      @RequestBody @Valid DetailsSaveVO vo){
        boolean success = detailsService.saveAccountDetails(id, vo);
        return success ? RestBean.success() : RestBean.failure(400, "此用户名已被其他用户使用，请重新更换！");
    }

    /**
     * 修改绑定邮箱
     *
     * @param id 当前用户ID
     * @param vo 邮箱修改参数
     * @return 操作结果
     */
    @PostMapping("/modify-email")
    public RestBean<Void> modifyEmail(@RequestAttribute(Const.ATTR_USER_ID) int id,
                                      @RequestBody @Valid ModifyEmailVO vo){
        return utils.messageHandle(() -> service.modifyEmail(id, vo));
    }

    /**
     * 修改密码
     *
     * @param id 当前用户ID
     * @param vo 密码修改参数
     * @return 操作结果
     */
    @PostMapping("/change-password")
    public RestBean<Void> changePassword(@RequestAttribute(Const.ATTR_USER_ID) int id,
                                         @RequestBody @Valid ChangePasswordVO vo){
        return utils.messageHandle(() -> service.changePassword(id, vo));
    }

    /**
     * 保存隐私设置
     *
     * @param id 当前用户ID
     * @param vo 隐私设置参数
     * @return 操作结果
     */
    @PostMapping("/save-privacy")
    public RestBean<Void> savePrivacy(@RequestAttribute(Const.ATTR_USER_ID) int id,
                                      @RequestBody @Valid PrivacySaveVO vo){
        privacyService.savePrivacy(id, vo);
        return RestBean.success();
    }

    /**
     * 获取隐私设置
     *
     * @param id 当前用户ID
     * @return 隐私设置信息
     */
    @GetMapping("/privacy")
    public RestBean<AccountPrivacyVO> privacy(@RequestAttribute(Const.ATTR_USER_ID) int id){
        return RestBean.success(privacyService.accountPrivacy(id).asViewObject(AccountPrivacyVO.class));
    }
}
