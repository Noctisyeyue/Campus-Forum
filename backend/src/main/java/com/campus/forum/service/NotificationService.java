package com.campus.forum.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.campus.forum.entity.dto.Notification;
import com.campus.forum.entity.vo.response.NotificationVO;

import java.util.List;

/**
 * 通知服务
 */
public interface NotificationService extends IService<Notification> {
    /** 查询用户的全部消息（已读+未读） */
    List<NotificationVO> findUserNotification(int uid);
    /** 查询用户的未读消息 */
    List<NotificationVO> findUserUnreadNotification(int uid);
    void deleteUserNotification(int id, int uid);
    void deleteUserAllNotification(int uid);
    /** 批量删除指定消息 */
    void deleteBatchNotification(List<Integer> ids, int uid);
    /** 将指定消息标记为已读 */
    void readNotification(int id, int uid);
    /** 将用户全部消息标记为已读 */
    void readAllNotification(int uid);
    void addNotification(int uid, String title, String content, String type, String url);
}
