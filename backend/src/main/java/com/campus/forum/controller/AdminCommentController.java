package com.campus.forum.controller;

import com.campus.forum.entity.RestBean;
import com.campus.forum.entity.vo.response.AdminCommentVO;
import com.campus.forum.entity.vo.response.PageResult;
import com.campus.forum.service.TopicService;
import com.campus.forum.utils.ControllerUtils;
import jakarta.annotation.Resource;
import jakarta.validation.constraints.Min;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 管理员评论管理控制器，提供评论列表查询和删除功能
 */
@RestController
@RequestMapping("/api/admin/comments")
public class AdminCommentController {

    @Resource
    TopicService topicService;

    @Resource
    ControllerUtils utils;

    /**
     * 分页查询评论列表（支持按状态/内容/用户名/帖子标题筛选）
     * @param page 页码（从0开始）
     * @param pageSize 每页条数（默认15）
     * @param status 评论状态（可选）
     * @param content 评论内容关键词（可选）
     * @param author 用户名（可选）
     * @param topicTitle 帖子标题（可选）
     * @return 评论列表
     */
    @GetMapping
    public RestBean<PageResult<AdminCommentVO>> listComments(@RequestParam(defaultValue = "0") @Min(0) int page,
                                                        @RequestParam(defaultValue = "15") @Min(1) int pageSize,
                                                        @RequestParam(required = false) String status,
                                                        @RequestParam(required = false) String content,
                                                        @RequestParam(required = false) String author,
                                                        @RequestParam(required = false) String topicTitle,
                                                        @RequestParam(required = false) Integer tid) {
        return RestBean.success(topicService.adminListComments(page + 1, pageSize, status, content, author, topicTitle, tid));
    }

    /**
     * 删除评论（物理删除，级联清理关联举报）
     * @param id 评论ID
     * @return 操作结果
     */
    @PostMapping("/{id}/delete")
    public RestBean<Void> deleteComment(@PathVariable int id) {
        return utils.messageHandle(() -> topicService.adminDeleteComment(id));
    }
}
