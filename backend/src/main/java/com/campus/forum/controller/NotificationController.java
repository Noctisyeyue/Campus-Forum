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
 * 通知控制器，处理通知列表查询和删除
 */
@RestController
@RequestMapping("/api/notification")
public class NotificationController {

    @Resource
    NotificationService service;

    // 获取当前用户的通知列表
    @GetMapping("/list")
    public RestBean<List<NotificationVO>> listNotification(@RequestAttribute(Const.ATTR_USER_ID) int id) {
        return RestBean.success(service.findUserNotification(id));
    }

    // 删除指定通知
    @GetMapping("/delete")
    public RestBean<Void> deleteNotification(@RequestParam @Min(0) int id,
                                             @RequestAttribute(Const.ATTR_USER_ID) int uid) {
        service.deleteUserNotification(id, uid);
        return RestBean.success();
    }

    // 删除当前用户全部通知
    @GetMapping("/delete-all")
    public RestBean<Void> deleteAllNotification(@RequestAttribute(Const.ATTR_USER_ID) int uid) {
        service.deleteUserAllNotification(uid);
        return RestBean.success();
    }
}
