package com.campus.forum.controller;

import com.campus.forum.entity.RestBean;
import com.campus.forum.entity.dto.Interact;
import com.campus.forum.entity.vo.request.AddCommentVO;
import com.campus.forum.entity.vo.request.ReportCreateVO;
import com.campus.forum.entity.vo.request.TopicCreateVO;
import com.campus.forum.entity.vo.request.TopicUpdateVO;
import com.campus.forum.entity.vo.response.*;
import com.campus.forum.service.ReportService;
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
 * 论坛业务控制器，处理帖子、评论、天气、互动、举报等请求
 */
@RestController
@RequestMapping("/api/forum")
public class ForumController {

    /** 天气服务 */
    @Resource
    WeatherService service;

    /** 帖子服务 */
    @Resource
    TopicService topicService;

    /** 举报服务 */
    @Resource
    ReportService reportService;

    /** 控制器工具类（统一消息处理） */
    @Resource
    ControllerUtils utils;

    /**
     * 获取天气信息，失败时返回空对象（静默降级）
     *
     * @param longitude 经度
     * @param latitude  纬度
     * @return 天气信息，失败时返回空的 WeatherVO
     */
    @GetMapping("/weather")
    public RestBean<WeatherVO> weather(double longitude, double latitude){
        WeatherVO vo = service.fetchWeather(longitude, latitude);
        return RestBean.success(vo == null ? new WeatherVO() : vo);
    }

    /**
     * 获取帖子分类列表
     *
     * @return 分类视图对象列表
     */
    @GetMapping("/types")
    public RestBean<List<TopicTypeVO>> listTypes(){
        return RestBean.success(topicService
                .listTypes()
                .stream()
                .map(type -> type.asViewObject(TopicTypeVO.class))
                .toList());
    }

    /**
     * 创建帖子，提交后进入待审核状态
     *
     * @param vo 帖子创建请求体
     * @param id 当前用户ID（由JWT过滤器注入）
     * @return 操作结果
     */
    @PostMapping("/create-topic")
    public RestBean<Void> createTopic(@Valid @RequestBody TopicCreateVO vo,
                                      @RequestAttribute(Const.ATTR_USER_ID) int id) {
        return utils.messageHandle(() -> topicService.createTopic(id, vo));
    }

    /**
     * 分页查询帖子列表（仅返回已发布的帖子）
     *
     * @param page  页码（从0开始）
     * @param type  分类ID（0=全部）
     * @param sort  排序方式（time/views/likes/collects/comments）
     * @param title 搜索标题关键词（可选）
     * @return 帖子预览列表
     */
    @GetMapping("/list-topic")
    public RestBean<List<TopicPreviewVO>> listTopic(@RequestParam @Min(0) int page,
                                                    @RequestParam @Min(0) int type,
                                                    @RequestParam(defaultValue = "time") String sort,
                                                    @RequestParam(required = false) String title) {
        return RestBean.success(topicService.listTopicByPage(page + 1, type, sort, title));
    }

    /**
     * 分页查询校园活动列表
     *
     * @param page  页码（从0开始）
     * @param title 搜索标题关键词（可选）
     * @return 活动帖子预览列表
     */
    @GetMapping("/list-activity")
    public RestBean<List<TopicPreviewVO>> listActivity(@RequestParam @Min(0) int page,
                                                       @RequestParam(required = false) String title) {
        return RestBean.success(topicService.listActivityByPage(page + 1, title));
    }

