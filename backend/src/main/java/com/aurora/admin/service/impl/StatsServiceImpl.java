package com.aurora.admin.service.impl;

import com.aurora.admin.entity.DailyStats;
import com.aurora.admin.entity.DashboardStats;
import com.aurora.admin.mapper.DailyStatsMapper;
import com.aurora.admin.mapper.StatsMapper;
import com.aurora.admin.service.StatsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class StatsServiceImpl implements StatsService {

    private static final Logger log = LoggerFactory.getLogger(StatsServiceImpl.class);

    private static final String TYPE_USER = "user";
    private static final String TYPE_FILE = "file";
    private static final String TYPE_MESSAGE = "message";
    private static final String TYPE_ORDER = "order";

    @Autowired
    private StatsMapper statsMapper;

    @Autowired
    private DailyStatsMapper dailyStatsMapper;

    private boolean isCurrentUserAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return false;
        }
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority -> "ROLE_SUPER_ADMIN".equals(authority) || "ROLE_ADMIN".equals(authority));
    }

    @Override
    public DashboardStats getDashboardStats(Long userId) {
        if (isCurrentUserAdmin()) {
            DashboardStats stats = new DashboardStats();
            stats.setTotalUsers(statsMapper.countUsers());
            stats.setTotalFiles(statsMapper.countFiles());
            stats.setTotalMessages(statsMapper.countMessages());
            stats.setActiveUsersToday(statsMapper.countActiveUsersToday());
            stats.setTotalOrders(statsMapper.countOrders());
            stats.setOrdersToday(statsMapper.countOrdersToday());
            stats.setTotalRevenue(statsMapper.sumOrderAmount());
            stats.setRevenueToday(statsMapper.sumOrderAmountToday());
            return stats;
        }

        DashboardStats stats = new DashboardStats();
        stats.setTotalUsers(0L);
        stats.setTotalFiles(statsMapper.countFilesByUserId(userId));
        stats.setTotalMessages(statsMapper.countMessagesByUserId(userId));
        stats.setActiveUsersToday(statsMapper.countActiveOperationsTodayByUserId(userId));
        stats.setTotalOrders(0L);
        stats.setOrdersToday(0L);
        stats.setTotalRevenue(java.math.BigDecimal.ZERO);
        stats.setRevenueToday(java.math.BigDecimal.ZERO);
        return stats;
    }

    @Override
    public Map<String, Object> getTrendData(int days, Long userId) {
        List<String> dates = new ArrayList<>();
        List<Long> userData = new ArrayList<>();
        List<Long> fileData = new ArrayList<>();
        List<Long> messageData = new ArrayList<>();
        List<Long> orderData = new ArrayList<>();
        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusDays(days - 1);

        if (isCurrentUserAdmin()) {
            // 批量查询预聚合数据（不含今天）
            Map<LocalDate, Map<String, Integer>> aggregated = loadAggregatedStats(startDate, today.minusDays(1));

            for (int i = days - 1; i >= 0; i--) {
                LocalDate date = today.minusDays(i);
                dates.add(date.toString());

                if (date.isBefore(today)) {
                    // 历史日期：优先读预聚合数据，没有则实时查
                    Map<String, Integer> dayStats = aggregated.get(date);
                    if (dayStats != null) {
                        userData.add(dayStats.getOrDefault(TYPE_USER, 0).longValue());
                        fileData.add(dayStats.getOrDefault(TYPE_FILE, 0).longValue());
                        messageData.add(dayStats.getOrDefault(TYPE_MESSAGE, 0).longValue());
                        orderData.add(dayStats.getOrDefault(TYPE_ORDER, 0).longValue());
                    } else {
                        userData.add(statsMapper.countUsersByDate(date));
                        fileData.add(statsMapper.countFilesByDate(date));
                        messageData.add(statsMapper.countMessagesByDate(date));
                        orderData.add(statsMapper.countOrdersByDate(date));
                    }
                } else {
                    // 今天：实时查
                    userData.add(statsMapper.countUsersByDate(date));
                    fileData.add(statsMapper.countFilesByDate(date));
                    messageData.add(statsMapper.countMessagesByDate(date));
                    orderData.add(statsMapper.countOrdersByDate(date));
                }
            }
        } else {
            for (int i = days - 1; i >= 0; i--) {
                LocalDate date = today.minusDays(i);
                dates.add(date.toString());
                userData.add(0L);
                fileData.add(statsMapper.countFilesByDate(date));
                messageData.add(statsMapper.countMessagesByDate(date));
                orderData.add(statsMapper.countOrdersByDate(date));
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("dates", dates);
        result.put("userData", userData);
        result.put("fileData", fileData);
        result.put("messageData", messageData);
        result.put("orderData", orderData);
        return result;
    }

    @Override
    public Map<String, Object> getTypeDistribution(Long userId) {
        Map<String, Object> result = new HashMap<>();
        if (isCurrentUserAdmin()) {
            result.put("users", statsMapper.countUsers());
            result.put("files", statsMapper.countFiles());
            result.put("messages", statsMapper.countMessages());
            result.put("orders", statsMapper.countOrders());
        } else {
            result.put("users", 0L);
            result.put("files", statsMapper.countFilesByUserId(userId));
            result.put("messages", statsMapper.countMessagesByUserId(userId));
            result.put("orders", 0L);
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> getOrderStatusDistribution() {
        return statsMapper.countOrdersByStatus();
    }

    @Override
    public void aggregateDailyStats(LocalDate date) {
        log.info("开始聚合 {} 的每日统计数据", date);

        long userCount = statsMapper.countUsersByDate(date);
        long fileCount = statsMapper.countFilesByDate(date);
        long messageCount = statsMapper.countMessagesByDate(date);
        long orderCount = statsMapper.countOrdersByDate(date);

        upsertStat(date, TYPE_USER, (int) userCount);
        upsertStat(date, TYPE_FILE, (int) fileCount);
        upsertStat(date, TYPE_MESSAGE, (int) messageCount);
        upsertStat(date, TYPE_ORDER, (int) orderCount);

        log.info("聚合 {} 完成: user={}, file={}, message={}, order={}", date, userCount, fileCount, messageCount, orderCount);
    }

    private void upsertStat(LocalDate date, String type, int count) {
        DailyStats stats = new DailyStats();
        stats.setStatDate(date);
        stats.setStatType(type);
        stats.setStatCount(count);
        dailyStatsMapper.upsert(stats);
    }

    /**
     * 批量加载预聚合数据，按日期分组返回
     */
    private Map<LocalDate, Map<String, Integer>> loadAggregatedStats(LocalDate startDate, LocalDate endDate) {
        Map<LocalDate, Map<String, Integer>> result = new HashMap<>();
        if (startDate.isAfter(endDate)) {
            return result;
        }
        List<DailyStats> list = dailyStatsMapper.findByDateRange(startDate, endDate);
        for (DailyStats s : list) {
            result.computeIfAbsent(s.getStatDate(), k -> new HashMap<>())
                    .put(s.getStatType(), s.getStatCount());
        }
        return result;
    }
}
