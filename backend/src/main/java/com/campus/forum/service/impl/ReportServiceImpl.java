package com.campus.forum.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.forum.entity.dto.Account;
import com.campus.forum.entity.dto.Report;
import com.campus.forum.entity.dto.Topic;
import com.campus.forum.entity.dto.TopicComment;
import com.campus.forum.entity.vo.request.ReportCreateVO;
import com.campus.forum.entity.vo.response.AdminReportVO;
import com.campus.forum.entity.vo.response.PageResult;
import com.campus.forum.mapper.AccountMapper;
import com.campus.forum.mapper.ReportMapper;
import com.campus.forum.mapper.TopicCommentMapper;
import com.campus.forum.mapper.TopicMapper;
import com.campus.forum.service.NotificationService;
import com.campus.forum.service.ReportService;
import com.campus.forum.service.TopicService;
import com.campus.forum.utils.Const;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
 * 举报服务实现类，处理举报的创建、查询与管理员审核
 */
@Service
public class ReportServiceImpl extends ServiceImpl<ReportMapper, Report> implements ReportService {

    /** 用户账号 Mapper */
    @Resource
    private AccountMapper accountMapper;

    /** 帖子 Mapper */
    @Resource
    private TopicMapper topicMapper;

    /** 评论 Mapper */
    @Resource
    private TopicCommentMapper topicCommentMapper;

    /** 帖子服务 */
    @Resource
    private TopicService topicService;

    /** 通知服务 */
    @Resource
    private NotificationService notificationService;

    /**
     * 创建举报记录
     *
     * @param uid 举报人用户 ID
     * @param vo  举报创建请求对象
     * @return 错误信息字符串，null 表示创建成功
     */
    @Override
    public String createReport(int uid, ReportCreateVO vo) {
        // 不能举报自己的内容
        if (Const.REPORT_TARGET_TOPIC.equals(vo.getTargetType())) {
            Topic topic = topicMapper.selectById(vo.getTargetId());
            if (topic == null) return "目标帖子不存在";
            if (Const.TOPIC_STATUS_DELETED.equals(topic.getStatus()) || Const.TOPIC_STATUS_HIDDEN.equals(topic.getStatus()))
                return "目标帖子已不可访问";
            if (topic.getUid() == uid) return "不能举报自己的内容";
        } else if (Const.REPORT_TARGET_COMMENT.equals(vo.getTargetType())) {
            TopicComment comment = topicCommentMapper.selectById(vo.getTargetId());
            if (comment == null) return "目标评论不存在";
            if (Const.COMMENT_STATUS_DELETED.equals(comment.getStatus()))
                return "目标评论已删除";
            if (comment.getUid() == uid) return "不能举报自己的内容";
        } else {
            return "无效的举报类型";
        }
        if (baseMapper.userReportCount(uid, vo.getTargetType(), vo.getTargetId()) > 0) {
            return "您已举报过该内容";
        }
        Report report = new Report();
        report.setUid(uid);
        report.setTargetType(vo.getTargetType());
        report.setTargetId(vo.getTargetId());
        report.setReason(vo.getReason());
        report.setDetail(vo.getDetail());
        report.setStatus(Const.REPORT_STATUS_PENDING);
        report.setTime(new Date());
        this.save(report);
        return null;
    }

    /**
     * 管理员分页查询举报列表
     *
     * @param page       当前页码
     * @param pageSize   每页条数
     * @param status     举报状态筛选条件，可为 null
     * @param targetType 举报目标类型筛选条件，可为 null
     * @return 分页包装的举报视图对象列表
     */
    @Override
    public PageResult<AdminReportVO> adminListReports(int page, int pageSize, String status, String targetType) {
        Page<Report> p = Page.of(page, pageSize);
        var wrapper = Wrappers.<Report>query();
        if (status != null && !status.isBlank()) {
            wrapper.eq("status", status);
        }
        if (targetType != null && !targetType.isBlank()) {
            wrapper.eq("target_type", targetType);
        }
        wrapper.orderByDesc("time");
        baseMapper.selectPage(p, wrapper);
        List<AdminReportVO> list = p.getRecords().stream().map(this::resolveToAdminVO).toList();
        return new PageResult<>(list, p.getTotal());
    }

