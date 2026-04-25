package com.campus.forum.controller;

import com.campus.forum.entity.RestBean;
import com.campus.forum.entity.vo.response.AdminTopicVO;
import com.campus.forum.entity.vo.response.TopicDetailVO;
import com.campus.forum.service.TopicService;
import com.campus.forum.utils.Const;
import jakarta.annotation.Resource;
import jakarta.validation.constraints.Min;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 管理员帖子管理控制器，提供帖子审核、隐藏、恢复、删除、置顶等功能
 */
@RestController
@RequestMapping("/api/admin/topics")
public class AdminTopicController {

    @Resource
    TopicService topicService;

    /**
     * 分页查询帖子列表（支持多条件筛选）
     * @param page 页码（从0开始）
     * @param status 帖子状态（可选）
     * @param type 分类ID（可选）
     * @param title 标题关键词（可选）
     * @param author 作者用户名（可选）
     * @return 帖子列表
     */
    @GetMapping
    public RestBean<List<AdminTopicVO>> listTopics(@RequestParam(defaultValue = "0") @Min(0) int page,
                                                    @RequestParam(required = false) String status,
                                                    @RequestParam(required = false) Integer type,
                                                    @RequestParam(required = false) String title,
                                                    @RequestParam(required = false) String author) {
        return RestBean.success(topicService.adminListTopics(page + 1, status, type, title, author));
    }

    /**
     * 获取帖子详情（管理员可查看任何状态的帖子）
     * @param id 帖子ID
     * @return 帖子详情
     */
    @GetMapping("/{id}")
    public RestBean<TopicDetailVO> getTopic(@PathVariable int id) {
        return RestBean.success(topicService.getTopic(id, 0));
    }

    /**
     * 审核通过帖子
     * @param id 帖子ID
     * @param adminId 当前管理员ID（从请求属性获取）
     * @return 操作结果
     */
    @PostMapping("/{id}/approve")
    public RestBean<Void> approveTopic(@PathVariable int id,
                                       @RequestAttribute(Const.ATTR_USER_ID) int adminId) {
        topicService.adminApproveTopic(id, adminId);
        return RestBean.success();
    }

    /**
     * 审核拒绝帖子
     * @param id 帖子ID
     * @param reason 拒绝理由（可选）
     * @param adminId 当前管理员ID
     * @return 操作结果
     */
    @PostMapping("/{id}/reject")
    public RestBean<Void> rejectTopic(@PathVariable int id,
                                      @RequestParam(required = false) String reason,
                                      @RequestAttribute(Const.ATTR_USER_ID) int adminId) {
        topicService.adminRejectTopic(id, adminId, reason);
        return RestBean.success();
    }

    /**
     * 下架帖子（需填写原因）
     * @param id 帖子ID
     * @param reason 下架原因
     * @return 操作结果
     */
    @PostMapping("/{id}/hide")
    public RestBean<Void> hideTopic(@PathVariable int id,
                                    @RequestParam String reason) {
        topicService.adminHideTopic(id, reason);
        return RestBean.success();
    }

    /**
     * 上架帖子（恢复已下架帖子）
     * @param id 帖子ID
     * @return 操作结果
     */
    @PostMapping("/{id}/restore")
    public RestBean<Void> restoreTopic(@PathVariable int id) {
        topicService.adminRestoreTopic(id);
        return RestBean.success();
    }

    /**
     * 删除帖子（物理删除，不可逆）
     * @param id 帖子ID
     * @param adminId 当前管理员ID
     * @return 操作结果
     */
    @PostMapping("/{id}/delete")
    public RestBean<Void> deleteTopic(@PathVariable int id,
                                      @RequestAttribute(Const.ATTR_USER_ID) int adminId) {
        topicService.adminDeleteTopic(id, adminId);
        return RestBean.success();
    }

    /**
     * 置顶帖子
     * @param id 帖子ID
     * @return 操作结果
     */
    @PostMapping("/{id}/top")
    public RestBean<Void> topTopic(@PathVariable int id) {
        topicService.adminTopTopic(id);
        return RestBean.success();
    }

    /**
     * 取消置顶
     * @param id 帖子ID
     * @return 操作结果
     */
    @PostMapping("/{id}/untop")
    public RestBean<Void> untopTopic(@PathVariable int id) {
        topicService.adminUntopTopic(id);
        return RestBean.success();
    }
}