    /**
     * 分页查询教务通知列表
     *
     * @param page  页码（从0开始）
     * @param title 搜索标题关键词（可选）
     * @return 通知帖子预览列表
     */
    @GetMapping("/list-notice-topic")
    public RestBean<List<TopicPreviewVO>> listNoticeTopic(@RequestParam @Min(0) int page,
                                                          @RequestParam(required = false) String title) {
        return RestBean.success(topicService.listNoticeTopicByPage(page + 1, title));
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
     * 分页查询当前用户自己的帖子（所有状态）
     *
     * @param page   页码（从0开始）
     * @param status 帖子状态筛选（可选）
     * @param id     当前用户ID（由JWT过滤器注入）
     * @return 用户帖子列表
     */
    @GetMapping("/my-topics")
    public RestBean<List<UserTopicVO>> myTopics(@RequestParam @Min(0) int page,
                                                @RequestParam(required = false) String status,
                                                @RequestAttribute(Const.ATTR_USER_ID) int id) {
        return RestBean.success(topicService.listUserTopics(id, page + 1, status));
    }

    /**
     * 获取置顶帖子列表
     *
     * @return 置顶帖子列表
     */
    @GetMapping("/top-topic")
    public RestBean<List<TopicTopVO>> topTopic(){
        return RestBean.success(topicService.listTopTopics());
    }

    /**
     * 获取帖子详情（浏览时增加浏览量）
     *
     * @param tid 帖子ID
     * @param id  当前用户ID（用于判断是否已点赞/收藏）
     * @return 帖子详情
     */
    @GetMapping("/topic")
    public RestBean<TopicDetailVO> topic(@RequestParam @Min(0) int tid,
                                         @RequestAttribute(Const.ATTR_USER_ID) int id){
        return RestBean.success(topicService.getTopic(tid, id));
    }

    /**
     * 获取当前用户自己的帖子详情（不增加浏览量，可查看所有状态）
     *
     * @param tid 帖子ID
     * @param id  当前用户ID
     * @return 帖子详情
     */
    @GetMapping("/my-topic")
    public RestBean<TopicDetailVO> myTopic(@RequestParam @Min(0) int tid,
                                           @RequestAttribute(Const.ATTR_USER_ID) int id) {
        return RestBean.success(topicService.getOwnTopic(tid, id));
    }

    /**
     * 点赞/收藏互动操作
     *
     * @param tid   帖子ID
     * @param type  互动类型（like=点赞，collect=收藏）
     * @param state true=执行，false=取消
     * @param id    当前用户ID
     * @return 操作结果
     */
    @GetMapping("/interact")
    public RestBean<Void> interact(@RequestParam @Min(0) int tid,
                                   @RequestParam @Pattern(regexp = "(like|collect)") String type,
                                   @RequestParam boolean state,
                                   @RequestAttribute(Const.ATTR_USER_ID) int id) {
        topicService.interact(new Interact(tid, id, new Date(), type), state);
        return RestBean.success();
    }

    /**
     * 获取当前用户收藏的帖子列表
     *
     * @param id 当前用户ID
     * @return 收藏帖子预览列表
     */
    @GetMapping("/collects")
    public RestBean<List<TopicPreviewVO>> collects(@RequestAttribute(Const.ATTR_USER_ID) int id){
        return RestBean.success(topicService.listTopicCollects(id));
    }

    /**
     * 编辑帖子，编辑后重新进入待审核状态
     *
     * @param vo 帖子更新请求体
     * @param id 当前用户ID
     * @return 操作结果
     */
    @PostMapping("/update-topic")
    public RestBean<Void> updateTopic(@Valid @RequestBody TopicUpdateVO vo,
                                      @RequestAttribute(Const.ATTR_USER_ID) int id){
        return utils.messageHandle(() -> topicService.updateTopic(id, vo));
    }

    /**
     * 用户软删除自己的帖子
     *
     * @param tid 帖子ID
     * @param uid 当前用户ID
     * @return 操作结果
     */
    @PostMapping("/delete-topic")
    public RestBean<Void> deleteTopic(@RequestParam @Min(0) int tid,
                                      @RequestAttribute(Const.ATTR_USER_ID) int uid){
        return utils.messageHandle(() -> topicService.deleteTopic(uid, tid));
    }

    /**
     * 发表评论
     *
     * @param vo 评论创建请求体
     * @param id 当前用户ID
     * @return 操作结果
     */
    @PostMapping("/add-comment")
    public RestBean<Void> addComment(@Valid @RequestBody AddCommentVO vo,
                                     @RequestAttribute(Const.ATTR_USER_ID) int id){
        return utils.messageHandle(() -> topicService.createComment(id, vo));
    }

    /**
     * 分页查询帖子评论
     *
     * @param tid  帖子ID
     * @param page 页码（从0开始）
     * @return 评论列表
     */
    @GetMapping("/comments")
    public RestBean<List<CommentVO>> comments(@RequestParam @Min(0) int tid,
                                              @RequestParam @Min(0) int page){
        return RestBean.success(topicService.comments(tid, page + 1));
    }

    /**
     * 软删除评论（仅评论作者可操作）
     *
     * @param id  评论ID
     * @param uid 当前用户ID
     * @return 操作结果
     */
    @GetMapping("/delete-comment")
    public RestBean<Void> deleteComment(@RequestParam @Min(0) int id,
                                        @RequestAttribute(Const.ATTR_USER_ID) int uid){
        return utils.messageHandle(() -> topicService.deleteComment(id, uid));
    }

    /**
     * 提交举报
     *
     * @param vo  举报创建请求体
     * @param uid 当前用户ID
     * @return 操作结果
     */
    @PostMapping("/report")
    public RestBean<Void> report(@Valid @RequestBody ReportCreateVO vo,
                                 @RequestAttribute(Const.ATTR_USER_ID) int uid) {
        return utils.messageHandle(() -> reportService.createReport(uid, vo));
    }
}
