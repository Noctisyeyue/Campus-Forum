package com.campus.forum.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.campus.forum.entity.dto.Report;
import com.campus.forum.entity.vo.request.ReportCreateVO;
import com.campus.forum.entity.vo.response.AdminReportVO;
import com.campus.forum.entity.vo.response.PageResult;

/**
 * 举报服务
 */
public interface ReportService extends IService<Report> {

    /**
     * 创建举报
     *
     * @param uid 用户ID
     * @param vo  举报创建参数
     * @return null 表示成功，非 null 为错误信息
     */
    String createReport(int uid, ReportCreateVO vo);

    /**
     * 分页查询举报列表（管理员，支持状态和目标类型筛选）
     *
     * @param page       页码
     * @param pageSize   每页条数
     * @param status     举报状态（可选）
     * @param targetType 目标类型（可选）
     * @return 分页结果
     */
    PageResult<AdminReportVO> adminListReports(int page, int pageSize, String status, String targetType);

    /**
     * 处理举报（通过或驳回）
     *
     * @param reportId 举报ID
     * @param adminId  操作管理员ID
     * @param action   处理动作（approve/dismiss）
     * @param note     管理员备注（可选）
     * @return null 表示成功，非 null 为错误信息
     */
    String resolveReport(int reportId, int adminId, String action, String note);

    /**
     * 检查用户是否已对指定目标发起过举报
     *
     * @param uid       用户ID
     * @param targetType 目标类型
     * @param targetId   目标ID
     * @return true=已举报，false=未举报
     */
    boolean hasReported(int uid, String targetType, int targetId);
}
