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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentService 单元测试")
class PaymentServiceImplTest {

    @Mock private PaymentMapper paymentMapper;
    @Mock private OrderMapper orderMapper;
    @Mock private StringRedisTemplate redisTemplate;
    @Mock private MessageProducer messageProducer;
    @Mock private ValueOperations<String, String> valueOps;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    private Order pendingOrder;

    @BeforeEach
    void setUp() {
        pendingOrder = new Order();
        pendingOrder.setId(1L);
        pendingOrder.setOrderNo("20260601120000123456");
        pendingOrder.setUserId(100L);
        pendingOrder.setTotalAmount(new BigDecimal("198.00"));
        pendingOrder.setStatus(OrderStatus.PENDING.name());
    }

    // ==================== pay ====================

    @Nested
    @DisplayName("支付")
    class Pay {

        private void mockRedisLockAcquired() {
            when(redisTemplate.opsForValue()).thenReturn(valueOps);
            when(valueOps.setIfAbsent(anyString(), eq("processing"), any(Duration.class))).thenReturn(true);
        }

        @Test
        @DisplayName("正常支付成功：插入记录、更新状态、发 MQ")
        void pay_success() {
            mockRedisLockAcquired();
            when(orderMapper.findByOrderNo("20260601120000123456")).thenReturn(pendingOrder);
            when(paymentMapper.insert(any(Payment.class))).thenAnswer(inv -> {
                Payment p = inv.getArgument(0);
                p.setId(1L);
                return 1;
            });
            when(orderMapper.updateStatus(1L, OrderStatus.PAID.name(), OrderStatus.PENDING.name())).thenReturn(1);

            PayRequest request = new PayRequest("20260601120000123456", "BALANCE");
            PayResponse response = paymentService.pay(100L, request);

            assertThat(response.status()).isEqualTo("SUCCESS");
            assertThat(response.orderNo()).isEqualTo("20260601120000123456");
            assertThat(response.amount()).isEqualByComparingTo(new BigDecimal("198.00"));

            verify(paymentMapper).insert(argThat(p ->
                    p.getOrderId().equals(1L) &&
                    p.getStatus().equals("SUCCESS") &&
                    p.getTransactionNo() != null && p.getTransactionNo().length() == 8
            ));
            verify(orderMapper).updateStatus(1L, OrderStatus.PAID.name(), OrderStatus.PENDING.name());
            verify(redisTemplate.opsForValue()).set(eq("pay:idempotent:20260601120000123456"), eq("success"), any(Duration.class));
            verify(messageProducer).sendOrderNotification(any(OrderMessage.class));
        }

        @Test
        @DisplayName("订单不存在抛 NotFoundException")
        void pay_orderNotFound() {
            when(orderMapper.findByOrderNo("NOTEXIST")).thenReturn(null);

            assertThatThrownBy(() -> paymentService.pay(100L, new PayRequest("NOTEXIST", "BALANCE")))
                    .isInstanceOf(NotFoundException.class);
        }

        @Test
        @DisplayName("订单不属于当前用户抛 NotFoundException")
        void pay_wrongOwner() {
            when(orderMapper.findByOrderNo("20260601120000123456")).thenReturn(pendingOrder);

            assertThatThrownBy(() -> paymentService.pay(999L, new PayRequest("20260601120000123456", "BALANCE")))
                    .isInstanceOf(NotFoundException.class);
        }

        @Test
        @DisplayName("非 PENDING 状态不允许支付")
        void pay_wrongStatus() {
            pendingOrder.setStatus(OrderStatus.PAID.name());
            when(orderMapper.findByOrderNo("20260601120000123456")).thenReturn(pendingOrder);

            assertThatThrownBy(() -> paymentService.pay(100L, new PayRequest("20260601120000123456", "BALANCE")))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("不允许支付");
        }

