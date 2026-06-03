package com.campus.forum.service.impl;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.forum.entity.dto.*;
import com.campus.forum.entity.vo.request.AddCommentVO;
import com.campus.forum.entity.vo.request.ForumNoticeSaveVO;
import com.campus.forum.entity.vo.request.PublishActivityVO;
import com.campus.forum.entity.vo.request.PublishNoticeTopicVO;
import com.campus.forum.entity.vo.request.TopicCreateVO;
import com.campus.forum.entity.vo.request.TopicUpdateVO;
import com.campus.forum.entity.vo.response.CommentVO;
import com.campus.forum.entity.vo.response.ForumNoticeVO;
import com.campus.forum.entity.vo.response.TopicDetailVO;
import com.campus.forum.entity.vo.response.TopicPreviewVO;
import com.campus.forum.entity.vo.response.TopicTopVO;
import com.campus.forum.entity.vo.response.AdminTopicVO;
import com.campus.forum.entity.vo.response.AdminCommentVO;
import com.campus.forum.entity.vo.response.PageResult;
import com.campus.forum.entity.vo.response.UserTopicVO;
import com.campus.forum.mapper.*;
import com.campus.forum.service.NotificationService;
import com.campus.forum.service.TopicService;
import com.campus.forum.utils.CacheUtils;
import com.campus.forum.utils.Const;
import com.campus.forum.utils.FlowUtils;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * 帖子服务实现，包含审核流程适配
 * <p>
 * 关键业务规则变更（相比老项目）：
 * - 用户发帖后状态为 pending_review（待审核），前台列表不可见
 * - 用户编辑帖子后重新变为 pending_review
 * - 前台列表只查询 published 状态的帖子
 * - 用户只能软删除自己的帖子
 * - 置顶帖子只查询 published 状态
 * </p>
 */
@Service
public class TopicServiceImpl extends ServiceImpl<TopicMapper, Topic> implements TopicService {
    /** 校园活动系统分类标识 */
    private static final String SYSTEM_TYPE_ACTIVITY = "activity";
    /** 教务通知系统分类标识 */
    private static final String SYSTEM_TYPE_NOTICE = "notice";

    /** 用户"我的帖子"页面允许查看的状态集合 */
    private static final Set<String> USER_TOPIC_VISIBLE_STATUS = Set.of(
            Const.TOPIC_STATUS_PENDING,
            Const.TOPIC_STATUS_PUBLISHED,
            Const.TOPIC_STATUS_REJECTED,
            Const.TOPIC_STATUS_HIDDEN,
            Const.TOPIC_STATUS_DELETED
    );

    /** 用户可编辑的帖子状态集合 */
    private static final Set<String> USER_TOPIC_EDITABLE_STATUS = Set.of(
            Const.TOPIC_STATUS_PUBLISHED,
            Const.TOPIC_STATUS_REJECTED
    );

    /** 用户可删除的帖子状态集合 */
    private static final Set<String> USER_TOPIC_DELETABLE_STATUS = Set.of(
            Const.TOPIC_STATUS_PUBLISHED,
            Const.TOPIC_STATUS_PENDING
    );

    /** 帖子分类 Mapper */
    @Resource
    TopicTypeMapper mapper;

    /** 活动扩展表 Mapper */
    @Resource
    TopicActivityMapper topicActivityMapper;

    /** 论坛公告 Mapper */
    @Resource
    ForumNoticeMapper forumNoticeMapper;

    /** 限流工具（基于 Redis 计数） */
    @Resource
    FlowUtils flowUtils;

    /** Redis 缓存读写工具 */
    @Resource
    CacheUtils cacheUtils;

    /** 用户账号 Mapper */
    @Resource
    AccountMapper accountMapper;

    /** 用户详情 Mapper */
    @Resource
    AccountDetailsMapper accountDetailsMapper;

    /** 用户隐私设置 Mapper */
    @Resource
    AccountPrivacyMapper accountPrivacyMapper;

    /** 评论 Mapper */
    @Resource
    TopicCommentMapper commentMapper;

    /** Redis 操作模板 */
    @Resource
    StringRedisTemplate template;

    /** 站内通知服务 */
    @Resource
    NotificationService notificationService;

    /** 举报 Mapper */
    @Resource
    ReportMapper reportMapper;

    /** 所有分类ID集合，用于快速判断分类是否存在 */
    private Set<Integer> types = new HashSet<>();
    /** 分类ID到分类实体的映射，用于快速查找分类信息 */
    private Map<Integer, TopicType> typeIndex = new HashMap<>();

    /**
     * Bean 初始化后立即刷新分类缓存
     */
    @PostConstruct
    private void initTypes() {
        this.refreshTypeCache();
    }
    /**
     * 查询所有帖子分类（按ID升序），同时刷新分类缓存
     *
     * @return 分类列表
     */
    @Override
    public List<TopicType> listTypes() {
        this.refreshTypeCache();
        return mapper.selectList(Wrappers.<TopicType>query().orderByAsc("id"));
    }

    /**
     * 创建帖子，状态设为 pending_review（待审核）
     *
     * @param uid 用户ID
     * @param vo  帖子创建参数
     * @return null 表示成功，非 null 为错误信息
     */
    @Override
    public String createTopic(int uid, TopicCreateVO vo) {
        if (!textLimitCheck(vo.getContent(), 20000))
            return "文章内容太多，发文失败！";
        TopicType type = this.findTypeById(vo.getType());
        if (type == null)
            return "文章类型非法！";
        if (this.isSystemType(type))
            return "当前分类不允许普通用户发帖！";
        String key = Const.FORUM_TOPIC_CREATE_COUNTER + uid;
        // 1 小时内最多发 3 篇
        if (!flowUtils.limitPeriodCounterCheck(key, 3, 3600))
            return "发文频繁，请稍后再试！";
        Topic topic = new Topic();
        BeanUtils.copyProperties(vo, topic);
        topic.setContent(vo.getContent().toJSONString());
        topic.setUid(uid);
        topic.setTime(new Date());
        topic.setAllowComment(1);
        topic.setStatus(Const.TOPIC_STATUS_PENDING);     // 新发帖为待审核
        topic.setTop(0);                                  // 默认不置顶
        topic.setLastSubmitTime(new Date());
        if (this.save(topic)) {
            cacheUtils.deleteCachePattern(Const.FORUM_TOPIC_PREVIEW_CACHE + "*");
            return null;
        } else {
            return "内部错误，请联系管理员！";
        }
    }

