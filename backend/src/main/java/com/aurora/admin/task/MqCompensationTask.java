package com.aurora.admin.task;

import com.aurora.admin.dto.OrderMessage;
import com.aurora.admin.service.MessageProducer;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * MQ 消息补偿定时任务。
 * 每 2 分钟扫描 t_mq_message_log 中发送失败（status=2）且未超过最大重试次数的消息，
 * 通过 MessageProducer 重新投递。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MqCompensationTask {

    private final JdbcTemplate jdbcTemplate;
    private final MessageProducer messageProducer;
    private final ObjectMapper objectMapper;

    @Scheduled(cron = "0 */2 * * * ?")
    @SchedulerLock(name = "mqCompensation", lockAtMostFor = "PT5M", lockAtLeastFor = "PT30S")
    public void compensate() {
        List<Map<String, Object>> failedMessages = jdbcTemplate.queryForList(
                "SELECT message_id, message_body FROM t_mq_message_log " +
                "WHERE status = 2 AND retry_count < max_retry " +
                "AND (next_retry_time IS NULL OR next_retry_time <= NOW()) " +
                "AND is_deleted = 0 LIMIT 50"
        );

        if (failedMessages.isEmpty()) return;

        log.info("[MQ补偿] 扫描到 {} 条失败消息", failedMessages.size());

        for (Map<String, Object> row : failedMessages) {
            String messageId = (String) row.get("message_id");
            String body = (String) row.get("message_body");

            try {
                // 递增重试计数
                jdbcTemplate.update(
                        "UPDATE t_mq_message_log SET retry_count = retry_count + 1 WHERE message_id = ?",
                        messageId
                );

                // 反序列化并重新投递（MessageProducer 内部会更新发送状态）
                OrderMessage msg = objectMapper.readValue(body, OrderMessage.class);
                messageProducer.sendOrderNotification(msg);

                log.info("[MQ补偿] 重发成功: messageId={}", messageId);
            } catch (Exception e) {
                log.error("[MQ补偿] 重发失败: messageId={}", messageId, e);
                jdbcTemplate.update(
                        "UPDATE t_mq_message_log SET error_msg = ?, next_retry_time = DATE_ADD(NOW(), INTERVAL 5 MINUTE) WHERE message_id = ?",
                        e.getMessage(), messageId
                );
            }
        }
    }
}
