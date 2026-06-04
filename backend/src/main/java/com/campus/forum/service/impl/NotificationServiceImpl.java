package com.campus.forum.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.forum.entity.dto.Notification;
import com.campus.forum.entity.vo.response.NotificationVO;
import com.campus.forum.mapper.NotificationMapper;
import com.campus.forum.service.NotificationService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 通知服务实现
 */
@Service
public class NotificationServiceImpl extends ServiceImpl<NotificationMapper, Notification> implements NotificationService {

    /**
     * 查询指定用户的所有通知，按时间倒序排列
     *
     * @param uid 用户ID
     * @return 该用户的所有通知列表
     */
    @Override
    public List<NotificationVO> findUserNotification(int uid) {
        return this.list(Wrappers.<Notification>query().eq("uid", uid).orderByDesc("time"))
                .stream()
                .map(notification -> notification.asViewObject(NotificationVO.class))
                .toList();
    }

    /**
     * 查询指定用户的未读通知，按时间倒序排列
     *
     * @param uid 用户ID
     * @return 该用户的未读通知列表
     */
    @Override
    public List<NotificationVO> findUserUnreadNotification(int uid) {
        return this.list(Wrappers.<Notification>query().eq("uid", uid).eq("status", "unread").orderByDesc("time"))
                .stream()
                .map(notification -> notification.asViewObject(NotificationVO.class))
                .toList();
    }

    /**
     * 删除指定用户的单条通知
     *
     * @param id 通知ID
     * @param uid 用户ID，用于权限校验
     */
    @Override
    public void deleteUserNotification(int id, int uid) {
        this.remove(Wrappers.<Notification>query().eq("id", id).eq("uid", uid));
    }

    /**
     * 删除指定用户的所有通知
     *
     * @param uid 用户ID
     */
    @Override
    public void deleteUserAllNotification(int uid) {
        this.remove(Wrappers.<Notification>query().eq("uid", uid));
    }

    /**
     * 批量删除指定用户的多条通知
     *
     * @param ids 要删除的通知ID列表
     * @param uid 用户ID，用于权限校验
     */
    @Override
    public void deleteBatchNotification(List<Integer> ids, int uid) {
        this.remove(Wrappers.<Notification>query().eq("uid", uid).in("id", ids));
    }

    /**
     * 将指定用户的单条通知标记为已读
     *
     * @param id 通知ID
     * @param uid 用户ID，用于权限校验
     */
    @Override
    public void readNotification(int id, int uid) {
        this.update(Wrappers.<Notification>update().eq("id", id).eq("uid", uid).set("status", "read"));
    }

    /**
     * 将指定用户的所有未读通知标记为已读
     *
     * @param uid 用户ID
     */
    @Override
    public void readAllNotification(int uid) {
        this.update(Wrappers.<Notification>update().eq("uid", uid).eq("status", "unread").set("status", "read"));
    }

    /**
     * 添加一条新通知
     *
     * @param uid     接收通知的用户ID
     * @param title   通知标题
     * @param content 通知内容
     * @param type    通知类型
     * @param url     通知关联的链接地址
     */
    @Override
    public void addNotification(int uid, String title, String content, String type, String url) {
        Notification notification = new Notification();
        notification.setUid(uid);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setType(type);
        notification.setUrl(url);
        notification.setTime(new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date()));
        this.save(notification);
    }
}
