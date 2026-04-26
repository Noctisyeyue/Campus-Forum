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

    @Resource
    TopicService topicService;

    @Resource
    ControllerUtils utils;

    @PostMapping("/publish-activity")
    public RestBean<Void> publishActivity(@Valid @RequestBody PublishActivityVO vo,
                                          @RequestAttribute(Const.ATTR_USER_ID) int adminId) {
        return utils.messageHandle(() -> topicService.publishActivity(adminId, vo));
    }

    @PostMapping("/publish-notice-topic")
    public RestBean<Void> publishNoticeTopic(@Valid @RequestBody PublishNoticeTopicVO vo,
                                             @RequestAttribute(Const.ATTR_USER_ID) int adminId) {
        return utils.messageHandle(() -> topicService.publishNoticeTopic(adminId, vo));
    }

    @GetMapping("/notice")
    public RestBean<ForumNoticeVO> notice() {
        return RestBean.success(topicService.getForumNotice());
    }

    @PostMapping("/notice")
    public RestBean<Void> saveNotice(@Valid @RequestBody ForumNoticeSaveVO vo,
                                     @RequestAttribute(Const.ATTR_USER_ID) int adminId) {
        return utils.messageHandle(() -> topicService.saveForumNotice(adminId, vo));
    }
}
