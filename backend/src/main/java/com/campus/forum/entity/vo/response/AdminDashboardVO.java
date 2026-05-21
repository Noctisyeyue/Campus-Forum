package com.campus.forum.entity.vo.response;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 管理员数据看板聚合响应
 */
@Data
public class AdminDashboardVO {
    /** 总览统计 */
    Overview overview;
    /** 活跃趋势 */
    ActivityTrend activityTrend;
    /** 帖子状态分布 */
    Map<String, Long> topicStatusMap;
    /** 分类帖子榜单 */
    List<NameValue> topicTypeTop;
    /** 举报原因分布 */
    Map<String, Long> reportReasonMap;
    /** 举报目标类型分布 */
    Map<String, Long> reportTargetTypeMap;
    /** 最近待审核帖子 */
    List<PendingTopic> latestPendingTopics;
    /** 最近待处理举报 */
    List<PendingReport> latestPendingReports;
    /** 热门内容榜单 */
    List<HotTopic> hotTopics;
    /** 最近注册用户 */
    List<LatestUser> latestUsers;

    /** 总览统计信息 */
    @Data
    public static class Overview {
        /** 用户总数 */
        Long totalUsers;
        /** 帖子总数 */
        Long totalTopics;
        /** 评论总数 */
        Long totalComments;
        /** 待审核帖子数 */
        Long pendingTopics;
        /** 待处理举报数 */
        Long pendingReports;
        /** 已禁用用户数 */
        Long disabledUsers;
    }

    /** 名称-数值结构 */
    @Data
    public static class NameValue {
        /** 名称 */
        String name;
        /** 数值 */
        Long value;
    }

    /** 活跃趋势结构 */
    @Data
    public static class ActivityTrend {
        /** 发帖趋势摘要 */
        TrendSummary topicSummary;
        /** 评论趋势摘要 */
        TrendSummary commentSummary;
        /** 注册趋势摘要 */
        TrendSummary userSummary;
        /** 每日趋势点 */
        List<TrendPoint> points;
    }

    /** 趋势摘要 */
    @Data
    public static class TrendSummary {
        /** 当前周期值 */
        Long current;
    }

    /** 每日趋势点 */
    @Data
    public static class TrendPoint {
        /** 日期标签 */
        String date;
        /** 当日发帖数 */
        Long topics;
        /** 当日评论数 */
        Long comments;
        /** 当日注册数 */
        Long users;
    }

    /** 待审核帖子 */
    @Data
    public static class PendingTopic {
        /** 帖子ID */
        Integer id;
        /** 标题 */
        String title;
        /** 作者用户名 */
        String username;
        /** 分类名称 */
        String typeName;
        /** 最近提交时间 */
        java.util.Date lastSubmitTime;
    }

    /** 待处理举报 */
    @Data
    public static class PendingReport {
        /** 举报ID */
        Integer id;
        /** 举报人 */
        String reporterName;
        /** 举报目标类型 */
        String targetType;
        /** 目标摘要 */
        String targetSummary;
        /** 举报原因 */
        String reason;
        /** 举报时间 */
        java.util.Date time;
    }

    /** 热门帖子 */
    @Data
    public static class HotTopic {
        /** 帖子ID */
        Integer id;
        /** 标题 */
        String title;
        /** 作者用户名 */
        String username;
        /** 分类名称 */
        String typeName;
        /** 帖子状态 */
        String status;
        /** 浏览量 */
        Integer viewCount;
        /** 评论数 */
        Integer commentCount;
        /** 点赞数 */
        Integer likeCount;
        /** 收藏数 */
        Integer collectCount;
    }

    /** 最近注册用户 */
    @Data
    public static class LatestUser {
        /** 用户ID */
        Integer id;
        /** 用户名 */
        String username;
        /** 邮箱 */
        String email;
        /** 角色 */
        String role;
        /** 状态 */
        String status;
        /** 头像 */
        String avatar;
        /** 注册时间 */
        java.util.Date registerTime;
    }
}
