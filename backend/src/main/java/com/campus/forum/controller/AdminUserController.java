package com.campus.forum.controller;

import com.campus.forum.entity.RestBean;
import com.campus.forum.entity.dto.Account;
import com.campus.forum.entity.vo.response.AccountVO;
import com.campus.forum.entity.vo.response.AdminUserVO;
import com.campus.forum.entity.vo.response.PageResult;
import com.campus.forum.service.AccountService;
import com.campus.forum.utils.Const;
import jakarta.annotation.Resource;
import jakarta.validation.constraints.Min;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 管理员用户管理控制器，提供用户列表、禁用/启用、重置密码、角色提升/降级等功能
 */
@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    @Resource
    AccountService accountService;

    /**
     * 分页查询用户列表，支持按用户名或邮箱搜索
     * @param page 页码（从0开始）
     * @param pageSize 每页条数（默认15）
     * @param search 搜索关键词（可选）
     * @param status 用户状态（可选）
     * @return 用户列表
     */
    @GetMapping
    public RestBean<PageResult<AdminUserVO>> listUsers(@RequestParam(defaultValue = "0") @Min(0) int page,
                                                  @RequestParam(defaultValue = "15") @Min(1) int pageSize,
                                                  @RequestParam(required = false) String search,
                                                  @RequestParam(required = false) String status) {
        return RestBean.success(accountService.adminListUsers(page + 1, pageSize, search, status));
    }

    /**
     * 获取指定用户详情
     * @param id 用户ID
     * @return 用户信息
     */
    @GetMapping("/{id}")
    public RestBean<AccountVO> getUser(@PathVariable int id) {
        Account account = accountService.findAccountById(id);
        if (account == null) return RestBean.failure(404, "用户不存在");
        return RestBean.success(account.asViewObject(AccountVO.class));
    }

    /**
     * 禁用用户
     * @param id 用户ID
     * @param operatorId 操作者用户ID
     * @return 操作结果
     */
    @PostMapping("/{id}/disable")
    public RestBean<Void> disableUser(@PathVariable int id,
                                       @RequestAttribute(Const.ATTR_USER_ID) int operatorId) {
        String error = accountService.adminDisableUser(operatorId, id);
        if (error != null) return RestBean.failure(400, error);
        return RestBean.success();
    }

    /**
     * 启用用户
     * @param id 用户ID
     * @param operatorId 操作者用户ID
     * @return 操作结果
     */
    @PostMapping("/{id}/enable")
    public RestBean<Void> enableUser(@PathVariable int id,
                                      @RequestAttribute(Const.ATTR_USER_ID) int operatorId) {
        String error = accountService.adminEnableUser(operatorId, id);
        if (error != null) return RestBean.failure(400, error);
        return RestBean.success();
    }

    /**
     * 重置用户密码为默认密码 123456
     * @param id 用户ID
     * @param operatorId 操作者用户ID
     * @return 操作结果
     */
    @PostMapping("/{id}/reset-password")
    public RestBean<Void> resetPassword(@PathVariable int id,
                                         @RequestAttribute(Const.ATTR_USER_ID) int operatorId) {
        String error = accountService.adminResetPassword(operatorId, id);
        if (error != null) return RestBean.failure(400, error);
        return RestBean.success();
    }

    /**
     * 超级管理员提升普通用户为管理员
     * @param id 用户ID
     * @param operatorId 操作者用户ID
     * @return 操作结果
     */
    @PostMapping("/{id}/promote")
    public RestBean<Void> promoteUser(@PathVariable int id,
                                       @RequestAttribute(Const.ATTR_USER_ID) int operatorId) {
        String error = accountService.adminPromoteUser(operatorId, id);
        if (error != null) return RestBean.failure(400, error);
        return RestBean.success();
    }

    /**
     * 超级管理员将管理员降级为普通用户
     * @param id 用户ID
     * @param operatorId 操作者用户ID
     * @return 操作结果
     */
    @PostMapping("/{id}/demote")
    public RestBean<Void> demoteUser(@PathVariable int id,
                                      @RequestAttribute(Const.ATTR_USER_ID) int operatorId) {
        String error = accountService.adminDemoteUser(operatorId, id);
        if (error != null) return RestBean.failure(400, error);
        return RestBean.success();
    }
}