    /**
     * 更新帖子，编辑后重新变为 pending_review（待审核）
     *
     * @param uid 用户ID
     * @param vo  帖子更新参数
     * @return null 表示成功，非 null 为错误信息
     */
    @Override
    public String updateTopic(int uid, TopicUpdateVO vo) {
        if (!textLimitCheck(vo.getContent(), 20000))
            return "文章内容太多，发文失败！";
        TopicType type = this.findTypeById(vo.getType());
        if (type == null)
            return "文章类型非法！";
        if (this.isSystemType(type))
            return "当前分类不允许普通用户选择！";
        Topic topic = baseMapper.selectById(vo.getId());
        if (topic == null) return "帖子不存在";
        if (!Objects.equals(topic.getUid(), uid)) return "无权操作";
        if (!USER_TOPIC_EDITABLE_STATUS.contains(topic.getStatus()))
            return "当前状态的帖子不允许编辑";
        baseMapper.update(null, Wrappers.<Topic>update()
                .eq("uid", uid)
                .eq("id", vo.getId())
                .set("title", vo.getTitle())
                .set("content", vo.getContent().toString())
                .set("type", vo.getType())
                .set("allow_comment", 1)
                .set("status", Const.TOPIC_STATUS_PENDING)      // 编辑后重新待审核
                .set("last_submit_time", new Date())
                .set("review_time", null)
                .set("review_by", null)
                .set("review_reason", null)
        );
        return null;
    }

    /**
     * 用户软删除自己的帖子
     *
     * @param uid 用户ID
     * @param tid 帖子ID
     * @return null 表示成功，非 null 为错误信息
     */
    @Override
    public String deleteTopic(int uid, int tid) {
        Topic topic = baseMapper.selectById(tid);
        if (topic == null) return "帖子不存在";
        if (!Objects.equals(topic.getUid(), uid)) return "无权操作";
        if (!USER_TOPIC_DELETABLE_STATUS.contains(topic.getStatus()))
            return "当前状态的帖子不允许删除";
        baseMapper.update(null, Wrappers.<Topic>update()
                .eq("id", tid)
                .eq("uid", uid)
                .set("status", Const.TOPIC_STATUS_DELETED)
                .set("deleted_time", new Date())
                .set("deleted_by", uid)
        );
        // 帖子删除后，自动关闭该帖子的 pending 举报并通知举报人
        var pendingTopicReports = reportMapper.selectList(Wrappers.<Report>query()
                .eq("target_type", Const.REPORT_TARGET_TOPIC)
                .eq("target_id", tid)
                .eq("status", Const.REPORT_STATUS_PENDING));
        for (Report report : pendingTopicReports) {
            reportMapper.update(null, Wrappers.<Report>update()
                    .eq("id", report.getId())
                    .set("status", Const.REPORT_STATUS_DISMISSED)
                    .set("admin_note", "目标帖子已被作者删除，举报自动关闭"));
            notificationService.addNotification(
                    report.getUid(),
                    "举报处理结果",
                    "您举报的帖子已被作者删除，举报自动关闭",
                    "info", null);
        }
        cacheUtils.deleteCachePattern(Const.FORUM_TOPIC_PREVIEW_CACHE + "*");
        return null;
    }

    /**
     * 创建评论
     *
     * @param uid 用户ID
     * @param vo  评论参数
     * @return null 表示成功，非 null 为错误信息
     */
    @Override
    public String createComment(int uid, AddCommentVO vo) {
        if (!textLimitCheck(JSONObject.parseObject(vo.getContent()), 2000))
            return "评论内容太多，发表失败！";
        Topic topic = baseMapper.selectById(vo.getTid());
        if (topic == null || !Const.TOPIC_STATUS_PUBLISHED.equals(topic.getStatus()))
            return "帖子不存在或当前不可评论";
        if (!Boolean.TRUE.equals(this.allowComment(topic)))
            return "当前帖子已关闭评论";
        String key = Const.FORUM_TOPIC_COMMENT_COUNTER + uid;
        if (!flowUtils.limitPeriodCounterCheck(key, 10, 60))
            return "发表评论频繁，请稍后再试！";
        TopicComment comment = new TopicComment();
        comment.setUid(uid);
        BeanUtils.copyProperties(vo, comment);
        comment.setTime(new Date());
        comment.setStatus(Const.COMMENT_STATUS_NORMAL);
        commentMapper.insert(comment);
        // 发送通知
        // 查询评论者信息（为了拿到用户名）
        Account account = accountMapper.selectById(uid);
        if (vo.getQuote() > 0) {
            // 回复别人的评论 → 通知被回复的人
            TopicComment com = commentMapper.selectById(vo.getQuote());
            if (com != null && !Objects.equals(account.getId(), com.getUid())) {
                notificationService.addNotification(
                        com.getUid(),
                        "您有新的帖子评论回复",
                        account.getUsername() + " 回复了你发表的评论，快去看看吧！",
                        "success", "/index/topic-detail/" + com.getTid()
                );
            }
        } else if (!Objects.equals(account.getId(), topic.getUid())) {
            // 直接评论帖子（不是回复）→ 通知帖子作者
            notificationService.addNotification(
                    topic.getUid(),
                    "您有新的帖子回复",
                    account.getUsername() + " 回复了你发表主题: " + topic.getTitle() + "，快去看看吧！",
                    "success", "/index/topic-detail/" + topic.getId()
            );
        }
        return null;
    }

    /**
     * 查询帖子评论列表（仅 normal 状态）
     *
     * @param tid        帖子ID
     * @param pageNumber 页码
     * @return 评论VO列表
     */
    @Override
    public List<CommentVO> comments(int tid, int pageNumber) {
        Topic topic = baseMapper.selectById(tid);
        if (topic == null || !Const.TOPIC_STATUS_PUBLISHED.equals(topic.getStatus()) || !Boolean.TRUE.equals(this.allowComment(topic)))
            return List.of();
        Page<TopicComment> page = Page.of(pageNumber, 10);
        commentMapper.selectPage(page, Wrappers.<TopicComment>query()
                .eq("tid", tid)
                .eq("status", Const.COMMENT_STATUS_NORMAL));    // 只查正常评论
        return page.getRecords().stream().map(dto -> {
            CommentVO vo = new CommentVO();
            BeanUtils.copyProperties(dto, vo);
            if (dto.getQuote() > 0) {
                // 根据 quote ID 查到被引用的那条评论
                TopicComment comment = commentMapper.selectOne(Wrappers.<TopicComment>query()
                        .eq("id", dto.getQuote()));
                if (comment != null) {
                    // 从评论内容（Quill Delta JSON）中提取纯文字
                    JSONObject object = JSONObject.parseObject(comment.getContent());
                    StringBuilder builder = new StringBuilder();
                    this.shortContent(object.getJSONArray("ops"), builder, ignore -> {});
                    vo.setQuote(builder.toString());  // 设置为文字内容
                } else {
                    vo.setQuote("此评论已被删除");  // 被引用的评论已删除
                }
            }
            CommentVO.User user = new CommentVO.User();
            this.fillUserDetailsByPrivacy(user, dto.getUid());
            vo.setUser(user);
            return vo;
        }).toList();
    }

