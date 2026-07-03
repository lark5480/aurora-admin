package com.aurora.admin.controller;

import com.aurora.admin.dto.ApiResponse;
import com.aurora.admin.entity.DashboardStats;
import com.aurora.admin.service.StatsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * 数据统计控制器。提供仪表盘概览、趋势图、类型分布、订单状态分布等统计数据的 REST 接口。
 * 映射路径：/api/stats
 */
@RestController
@RequestMapping("/api/stats")
public class StatsController {

    @Autowired
    private StatsService statsService;

    /**
     * 获取仪表盘概览数据。管理员可查看全平台统计（用户数、文件数、消息数、订单数、收入等）；
     * 普通用户仅查看个人维度的数据。
     *
     * @return 仪表盘统计数据
     */
    @GetMapping("/dashboard")
    public ApiResponse getDashboard() {
        DashboardStats stats = statsService.getDashboardStats(getCurrentUserId());
        return ApiResponse.success(stats);
    }

    /**
     * 获取指定天数的趋势数据。按日期返回用户、文件、消息、订单的增量变化趋势。
     *
     * @param days 统计天数，默认 7 天
     * @return 趋势数据，包含 dates、userData、fileData、messageData、orderData 列表
     */
    @GetMapping("/trend")
    public ApiResponse getTrend(@RequestParam(defaultValue = "7") int days) {
        return ApiResponse.success(statsService.getTrendData(days, getCurrentUserId()));
    }

    /**
     * 获取各类型（用户、文件、消息、订单）的总量分布。
     *
     * @return 类型到数量的映射数据
     */
    @GetMapping("/type-distribution")
    public ApiResponse getTypeDistribution() {
        return ApiResponse.success(statsService.getTypeDistribution(getCurrentUserId()));
    }

    /**
     * 获取订单状态分布统计。返回各状态（如待付款、已付款、已取消等）对应的订单数量。
     *
     * @return 订单状态统计列表
     */
    @GetMapping("/order-status")
    public ApiResponse getOrderStatusDistribution() {
        return ApiResponse.success(statsService.getOrderStatusDistribution());
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getCredentials() != null) {
            return (Long) auth.getCredentials();
        }
        return 0L;
    }
}
