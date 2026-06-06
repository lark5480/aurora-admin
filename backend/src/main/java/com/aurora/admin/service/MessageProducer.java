package com.aurora.admin.service;

import com.aurora.admin.dto.OrderMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageProducer implements RabbitTemplate.ConfirmCallback, RabbitTemplate.ReturnsCallback {

    private final RabbitTemplate rabbitTemplate;
    private final JdbcTemplate jdbcTemplate;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String CONSUMED_KEY_PREFIX = "mq:consumed:";
    private static final Duration CONSUMED_KEY_TTL = Duration.ofHours(24);

    public void sendOrderNotification(OrderMessage msg) {
        String msgJson = null;
        try {
            msgJson = objectMapper.writeValueAsString(msg);
        } catch (Exception e) {
            log.error("[MQ] 序列化消息失败: messageId={}", msg.messageId(), e);
            return;
        }

        // 1. 先写本地消息表（status=0 待发送）
        try {
            jdbcTemplate.update(
                    "INSERT IGNORE INTO t_mq_message_log(message_id, exchange_name, routing_key, message_body, status) VALUES(?,?,?,?,0)",
                    msg.messageId(), "order.exchange", "order.notify", msgJson
            );
        } catch (Exception e) {
            log.warn("[MQ] 写消息表失败（可能已存在）: messageId={}", msg.messageId(), e);
        }

        // 2. 发送消息，带 CorrelationData 用于 confirm 回调
        CorrelationData correlationData = new CorrelationData(msg.messageId());
        rabbitTemplate.setConfirmCallback(this);
        rabbitTemplate.setReturnsCallback(this);
        rabbitTemplate.convertAndSend("order.exchange", "order.notify", msg, correlationData);

        log.info("[MQ] 消息已发送: messageId={}, orderNo={}, type={}", msg.messageId(), msg.orderNo(), msg.type());
    }

    /**
     * Publisher Confirm 回调：Broker 确认收到消息
     */
    @Override
    public void confirm(CorrelationData correlationData, boolean ack, String cause) {
        if (correlationData == null) return;
        String messageId = correlationData.getId();

        if (ack) {
            // Broker 已确认，更新状态为已发送
            jdbcTemplate.update("UPDATE t_mq_message_log SET status = 1 WHERE message_id = ?", messageId);
            log.debug("[MQ] Broker确认: messageId={}", messageId);
        } else {
            // Broker 拒绝，标记为发送失败，等补偿任务重发
            jdbcTemplate.update(
                    "UPDATE t_mq_message_log SET status = 2, error_msg = ?, next_retry_time = ? WHERE message_id = ?",
                    cause, LocalDateTime.now().plusMinutes(1), messageId
            );
            log.error("[MQ] Broker拒绝: messageId={}, cause={}", messageId, cause);
        }
    }

    /**
     * Publisher Return 回调：消息无法路由到队列
     */
    @Override
    public void returnedMessage(org.springframework.amqp.core.ReturnedMessage returned) {
        log.error("[MQ] 消息无法路由: exchange={}, routingKey={}, replyCode={}, replyText={}",
                returned.getExchange(), returned.getRoutingKey(),
                returned.getReplyCode(), returned.getReplyText());
    }

    /**
     * 消费端幂等检查：判断消息是否已被消费
     */
    public boolean isConsumed(String messageId) {
        if (messageId == null) return false;
        return Boolean.TRUE.equals(redisTemplate.hasKey(CONSUMED_KEY_PREFIX + messageId));
    }

    /**
     * 标记消息已消费
     */
    public void markConsumed(String messageId) {
        if (messageId == null) return;
        redisTemplate.opsForValue().set(CONSUMED_KEY_PREFIX + messageId, "1", CONSUMED_KEY_TTL);
    }
}
