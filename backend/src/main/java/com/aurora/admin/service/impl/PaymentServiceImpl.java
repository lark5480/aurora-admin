package com.aurora.admin.service.impl;

import com.aurora.admin.dto.OrderMessage;
import com.aurora.admin.dto.PayRequest;
import com.aurora.admin.dto.PayResponse;
import com.aurora.admin.dto.PaymentResponse;
import com.aurora.admin.entity.Order;
import com.aurora.admin.entity.OrderStatus;
import com.aurora.admin.entity.Payment;
import com.aurora.admin.exception.BusinessException;
import com.aurora.admin.exception.NotFoundException;
import com.aurora.admin.mapper.OrderMapper;
import com.aurora.admin.mapper.PaymentMapper;
import com.aurora.admin.service.MessageProducer;
import com.aurora.admin.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 支付服务实现。
 *
 * <h3>幂等 Key 原理说明</h3>
 *
 * <p>为防止用户重复提交支付请求导致重复扣款，本实现使用 Redis 分布式锁实现幂等控制。
 * 幂等 Key 的格式为 <code>pay:idempotent:{orderNo}</code>，流程如下：</p>
 *
 * <ol>
 *   <li><b>尝试加锁</b>：调用
 *       {@link StringRedisTemplate#opsForValue() setIfAbsent(key, "processing", Duration.ofMinutes(5))}，
 *       利用 Redis 的 <code>SET NX EX</code> 原子语义。返回 {@code true} 表示当前请求获得锁，可以继续执行支付逻辑。</li>
 *   <li><b>锁已被持有</b>：返回 {@code false} 表示已有请求在处理该订单的支付。
 *       此时检查 Redis 中该 Key 的值是否为 {@code "success"}：
 *       <ul>
 *         <li>是 {@code "success"}：说明支付已成功完成，直接返回成功结果（幂等返回，不重复扣款）；</li>
 *         <li>仍为 {@code "processing"}：说明支付正在处理中，返回"处理中"提示，避免并发重复支付。</li>
 *       </ul>
 *   </li>
 *   <li><b>业务执行</b>：获得锁后，执行业务逻辑（模拟支付、插入支付记录、更新订单状态）。</li>
 *   <li><b>标记完成</b>：业务执行成功后，将 Key 的值更新为 {@code "success"}，标识该订单已支付。</li>
 *   <li><b>异常释放</b>：业务执行过程中发生异常时，在 {@code catch} 块中主动删除该 Key，
 *       允许用户在异常恢复后重新发起支付。</li>
 * </ol>
 *
 * <h3>应用场景</h3>
 * <ul>
 *   <li>用户快速双击"支付"按钮</li>
 *   <li>前端网络超时后用户重试</li>
 *   <li>MQ 消息重复消费导致的重复支付请求</li>
 * </ul>
 *
 * <h3>Key 自动过期</h3>
 * <p>幂等 Key 设置 5 分钟过期时间，防止长期占用内存。正常情况下支付在秒级完成，
 * Key 仅在支付过程中和支付完成后短时存在。</p>
 */
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentServiceImpl.class);

    private static final String IDEMPOTENT_KEY_PREFIX = "pay:idempotent:";
    private static final Duration IDEMPOTENT_KEY_TTL = Duration.ofMinutes(5);

    private final PaymentMapper paymentMapper;
    private final OrderMapper orderMapper;
    private final StringRedisTemplate redisTemplate;
    private final MessageProducer messageProducer;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PayResponse pay(Long userId, PayRequest request) {
        // 1. 查询订单，校验订单属于当前用户
        Order order = orderMapper.findByOrderNo(request.orderNo());
        if (order == null) {
            throw new NotFoundException("订单", request.orderNo());
        }
        if (!order.getUserId().equals(userId)) {
            throw new NotFoundException("订单", request.orderNo());
        }

        // 2. 校验订单状态为 PENDING
        if (!OrderStatus.PENDING.name().equals(order.getStatus())) {
            throw new BusinessException("当前订单状态不允许支付");
        }

        // 3. 幂等检查：Redis SET NX EX 原子操作
        String idempotentKey = IDEMPOTENT_KEY_PREFIX + request.orderNo();
        Boolean acquired = redisTemplate.opsForValue()
                .setIfAbsent(idempotentKey, "processing", IDEMPOTENT_KEY_TTL);

        if (Boolean.FALSE.equals(acquired)) {
            // Key 已存在，说明已有支付请求在处理或已完成
            String value = redisTemplate.opsForValue().get(idempotentKey);
            if ("success".equals(value)) {
                log.info("[支付幂等] 订单 {} 已支付成功，直接返回", request.orderNo());
                return new PayResponse(order.getOrderNo(), order.getTotalAmount(), "SUCCESS", "支付成功");
            }
            // 仍为 "processing" 状态
            log.warn("[支付幂等] 订单 {} 支付处理中，拒绝重复提交", request.orderNo());
            throw new BusinessException("支付处理中，请勿重复提交");
        }

        // 成功获得锁，执行业务逻辑
        try {
            // 4. 生成支付流水号（UUID 前 8 位）
            String transactionNo = UUID.randomUUID().toString()
                    .replace("-", "")
                    .substring(0, 8)
                    .toUpperCase();

            // 5. 模拟支付（直接成功）

            // 6. 插入支付记录
            Payment payment = new Payment();
            payment.setOrderId(order.getId());
            payment.setOrderNo(order.getOrderNo());
            payment.setTransactionNo(transactionNo);
            payment.setAmount(order.getTotalAmount());
            payment.setPayMethod(request.payMethod());
            payment.setStatus("SUCCESS");
            paymentMapper.insert(payment);

            // 7. 更新订单状态 PENDING -> PAID
            int affected = orderMapper.updateStatus(order.getId(), OrderStatus.PAID.name(), OrderStatus.PENDING.name());
            if (affected == 0) {
                throw new BusinessException("支付失败，订单状态已变更");
            }

            // 8. 更新 Redis 幂等 Key 值为 "success"
            redisTemplate.opsForValue().set(idempotentKey, "success", IDEMPOTENT_KEY_TTL);

            log.info("[支付成功] orderNo={}, transactionNo={}, amount={}",
                    request.orderNo(), transactionNo, order.getTotalAmount());

            // 9. 异步发送支付通知
            messageProducer.sendOrderNotification(new OrderMessage(
                    request.orderNo(), userId, "system", "ORDER_PAID",
                    "支付成功，金额：" + order.getTotalAmount(),
                    LocalDateTime.now().toString()
            ));

            // 10. 返回支付结果
            return new PayResponse(order.getOrderNo(), order.getTotalAmount(), "SUCCESS", "支付成功");

        } catch (Exception e) {
            // 业务异常时删除幂等 Key，允许用户重试
            log.warn("[支付异常] 订单 {} 支付异常，释放幂等锁: {}", request.orderNo(), e.getMessage());
            redisTemplate.delete(idempotentKey);
            throw e;
        }
    }

    @Override
    public List<PaymentResponse> getPaymentByOrderId(Long orderId) {
        List<Payment> payments = paymentMapper.findByOrderId(orderId);
        return payments.stream()
                .map(this::toPaymentResponse)
                .toList();
    }

    @Override
    public List<PaymentResponse> getPaymentByOrderNo(String orderNo) {
        List<Payment> payments = paymentMapper.findByOrderNo(orderNo);
        return payments.stream()
                .map(this::toPaymentResponse)
                .toList();
    }

    private PaymentResponse toPaymentResponse(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getOrderNo(),
                payment.getTransactionNo(),
                payment.getAmount(),
                payment.getPayMethod(),
                payment.getStatus()
        );
    }
}
