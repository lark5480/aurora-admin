package com.aurora.admin.service;

import com.aurora.admin.entity.DashboardStats;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface StatsService {
    DashboardStats getDashboardStats(Long userId);
    Map<String, Object> getTrendData(int days, Long userId);
    Map<String, Object> getTypeDistribution(Long userId);
    List<Map<String, Object>> getOrderStatusDistribution();

    /**
     * 聚合指定日期的统计数据并写入 t_daily_stats
     */
    void aggregateDailyStats(LocalDate date);
}
