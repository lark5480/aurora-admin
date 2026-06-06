package com.aurora.admin.task;

import com.aurora.admin.service.NoticeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class NoticeScheduledTask {

    private static final Logger log = LoggerFactory.getLogger(NoticeScheduledTask.class);

    @Autowired
    private NoticeService noticeService;

    /**
     * 每分钟执行一次定时任务
     * - publish_time <= now && status == 'DRAFT' → 改为 'PUBLISHED'
     * - expire_time <= now && status == 'PUBLISHED' → 改为 'EXPIRED'
     */
    @Scheduled(fixedRate = 60000)
    @SchedulerLock(name = "processNotices", lockAtMostFor = "PT50S", lockAtLeastFor = "PT5S")
    public void processNotices() {
        log.debug("开始处理定时公告任务");
        try {
            noticeService.processScheduledNotices();
        } catch (Exception e) {
            log.error("定时公告任务执行异常", e);
        }
    }
}
