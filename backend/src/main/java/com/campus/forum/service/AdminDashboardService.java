package com.campus.forum.service;

import com.campus.forum.entity.vo.response.AdminDashboardVO;

import java.time.LocalDate;

/**
 * 管理员数据看板服务
 */
public interface AdminDashboardService {
    /**
     * 获取管理员数据看板聚合数据
     *
     * @param startDate 趋势开始日期
     * @param endDate 趋势结束日期
     * @return 数据看板聚合结果
     */
    AdminDashboardVO dashboard(LocalDate startDate, LocalDate endDate);
}