    /**
     * 软删除评论
     *
     * @param id  评论ID
     * @param uid 用户ID
     */
    @Override
    public void deleteComment(int id, int uid) {
        commentMapper.update(null, Wrappers.<TopicComment>update()
                .eq("id", id)
                .eq("uid", uid)
                .set("status", Const.COMMENT_STATUS_DELETED));
        // 评论删除后，自动关闭该评论的 pending 举报并通知举报人
        var pendingReports = reportMapper.selectList(Wrappers.<Report>query()
                .eq("target_type", Const.REPORT_TARGET_COMMENT)
                .eq("target_id", id)
                .eq("status", Const.REPORT_STATUS_PENDING));
        for (Report report : pendingReports) {
            reportMapper.update(null, Wrappers.<Report>update()
                    .eq("id", report.getId())
                    .set("status", Const.REPORT_STATUS_DISMISSED)
                    .set("admin_note", "目标评论已被作者删除，举报自动关闭"));
            notificationService.addNotification(
                    report.getUid(),
                    "举报处理结果",
                    "您举报的评论已被作者删除，举报自动关闭",
                    "info", null);
        }
    }

    /**
     * 查询用户收藏的帖子
     *
     * @param uid 用户ID
     * @return 收藏帖子预览列表
     */
    @Override
    public List<TopicPreviewVO> listTopicCollects(int uid) {
        return baseMapper.collectTopics(uid)
                .stream()
                .map(this::resolveToPreview)
                .toList();
    }

    /**
     * 分页查询当前用户自己的帖子
     *
     * @param uid        用户ID
     * @param pageNumber 页码
     * @param status     帖子状态筛选（可选，"all" 或 null 表示全部）
     * @return 用户帖子列表
     */
    @Override
    public List<UserTopicVO> listUserTopics(int uid, int pageNumber, String status) {
        Page<Topic> page = Page.of(pageNumber, 10);
        var wrapper = Wrappers.<Topic>query()
                .eq("uid", uid)
                .orderByDesc("time");
        if (status != null && !status.isBlank() && !"all".equals(status)) {
            if (!USER_TOPIC_VISIBLE_STATUS.contains(status)) {
                return List.of();
            }
            wrapper.eq("status", status);
        }
        baseMapper.selectPage(page, wrapper);
        return page.getRecords().stream()
                .map(this::resolveToUserTopic)
                .toList();
    }

    /**
     * 分页查询帖子列表（仅 published 状态，前台可见）
     *
     * @param pageNumber 页码
     * @param type       分类ID（0=全部）
     * @param sort       排序方式（time/views/likes/collects/comments）
     * @param title      标题关键词（可选）
     * @return 帖子预览列表，参数非法时返回 null
     */
    @Override
    public List<TopicPreviewVO> listTopicByPage(int pageNumber, int type, String sort, String title) {
        if (type > 0) {
            TopicType topicType = this.findTypeById(type);
            if (topicType == null || this.isSystemType(topicType)) {
                return null;
            }
        }
        return this.listPublishedTopicByPage(pageNumber, type == 0 ? null : type, "forum", true, sort, title);
    }

    /**
     * 分页查询校园活动帖子列表
     *
     * @param pageNumber 页码
     * @param title      标题关键词（可选）
     * @return 活动帖子预览列表，分类不存在时返回 null
     */
    @Override
    public List<TopicPreviewVO> listActivityByPage(int pageNumber, String title) {
        Integer typeId = this.resolveSystemTypeId(SYSTEM_TYPE_ACTIVITY);
        if (typeId == null) return null;
        return this.listPublishedTopicByPage(pageNumber, typeId, "activity", false, "time", title);
    }

    /**
     * 分页查询教务通知帖子列表
     *
     * @param pageNumber 页码
     * @param title      标题关键词（可选）
     * @return 通知帖子预览列表，分类不存在时返回 null
     */
    @Override
    public List<TopicPreviewVO> listNoticeTopicByPage(int pageNumber, String title) {
        Integer typeId = this.resolveSystemTypeId(SYSTEM_TYPE_NOTICE);
        if (typeId == null) return null;
        return this.listPublishedTopicByPage(pageNumber, typeId, "notice", false, "time", title);
    }

    /**
     * 查询置顶帖子（仅 published 状态）
     *
     * @return 置顶帖子列表
     */
    @Override
    public List<TopicTopVO> listTopTopics() {
        List<Integer> systemTypeIds = this.systemTypeIds();
        var wrapper = Wrappers.<Topic>query()
                .select("id", "title", "time")
                .eq("top", 1)
                .eq("status", Const.TOPIC_STATUS_PUBLISHED);
        if (!systemTypeIds.isEmpty()) {
            wrapper.notIn("type", systemTypeIds);
        }
        List<Topic> topics = baseMapper.selectList(wrapper);    // 置顶帖子也只显示已发布
        return topics.stream().map(topic -> {
            TopicTopVO vo = new TopicTopVO();
            BeanUtils.copyProperties(topic, vo);
            return vo;
        }).toList();
    }

    /**
     * 获取帖子详情（用户端，仅 published 可见）
     *
     * @param tid 帖子ID
     * @param uid 当前用户ID
     * @return 帖子详情，不可见时返回 null
     */
    @Override
    public TopicDetailVO getTopic(int tid, int uid) {
        Topic topic = baseMapper.selectById(tid);
        if (topic == null) return null;
        // 用户端只能查看已发布的帖子
        if (!Const.TOPIC_STATUS_PUBLISHED.equals(topic.getStatus())) {
            return null;
        }
        return this.buildTopicDetail(topic, uid);
    }

    /**
     * 获取当前用户自己的帖子详情（不受 published 限制）
     *
     * @param tid 帖子ID
     * @param uid 当前用户ID
     * @return 帖子详情，非本人帖子时返回 null
     */
    @Override
    public TopicDetailVO getOwnTopic(int tid, int uid) {
        Topic topic = baseMapper.selectById(tid);
        if (topic == null || !Objects.equals(topic.getUid(), uid)) {
            return null;
        }
        return this.buildTopicDetail(topic, uid);
    }

