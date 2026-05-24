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

    @Override
    public List<NotificationVO> findUserNotification(int uid) {
        return this.list(Wrappers.<Notification>query().eq("uid", uid).orderByDesc("time"))
                .stream()
                .map(notification -> notification.asViewObject(NotificationVO.class))
                .toList();
    }

    @Override
    public List<NotificationVO> findUserUnreadNotification(int uid) {
        return this.list(Wrappers.<Notification>query().eq("uid", uid).eq("status", "unread").orderByDesc("time"))
                .stream()
                .map(notification -> notification.asViewObject(NotificationVO.class))
                .toList();
    }

    @Override
    public void deleteUserNotification(int id, int uid) {
        this.remove(Wrappers.<Notification>query().eq("id", id).eq("uid", uid));
    }

    @Override
    public void deleteUserAllNotification(int uid) {
        this.remove(Wrappers.<Notification>query().eq("uid", uid));
    }

    @Override
    public void deleteBatchNotification(List<Integer> ids, int uid) {
        this.remove(Wrappers.<Notification>query().eq("uid", uid).in("id", ids));
    }

    @Override
    public void readNotification(int id, int uid) {
        this.update(Wrappers.<Notification>update().eq("id", id).eq("uid", uid).set("status", "read"));
    }

    @Override
    public void readAllNotification(int uid) {
        this.update(Wrappers.<Notification>update().eq("uid", uid).eq("status", "unread").set("status", "read"));
    }

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
