package com.campus.forum.controller;

import com.campus.forum.entity.RestBean;
import com.campus.forum.entity.vo.request.ForumNoticeSaveVO;
import com.campus.forum.entity.vo.request.PublishActivityVO;
import com.campus.forum.entity.vo.request.PublishNoticeTopicVO;
import com.campus.forum.entity.vo.response.ForumNoticeVO;
import com.campus.forum.service.TopicService;
import com.campus.forum.utils.Const;
import com.campus.forum.utils.ControllerUtils;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理员论坛扩展控制器
 */
@RestController
@RequestMapping("/api/admin/forum")
public class AdminForumController {

    /** 帖子服务 */
    @Resource
    TopicService topicService;

    /** 通用控制器工具 */
    @Resource
    ControllerUtils utils;

    /**
     * 发布校园活动帖子
     *
     * @param vo       活动发布参数
     * @param adminId  操作管理员ID
     * @return 操作结果
     */
    @PostMapping("/publish-activity")
    public RestBean<Void> publishActivity(@Valid @RequestBody PublishActivityVO vo,
                                          @RequestAttribute(Const.ATTR_USER_ID) int adminId) {
        return utils.messageHandle(() -> topicService.publishActivity(adminId, vo));
    }

    /**
     * 发布教务通知帖子
     *
     * @param vo       通知发布参数
     * @param adminId  操作管理员ID
     * @return 操作结果
     */
    @PostMapping("/publish-notice-topic")
    public RestBean<Void> publishNoticeTopic(@Valid @RequestBody PublishNoticeTopicVO vo,
                                             @RequestAttribute(Const.ATTR_USER_ID) int adminId) {
        return utils.messageHandle(() -> topicService.publishNoticeTopic(adminId, vo));
    }

    /**
     * 获取论坛公告
     *
     * @return 论坛公告内容
     */
    @GetMapping("/notice")
    public RestBean<ForumNoticeVO> notice() {
        return RestBean.success(topicService.getForumNotice());
    }

    /**
     * 保存论坛公告
     *
     * @param vo       公告保存参数
     * @param adminId  操作管理员ID
     * @return 操作结果
     */
    @PostMapping("/notice")
    public RestBean<Void> saveNotice(@Valid @RequestBody ForumNoticeSaveVO vo,
                                     @RequestAttribute(Const.ATTR_USER_ID) int adminId) {
        return utils.messageHandle(() -> topicService.saveForumNotice(adminId, vo));
    }
}