    /**
     * 管理员查看帖子详情，不受帖子状态限制
     *
     * @param tid 帖子ID
     * @return 帖子详情，不存在时返回 null
     */
    @Override
    public TopicDetailVO adminGetTopic(int tid) {
        Topic topic = baseMapper.selectById(tid);
        if (topic == null) return null;
        return this.buildTopicDetail(topic, 0);
    }

    /**
     * 构建帖子详情，累加浏览量并填充互动、用户信息及活动扩展字段
     *
     * @param topic 帖子实体
     * @param uid   当前访问用户ID，0 表示无需计算互动状态
     * @return 帖子详情VO
     */
    private TopicDetailVO buildTopicDetail(Topic topic, int uid) {
        // 浏览量 +1
        baseMapper.update(null, Wrappers.<Topic>update()
                .eq("id", topic.getId())
                .setSql("view_count = view_count + 1"));
        // 创建 VO 对象，复制帖子基础字段
        TopicDetailVO vo = new TopicDetailVO();
        BeanUtils.copyProperties(topic, vo);
        vo.setViewCount(topic.getViewCount() == null ? 1 : topic.getViewCount() + 1);
        // 查询是否允许评论
        vo.setAllowComment(this.allowComment(topic));
        // 查询当前用户的点赞/收藏状态
        TopicDetailVO.Interact interact = new TopicDetailVO.Interact(
                uid > 0 && hasInteract(topic.getId(), uid, "like"),
                uid > 0 && hasInteract(topic.getId(), uid, "collect")
        );
        vo.setInteract(interact);
        // 查询帖子作者信息（按隐私设置过滤）
        TopicDetailVO.User user = new TopicDetailVO.User();
        vo.setUser(this.fillUserDetailsByPrivacy(user, topic.getUid()));
        // 查询评论总数  允许评论 → 查评论数量；不允许 → 直接设为 0
        vo.setComments(Boolean.TRUE.equals(vo.getAllowComment())
                ? commentMapper.selectCount(Wrappers.<TopicComment>query()
                .eq("tid", topic.getId())
                .eq("status", Const.COMMENT_STATUS_NORMAL))
                : 0L);
        // 填充活动扩展信息
        this.fillActivityFields(topic, vo);
        return vo;
    }

    /**
     * 互动（点赞/收藏）操作，先写入 Redis 再延迟批量同步到数据库
     *
     * @param interact 互动信息（含帖子ID、用户ID、类型）
     * @param state    true=点赞/收藏，false=取消
     */
    @Override
    public void interact(Interact interact, boolean state) {
        // 取出互动类型
        String type = interact.getType();
        synchronized (type.intern()) {
            template.opsForHash().put(type, interact.toKey(), Boolean.toString(state));
            this.saveInteractSchedule(type);  // 安排延迟批量同步任务
        }
        // 立即同步该条互动到数据库，确保列表数量实时更新
        if (state) {
            baseMapper.addInteract(List.of(interact), type);
        } else {
            baseMapper.deleteInteract(List.of(interact), type);
        }
        // 清除帖子列表缓存（让首页看到最新数据）
        cacheUtils.deleteCachePattern(Const.FORUM_TOPIC_PREVIEW_CACHE + "*");
    }

    /**
     * 检查用户是否对帖子有指定类型的互动（先查 Redis，再查数据库）
     *
     * @param tid  帖子ID
     * @param uid  用户ID
     * @param type 互动类型（like/collect）
     * @return 是否存在该互动
     */
    private boolean hasInteract(int tid, int uid, String type) {
        String key = tid + ":" + uid;
        if (template.opsForHash().hasKey(type, key))
            return Boolean.parseBoolean(template.opsForHash().entries(type).get(key).toString());
        return baseMapper.userInteractCount(tid, uid, type) > 0;
    }

    /** 各互动类型是否已有待执行的延迟同步任务 */
    private final Map<String, Boolean> state = new HashMap<>();
    /** 用于延迟批量同步互动数据的定时线程池 */
    ScheduledExecutorService service = Executors.newScheduledThreadPool(2);

    /**
     * 为指定互动类型安排一次延迟批量同步任务（3秒后执行，同类型不重复调度）
     *
     * @param type 互动类型（like/collect）
     */
    private void saveInteractSchedule(String type) {
        // 1. 如果这个类型已经有一个 3 秒后的任务在等了，就不再创建新的
        if (!state.getOrDefault(type, false)) {
            state.put(type, true); // 标记：已安排
            // 2. 安排 3 秒后执行
            service.schedule(() -> {
                this.saveInteract(type);        // 3秒后，批量同步到数据库
                state.put(type, false);  // 标记：任务完成，可以安排下一个了
            }, 3, TimeUnit.SECONDS);
        }
    }

    /**
     * 将 Redis 中缓存的互动数据按状态分组，批量同步到数据库后清空 Redis 缓存
     *
     * @param type 互动类型（like/collect）
     */
    private void saveInteract(String type) {
        synchronized (type.intern()) {
            List<Interact> check = new LinkedList<>();        // 要新增的
            List<Interact> uncheck = new LinkedList<>();      // 要删除的
            // 遍历 Redis Hash 里的所有互动数据
            template.opsForHash().entries(type).forEach((k, v) -> {
                if (Boolean.parseBoolean(v.toString()))
                    check.add(Interact.parseInteract(k.toString(), type));    // "true" → 新增列表
                else
                    uncheck.add(Interact.parseInteract(k.toString(), type));  // "false" → 删除列表
            });
            // 批量写数据库
            if (!check.isEmpty())
                baseMapper.addInteract(check, type);   // 一次 INSERT 多条
            if (!uncheck.isEmpty())
                baseMapper.deleteInteract(uncheck, type);  // 一次 DELETE 多条
            template.delete(type);   // 清空 Redis Hash，等下一轮
        }
    }

    /**
     * 根据用户隐私设置，将账号和详情信息拷贝到目标对象（自动跳过隐私字段）
     *
     * @param target 目标对象（需与 Account/AccountDetails 字段名一致）
     * @param uid    用户ID
     * @param <T>    目标类型
     * @return 填充后的目标对象
     */
    private <T> T fillUserDetailsByPrivacy(T target, int uid) {
        AccountDetails details = accountDetailsMapper.selectById(uid);
        Account account = accountMapper.selectById(uid);
        AccountPrivacy accountPrivacy = accountPrivacyMapper.selectById(uid);
        String[] ignores = accountPrivacy.hiddenFields();
        BeanUtils.copyProperties(account, target, ignores);
        BeanUtils.copyProperties(details, target, ignores);
        return target;
    }

