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
     * 分页查询评论列表（支持按帖子/用户筛选）
     * @param page 页码（从0开始）
     * @param tid 帖子ID（可选）
     * @param uid 用户ID（可选）
     * @return 评论列表
     */
    @GetMapping
    public RestBean<List<AdminCommentVO>> listComments(@RequestParam(defaultValue = "0") @Min(0) int page,
                                                        @RequestParam(required = false) Integer tid,
                                                        @RequestParam(required = false) Integer uid) {
        return RestBean.success(topicService.adminListComments(page + 1, tid, uid));
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
