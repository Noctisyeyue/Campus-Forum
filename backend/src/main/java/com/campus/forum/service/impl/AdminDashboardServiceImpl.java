package com.campus.forum.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.campus.forum.entity.dto.Account;
import com.campus.forum.entity.dto.Report;
import com.campus.forum.entity.dto.Topic;
import com.campus.forum.entity.dto.TopicComment;
import com.campus.forum.entity.dto.TopicType;
import com.campus.forum.entity.vo.response.AdminDashboardVO;
import com.campus.forum.mapper.AccountMapper;
import com.campus.forum.mapper.ReportMapper;
import com.campus.forum.mapper.TopicCommentMapper;
import com.campus.forum.mapper.TopicMapper;
import com.campus.forum.mapper.TopicTypeMapper;
import com.campus.forum.service.AdminDashboardService;
import com.campus.forum.utils.Const;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 管理员数据看板服务实现
 */
@Service
public class AdminDashboardServiceImpl implements AdminDashboardService {
    /** 默认趋势天数 */
    private static final int DEFAULT_RANGE_DAYS = 7;
    /** 系统默认时区 */
    private static final ZoneId ZONE_ID = ZoneId.systemDefault();
    /** 趋势图日期格式 */
    private static final DateTimeFormatter DAY_FORMATTER = DateTimeFormatter.ofPattern("MM-dd");

    /** 帖子数据访问 */
    @Resource
    private TopicMapper topicMapper;

    /** 评论数据访问 */
    @Resource
    private TopicCommentMapper topicCommentMapper;

    /** 分类数据访问 */
    @Resource
    private TopicTypeMapper topicTypeMapper;

    /** 举报数据访问 */
    @Resource
    private ReportMapper reportMapper;

    /** 用户数据访问 */
    @Resource
    private AccountMapper accountMapper;

    /**
     * 获取管理员数据看板聚合结果
     *
     * @param startDate 趋势开始日期
     * @param endDate 趋势结束日期
     * @return 数据看板聚合结果
     */
    @Override
    public AdminDashboardVO dashboard(LocalDate startDate, LocalDate endDate) {
        DateRange range = normalizeRange(startDate, endDate);
        AdminDashboardVO vo = new AdminDashboardVO();
        vo.setOverview(buildOverview());
        vo.setActivityTrend(buildActivityTrend(range.startDate(), range.endDate()));
        vo.setTopicStatusMap(buildTopicStatusMap());
        vo.setTopicTypeTop(buildTopicTypeTop());
        vo.setReportReasonMap(buildReportReasonMap());
        vo.setReportTargetTypeMap(buildReportTargetTypeMap());
        vo.setLatestPendingTopics(buildLatestPendingTopics());
        vo.setLatestPendingReports(buildLatestPendingReports());
        vo.setHotTopics(buildHotTopics());
        vo.setLatestUsers(buildLatestUsers());
        return vo;
    }

    /**
     * 构建总览统计数据
     *
     * @return 总览统计
     */
    private AdminDashboardVO.Overview buildOverview() {
        AdminDashboardVO.Overview overview = new AdminDashboardVO.Overview();
        overview.setTotalUsers(accountMapper.selectCount(null));
        overview.setTotalTopics(topicMapper.selectCount(null));
        overview.setTotalComments(topicCommentMapper.selectCount(null));
        overview.setPendingTopics(topicMapper.selectCount(Wrappers.<Topic>query()
                .eq("status", Const.TOPIC_STATUS_PENDING)));
        overview.setPendingReports(reportMapper.selectCount(Wrappers.<Report>query()
                .eq("status", Const.REPORT_STATUS_PENDING)));
        overview.setDisabledUsers(accountMapper.selectCount(Wrappers.<Account>query()
                .eq("status", "disabled")));
        return overview;
    }

