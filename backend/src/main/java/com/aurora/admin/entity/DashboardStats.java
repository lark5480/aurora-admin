package com.aurora.admin.entity;

import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Getter
@Setter
public class DashboardStats {
    private Long totalUsers;
    private Long totalFiles;
    private Long totalMessages;
    private Long activeUsersToday;
    private Long totalOrders;
    private Long ordersToday;
    private BigDecimal totalRevenue;
    private BigDecimal revenueToday;
}
