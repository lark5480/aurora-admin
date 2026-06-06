package com.aurora.admin.task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.aurora.admin.service.AfterSaleService;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;

@Component
public class AfterSaleScheduledTask {

    private static final Logger log = LoggerFactory.getLogger(AfterSaleScheduledTask.class);

    @Autowired
    private AfterSaleService afterSaleService;

    /** 每10分钟执行一次，自动审核超过24小时的售后单 */
    @Scheduled(fixedRate = 600000)
    @SchedulerLock(name = "autoApproveAfterSales", lockAtMostFor = "PT5M", lockAtLeastFor = "PT30S")
    public void autoApproveExpiredAfterSales() {
        log.debug("开始执行售后自动审核任务");
        try {
            int count = afterSaleService.autoApproveExpired();
            if (count > 0) {
                log.info("售后自动审核完成: {} 笔", count);
            }
        } catch (Exception e) {
            log.error("售后自动审核任务执行异常", e);
        }
    }
}
