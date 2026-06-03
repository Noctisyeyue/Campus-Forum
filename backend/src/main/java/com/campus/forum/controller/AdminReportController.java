package com.campus.forum.controller;

import com.campus.forum.entity.RestBean;
import com.campus.forum.entity.vo.response.AdminReportVO;
import com.campus.forum.entity.vo.response.PageResult;
import com.campus.forum.service.ReportService;
import com.campus.forum.utils.Const;
import com.campus.forum.utils.ControllerUtils;
import jakarta.annotation.Resource;
import jakarta.validation.constraints.Min;
import org.springframework.web.bind.annotation.*;

/**
 * 管理员举报管理控制器
 */
@RestController
@RequestMapping("/api/admin/reports")
public class AdminReportController {

    /** 举报服务 */
    @Resource
    ReportService reportService;

    /** 通用控制器工具 */
    @Resource
    ControllerUtils utils;

    /**
     * 分页查询举报列表（支持按状态和目标类型筛选）
     * @param page 页码（从0开始）
     * @param pageSize 每页条数（默认15）
     * @param status 举报状态（可选）
     * @param targetType 目标类型（可选）
     * @return 举报列表
     */
    @GetMapping
    public RestBean<PageResult<AdminReportVO>> listReports(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "15") @Min(1) int pageSize,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String targetType) {
        return RestBean.success(reportService.adminListReports(page + 1, pageSize, status, targetType));
    }

    /**
     * 处理举报（通过或驳回）
     *
     * @param id      举报ID
     * @param action  处理动作（approve/dismiss）
     * @param note    管理员备注（可选）
     * @param adminId 操作管理员ID
     * @return 操作结果
     */
    @PostMapping("/{id}/resolve")
    public RestBean<Void> resolveReport(@PathVariable int id,
                                        @RequestParam String action,
                                        @RequestParam(required = false) String note,
                                        @RequestAttribute(Const.ATTR_USER_ID) int adminId) {
        return utils.messageHandle(() -> reportService.resolveReport(id, adminId, action, note));
    }
}