    /**
     * 构建活跃趋势数据
     *
     * @param startDate 当前统计周期开始日期
     * @param endDate 当前统计周期结束日期
     * @return 活跃趋势
     */
    private AdminDashboardVO.ActivityTrend buildActivityTrend(LocalDate startDate, LocalDate endDate) {
        Date rangeStart = toDate(startDate);
        Date rangeEnd = toDate(endDate.plusDays(1));

        List<Topic> topics = topicMapper.selectList(Wrappers.<Topic>query()
                .ge("time", rangeStart)
                .lt("time", rangeEnd));
        List<TopicComment> comments = topicCommentMapper.selectList(Wrappers.<TopicComment>query()
                .ge("time", rangeStart)
                .lt("time", rangeEnd));
        List<Account> users = accountMapper.selectList(Wrappers.<Account>query()
                .ge("register_time", rangeStart)
                .lt("register_time", rangeEnd));

        Map<LocalDate, Long> topicDayMap = groupByDate(topics.stream().map(Topic::getTime).toList());
        Map<LocalDate, Long> commentDayMap = groupByDate(comments.stream().map(TopicComment::getTime).toList());
        Map<LocalDate, Long> userDayMap = groupByDate(users.stream().map(Account::getRegisterTime).toList());

        AdminDashboardVO.ActivityTrend trend = new AdminDashboardVO.ActivityTrend();
        trend.setTopicSummary(buildTrendSummary(topicDayMap, startDate, endDate));
        trend.setCommentSummary(buildTrendSummary(commentDayMap, startDate, endDate));
        trend.setUserSummary(buildTrendSummary(userDayMap, startDate, endDate));
        trend.setPoints(buildTrendPoints(topicDayMap, commentDayMap, userDayMap, startDate, endDate));
        return trend;
    }

    /**
     * 构建帖子状态分布
     *
     * @return 帖子状态统计
     */
    private Map<String, Long> buildTopicStatusMap() {
        Map<String, Long> map = new LinkedHashMap<>();
        map.put(Const.TOPIC_STATUS_PENDING, topicMapper.selectCount(Wrappers.<Topic>query()
                .eq("status", Const.TOPIC_STATUS_PENDING)));
        map.put(Const.TOPIC_STATUS_PUBLISHED, topicMapper.selectCount(Wrappers.<Topic>query()
                .eq("status", Const.TOPIC_STATUS_PUBLISHED)));
        map.put(Const.TOPIC_STATUS_REJECTED, topicMapper.selectCount(Wrappers.<Topic>query()
                .eq("status", Const.TOPIC_STATUS_REJECTED)));
        map.put(Const.TOPIC_STATUS_HIDDEN, topicMapper.selectCount(Wrappers.<Topic>query()
                .eq("status", Const.TOPIC_STATUS_HIDDEN)));
        map.put(Const.TOPIC_STATUS_DELETED, topicMapper.selectCount(Wrappers.<Topic>query()
                .eq("status", Const.TOPIC_STATUS_DELETED)));
        return map;
    }

