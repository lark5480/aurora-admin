package com.aurora.admin.service;

import com.aurora.admin.dto.OrderMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Duration;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MessageProducer 单元测试")
class MessageProducerTest {

    @Mock private RabbitTemplate rabbitTemplate;
    @Mock private JdbcTemplate jdbcTemplate;
    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOps;

    @Spy private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private MessageProducer messageProducer;

    private OrderMessage buildMessage(String messageId) {
        return new OrderMessage(messageId, "ORDER001", 1L, "testuser",
                "ORDER_CREATED", "订单创建成功", "2026-06-01T12:00:00");
    }

    // ==================== sendOrderNotification ====================

    @Nested
    @DisplayName("发送消息")
    class SendNotification {

        @Test
        @DisplayName("正常发送：写入消息表 + 发送到 RabbitMQ")
        void send_success() {
            OrderMessage msg = buildMessage("msg-001");

            messageProducer.sendOrderNotification(msg);

            verify(jdbcTemplate).update(
                    contains("INSERT IGNORE INTO t_mq_message_log"),
                    eq("msg-001"), eq("order.exchange"), eq("order.notify"), anyString()
            );
            verify(rabbitTemplate).setConfirmCallback(messageProducer);
            verify(rabbitTemplate).setReturnsCallback(messageProducer);
            verify(rabbitTemplate).convertAndSend(
                    eq("order.exchange"), eq("order.notify"), eq(msg),
                    argThat((CorrelationData cd) -> "msg-001".equals(cd.getId()))
            );
        }

        @Test
        @DisplayName("写消息表失败不阻断发送")
        void send_dbWriteFails_stillSends() {
            OrderMessage msg = buildMessage("msg-002");
            doThrow(new RuntimeException("DB error"))
                    .when(jdbcTemplate).update(anyString(), any(), any(), any(), any());

            // 不应抛异常
            messageProducer.sendOrderNotification(msg);

            verify(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class), any(CorrelationData.class));
        }
    }

    // ==================== confirm callback ====================

    @Nested
    @DisplayName("Publisher Confirm 回调")
    class ConfirmCallback {

        @Test
        @DisplayName("Broker ACK：更新状态为 1")
        void confirm_ack() {
            CorrelationData cd = new CorrelationData("msg-001");

            messageProducer.confirm(cd, true, null);

            verify(jdbcTemplate).update(
                    contains("SET status = 1"),
                    eq("msg-001")
            );
        }

        @Test
        @DisplayName("Broker NACK：更新状态为 2，记录错误信息")
        void confirm_nack() {
            CorrelationData cd = new CorrelationData("msg-002");

            messageProducer.confirm(cd, false, "channel closed");

            verify(jdbcTemplate).update(
                    contains("SET status = 2"),
                    eq("channel closed"), any(), eq("msg-002")
            );
        }

        @Test
        @DisplayName("correlationData 为 null 时安全跳过")
        void confirm_nullCorrelation() {
            messageProducer.confirm(null, true, null);

            verify(jdbcTemplate, never()).update(anyString(), any(), any());
        }
    }

    // ==================== returnedMessage ====================

    @Test
    @DisplayName("消息无法路由：仅记录日志，不抛异常")
    void returnedMessage_loggedOnly() {
        ReturnedMessage returned = mock(ReturnedMessage.class);
        when(returned.getExchange()).thenReturn("order.exchange");
        when(returned.getRoutingKey()).thenReturn("bad.key");
        when(returned.getReplyCode()).thenReturn(312);
        when(returned.getReplyText()).thenReturn("NO_ROUTE");

        assertThatCode(() -> messageProducer.returnedMessage(returned))
                .doesNotThrowAnyException();
    }

    // ==================== isConsumed ====================

    @Nested
    @DisplayName("消费端幂等检查")
    class IsConsumed {

        @Test
        @DisplayName("已消费返回 true")
        void isConsumed_true() {
            when(redisTemplate.hasKey("mq:consumed:msg-001")).thenReturn(true);

            assertThat(messageProducer.isConsumed("msg-001")).isTrue();
        }

        @Test
        @DisplayName("未消费返回 false")
        void isConsumed_false() {
            when(redisTemplate.hasKey("mq:consumed:msg-new")).thenReturn(false);

            assertThat(messageProducer.isConsumed("msg-new")).isFalse();
        }

        @Test
        @DisplayName("messageId 为 null 返回 false")
        void isConsumed_nullMessageId() {
            assertThat(messageProducer.isConsumed(null)).isFalse();
            verifyNoInteractions(redisTemplate);
        }
    }

    // ==================== markConsumed ====================

    @Nested
    @DisplayName("标记已消费")
    class MarkConsumed {

        @Test
        @DisplayName("写入 Redis Key 并设 24h TTL")
        void markConsumed_success() {
            when(redisTemplate.opsForValue()).thenReturn(valueOps);

            messageProducer.markConsumed("msg-001");

            verify(valueOps).set(eq("mq:consumed:msg-001"), eq("1"), eq(Duration.ofHours(24)));
        }

        @Test
        @DisplayName("messageId 为 null 时不操作 Redis")
        void markConsumed_nullMessageId() {
            messageProducer.markConsumed(null);

            verifyNoInteractions(redisTemplate);
        }
    }
}
