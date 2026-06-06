package com.aurora.admin.listener;

import com.aurora.admin.dto.ApiResponse;
import com.aurora.admin.dto.OrderMessage;
import com.aurora.admin.entity.Message;
import com.aurora.admin.mapper.MessageMapper;
import com.aurora.admin.service.MessageProducer;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderNotificationListener {

    private final MessageMapper messageMapper;
    private final MessageProducer messageProducer;
    private final SimpMessagingTemplate messagingTemplate;

    @RabbitListener(queues = "order.notify")
    public void handleOrderNotification(
            OrderMessage msg,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {

        log.info("[MQ] 收到订单通知: messageId={}, orderNo={}, type={}",
                msg.messageId(), msg.orderNo(), msg.type());

        // 1. 幂等检查：messageId 去重
        if (msg.messageId() != null && messageProducer.isConsumed(msg.messageId())) {
            log.info("[MQ] 重复消息，跳过: messageId={}", msg.messageId());
            try {
                channel.basicAck(deliveryTag, false);
            } catch (Exception e) {
                log.warn("[MQ] ACK重复消息失败", e);
            }
            return;
        }

        try {
            // 2. 写入 DB 消息记录
            Message message = new Message();
            message.setTitle(buildTitle(msg.type()));
            message.setContent(String.format("订单号：%s | %s", msg.orderNo(), msg.message()));
            message.setType("ORDER");
            message.setPriority("NORMAL");
            message.setSenderId(0L);
            message.setSenderName("系统");
            message.setRecipientId(msg.userId());
            message.setIsRead(0);
            message.setIsDeleted(0);
            message.setCreateTime(LocalDateTime.now());
            messageMapper.insert(message);

            // 3. 推送 WebSocket 实时通知
            try {
                messagingTemplate.convertAndSendToUser(
                        msg.userId().toString(),
                        "/queue/messages",
                        ApiResponse.success(message)
                );
                log.info("[MQ] WebSocket 推送成功: userId={}, orderNo={}", msg.userId(), msg.orderNo());
            } catch (Exception e) {
                log.warn("[MQ] WebSocket 推送失败: userId={}, orderNo={}, error={}",
                        msg.userId(), msg.orderNo(), e.getMessage());
            }

            // 4. 标记已消费 + ACK
            messageProducer.markConsumed(msg.messageId());
            channel.basicAck(deliveryTag, false);
            log.info("[MQ] 消息处理完成: messageId={}", msg.messageId());

        } catch (Exception e) {
            log.error("[MQ] 消息处理失败: messageId={}", msg.messageId(), e);
            try {
                // 不重新入队，由 DLQ 接管（队列已配置 x-dead-letter-exchange）
                channel.basicNack(deliveryTag, false, false);
            } catch (Exception ex) {
                log.error("[MQ] NACK 失败", ex);
            }
        }
    }

    private String buildTitle(String type) {
        return switch (type) {
            case "ORDER_CREATED" -> "订单创建成功";
            case "ORDER_PAID" -> "支付成功";
            case "ORDER_CANCELLED" -> "订单已取消";
            case "ORDER_SHIPPED" -> "订单已发货";
            case "ORDER_COMPLETED" -> "订单已完成";
            default -> "订单通知";
        };
    }
}
