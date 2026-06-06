package com.aurora.admin.task;

import com.aurora.admin.service.StatsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * 每日统计数据聚合定时任务。
 * 每天凌晨 1 点执行，聚合昨天的用户/文件/消息新增数到 t_daily_stats。
 * 同时回填最近 7 天内可能缺失的数据（防止任务漏执行导致数据断层）。
 */
@Component
public class DailyStatsScheduledTask {

    private static final Logger log = LoggerFactory.getLogger(DailyStatsScheduledTask.class);

    /** 回填天数，防止任务漏执行导致历史数据缺失 */
    private static final int BACKFILL_DAYS = 7;

    @Autowired
    private StatsService statsService;

    @Scheduled(cron = "0 0 1 * * ?")
    @SchedulerLock(name = "aggregateDailyStats", lockAtMostFor = "PT30M", lockAtLeastFor = "PT1M")
    public void aggregateDailyStats() {
        log.info("每日统计聚合任务开始");
        try {
            // 聚合昨天
            LocalDate yesterday = LocalDate.now().minusDays(1);
            statsService.aggregateDailyStats(yesterday);

            // 回填最近 BACKFILL_DAYS 天内可能缺失的数据
            for (int i = 2; i <= BACKFILL_DAYS; i++) {
                LocalDate date = LocalDate.now().minusDays(i);
                statsService.aggregateDailyStats(date);
            }

            log.info("每日统计聚合任务完成");
        } catch (Exception e) {
            log.error("每日统计聚合任务执行异常", e);
        }
    }
}