    /**
     * 将 Topic 转换为前台列表预览VO，提取文本摘要和图片，附带互动统计
     *
     * @param topic 帖子实体
     * @return 预览VO
     */
    private TopicPreviewVO resolveToPreview(Topic topic) {
        // 1. 创建空的 VO 对象
        TopicPreviewVO vo = new TopicPreviewVO();
        // 2. 把作者信息复制到 VO
        BeanUtils.copyProperties(accountMapper.selectById(topic.getUid()), vo);
        // 3. 把帖子信息复制到 VO
        BeanUtils.copyProperties(topic, vo);
        // 4. 统计互动数据（点赞数、收藏数、评论数）
        vo.setLike(baseMapper.interactCount(topic.getId(), "like"));
        vo.setCollect(baseMapper.interactCount(topic.getId(), "collect"));
        vo.setComments(baseMapper.commentCount(topic.getId()));
        // 5. 从富文本内容中提取摘要和图片
        List<String> images = new ArrayList<>();
        StringBuilder previewText = new StringBuilder();
        JSONArray ops = JSONObject.parseObject(topic.getContent()).getJSONArray("ops");
        this.shortContent(ops, previewText, obj -> images.add(obj.toString()));
        // 6. 摘要最多300字，超出就截断
        vo.setText(previewText.length() > 300 ? previewText.substring(0, 300) : previewText.toString());
        vo.setImages(images);
        // 7. 如果是活动帖，补充活动信息
        this.fillActivityPreview(topic, vo);
        return vo;
    }

    /**
     * 将 Topic 转换为"我的帖子"列表项VO，提取文本摘要和图片
     *
     * @param topic 帖子实体
     * @return 用户帖子VO
     */
    private UserTopicVO resolveToUserTopic(Topic topic) {
        UserTopicVO vo = new UserTopicVO();
        BeanUtils.copyProperties(topic, vo);
        vo.setLike(baseMapper.interactCount(topic.getId(), "like"));
        vo.setCollect(baseMapper.interactCount(topic.getId(), "collect"));
        vo.setComments(baseMapper.commentCount(topic.getId()));
        List<String> images = new ArrayList<>();
        StringBuilder previewText = new StringBuilder();
        JSONArray ops = JSONObject.parseObject(topic.getContent()).getJSONArray("ops");
        this.shortContent(ops, previewText, obj -> images.add(obj.toString()));
        vo.setText(previewText.length() > 300 ? previewText.substring(0, 300) : previewText.toString());
        vo.setImages(images);
        return vo;
    }

    /**
     * 从 Quill Delta ops 中提取纯文本摘要和图片URL
     *
     * @param ops          Quill Delta 的 ops 数组
     * @param previewText  用于拼接纯文本摘要的 StringBuilder
     * @param imageHandler 图片URL回调
     */
    private void shortContent(JSONArray ops, StringBuilder previewText, Consumer<Object> imageHandler) {
        for (Object op : ops) {
            Object insert = JSONObject.from(op).get("insert");
            if (insert instanceof String text) {
                if (previewText.length() >= 300) continue;
                previewText.append(text);
            } else if (insert instanceof Map<?, ?> map) {
                Optional.ofNullable(map.get("image")).ifPresent(imageHandler);
            }
        }
    }

    /**
     * 检查 Quill Delta 内容的纯文本长度是否超过限制
     *
     * @param object Quill Delta JSON 对象
     * @param max    最大允许字符数
     * @return 未超限返回 true，超限返回 false
     */
    private boolean textLimitCheck(JSONObject object, int max) {
        if (object == null) return false;
        long length = 0;
        for (Object op : object.getJSONArray("ops")) {
            length += JSONObject.from(op).getString("insert").length();
            if (length > max) return false;
        }
        return true;
    }

    // ==================== 管理员方法 ====================

    /**
     * 管理员分页查询全部帖子（支持多条件筛选）
     * @param page 页码
     * @param status 帖子状态（可选）
     * @param type 分类ID（可选）
     * @param title 标题关键词（可选）
     * @param author 作者用户名（可选）
     * @return 帖子列表
     */
    @Override
    public PageResult<AdminTopicVO> adminListTopics(int page, int pageSize, String status, Integer type, String title, String author) {
        Page<Topic> p = Page.of(page, pageSize);
        var wrapper = Wrappers.<Topic>query();
        if (status != null && !status.isBlank())
            wrapper.eq("status", status);
        if (type != null && type > 0)
            wrapper.eq("type", type);
        if (title != null && !title.isBlank())
            wrapper.like("title", title);
        if (author != null && !author.isBlank()) {
            wrapper.inSql("uid", "select id from db_account where username like '%" + author + "%'");
        }
        wrapper.orderByDesc("time");
        baseMapper.selectPage(p, wrapper);
        List<AdminTopicVO> list = p.getRecords().stream().map(topic -> {
            AdminTopicVO vo = new AdminTopicVO();
            BeanUtils.copyProperties(topic, vo);
            Account account = accountMapper.selectById(topic.getUid());
            if (account != null) vo.setUsername(account.getUsername());
            TopicType topicType = mapper.selectById(topic.getType());
            if (topicType != null) vo.setTypeName(topicType.getName());
            vo.setCommentCount(commentMapper.selectCount(Wrappers.<TopicComment>query()
                    .eq("tid", topic.getId())
                    .eq("status", Const.COMMENT_STATUS_NORMAL)));
            return vo;
        }).toList();
        return new PageResult<>(list, p.getTotal());
    }

    /**
     * 审核通过帖子
     * @param tid 帖子ID
     * @param adminId 审核人ID
     */
    @Override
    public void adminApproveTopic(int tid, int adminId) {
        baseMapper.update(null, Wrappers.<Topic>update()
                .eq("id", tid)
                .set("status", Const.TOPIC_STATUS_PUBLISHED)
                .set("review_time", new Date())
                .set("review_by", adminId)
                .set("review_reason", null));
        cacheUtils.deleteCachePattern(Const.FORUM_TOPIC_PREVIEW_CACHE + "*");
        // 通知作者审核通过
        Topic topic = baseMapper.selectById(tid);
        if (topic != null) {
            notificationService.addNotification(topic.getUid(),
                    "帖子审核通过",
                    "您的帖子「" + topic.getTitle() + "」已通过审核，现在所有人都可以看到了！",
                    "success", "/index/topic-detail/" + tid);
        }
    }

