package com.aurora.admin.service;

import com.aurora.admin.entity.DashboardStats;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface StatsService {
    /**
     * 获取仪表盘统计数据。管理员返回全平台总量（用户数、文件数、消息数、订单数、收入等），
     * 普通用户仅返回个人维度的文件数和消息数。
     *
     * @param userId 当前用户 ID
     * @return 仪表盘统计对象
     */
    DashboardStats getDashboardStats(Long userId);

    /**
     * 获取趋势数据。按日期维度统计用户/文件/消息/订单的增量变化，管理员查看全平台趋势，
     * 普通用户仅查看个人趋势。
     *
     * @param days   统计天数
     * @param userId 当前用户 ID
     * @return 包含 dates、userData、fileData、messageData、orderData 列表的 Map
     */
    Map<String, Object> getTrendData(int days, Long userId);

    /**
     * 获取各类型资源的总量分布（用户、文件、消息、订单）。
     * 管理员查看全平台数据，普通用户仅查看个人文件数和消息数。
     *
     * @param userId 当前用户 ID
     * @return 类型到数量的映射 Map
     */
    Map<String, Object> getTypeDistribution(Long userId);

    /**
     * 获取订单状态分布。返回每种状态（如待付款、已付款、已取消等）对应的订单数量。
     *
     * @return 订单状态统计列表，每项包含状态名称和数量
     */
    List<Map<String, Object>> getOrderStatusDistribution();

    /**
     * 聚合指定日期的统计数据并写入 t_daily_stats
     */
    void aggregateDailyStats(LocalDate date);
}
