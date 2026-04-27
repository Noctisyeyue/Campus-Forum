package com.campus.forum.controller;

import com.campus.forum.entity.RestBean;
import com.campus.forum.entity.vo.response.AdminReportVO;
import com.campus.forum.service.ReportService;
import com.campus.forum.utils.Const;
import com.campus.forum.utils.ControllerUtils;
import jakarta.annotation.Resource;
import jakarta.validation.constraints.Min;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/reports")
public class AdminReportController {

    @Resource
    ReportService reportService;

    @Resource
    ControllerUtils utils;

    @GetMapping
    public RestBean<List<AdminReportVO>> listReports(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String targetType) {
        return RestBean.success(reportService.adminListReports(page + 1, status, targetType));
    }

    @PostMapping("/{id}/resolve")
    public RestBean<Void> resolveReport(@PathVariable int id,
                                        @RequestParam String action,
                                        @RequestParam(required = false) String note,
                                        @RequestAttribute(Const.ATTR_USER_ID) int adminId) {
        return utils.messageHandle(() -> reportService.resolveReport(id, adminId, action, note));
    }
}