    /**
     * 审核拒绝帖子
     * @param tid 帖子ID
     * @param adminId 审核人ID
     * @param reason 拒绝理由
     */
    @Override
    public void adminRejectTopic(int tid, int adminId, String reason) {
        baseMapper.update(null, Wrappers.<Topic>update()
                .eq("id", tid)
                .set("status", Const.TOPIC_STATUS_REJECTED)
                .set("review_time", new Date())
                .set("review_by", adminId)
                .set("review_reason", reason));
        // 通知作者审核拒绝
        Topic topic = baseMapper.selectById(tid);
        if (topic != null) {
            notificationService.addNotification(topic.getUid(),
                    "帖子审核未通过",
                    "您的帖子「" + topic.getTitle() + "」未通过审核，原因：" + (reason != null ? reason : "无") + "。可修改后重新提交。",
                    "warning", null);
        }
    }

    /**
     * 下架帖子（需填写原因）
     *
     * @param tid    帖子ID
     * @param reason 下架原因
     * @return null 表示成功，非 null 为错误信息
     */
    @Override
    public String adminHideTopic(int tid, String reason) {
        Topic topic = baseMapper.selectById(tid);
        if (topic == null) return "帖子不存在";
        if (!Const.TOPIC_STATUS_PUBLISHED.equals(topic.getStatus()))
            return "只有已发布的帖子才能下架";
        if (reason == null || reason.isBlank())
            return "请填写下架原因";
        baseMapper.update(null, Wrappers.<Topic>update()
                .eq("id", tid)
                .set("status", Const.TOPIC_STATUS_HIDDEN)
                .set("hide_reason", reason));
        cacheUtils.deleteCachePattern(Const.FORUM_TOPIC_PREVIEW_CACHE + "*");
        notificationService.addNotification(topic.getUid(),
                "帖子已被下架",
                "您的帖子「" + topic.getTitle() + "」已被管理员下架，原因：" + reason,
                "warning", null);
        return null;
    }

    /**
     * 上架帖子（恢复已下架帖子）
     *
     * @param tid 帖子ID
     * @return null 表示成功，非 null 为错误信息
     */
    @Override
    public String adminRestoreTopic(int tid) {
        Topic topic = baseMapper.selectById(tid);
        if (topic == null) return "帖子不存在";
        if (Const.TOPIC_STATUS_HIDDEN.equals(topic.getStatus())) {
            baseMapper.update(null, Wrappers.<Topic>update()
                    .eq("id", tid)
                    .set("status", Const.TOPIC_STATUS_PUBLISHED)
                    .set("hide_reason", null));
        } else if (Const.TOPIC_STATUS_DELETED.equals(topic.getStatus())) {
            baseMapper.update(null, Wrappers.<Topic>update()
                    .eq("id", tid)
                    .set("status", Const.TOPIC_STATUS_PUBLISHED)
                    .set("deleted_time", null)
                    .set("deleted_by", null));
        } else {
            return "当前帖子状态不支持恢复";
        }
        cacheUtils.deleteCachePattern(Const.FORUM_TOPIC_PREVIEW_CACHE + "*");
        reportMapper.delete(Wrappers.<Report>query()
                .eq("target_type", Const.REPORT_TARGET_TOPIC)
                .eq("target_id", tid)
                .eq("status", Const.REPORT_STATUS_PENDING));
        return null;
    }

    /**
     * 管理员删除帖子（物理删除，不可逆）
     *
     * @param tid     帖子ID
     * @param adminId 操作管理员ID
     * @return null 表示成功，非 null 为错误信息
     */
    @Override
    public String adminDeleteTopic(int tid, int adminId) {
        Topic topic = baseMapper.selectById(tid);
        if (topic == null) return "帖子不存在";
        if (Const.TOPIC_STATUS_PENDING.equals(topic.getStatus()))
            return "待审核帖子不能直接删除，请先通过或拒绝";
        reportMapper.delete(Wrappers.<Report>query()
                .eq("target_type", Const.REPORT_TARGET_TOPIC).eq("target_id", tid)
                .or()
                .eq("target_type", Const.REPORT_TARGET_COMMENT).inSql("target_id",
                        "select id from db_topic_comment where tid = " + tid));
        commentMapper.delete(Wrappers.<TopicComment>query().eq("tid", tid));
        topicActivityMapper.deleteById(tid);
        baseMapper.deleteLikeByTid(tid);
        baseMapper.deleteCollectByTid(tid);
        notificationService.remove(Wrappers.<Notification>query()
                .eq("url", "/index/topic-detail/" + tid));
        baseMapper.deleteById(tid);
        cacheUtils.deleteCachePattern(Const.FORUM_TOPIC_PREVIEW_CACHE + "*");
        return null;
    }

    /**
     * 置顶帖子
     * @param tid 帖子ID
     */
    @Override
    public void adminTopTopic(int tid) {
        baseMapper.update(null, Wrappers.<Topic>update()
                .eq("id", tid)
                .set("top", 1));
    }

    /**
     * 取消置顶
     * @param tid 帖子ID
     */
    @Override
    public void adminUntopTopic(int tid) {
        baseMapper.update(null, Wrappers.<Topic>update()
                .eq("id", tid)
                .set("top", 0));
    }

    /**
     * 获取论坛公告，附带最后更新人用户名
     *
     * @return 公告VO，不存在时返回 null
     */
    @Override
    public ForumNoticeVO getForumNotice() {
        ForumNotice notice = this.firstForumNotice();
        if (notice == null) return null;
        ForumNoticeVO vo = new ForumNoticeVO();
        BeanUtils.copyProperties(notice, vo);
        Account account = notice.getUpdateBy() == null ? null : accountMapper.selectById(notice.getUpdateBy());
        if (account != null) {
            vo.setUpdateByName(account.getUsername());
        }
        return vo;
    }

