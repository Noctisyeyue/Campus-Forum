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
    private static final String SYSTEM_TYPE_ACTIVITY = "activity";
    private static final String SYSTEM_TYPE_NOTICE = "notice";

    private static final Set<String> USER_TOPIC_VISIBLE_STATUS = Set.of(
            Const.TOPIC_STATUS_PENDING,
            Const.TOPIC_STATUS_PUBLISHED,
            Const.TOPIC_STATUS_REJECTED,
            Const.TOPIC_STATUS_HIDDEN,
            Const.TOPIC_STATUS_DELETED
    );

    private static final Set<String> USER_TOPIC_EDITABLE_STATUS = Set.of(
            Const.TOPIC_STATUS_PUBLISHED,
            Const.TOPIC_STATUS_REJECTED
    );

    private static final Set<String> USER_TOPIC_DELETABLE_STATUS = Set.of(
            Const.TOPIC_STATUS_PUBLISHED,
            Const.TOPIC_STATUS_PENDING
    );

    @Resource
    TopicTypeMapper mapper;

    @Resource
    TopicActivityMapper topicActivityMapper;

    @Resource
    ForumNoticeMapper forumNoticeMapper;

    @Resource
    FlowUtils flowUtils;

    @Resource
    CacheUtils cacheUtils;

    @Resource
    AccountMapper accountMapper;

    @Resource
    AccountDetailsMapper accountDetailsMapper;

    @Resource
    AccountPrivacyMapper accountPrivacyMapper;

    @Resource
    TopicCommentMapper commentMapper;

    @Resource
    StringRedisTemplate template;

    @Resource
    NotificationService notificationService;

    private Set<Integer> types = new HashSet<>();
    private Map<Integer, TopicType> typeIndex = new HashMap<>();

    @PostConstruct
    private void initTypes() {
        this.refreshTypeCache();
    }

    @Override
    public List<TopicType> listTypes() {
        this.refreshTypeCache();
        return mapper.selectList(Wrappers.<TopicType>query().orderByAsc("id"));
    }

    /**
     * 创建帖子，状态设为 pending_review（待审核）
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
        cacheUtils.deleteCachePattern(Const.FORUM_TOPIC_PREVIEW_CACHE + "*");
        return null;
    }

    /**
     * 创建评论
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
        if (!flowUtils.limitPeriodCounterCheck(key, 2, 60))
            return "发表评论频繁，请稍后再试！";
        TopicComment comment = new TopicComment();
        comment.setUid(uid);
        BeanUtils.copyProperties(vo, comment);
        comment.setTime(new Date());
        comment.setStatus(Const.COMMENT_STATUS_NORMAL);
        commentMapper.insert(comment);
        // 发送通知
        Account account = accountMapper.selectById(uid);
        if (vo.getQuote() > 0) {
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
                TopicComment comment = commentMapper.selectOne(Wrappers.<TopicComment>query()
                        .eq("id", dto.getQuote()));
                if (comment != null) {
                    JSONObject object = JSONObject.parseObject(comment.getContent());
                    StringBuilder builder = new StringBuilder();
                    this.shortContent(object.getJSONArray("ops"), builder, ignore -> {});
                    vo.setQuote(builder.toString());
                } else {
                    vo.setQuote("此评论已被删除");
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
     */
    @Override
    public void deleteComment(int id, int uid) {
        commentMapper.update(null, Wrappers.<TopicComment>update()
                .eq("id", id)
                .eq("uid", uid)
                .set("status", Const.COMMENT_STATUS_DELETED));
    }

    /**
     * 查询用户收藏的帖子
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

    @Override
    public List<TopicPreviewVO> listActivityByPage(int pageNumber, String title) {
        Integer typeId = this.resolveSystemTypeId(SYSTEM_TYPE_ACTIVITY);
        if (typeId == null) return null;
        return this.listPublishedTopicByPage(pageNumber, typeId, "activity", false, "time", title);
    }

    @Override
    public List<TopicPreviewVO> listNoticeTopicByPage(int pageNumber, String title) {
        Integer typeId = this.resolveSystemTypeId(SYSTEM_TYPE_NOTICE);
        if (typeId == null) return null;
        return this.listPublishedTopicByPage(pageNumber, typeId, "notice", false, "time", title);
    }

    /**
     * 查询置顶帖子（仅 published 状态）
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
     * 获取帖子详情
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
     * 获取当前用户自己的帖子详情
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
     */
    @Override
    public TopicDetailVO adminGetTopic(int tid) {
        Topic topic = baseMapper.selectById(tid);
        if (topic == null) return null;
        return this.buildTopicDetail(topic, 0);
    }

    private TopicDetailVO buildTopicDetail(Topic topic, int uid) {
        // 浏览量 +1
        baseMapper.update(null, Wrappers.<Topic>update()
                .eq("id", topic.getId())
                .setSql("view_count = view_count + 1"));
        TopicDetailVO vo = new TopicDetailVO();
        BeanUtils.copyProperties(topic, vo);
        vo.setViewCount(topic.getViewCount() == null ? 1 : topic.getViewCount() + 1);
        vo.setAllowComment(this.allowComment(topic));
        TopicDetailVO.Interact interact = new TopicDetailVO.Interact(
                uid > 0 && hasInteract(topic.getId(), uid, "like"),
                uid > 0 && hasInteract(topic.getId(), uid, "collect")
        );
        vo.setInteract(interact);
        TopicDetailVO.User user = new TopicDetailVO.User();
        vo.setUser(this.fillUserDetailsByPrivacy(user, topic.getUid()));
        vo.setComments(Boolean.TRUE.equals(vo.getAllowComment())
                ? commentMapper.selectCount(Wrappers.<TopicComment>query()
                .eq("tid", topic.getId())
                .eq("status", Const.COMMENT_STATUS_NORMAL))
                : 0L);
        this.fillActivityFields(topic, vo);
        return vo;
    }

    /**
     * 互动（点赞/收藏）操作，先写入 Redis 再延迟批量同步到数据库
     */
    @Override
    public void interact(Interact interact, boolean state) {
        String type = interact.getType();
        synchronized (type.intern()) {
            template.opsForHash().put(type, interact.toKey(), Boolean.toString(state));
            this.saveInteractSchedule(type);
        }
        // 立即同步该条互动到数据库，确保列表数量实时更新
        if (state) {
            baseMapper.addInteract(List.of(interact), type);
        } else {
            baseMapper.deleteInteract(List.of(interact), type);
        }
        cacheUtils.deleteCachePattern(Const.FORUM_TOPIC_PREVIEW_CACHE + "*");
    }

    // 检查用户是否对帖子有指定互动
    private boolean hasInteract(int tid, int uid, String type) {
        String key = tid + ":" + uid;
        if (template.opsForHash().hasKey(type, key))
            return Boolean.parseBoolean(template.opsForHash().entries(type).get(key).toString());
        return baseMapper.userInteractCount(tid, uid, type) > 0;
    }

    // 延迟批量同步互动数据到数据库
    private final Map<String, Boolean> state = new HashMap<>();
    ScheduledExecutorService service = Executors.newScheduledThreadPool(2);

    private void saveInteractSchedule(String type) {
        if (!state.getOrDefault(type, false)) {
            state.put(type, true);
            service.schedule(() -> {
                this.saveInteract(type);
                state.put(type, false);
            }, 3, TimeUnit.SECONDS);
        }
    }

    private void saveInteract(String type) {
        synchronized (type.intern()) {
            List<Interact> check = new LinkedList<>();
            List<Interact> uncheck = new LinkedList<>();
            template.opsForHash().entries(type).forEach((k, v) -> {
                if (Boolean.parseBoolean(v.toString()))
                    check.add(Interact.parseInteract(k.toString(), type));
                else
                    uncheck.add(Interact.parseInteract(k.toString(), type));
            });
            if (!check.isEmpty())
                baseMapper.addInteract(check, type);
            if (!uncheck.isEmpty())
                baseMapper.deleteInteract(uncheck, type);
            template.delete(type);
        }
    }

    // 根据隐私设置填充用户信息
    private <T> T fillUserDetailsByPrivacy(T target, int uid) {
        AccountDetails details = accountDetailsMapper.selectById(uid);
        Account account = accountMapper.selectById(uid);
        AccountPrivacy accountPrivacy = accountPrivacyMapper.selectById(uid);
        String[] ignores = accountPrivacy.hiddenFields();
        BeanUtils.copyProperties(account, target, ignores);
        BeanUtils.copyProperties(details, target, ignores);
        return target;
    }

    // 将 Topic 转换为 TopicPreviewVO（提取文本摘要和图片）
    private TopicPreviewVO resolveToPreview(Topic topic) {
        TopicPreviewVO vo = new TopicPreviewVO();
        BeanUtils.copyProperties(accountMapper.selectById(topic.getUid()), vo);
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
        this.fillActivityPreview(topic, vo);
        return vo;
    }

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

    // 从 Quill Delta ops 中提取纯文本和图片
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

    // 检查内容长度是否超限
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
    public List<AdminTopicVO> adminListTopics(int page, String status, Integer type, String title, String author) {
        Page<Topic> p = Page.of(page, 15);
        var wrapper = Wrappers.<Topic>query();
        if (status != null && !status.isBlank())
            wrapper.eq("status", status);
        if (type != null && type > 0)
            wrapper.eq("type", type);
        if (title != null && !title.isBlank())
            wrapper.like("title", title);
        if (author != null && !author.isBlank()) {
            // 子查询匹配作者用户名
            wrapper.inSql("uid", "select id from db_account where username like '%" + author + "%'");
        }
        wrapper.orderByDesc("time");
        baseMapper.selectPage(p, wrapper);
        return p.getRecords().stream().map(topic -> {
            AdminTopicVO vo = new AdminTopicVO();
            BeanUtils.copyProperties(topic, vo);
            // 填充作者用户名
            Account account = accountMapper.selectById(topic.getUid());
            if (account != null) vo.setUsername(account.getUsername());
            // 填充分类名称
            TopicType topicType = mapper.selectById(topic.getType());
            if (topicType != null) vo.setTypeName(topicType.getName());
            // 评论数
            vo.setCommentCount(commentMapper.selectCount(Wrappers.<TopicComment>query()
                    .eq("tid", topic.getId())
                    .eq("status", Const.COMMENT_STATUS_NORMAL)));
            return vo;
        }).toList();
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
     * @param tid 帖子ID
     * @param reason 下架原因
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
     * @param tid 帖子ID
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
        return null;
    }

    /**
     * 管理员删除帖子（物理删除，不可逆）
     * @param tid 帖子ID
     * @param adminId 操作管理员ID
     */
    @Override
    public String adminDeleteTopic(int tid, int adminId) {
        Topic topic = baseMapper.selectById(tid);
        if (topic == null) return "帖子不存在";
        if (Const.TOPIC_STATUS_PENDING.equals(topic.getStatus()))
            return "待审核帖子不能直接删除，请先通过或拒绝";
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

    private ForumNotice firstForumNotice() {
        return forumNoticeMapper.selectList(Wrappers.<ForumNotice>query()
                        .orderByAsc("id")
                        .last("limit 1"))
                .stream()
                .findFirst()
                .orElse(null);
    }

    private void refreshTypeCache() {
        List<TopicType> list = mapper.selectList(Wrappers.<TopicType>query().orderByAsc("id"));
        this.types = list.stream().map(TopicType::getId).collect(Collectors.toSet());
        this.typeIndex = list.stream().collect(Collectors.toMap(TopicType::getId, type -> type, (a, b) -> a, LinkedHashMap::new));
    }

    private TopicType findTypeById(int id) {
        TopicType type = this.typeIndex.get(id);
        if (type != null) {
            return type;
        }
        this.refreshTypeCache();
        return this.typeIndex.get(id);
    }

    private boolean isSystemType(TopicType type) {
        return type != null && type.getSystemKey() != null && !type.getSystemKey().isBlank();
    }

    private Integer resolveSystemTypeId(String systemKey) {
        this.refreshTypeCache();
        return this.typeIndex.values().stream()
                .filter(type -> systemKey.equals(type.getSystemKey()))
                .map(TopicType::getId)
                .findFirst()
                .orElse(null);
    }

    private List<Integer> systemTypeIds() {
        this.refreshTypeCache();
        return this.typeIndex.values().stream()
                .filter(this::isSystemType)
                .map(TopicType::getId)
                .toList();
    }

    private Boolean allowComment(Topic topic) {
        return topic.getAllowComment() == null || topic.getAllowComment() == 1;
    }

    private List<TopicPreviewVO> listPublishedTopicByPage(int pageNumber, Integer typeId, String cacheScope, boolean excludeSystemType, String sort, String title) {
        boolean hasTitle = title != null && !title.isBlank();
        String key = Const.FORUM_TOPIC_PREVIEW_CACHE + cacheScope + ":" + pageNumber + ":" + (typeId == null ? 0 : typeId) + ":" + sort + (hasTitle ? ":" + title : "");
        if (!hasTitle) {
            List<TopicPreviewVO> list = cacheUtils.takeListFromCache(key, TopicPreviewVO.class);
            if (list != null)
                return list;
        }
        Page<Topic> page = Page.of(pageNumber, 10);
        var wrapper = Wrappers.<Topic>query()
                .eq("status", Const.TOPIC_STATUS_PUBLISHED);
        if (hasTitle) {
            wrapper.like("title", title);
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
        baseMapper.selectPage(page, wrapper);
        List<Topic> topics = page.getRecords();
        if (topics.isEmpty()) return null;
        List<TopicPreviewVO> result = topics.stream().map(this::resolveToPreview).toList();
        if (!hasTitle) {
            cacheUtils.saveListToCache(key, result, 60);
        }
        return result;
    }

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
     * 管理员分页查询全部评论（支持帖子/用户筛选）
     * @param page 页码
     * @param tid 帖子ID（可选）
     * @param uid 用户ID（可选）
     * @return 评论列表
     */
    @Override
    public List<AdminCommentVO> adminListComments(int page, Integer tid, Integer uid) {
        Page<TopicComment> p = Page.of(page, 15);
        var wrapper = Wrappers.<TopicComment>query().ne("status", Const.COMMENT_STATUS_DELETED);
        if (tid != null && tid > 0)
            wrapper.eq("tid", tid);
        if (uid != null && uid > 0)
            wrapper.eq("uid", uid);
        wrapper.orderByDesc("time");
        commentMapper.selectPage(p, wrapper);
        return p.getRecords().stream().map(comment -> {
            AdminCommentVO vo = new AdminCommentVO();
            BeanUtils.copyProperties(comment, vo);
            // 提取纯文本内容
            try {
                JSONObject json = JSONObject.parseObject(comment.getContent());
                StringBuilder sb = new StringBuilder();
                this.shortContent(json.getJSONArray("ops"), sb, ignore -> {});
                vo.setContent(sb.length() > 200 ? sb.substring(0, 200) : sb.toString());
            } catch (Exception e) {
                vo.setContent(comment.getContent());
            }
            // 填充用户名
            Account account = accountMapper.selectById(comment.getUid());
            if (account != null) vo.setUsername(account.getUsername());
            // 填充帖子标题
            Topic topic = baseMapper.selectById(comment.getTid());
            if (topic != null) vo.setTopicTitle(topic.getTitle());
            return vo;
        }).toList();
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