    /**
     * 管理员处理举报，支持删除内容或驳回举报，并批量关闭同一目标的其他待处理举报
     *
     * @param reportId 举报记录 ID
     * @param adminId  处理的管理员用户 ID
     * @param action   处理操作类型："delete" 表示删除内容，"dismiss" 表示驳回
     * @param note     管理员备注
     * @return 错误信息字符串，null 表示处理成功
     */
    @Override
    public String resolveReport(int reportId, int adminId, String action, String note) {
        Report report = baseMapper.selectById(reportId);
        if (report == null) return "举报记录不存在";
        if (!Const.REPORT_STATUS_PENDING.equals(report.getStatus())) return "该举报已处理";

        if ("delete".equals(action)) {
            String targetType = report.getTargetType();
            if (Const.REPORT_TARGET_TOPIC.equals(targetType)) {
                String err = topicService.adminHideTopic(report.getTargetId(), "举报处理：因违反社区规范被下架");
                if (err != null) return err;
            } else if (Const.REPORT_TARGET_COMMENT.equals(targetType)) {
                topicService.adminDeleteComment(report.getTargetId());
            }
            report.setStatus(Const.REPORT_STATUS_RESOLVED);
        } else if ("dismiss".equals(action)) {
            report.setStatus(Const.REPORT_STATUS_DISMISSED);
        } else {
            return "无效的处理操作";
        }

        report.setAdminId(adminId);
        report.setAdminNote(note);
        report.setResolveTime(new Date());
        baseMapper.updateById(report);

        String resultMsg = "delete".equals(action) ? "已处理（内容已下架/删除）" : "已驳回";
        notificationService.addNotification(
                report.getUid(),
                "举报处理结果",
                "您举报的" + (Const.REPORT_TARGET_TOPIC.equals(report.getTargetType()) ? "帖子" : "评论") + resultMsg,
                "success",
                null
        );

        // 批量关闭同一目标的其他 pending 举报
        String batchStatus = "delete".equals(action) ? Const.REPORT_STATUS_RESOLVED : Const.REPORT_STATUS_DISMISSED;
        var otherPending = baseMapper.selectList(Wrappers.<Report>query()
                .eq("target_type", report.getTargetType())
                .eq("target_id", report.getTargetId())
                .eq("status", Const.REPORT_STATUS_PENDING));
        for (Report other : otherPending) {
            other.setStatus(batchStatus);
            other.setAdminId(adminId);
            other.setAdminNote(note);
            other.setResolveTime(new Date());
            baseMapper.updateById(other);
            notificationService.addNotification(
                    other.getUid(),
                    "举报处理结果",
                    "您举报的" + (Const.REPORT_TARGET_TOPIC.equals(other.getTargetType()) ? "帖子" : "评论") + resultMsg,
                    "success", null);
        }

        return null;
    }

    /**
     * 判断用户是否已对指定目标发起过举报
     *
     * @param uid        用户 ID
     * @param targetType 目标类型
     * @param targetId   目标 ID
     * @return true 表示已举报过，false 表示未举报
     */
    @Override
    public boolean hasReported(int uid, String targetType, int targetId) {
        return baseMapper.userReportCount(uid, targetType, targetId) > 0;
    }

    /**
     * 将举报实体转换为管理员视图对象，包含举报人名称和目标摘要
     *
     * @param report 举报实体
     * @return 管理员举报视图对象
     */
    private AdminReportVO resolveToAdminVO(Report report) {
        AdminReportVO vo = new AdminReportVO();
        vo.setId(report.getId());
        vo.setUid(report.getUid());
        Account reporter = accountMapper.selectById(report.getUid());
        vo.setReporterName(reporter != null ? reporter.getUsername() : "未知用户");
        vo.setTargetType(report.getTargetType());
        vo.setTargetId(report.getTargetId());
        vo.setTargetSummary(getTargetSummary(report.getTargetType(), report.getTargetId()));
        vo.setTopicId(resolveTopicId(report.getTargetType(), report.getTargetId()));
        vo.setReason(report.getReason());
        vo.setDetail(report.getDetail());
        vo.setStatus(report.getStatus());
        vo.setAdminNote(report.getAdminNote());
        vo.setTime(report.getTime());
        vo.setResolveTime(report.getResolveTime());
        return vo;
    }

    /**
     * 根据举报目标类型和 ID 解析所属帖子 ID
     *
     * @param targetType 目标类型（帖子或评论）
     * @param targetId   目标 ID
     * @return 所属帖子 ID，若目标已删除则返回 null
     */
    private Integer resolveTopicId(String targetType, int targetId) {
        if (Const.REPORT_TARGET_TOPIC.equals(targetType)) {
            return targetId;
        } else if (Const.REPORT_TARGET_COMMENT.equals(targetType)) {
            TopicComment comment = topicCommentMapper.selectById(targetId);
            return comment != null ? comment.getTid() : null;
        }
        return null;
    }

    /**
     * 获取举报目标的摘要文本，超长内容截断为 30 字符并追加省略号
     *
     * @param targetType 目标类型（帖子或评论）
     * @param targetId   目标 ID
     * @return 目标摘要字符串
     */
    private String getTargetSummary(String targetType, int targetId) {
        if (Const.REPORT_TARGET_TOPIC.equals(targetType)) {
            Topic topic = topicMapper.selectById(targetId);
            if (topic != null) {
                String title = topic.getTitle();
                return title != null && title.length() > 30 ? title.substring(0, 30) + "..." : title;
            }
        } else if (Const.REPORT_TARGET_COMMENT.equals(targetType)) {
            TopicComment comment = topicCommentMapper.selectById(targetId);
            if (comment != null) {
                String text = extractPlainText(comment.getContent());
                return text != null && text.length() > 30 ? text.substring(0, 30) + "..." : text;
            }
        }
        return "目标内容已删除";
    }

    /**
     * 从 Quill 富文本 JSON 内容中提取纯文本，解析失败时原样返回
     *
     * @param content Quill 编辑器产生的 JSON 格式富文本内容
     * @return 提取的纯文本字符串
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
}