    /**
     * 管理员发布校园活动帖子，同时创建活动扩展信息
     *
     * @param adminId 管理员用户ID
     * @param vo      活动发布表单
     * @return 错误信息，成功时返回 null
     */
    @Override
    public String publishActivity(int adminId, PublishActivityVO vo) {
        if (!textLimitCheck(vo.getContent(), 20000))
            return "文章内容太多，发布失败！";
        Integer typeId = this.resolveSystemTypeId(SYSTEM_TYPE_ACTIVITY);
        if (typeId == null)
            return "未找到校园活动系统分类";
        if (vo.getSignupDeadline() != null && vo.getSignupDeadline().after(vo.getActivityTime()))
            return "报名截止时间不能晚于活动时间";
        Topic topic = new Topic();
        topic.setTitle(vo.getTitle());
        topic.setContent(vo.getContent().toJSONString());
        topic.setUid(adminId);
        topic.setType(typeId);
        topic.setTime(new Date());
        topic.setTop(0);
        topic.setAllowComment(1);
        topic.setStatus(Const.TOPIC_STATUS_PUBLISHED);
        topic.setReviewTime(new Date());
        topic.setReviewBy(adminId);
        topic.setLastSubmitTime(new Date());
        if (!this.save(topic))
            return "内部错误，请联系管理员！";
        TopicActivity activity = new TopicActivity();
        activity.setTid(topic.getId());
        activity.setActivityTime(vo.getActivityTime());
        activity.setLocation(vo.getLocation());
        activity.setOrganizer(vo.getOrganizer());
        activity.setSignupDeadline(vo.getSignupDeadline());
        topicActivityMapper.insert(activity);
        cacheUtils.deleteCachePattern(Const.FORUM_TOPIC_PREVIEW_CACHE + "*");
        return null;
    }

    /**
     * 管理员发布教务通知帖子（默认关闭评论）
     *
     * @param adminId 管理员用户ID
     * @param vo      通知发布表单
     * @return 错误信息，成功时返回 null
     */
    @Override
    public String publishNoticeTopic(int adminId, PublishNoticeTopicVO vo) {
        if (!textLimitCheck(vo.getContent(), 20000))
            return "文章内容太多，发布失败！";
        Integer typeId = this.resolveSystemTypeId(SYSTEM_TYPE_NOTICE);
        if (typeId == null)
            return "未找到教务通知系统分类";
        Topic topic = new Topic();
        topic.setTitle(vo.getTitle());
        topic.setContent(vo.getContent().toJSONString());
        topic.setUid(adminId);
        topic.setType(typeId);
        topic.setTime(new Date());
        topic.setTop(0);
        topic.setAllowComment(0);
        topic.setStatus(Const.TOPIC_STATUS_PUBLISHED);
        topic.setReviewTime(new Date());
        topic.setReviewBy(adminId);
        topic.setLastSubmitTime(new Date());
        if (this.save(topic)) {
            cacheUtils.deleteCachePattern(Const.FORUM_TOPIC_PREVIEW_CACHE + "*");
            return null;
        }
        return "内部错误，请联系管理员！";
    }

    /**
     * 新增或更新论坛公告（单条记录，id=1）
     *
     * @param adminId 管理员用户ID
     * @param vo      公告保存表单
     * @return 错误信息，成功时返回 null
     */
    @Override
    public String saveForumNotice(int adminId, ForumNoticeSaveVO vo) {
        ForumNotice notice = this.firstForumNotice();
        Date now = new Date();
        if (notice == null) {
            notice = new ForumNotice();
            notice.setId(1);
            notice.setContent(vo.getContent());
            notice.setUpdateTime(now);
            notice.setUpdateBy(adminId);
            forumNoticeMapper.insert(notice);
        } else {
            notice.setContent(vo.getContent());
            notice.setUpdateTime(now);
            notice.setUpdateBy(adminId);
            forumNoticeMapper.updateById(notice);
        }
        return null;
    }

    /**
     * 查询第一条论坛公告记录
     *
     * @return 公告实体，不存在时返回 null
     */
    private ForumNotice firstForumNotice() {
        return forumNoticeMapper.selectList(Wrappers.<ForumNotice>query()
                        .orderByAsc("id")
                        .last("limit 1"))
                .stream()
                .findFirst()
                .orElse(null);
    }

    /**
     * 从数据库重新加载全部分类，刷新本地 types 集合和 typeIndex 映射
     */
    private void refreshTypeCache() {
        List<TopicType> list = mapper.selectList(Wrappers.<TopicType>query().orderByAsc("id"));
        this.types = list.stream().map(TopicType::getId).collect(Collectors.toSet());
        this.typeIndex = list.stream().collect(Collectors.toMap(TopicType::getId, type -> type, (a, b) -> a, LinkedHashMap::new));
    }

    /**
     * 根据ID查找分类，缓存未命中时自动刷新一次
     *
     * @param id 分类ID
     * @return 分类实体，不存在时返回 null
     */
    private TopicType findTypeById(int id) {
        TopicType type = this.typeIndex.get(id);
        if (type != null) {
            return type;
        }
        this.refreshTypeCache();
        return this.typeIndex.get(id);
    }

    /**
     * 判断分类是否为系统分类（systemKey 非空的分类禁止普通用户使用）
     *
     * @param type 分类实体
     * @return 是否为系统分类
     */
    private boolean isSystemType(TopicType type) {
        return type != null && type.getSystemKey() != null && !type.getSystemKey().isBlank();
    }

    /**
     * 根据系统分类标识（如 activity、notice）查找对应的分类ID
     *
     * @param systemKey 系统分类标识
     * @return 分类ID，不存在时返回 null
     */
    private Integer resolveSystemTypeId(String systemKey) {
        this.refreshTypeCache();
        return this.typeIndex.values().stream()
                .filter(type -> systemKey.equals(type.getSystemKey()))
                .map(TopicType::getId)
                .findFirst()
                .orElse(null);
    }

    /**
     * 获取所有系统分类的ID列表
     *
     * @return 系统分类ID列表
     */
    private List<Integer> systemTypeIds() {
        this.refreshTypeCache();
        return this.typeIndex.values().stream()
                .filter(this::isSystemType)
                .map(TopicType::getId)
                .toList();
    }

    /**
     * 判断帖子是否允许评论（allowComment 为 null 或 1 时允许）
     *
     * @param topic 帖子实体
     * @return 是否允许评论
     */
    private Boolean allowComment(Topic topic) {
        return topic.getAllowComment() == null || topic.getAllowComment() == 1;
    }

