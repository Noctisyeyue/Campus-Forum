package com.campus.forum.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.campus.forum.entity.dto.Notification;
import com.campus.forum.entity.vo.response.NotificationVO;

import java.util.List;

/**
 * 通知服务
 */
public interface NotificationService extends IService<Notification> {

    /**
     * 查询用户的全部消息（已读+未读）
     *
     * @param uid 用户ID
     * @return 消息列表
     */
    List<NotificationVO> findUserNotification(int uid);

    /**
     * 查询用户的未读消息
     *
     * @param uid 用户ID
     * @return 未读消息列表
     */
    List<NotificationVO> findUserUnreadNotification(int uid);

    /**
     * 删除指定消息
     *
     * @param id  消息ID
     * @param uid 用户ID
     */
    void deleteUserNotification(int id, int uid);

    /**
     * 删除用户全部消息
     *
     * @param uid 用户ID
     */
    void deleteUserAllNotification(int uid);

    /**
     * 批量删除指定消息
     *
     * @param ids 消息ID列表
     * @param uid 用户ID
     */
    void deleteBatchNotification(List<Integer> ids, int uid);

    /**
     * 将指定消息标记为已读
     *
     * @param id  消息ID
     * @param uid 用户ID
     */
    void readNotification(int id, int uid);

    /**
     * 将用户全部消息标记为已读
     *
     * @param uid 用户ID
     */
    void readAllNotification(int uid);

    /**
     * 添加一条通知消息
     *
     * @param uid     目标用户ID
     * @param title   通知标题
     * @param content 通知内容
     * @param type    通知类型（success/info/warning）
     * @param url     关联链接（可选）
     */
    void addNotification(int uid, String title, String content, String type, String url);
}
