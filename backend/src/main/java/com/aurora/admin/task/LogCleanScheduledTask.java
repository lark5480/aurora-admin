package com.aurora.admin.task;

import com.aurora.admin.service.OperationLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 操作日志定时清理任务。
 * 每月 1 号凌晨 0 点执行，删除 30 天之前的旧日志。
 */
@Component
public class LogCleanScheduledTask {

    private static final Logger log = LoggerFactory.getLogger(LogCleanScheduledTask.class);

    private static final int RETAIN_DAYS = 30;

    @Autowired
    private OperationLogService operationLogService;

    @Scheduled(cron = "0 0 0 1 * ?")
    @SchedulerLock(name = "cleanOldLogs", lockAtMostFor = "PT30M", lockAtLeastFor = "PT1M")
    public void cleanOldLogs() {
        log.info("定时清理操作日志开始，保留最近{}天", RETAIN_DAYS);
        try {
            operationLogService.cleanOldLogs(RETAIN_DAYS);
            log.info("定时清理操作日志完成");
        } catch (Exception e) {
            log.error("定时清理操作日志异常", e);
        }
    }
}