    /**
     * 构建分类发帖 Top5 榜单
     *
     * @return 分类发帖榜单
     */
    private List<AdminDashboardVO.NameValue> buildTopicTypeTop() {
        Map<Integer, Long> countMap = toLongCountMap(topicMapper.batchTypeCounts());
        Map<Integer, TopicType> typeMap = mapTopicTypesByIds(countMap.keySet());
        return countMap.entrySet().stream()
                .map(entry -> {
                    TopicType type = typeMap.get(entry.getKey());
                    if (type == null) return null;
                    AdminDashboardVO.NameValue item = new AdminDashboardVO.NameValue();
                    item.setName(type.getName());
                    item.setValue(entry.getValue());
                    return item;
                })
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(AdminDashboardVO.NameValue::getValue).reversed())
                .limit(5)
                .toList();
    }

    /**
     * 构建举报原因分布
     *
     * @return 举报原因统计
     */
    private Map<String, Long> buildReportReasonMap() {
        return reportMapper.selectList(null).stream()
                .collect(Collectors.groupingBy(Report::getReason, LinkedHashMap::new, Collectors.counting()));
    }

    /**
     * 构建举报目标类型分布
     *
     * @return 举报目标类型统计
     */
    private Map<String, Long> buildReportTargetTypeMap() {
        return reportMapper.selectList(null).stream()
                .collect(Collectors.groupingBy(Report::getTargetType, LinkedHashMap::new, Collectors.counting()));
    }

    /**
     * 构建最近待审核帖子列表
     *
     * @return 待审核帖子列表
     */
    private List<AdminDashboardVO.PendingTopic> buildLatestPendingTopics() {
        List<Topic> topics = topicMapper.selectList(Wrappers.<Topic>query()
                .eq("status", Const.TOPIC_STATUS_PENDING)
                .orderByDesc("last_submit_time")
                .last("limit 5"));
        Map<Integer, Account> accountMap = mapAccountsByIds(topics.stream().map(Topic::getUid).collect(Collectors.toSet()));
        Map<Integer, TopicType> typeMap = mapTopicTypesByIds(topics.stream().map(Topic::getType).collect(Collectors.toSet()));
        return topics
                .stream()
                .map(topic -> {
                    AdminDashboardVO.PendingTopic item = new AdminDashboardVO.PendingTopic();
                    item.setId(topic.getId());
                    item.setTitle(topic.getTitle());
                    item.setLastSubmitTime(topic.getLastSubmitTime() != null ? topic.getLastSubmitTime() : topic.getTime());
                    Account account = accountMap.get(topic.getUid());
                    item.setUsername(account != null ? account.getUsername() : "未知用户");
                    TopicType type = typeMap.get(topic.getType());
                    item.setTypeName(type != null ? type.getName() : "未知分类");
                    return item;
                })
                .toList();
    }

    /**
     * 构建最近待处理举报列表
     *
     * @return 待处理举报列表
     */
    private List<AdminDashboardVO.PendingReport> buildLatestPendingReports() {
        List<Report> reports = reportMapper.selectList(Wrappers.<Report>query()
                .eq("status", Const.REPORT_STATUS_PENDING)
                .orderByDesc("time")
                .last("limit 5"));
        Map<Integer, Account> accountMap = mapAccountsByIds(reports.stream().map(Report::getUid).collect(Collectors.toSet()));
        Map<Integer, Topic> topicMap = mapTopicsByIds(reports.stream()
                .filter(report -> Const.REPORT_TARGET_TOPIC.equals(report.getTargetType()))
                .map(Report::getTargetId)
                .collect(Collectors.toSet()));
        Map<Integer, TopicComment> commentMap = mapCommentsByIds(reports.stream()
                .filter(report -> Const.REPORT_TARGET_COMMENT.equals(report.getTargetType()))
                .map(Report::getTargetId)
                .collect(Collectors.toSet()));
        return reports
                .stream()
                .map(report -> {
                    AdminDashboardVO.PendingReport item = new AdminDashboardVO.PendingReport();
                    item.setId(report.getId());
                    item.setTargetType(report.getTargetType());
                    item.setReason(report.getReason());
                    item.setTime(report.getTime());
                    Account account = accountMap.get(report.getUid());
                    item.setReporterName(account != null ? account.getUsername() : "未知用户");
                    item.setTargetSummary(resolveReportTargetSummary(report, topicMap, commentMap));
                    return item;
                })
                .toList();
    }

    /**
     * 解析举报目标摘要
     *
     * @param report 举报记录
     * @param topicMap 帖子映射
     * @param commentMap 评论映射
     * @return 目标摘要
     */
    private String resolveReportTargetSummary(Report report,
                                              Map<Integer, Topic> topicMap,
                                              Map<Integer, TopicComment> commentMap) {
        if (Const.REPORT_TARGET_TOPIC.equals(report.getTargetType())) {
            Topic topic = topicMap.get(report.getTargetId());
            return topic != null ? topic.getTitle() : "帖子已不存在";
        }
        if (Const.REPORT_TARGET_COMMENT.equals(report.getTargetType())) {
            TopicComment comment = commentMap.get(report.getTargetId());
            if (comment == null) return "评论已不存在";
            String content = extractPlainText(comment.getContent());
            if (content == null || content.isBlank()) return "评论内容为空";
            return content.length() > 36 ? content.substring(0, 36) + "..." : content;
        }
        return "未知目标";
    }

    /**
     * 构建热门内容榜单
     *
     * @return 热门内容列表
     */
    private List<AdminDashboardVO.HotTopic> buildHotTopics() {
        List<Topic> topics = topicMapper.selectList(Wrappers.<Topic>query()
                .ne("status", Const.TOPIC_STATUS_DELETED)
                .orderByDesc("view_count")
                .last("limit 20"));
        if (topics.isEmpty()) return List.of();
        List<Integer> topicIds = topics.stream().map(Topic::getId).toList();
        Map<Integer, Integer> commentCountMap = toIntegerCountMap(topicMapper.batchCommentCounts(topicIds));
        Map<Integer, Integer> likeCountMap = toIntegerCountMap(topicMapper.batchLikeCounts(topicIds));
        Map<Integer, Integer> collectCountMap = toIntegerCountMap(topicMapper.batchCollectCounts(topicIds));
        Map<Integer, Account> accountMap = mapAccountsByIds(topics.stream().map(Topic::getUid).collect(Collectors.toSet()));
        Map<Integer, TopicType> typeMap = mapTopicTypesByIds(topics.stream().map(Topic::getType).collect(Collectors.toSet()));
        return topics
                .stream()
                .map(topic -> {
                    AdminDashboardVO.HotTopic item = new AdminDashboardVO.HotTopic();
                    item.setId(topic.getId());
                    item.setTitle(topic.getTitle());
                    item.setStatus(topic.getStatus());
                    item.setViewCount(topic.getViewCount() == null ? 0 : topic.getViewCount());
                    item.setCommentCount(commentCountMap.getOrDefault(topic.getId(), 0));
                    item.setLikeCount(likeCountMap.getOrDefault(topic.getId(), 0));
                    item.setCollectCount(collectCountMap.getOrDefault(topic.getId(), 0));
                    Account account = accountMap.get(topic.getUid());
                    item.setUsername(account != null ? account.getUsername() : "未知用户");
                    TopicType type = typeMap.get(topic.getType());
                    item.setTypeName(type != null ? type.getName() : "未知分类");
                    return item;
                })
                .sorted(Comparator
                        .comparing(AdminDashboardVO.HotTopic::getViewCount, Comparator.reverseOrder())
                        .thenComparing(AdminDashboardVO.HotTopic::getCommentCount, Comparator.reverseOrder())
                        .thenComparing(AdminDashboardVO.HotTopic::getLikeCount, Comparator.reverseOrder())
                        .thenComparing(AdminDashboardVO.HotTopic::getCollectCount, Comparator.reverseOrder()))
                .limit(5)
                .toList();
    }

    /**
     * 构建最近注册用户列表
     *
     * @return 最近注册用户列表
     */
    private List<AdminDashboardVO.LatestUser> buildLatestUsers() {
        return accountMapper.selectList(Wrappers.<Account>query()
                        .eq("role", Const.ROLE_DEFAULT)
                        .orderByDesc("register_time")
                        .last("limit 5"))
                .stream()
                .map(account -> {
                    AdminDashboardVO.LatestUser item = new AdminDashboardVO.LatestUser();
                    item.setId(account.getId());
                    item.setUsername(account.getUsername());
                    item.setEmail(account.getEmail());
                    item.setRole(account.getRole());
                    item.setStatus(account.getStatus());
                    item.setAvatar(account.getAvatar());
                    item.setRegisterTime(account.getRegisterTime());
                    return item;
                })
                .toList();
    }

    /**
     * 批量加载用户映射
     *
     * @param userIds 用户ID集合
     * @return 以用户ID为键的映射
     */
    private Map<Integer, Account> mapAccountsByIds(Collection<Integer> userIds) {
        List<Integer> ids = sanitizeIds(userIds);
        if (ids.isEmpty()) return Map.of();
        return accountMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(Account::getId, account -> account));
    }

    /**
     * 批量加载分类映射
     *
     * @param typeIds 分类ID集合
     * @return 以分类ID为键的映射
     */
    private Map<Integer, TopicType> mapTopicTypesByIds(Collection<Integer> typeIds) {
        List<Integer> ids = sanitizeIds(typeIds);
        if (ids.isEmpty()) return Map.of();
        return topicTypeMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(TopicType::getId, type -> type));
    }

    /**
     * 批量加载帖子映射
     *
     * @param topicIds 帖子ID集合
     * @return 以帖子ID为键的映射
     */
    private Map<Integer, Topic> mapTopicsByIds(Collection<Integer> topicIds) {
        List<Integer> ids = sanitizeIds(topicIds);
        if (ids.isEmpty()) return Map.of();
        return topicMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(Topic::getId, topic -> topic));
    }

    /**
     * 批量加载评论映射
     *
     * @param commentIds 评论ID集合
     * @return 以评论ID为键的映射
     */
    private Map<Integer, TopicComment> mapCommentsByIds(Collection<Integer> commentIds) {
        List<Integer> ids = sanitizeIds(commentIds);
        if (ids.isEmpty()) return Map.of();
        return topicCommentMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(TopicComment::getId, comment -> comment));
    }

    /**
     * 过滤空值并转换为可查询的ID列表
     *
     * @param ids 原始ID集合
     * @return 过滤后的ID列表
     */
    private List<Integer> sanitizeIds(Collection<Integer> ids) {
        if (ids == null || ids.isEmpty()) return List.of();
        return ids.stream().filter(Objects::nonNull).distinct().toList();
    }

    /**
     * 将批量统计结果转换为整型计数映射
     *
     * @param rows 原始统计结果
     * @return 以ID为键、数量为值的映射
     */
    private Map<Integer, Integer> toIntegerCountMap(List<Map<String, Object>> rows) {
        Map<Integer, Integer> result = new HashMap<>();
        if (rows == null) return result;
        for (Map<String, Object> row : rows) {
            Number id = (Number) row.get("id");
            Number value = (Number) row.get("value");
            if (id != null && value != null) {
                result.put(id.intValue(), value.intValue());
            }
        }
        return result;
    }

    /**
     * 将批量统计结果转换为长整型计数映射
     *
     * @param rows 原始统计结果
     * @return 以ID为键、数量为值的映射
     */
    private Map<Integer, Long> toLongCountMap(List<Map<String, Object>> rows) {
        Map<Integer, Long> result = new HashMap<>();
        if (rows == null) return result;
        for (Map<String, Object> row : rows) {
            Number id = (Number) row.get("id");
            Number value = (Number) row.get("value");
            if (id != null && value != null) {
                result.put(id.intValue(), value.longValue());
            }
        }
        return result;
    }

    /**
     * 提取富文本中的纯文本
     *
     * @param content 原始富文本内容
     * @return 纯文本内容
     */
    private String extractPlainText(String content) {
        if (content == null) return null;
        try {
            var ops = com.alibaba.fastjson2.JSON.parseObject(content).getJSONArray("ops");
            StringBuilder sb = new StringBuilder();
            for (Object op : ops) {
                Object insert = com.alibaba.fastjson2.JSONObject.from(op).get("insert");
                if (insert instanceof String text) {
                    sb.append(text);
                }
            }
            return sb.toString().trim();
        } catch (Exception e) {
            return content;
        }
    }

    /**
     * 按日期聚合统计数量
     *
     * @param dates 时间列表
     * @return 日期统计映射
     */
    private Map<LocalDate, Long> groupByDate(List<Date> dates) {
        return dates.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(this::toLocalDate, LinkedHashMap::new, Collectors.counting()));
    }

    /**
     * 构建趋势摘要
     *
     * @param map 日期统计映射
     * @param startDate 当前周期开始日期
     * @param endDate 当前周期结束日期
     * @return 趋势摘要
     */
    private AdminDashboardVO.TrendSummary buildTrendSummary(Map<LocalDate, Long> map,
                                                            LocalDate startDate,
                                                            LocalDate endDate) {
        AdminDashboardVO.TrendSummary summary = new AdminDashboardVO.TrendSummary();
        summary.setCurrent(sumRange(map, startDate, endDate));
        return summary;
    }

    /**
     * 构建趋势点列表
     *
     * @param topicDayMap 发帖统计
     * @param commentDayMap 评论统计
     * @param userDayMap 注册统计
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 趋势点列表
     */
    private List<AdminDashboardVO.TrendPoint> buildTrendPoints(Map<LocalDate, Long> topicDayMap,
                                                               Map<LocalDate, Long> commentDayMap,
                                                               Map<LocalDate, Long> userDayMap,
                                                               LocalDate startDate,
                                                               LocalDate endDate) {
        return startDate.datesUntil(endDate.plusDays(1))
                .map(day -> {
                    AdminDashboardVO.TrendPoint point = new AdminDashboardVO.TrendPoint();
                    point.setDate(day.format(DAY_FORMATTER));
                    point.setTopics(topicDayMap.getOrDefault(day, 0L));
                    point.setComments(commentDayMap.getOrDefault(day, 0L));
                    point.setUsers(userDayMap.getOrDefault(day, 0L));
                    return point;
                })
                .toList();
    }

    /**
     * 统计日期区间内的总和
     *
     * @param map 日期统计映射
     * @param start 开始日期
     * @param end 结束日期
     * @return 区间总数
     */
    private long sumRange(Map<LocalDate, Long> map, LocalDate start, LocalDate end) {
        return start.datesUntil(end.plusDays(1))
                .mapToLong(day -> map.getOrDefault(day, 0L))
                .sum();
    }

    /**
     * 将 Date 转换为 LocalDate
     *
     * @param date 原始时间
     * @return 本地日期
     */
    private LocalDate toLocalDate(Date date) {
        return date.toInstant().atZone(ZONE_ID).toLocalDate();
    }

    /**
     * 将 LocalDate 转换为 Date
     *
     * @param date 本地日期
     * @return Date 时间
     */
    private Date toDate(LocalDate date) {
        return Date.from(date.atStartOfDay(ZONE_ID).toInstant());
    }

    /**
     * 规范化趋势日期范围
     *
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 允许的日期范围
     */
    private DateRange normalizeRange(LocalDate startDate, LocalDate endDate) {
        LocalDate today = LocalDate.now();
        LocalDate actualEnd = endDate == null || endDate.isAfter(today) ? today : endDate;
        LocalDate actualStart = startDate == null ? actualEnd.minusDays(DEFAULT_RANGE_DAYS - 1L) : startDate;
        if (actualStart.isAfter(today)) {
            actualStart = today;
        }
        if (actualStart.isAfter(actualEnd)) {
            LocalDate temp = actualStart;
            actualStart = actualEnd;
            actualEnd = temp;
        }
        return new DateRange(actualStart, actualEnd);
    }

    /**
     * 日期范围值对象
     *
     * @param startDate 开始日期
     * @param endDate 结束日期
     */
    private record DateRange(LocalDate startDate, LocalDate endDate) {
    }
}