        @Test
        @DisplayName("幂等：已支付成功直接返回结果")
        void pay_idempotent_alreadySuccess() {
            when(redisTemplate.opsForValue()).thenReturn(valueOps);
            when(valueOps.setIfAbsent(anyString(), eq("processing"), any(Duration.class))).thenReturn(false);
            when(valueOps.get("pay:idempotent:20260601120000123456")).thenReturn("success");
            when(orderMapper.findByOrderNo("20260601120000123456")).thenReturn(pendingOrder);

            PayResponse response = paymentService.pay(100L, new PayRequest("20260601120000123456", "BALANCE"));

            assertThat(response.status()).isEqualTo("SUCCESS");
            verify(paymentMapper, never()).insert(any());
            verify(orderMapper, never()).updateStatus(anyLong(), anyString(), anyString());
        }

        @Test
        @DisplayName("幂等：处理中拒绝重复提交")
        void pay_idempotent_processing() {
            when(redisTemplate.opsForValue()).thenReturn(valueOps);
            when(valueOps.setIfAbsent(anyString(), eq("processing"), any(Duration.class))).thenReturn(false);
            when(valueOps.get("pay:idempotent:20260601120000123456")).thenReturn("processing");
            when(orderMapper.findByOrderNo("20260601120000123456")).thenReturn(pendingOrder);

            assertThatThrownBy(() -> paymentService.pay(100L, new PayRequest("20260601120000123456", "BALANCE")))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("处理中");
        }

        @Test
        @DisplayName("支付过程中异常：删除幂等 Key 允许重试")
        void pay_exception_releasesLock() {
            mockRedisLockAcquired();
            when(orderMapper.findByOrderNo("20260601120000123456")).thenReturn(pendingOrder);
            when(paymentMapper.insert(any(Payment.class))).thenThrow(new RuntimeException("DB error"));

            assertThatThrownBy(() -> paymentService.pay(100L, new PayRequest("20260601120000123456", "BALANCE")))
                    .isInstanceOf(RuntimeException.class);

            verify(redisTemplate).delete("pay:idempotent:20260601120000123456");
        }

        @Test
        @DisplayName("订单状态已被变更（并发场景）支付失败")
        void pay_concurrent_statusChange() {
            mockRedisLockAcquired();
            when(orderMapper.findByOrderNo("20260601120000123456")).thenReturn(pendingOrder);
            when(paymentMapper.insert(any(Payment.class))).thenReturn(1);
            when(orderMapper.updateStatus(1L, OrderStatus.PAID.name(), OrderStatus.PENDING.name())).thenReturn(0);

            assertThatThrownBy(() -> paymentService.pay(100L, new PayRequest("20260601120000123456", "BALANCE")))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("订单状态已变更");

            verify(redisTemplate).delete("pay:idempotent:20260601120000123456");
        }
    }

    // ==================== getPaymentByOrderId ====================

    @Nested
    @DisplayName("查询支付记录")
    class QueryPayments {

        @Test
        @DisplayName("按 orderId 查询返回支付记录列表")
        void getByOrderId_success() {
            Payment p = new Payment();
            p.setId(1L);
            p.setOrderNo("NO123");
            p.setTransactionNo("TXN001");
            p.setAmount(new BigDecimal("198.00"));
            p.setPayMethod("BALANCE");
            p.setStatus("SUCCESS");

            when(paymentMapper.findByOrderId(1L)).thenReturn(List.of(p));

            List<PaymentResponse> responses = paymentService.getPaymentByOrderId(1L);

            assertThat(responses).hasSize(1);
            assertThat(responses.get(0).orderNo()).isEqualTo("NO123");
            assertThat(responses.get(0).transactionNo()).isEqualTo("TXN001");
        }

        @Test
        @DisplayName("按 orderNo 查询无记录返回空列表")
        void getByOrderNo_empty() {
            when(paymentMapper.findByOrderNo("NOTEXIST")).thenReturn(Collections.emptyList());

            List<PaymentResponse> responses = paymentService.getPaymentByOrderNo("NOTEXIST");

            assertThat(responses).isEmpty();
        }
    }
}
