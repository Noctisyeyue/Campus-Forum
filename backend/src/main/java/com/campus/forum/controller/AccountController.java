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

    @Resource
    AccountService service;

    @Resource
    AccountDetailsService detailsService;

    @Resource
    AccountPrivacyService privacyService;

    @Resource
    ControllerUtils utils;

    // 获取当前用户基本信息
    @GetMapping("/info")
    public RestBean<AccountVO> info(@RequestAttribute(Const.ATTR_USER_ID) int id){
        Account account = service.findAccountById(id);
        return RestBean.success(account.asViewObject(AccountVO.class));
    }

    // 获取当前用户详情（不存在则返回空对象）
    @GetMapping("/details")
    public RestBean<AccountDetailsVO> details(@RequestAttribute(Const.ATTR_USER_ID) int id){
        AccountDetails details = Optional
                .ofNullable(detailsService.findAccountDetailsById(id))
                .orElseGet(AccountDetails::new);
        return RestBean.success(details.asViewObject(AccountDetailsVO.class));
    }

    // 保存用户详情信息
    @PostMapping("/save-details")
    public RestBean<Void> saveDetails(@RequestAttribute(Const.ATTR_USER_ID) int id,
                                      @RequestBody @Valid DetailsSaveVO vo){
        boolean success = detailsService.saveAccountDetails(id, vo);
        return success ? RestBean.success() : RestBean.failure(400, "此用户名已被其他用户使用，请重新更换！");
    }

    // 修改绑定邮箱
    @PostMapping("/modify-email")
    public RestBean<Void> modifyEmail(@RequestAttribute(Const.ATTR_USER_ID) int id,
                                      @RequestBody @Valid ModifyEmailVO vo){
        return utils.messageHandle(() -> service.modifyEmail(id, vo));
    }

    // 修改密码
    @PostMapping("/change-password")
    public RestBean<Void> changePassword(@RequestAttribute(Const.ATTR_USER_ID) int id,
                                         @RequestBody @Valid ChangePasswordVO vo){
        return utils.messageHandle(() -> service.changePassword(id, vo));
    }

    // 保存隐私设置
    @PostMapping("/save-privacy")
    public RestBean<Void> savePrivacy(@RequestAttribute(Const.ATTR_USER_ID) int id,
                                      @RequestBody @Valid PrivacySaveVO vo){
        privacyService.savePrivacy(id, vo);
        return RestBean.success();
    }

    // 获取隐私设置
    @GetMapping("/privacy")
    public RestBean<AccountPrivacyVO> privacy(@RequestAttribute(Const.ATTR_USER_ID) int id){
        return RestBean.success(privacyService.accountPrivacy(id).asViewObject(AccountPrivacyVO.class));
    }
}
