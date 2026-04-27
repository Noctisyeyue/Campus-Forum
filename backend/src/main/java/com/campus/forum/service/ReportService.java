package com.campus.forum.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.campus.forum.entity.dto.Report;
import com.campus.forum.entity.vo.request.ReportCreateVO;
import com.campus.forum.entity.vo.response.AdminReportVO;

import java.util.List;

public interface ReportService extends IService<Report> {
    String createReport(int uid, ReportCreateVO vo);
    List<AdminReportVO> adminListReports(int page, String status, String targetType);
    String resolveReport(int reportId, int adminId, String action, String note);
    boolean hasReported(int uid, String targetType, int targetId);
}
