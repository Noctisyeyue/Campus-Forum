package com.campus.forum.controller;

import com.campus.forum.entity.RestBean;
import com.campus.forum.entity.vo.response.AdminDashboardVO;
import com.campus.forum.service.AdminDashboardService;
import jakarta.annotation.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * 管理员数据看板控制器
 */
@RestController
@RequestMapping("/api/admin/dashboard")
public class AdminDashboardController {

    /** 数据看板服务 */
    @Resource
    private AdminDashboardService adminDashboardService;

    /**
     * 获取管理员数据看板聚合数据
     *
     * @param startDate 趋势开始日期
     * @param endDate 趋势结束日期
     * @return 数据看板聚合结果
     */
    @GetMapping
    public RestBean<AdminDashboardVO> dashboard(
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        return RestBean.success(adminDashboardService.dashboard(startDate, endDate));
    }
}
