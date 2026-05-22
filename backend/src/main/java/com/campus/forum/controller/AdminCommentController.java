package com.campus.forum.controller;

import com.campus.forum.entity.RestBean;
import com.campus.forum.entity.vo.response.AdminCommentVO;
import com.campus.forum.service.TopicService;
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

    /**
     * 分页查询评论列表（支持按状态/内容/用户名/帖子标题筛选）
     * @param page 页码（从0开始）
     * @param status 评论状态（可选）
     * @param content 评论内容关键词（可选）
     * @param author 用户名（可选）
     * @param topicTitle 帖子标题（可选）
     * @return 评论列表
     */
    @GetMapping
    public RestBean<List<AdminCommentVO>> listComments(@RequestParam(defaultValue = "0") @Min(0) int page,
                                                        @RequestParam(required = false) String status,
                                                        @RequestParam(required = false) String content,
                                                        @RequestParam(required = false) String author,
                                                        @RequestParam(required = false) String topicTitle) {
        return RestBean.success(topicService.adminListComments(page + 1, status, content, author, topicTitle));
    }

    /**
     * 删除评论（软删除）
     * @param id 评论ID
     * @return 操作结果
     */
    @PostMapping("/{id}/delete")
    public RestBean<Void> deleteComment(@PathVariable int id) {
        topicService.adminDeleteComment(id);
        return RestBean.success();
    }
}