    /**
     * 分页查询已发布帖子列表（通用底层方法），支持分类筛选、排序、搜索和Redis缓存
     *
     * @param pageNumber        页码
     * @param typeId            分类ID，null 表示不限分类
     * @param cacheScope        缓存作用域标识（如 "forum"、"activity"、"notice"）
     * @param excludeSystemType 当 typeId 为 null 时，是否排除系统分类的帖子
     * @param sort              排序方式（time/views/likes/collects/comments）
     * @param title             标题关键词（可选，有值时跳过缓存）
     * @return 帖子预览列表，无数据时返回 null
     */
    private List<TopicPreviewVO> listPublishedTopicByPage(int pageNumber, Integer typeId, String cacheScope, boolean excludeSystemType, String sort, String title) {
        boolean hasTitle = title != null && !title.isBlank();
        // topic:preview:forum:0:0:time
        String key = Const.FORUM_TOPIC_PREVIEW_CACHE + cacheScope + ":" + pageNumber + ":" + (typeId == null ? 0 : typeId) + ":" + sort + (hasTitle ? ":" + title : "");
        if (!hasTitle) {
            List<TopicPreviewVO> list = cacheUtils.takeListFromCache(key, TopicPreviewVO.class);
            if (list != null)
                return list;       // 缓存命中，直接返回
        }
        Page<Topic> page = Page.of(pageNumber, 10);
        var wrapper = Wrappers.<Topic>query()
                .eq("status", Const.TOPIC_STATUS_PUBLISHED);  // 只查已发布
        if (hasTitle) {
            wrapper.like("title", title);  // 按标题模糊搜索
        }
        switch (sort) {
            case "views"    -> wrapper.orderByDesc("view_count");
            case "likes"    -> { page.setOptimizeCountSql(false); wrapper.last("ORDER BY (SELECT COUNT(*) FROM db_topic_interact_like WHERE tid = db_topic.id) DESC"); }
            case "collects" -> { page.setOptimizeCountSql(false); wrapper.last("ORDER BY (SELECT COUNT(*) FROM db_topic_interact_collect WHERE tid = db_topic.id) DESC"); }
            case "comments" -> { page.setOptimizeCountSql(false); wrapper.last("ORDER BY (SELECT COUNT(*) FROM db_topic_comment WHERE tid = db_topic.id AND status = 'normal') DESC"); }
            default         -> wrapper.orderByDesc("time");
        }
        if (typeId != null) {
            wrapper.eq("type", typeId);
        } else if (excludeSystemType) {
            List<Integer> systemTypeIds = this.systemTypeIds();
            if (!systemTypeIds.isEmpty()) {
                wrapper.notIn("type", systemTypeIds);
            }
        }
        baseMapper.selectPage(page, wrapper);      // 分页查数据库
        List<Topic> topics = page.getRecords();
        if (topics.isEmpty()) return null;
        List<TopicPreviewVO> result = topics.stream().map(this::resolveToPreview).toList();
        if (!hasTitle) {
            cacheUtils.saveListToCache(key, result, 60);
        }
        return result;
    }

    /**
     * 若帖子属于校园活动分类，将活动扩展字段填充到详情VO
     *
     * @param topic 帖子实体
     * @param vo    帖子详情VO
     */
    private void fillActivityFields(Topic topic, TopicDetailVO vo) {
        TopicType type = this.findTypeById(topic.getType());
        if (type == null || !SYSTEM_TYPE_ACTIVITY.equals(type.getSystemKey())) {
            return;
        }
        TopicActivity activity = topicActivityMapper.selectById(topic.getId());
        if (activity != null) {
            vo.setActivityTime(activity.getActivityTime());
            vo.setLocation(activity.getLocation());
            vo.setOrganizer(activity.getOrganizer());
            vo.setSignupDeadline(activity.getSignupDeadline());
        }
    }

    /**
     * 若帖子属于校园活动分类，将活动时间和地点填充到列表预览VO
     *
     * @param topic 帖子实体
     * @param vo    帖子预览VO
     */
    private void fillActivityPreview(Topic topic, TopicPreviewVO vo) {
        TopicType type = this.findTypeById(topic.getType());
        if (type == null || !SYSTEM_TYPE_ACTIVITY.equals(type.getSystemKey())) {
            return;
        }
        TopicActivity activity = topicActivityMapper.selectById(topic.getId());
        if (activity != null) {
            vo.setActivityTime(activity.getActivityTime());
            vo.setLocation(activity.getLocation());
        }
    }

    /**
     * 管理员分页查询评论列表（支持多条件筛选）
     *
     * @param page       页码
     * @param pageSize   每页条数
     * @param status     评论状态（可选）
     * @param content    评论内容关键词（可选）
     * @param author     评论作者用户名（可选）
     * @param topicTitle 所属帖子标题关键词（可选）
     * @param tid        帖子ID（可选）
     * @return 评论分页结果
     */
    @Override
    public PageResult<AdminCommentVO> adminListComments(int page, int pageSize, String status, String content, String author, String topicTitle, Integer tid) {
        Page<TopicComment> p = Page.of(page, pageSize);
        var wrapper = Wrappers.<TopicComment>query();
        if (tid != null)
            wrapper.eq("tid", tid);
        if (status != null && !status.isBlank())
            wrapper.eq("status", status);
        if (content != null && !content.isBlank())
            wrapper.like("content", content);
        if (author != null && !author.isBlank())
            wrapper.inSql("uid", "select id from db_account where username like '%" + author + "%'");
        if (topicTitle != null && !topicTitle.isBlank())
            wrapper.inSql("tid", "select id from db_topic where title like '%" + topicTitle + "%'");
        wrapper.orderByDesc("time");
        commentMapper.selectPage(p, wrapper);
        List<AdminCommentVO> list = p.getRecords().stream().map(comment -> {
            AdminCommentVO vo = new AdminCommentVO();
            BeanUtils.copyProperties(comment, vo);
            try {
                JSONObject json = JSONObject.parseObject(comment.getContent());
                StringBuilder sb = new StringBuilder();
                this.shortContent(json.getJSONArray("ops"), sb, ignore -> {});
                vo.setContent(sb.length() > 200 ? sb.substring(0, 200) : sb.toString());
            } catch (Exception e) {
                vo.setContent(comment.getContent());
            }
            Account account = accountMapper.selectById(comment.getUid());
            if (account != null) vo.setUsername(account.getUsername());
            Topic topic = baseMapper.selectById(comment.getTid());
            if (topic != null) vo.setTopicTitle(topic.getTitle());
            return vo;
        }).toList();
        return new PageResult<>(list, p.getTotal());
    }

    /**
     * 管理员删除评论（软删除）
     * @param id 评论ID
     */
    @Override
    public void adminDeleteComment(int id) {
        commentMapper.update(null, Wrappers.<TopicComment>update()
                .eq("id", id)
                .set("status", Const.COMMENT_STATUS_DELETED));
    }
}
