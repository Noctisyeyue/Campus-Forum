package com.campus.forum.controller;

import com.campus.forum.entity.RestBean;
import com.campus.forum.entity.vo.response.NotificationVO;
import com.campus.forum.service.NotificationService;
import com.campus.forum.utils.Const;
import jakarta.annotation.Resource;
import jakarta.validation.constraints.Min;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 通知控制器，处理通知列表查询、标记已读和删除
 */
@RestController
@RequestMapping("/api/notification")
public class NotificationController {

    @Resource
    NotificationService service;

    /**
     * 获取当前用户的全部消息（已读+未读）
     */
    @GetMapping("/list")
    public RestBean<List<NotificationVO>> listNotification(@RequestAttribute(Const.ATTR_USER_ID) int id) {
        return RestBean.success(service.findUserNotification(id));
    }

    /**
     * 获取当前用户的未读消息
     */
    @GetMapping("/unread")
    public RestBean<List<NotificationVO>> listUnreadNotification(@RequestAttribute(Const.ATTR_USER_ID) int id) {
        return RestBean.success(service.findUserUnreadNotification(id));
    }

    /**
     * 将指定消息标记为已读
     *
     * @param id 消息 ID
     */
    @GetMapping("/read")
    public RestBean<Void> readNotification(@RequestParam @Min(0) int id,
                                           @RequestAttribute(Const.ATTR_USER_ID) int uid) {
        service.readNotification(id, uid);
        return RestBean.success();
    }

    /**
     * 将当前用户全部消息标记为已读
     */
    @GetMapping("/read-all")
    public RestBean<Void> readAllNotification(@RequestAttribute(Const.ATTR_USER_ID) int uid) {
        service.readAllNotification(uid);
        return RestBean.success();
    }

    /**
     * 删除指定消息
     *
     * @param id 消息 ID
     */
    @GetMapping("/delete")
    public RestBean<Void> deleteNotification(@RequestParam @Min(0) int id,
                                             @RequestAttribute(Const.ATTR_USER_ID) int uid) {
        service.deleteUserNotification(id, uid);
        return RestBean.success();
    }

    /**
     * 批量删除指定消息
     *
     * @param ids 消息 ID 列表
     */
    @PostMapping("/delete-batch")
    public RestBean<Void> deleteBatchNotification(@RequestBody List<Integer> ids,
                                                  @RequestAttribute(Const.ATTR_USER_ID) int uid) {
        service.deleteBatchNotification(ids, uid);
        return RestBean.success();
    }

    /**
     * 删除当前用户全部消息
     */
    @GetMapping("/delete-all")
    public RestBean<Void> deleteAllNotification(@RequestAttribute(Const.ATTR_USER_ID) int uid) {
        service.deleteUserAllNotification(uid);
        return RestBean.success();
    }
}
