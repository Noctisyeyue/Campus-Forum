package com.campus.forum.controller;

import com.campus.forum.entity.RestBean;
import com.campus.forum.entity.dto.Interact;
import com.campus.forum.entity.vo.request.AddCommentVO;
import com.campus.forum.entity.vo.request.TopicCreateVO;
import com.campus.forum.entity.vo.request.TopicUpdateVO;
import com.campus.forum.entity.vo.response.*;
import com.campus.forum.service.TopicService;
import com.campus.forum.service.WeatherService;
import com.campus.forum.utils.Const;
import com.campus.forum.utils.ControllerUtils;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;

/**
 * 论坛业务控制器，处理帖子、评论、天气、互动等请求
 */
@RestController
@RequestMapping("/api/forum")
public class ForumController {

    @Resource
    WeatherService service;

    @Resource
    TopicService topicService;

    @Resource
    ControllerUtils utils;

    // 获取天气信息（失败时静默降级）
    @GetMapping("/weather")
    public RestBean<WeatherVO> weather(double longitude, double latitude){
        WeatherVO vo = service.fetchWeather(longitude, latitude);
        return vo == null ?
                RestBean.failure(400, "天气服务暂时不可用") : RestBean.success(vo);
    }

    // 获取帖子分类列表
    @GetMapping("/types")
    public RestBean<List<TopicTypeVO>> listTypes(){
        return RestBean.success(topicService
                .listTypes()
                .stream()
                .map(type -> type.asViewObject(TopicTypeVO.class))
                .toList());
    }

    // 创建帖子（提交后进入待审核状态）
    @PostMapping("/create-topic")
    public RestBean<Void> createTopic(@Valid @RequestBody TopicCreateVO vo,
                                      @RequestAttribute(Const.ATTR_USER_ID) int id) {
        return utils.messageHandle(() -> topicService.createTopic(id, vo));
    }

    // 分页查询帖子列表（仅已发布）
    @GetMapping("/list-topic")
    public RestBean<List<TopicPreviewVO>> listTopic(@RequestParam @Min(0) int page,
                                                    @RequestParam @Min(0) int type) {
        return RestBean.success(topicService.listTopicByPage(page + 1, type));
    }

    // 获取置顶帖子列表
    @GetMapping("/top-topic")
    public RestBean<List<TopicTopVO>> topTopic(){
        return RestBean.success(topicService.listTopTopics());
    }

    // 获取帖子详情
    @GetMapping("/topic")
    public RestBean<TopicDetailVO> topic(@RequestParam @Min(0) int tid,
                                         @RequestAttribute(Const.ATTR_USER_ID) int id){
        return RestBean.success(topicService.getTopic(tid, id));
    }

    // 点赞/收藏互动操作
    @GetMapping("/interact")
    public RestBean<Void> interact(@RequestParam @Min(0) int tid,
                                   @RequestParam @Pattern(regexp = "(like|collect)") String type,
                                   @RequestParam boolean state,
                                   @RequestAttribute(Const.ATTR_USER_ID) int id) {
        topicService.interact(new Interact(tid, id, new Date(), type), state);
        return RestBean.success();
    }

    // 获取用户收藏的帖子列表
    @GetMapping("/collects")
    public RestBean<List<TopicPreviewVO>> collects(@RequestAttribute(Const.ATTR_USER_ID) int id){
        return RestBean.success(topicService.listTopicCollects(id));
    }

    // 编辑帖子（编辑后重新进入待审核状态）
    @PostMapping("/update-topic")
    public RestBean<Void> updateTopic(@Valid @RequestBody TopicUpdateVO vo,
                                      @RequestAttribute(Const.ATTR_USER_ID) int id){
        return utils.messageHandle(() -> topicService.updateTopic(id, vo));
    }

    /**
     * 用户软删除自己的帖子
     * @param tid 帖子ID
     * @param uid 当前用户ID
     * @return 操作结果
     */
    @PostMapping("/delete-topic")
    public RestBean<Void> deleteTopic(@RequestParam @Min(0) int tid,
                                      @RequestAttribute(Const.ATTR_USER_ID) int uid){
        return utils.messageHandle(() -> topicService.deleteTopic(uid, tid));
    }

    // 发表评论
    @PostMapping("/add-comment")
    public RestBean<Void> addComment(@Valid @RequestBody AddCommentVO vo,
                                     @RequestAttribute(Const.ATTR_USER_ID) int id){
        return utils.messageHandle(() -> topicService.createComment(id, vo));
    }

    // 分页查询帖子评论
    @GetMapping("/comments")
    public RestBean<List<CommentVO>> comments(@RequestParam @Min(0) int tid,
                                              @RequestParam @Min(0) int page){
        return RestBean.success(topicService.comments(tid, page + 1));
    }

    // 软删除评论
    @GetMapping("/delete-comment")
    public RestBean<Void> deleteComment(@RequestParam @Min(0) int id,
                                        @RequestAttribute(Const.ATTR_USER_ID) int uid){
        topicService.deleteComment(id, uid);
        return RestBean.success();
    }
}
