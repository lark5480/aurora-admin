package com.aurora.admin.controller;

import com.aurora.admin.dto.ApiResponse;
import com.aurora.admin.entity.DashboardStats;
import com.aurora.admin.service.StatsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/stats")
public class StatsController {

    @Autowired
    private StatsService statsService;

    @GetMapping("/dashboard")
    public ApiResponse getDashboard() {
        DashboardStats stats = statsService.getDashboardStats(getCurrentUserId());
        return ApiResponse.success(stats);
    }

    @GetMapping("/trend")
    public ApiResponse getTrend(@RequestParam(defaultValue = "7") int days) {
        return ApiResponse.success(statsService.getTrendData(days, getCurrentUserId()));
    }

    @GetMapping("/type-distribution")
    public ApiResponse getTypeDistribution() {
        return ApiResponse.success(statsService.getTypeDistribution(getCurrentUserId()));
    }

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
