package com.aurora.admin.service.impl;

import com.aurora.admin.dto.*;
import com.aurora.admin.entity.*;
import com.aurora.admin.exception.BusinessException;
import com.aurora.admin.mapper.*;
import com.aurora.admin.service.MessageProducer;
import com.aurora.admin.util.SecurityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AfterSaleService 单元测试")
class AfterSaleServiceImplTest {

    @Mock private AfterSaleMapper afterSaleMapper;
    @Mock private OrderMapper orderMapper;
    @Mock private OrderItemMapper orderItemMapper;
    @Mock private ProductStockMapper productStockMapper;
    @Mock private MessageProducer messageProducer;

    @InjectMocks
    private AfterSaleServiceImpl afterSaleService;

    private Order paidOrder;
    private OrderItem normalItem;
    private AfterSale appliedAfterSale;

    @BeforeEach
    void setUp() {
        paidOrder = new Order();
        paidOrder.setId(1L);
        paidOrder.setOrderNo("20260606120000123456");
        paidOrder.setUserId(100L);
        paidOrder.setStatus("PAID");

        normalItem = new OrderItem();
        normalItem.setId(10L);
        normalItem.setOrderId(1L);
        normalItem.setProductId(1L);
        normalItem.setSkuId(null);
        normalItem.setProductName("测试商品");
        normalItem.setSpecName("");
        normalItem.setPrice(new BigDecimal("99.00"));
        normalItem.setQuantity(2);
        normalItem.setRefundStatus("NONE");

        appliedAfterSale = new AfterSale();
        appliedAfterSale.setId(1L);
        appliedAfterSale.setAfterSaleNo("AS20260606120000123456");
        appliedAfterSale.setOrderId(1L);
        appliedAfterSale.setOrderItemId(10L);
        appliedAfterSale.setUserId(100L);
        appliedAfterSale.setType("REFUND");
        appliedAfterSale.setReason("不想要了");
        appliedAfterSale.setRefundAmount(new BigDecimal("198.00"));
        appliedAfterSale.setStatus(AfterSaleStatus.APPLIED.name());
        appliedAfterSale.setOriginalOrderStatus("PAID");
        appliedAfterSale.setCreateTime(LocalDateTime.now());
        appliedAfterSale.setOrderNo("20260606120000123456");
    }

    // ==================== createAfterSale ====================

    @Test
    @DisplayName("PAID 订单仅退款申请成功，状态为 APPLIED")
    void createAfterSale_paidOrderRefund_success() {
        try (var mockedSecurity = mockStatic(SecurityUtils.class)) {
            mockedSecurity.when(SecurityUtils::getCurrentUserId).thenReturn(100L);
            mockedSecurity.when(SecurityUtils::isCurrentUserAdmin).thenReturn(false);
            mockedSecurity.when(SecurityUtils::getCurrentUsername).thenReturn("testuser");

            when(orderItemMapper.findById(10L)).thenReturn(normalItem);
            when(orderMapper.findById(1L)).thenReturn(paidOrder);
            when(afterSaleMapper.findAppliedByOrderItemId(10L)).thenReturn(null);
            when(afterSaleMapper.insert(any(AfterSale.class))).thenAnswer(inv -> {
                AfterSale a = inv.getArgument(0);
                a.setId(1L);
                return 1;
            });
            when(orderMapper.updateStatus(eq(1L), eq(OrderStatus.REFUNDING.name()), eq("PAID"))).thenReturn(1);

            CreateAfterSaleRequest request = new CreateAfterSaleRequest(10L, "REFUND", "不想要了");
            AfterSaleResponse response = afterSaleService.createAfterSale(100L, request);

            assertThat(response.type()).isEqualTo("REFUND");
            assertThat(response.refundAmount()).isEqualByComparingTo(new BigDecimal("198.00"));
            assertThat(response.status()).isEqualTo("APPLIED");

            // 验证：不会立即退款/恢复库存
            verify(orderItemMapper, never()).updateRefundStatus(anyLong(), anyString());
            verify(productStockMapper, never()).restoreProductStock(anyLong(), anyInt());
        }
    }

    @Test
    @DisplayName("PAID 订单申请退货退款应被拒绝")
    void createAfterSale_paidOrderReturn_rejected() {
        try (var mockedSecurity = mockStatic(SecurityUtils.class)) {
            mockedSecurity.when(SecurityUtils::getCurrentUserId).thenReturn(100L);
            mockedSecurity.when(SecurityUtils::isCurrentUserAdmin).thenReturn(false);

            when(orderItemMapper.findById(10L)).thenReturn(normalItem);
            when(orderMapper.findById(1L)).thenReturn(paidOrder);

            CreateAfterSaleRequest request = new CreateAfterSaleRequest(10L, "RETURN", null);

            assertThatThrownBy(() -> afterSaleService.createAfterSale(100L, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("仅支持退款");
        }
    }

    @Test
    @DisplayName("已退款明细行重复申请应被拒绝")
    void createAfterSale_alreadyRefunded_rejected() {
        normalItem.setRefundStatus("REFUNDED");

        try (var mockedSecurity = mockStatic(SecurityUtils.class)) {
            mockedSecurity.when(SecurityUtils::getCurrentUserId).thenReturn(100L);
            mockedSecurity.when(SecurityUtils::isCurrentUserAdmin).thenReturn(false);

            when(orderItemMapper.findById(10L)).thenReturn(normalItem);
            when(orderMapper.findById(1L)).thenReturn(paidOrder);

            CreateAfterSaleRequest request = new CreateAfterSaleRequest(10L, "REFUND", null);

            assertThatThrownBy(() -> afterSaleService.createAfterSale(100L, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("已退款");
        }
    }

    // ==================== approve ====================

    @Test
    @DisplayName("管理员审核通过，退款生效")
    void approve_success() {
        when(afterSaleMapper.findById(1L)).thenReturn(appliedAfterSale);
        when(orderItemMapper.findById(10L)).thenReturn(normalItem);
        when(orderMapper.findById(1L)).thenReturn(paidOrder);
        when(afterSaleMapper.updateStatus(eq(1L), eq("COMPLETED"), anyString(), eq(999L), eq("APPLIED"))).thenAnswer(inv -> {
            appliedAfterSale.setStatus("COMPLETED");
            return 1;
        });
        when(orderItemMapper.updateRefundStatus(eq(10L), eq("REFUNDED"))).thenAnswer(inv -> {
            normalItem.setRefundStatus("REFUNDED");
            return 1;
        });
        when(orderMapper.deductTotalAmount(eq(1L), eq(new BigDecimal("198.00")))).thenReturn(1);
        when(orderItemMapper.findByOrderId(1L)).thenReturn(List.of(normalItem));
        when(orderMapper.updateStatus(eq(1L), eq("REFUNDED"), eq("PAID"))).thenReturn(1);
        when(productStockMapper.restoreProductStock(eq(1L), eq(2))).thenReturn(1);

        try (var mockedSecurity = mockStatic(SecurityUtils.class)) {
            mockedSecurity.when(SecurityUtils::getCurrentUsername).thenReturn("admin");

            AfterSaleResponse response = afterSaleService.approve(999L, 1L, "审核通过");

            assertThat(response.status()).isEqualTo("COMPLETED");
            verify(productStockMapper).restoreProductStock(eq(1L), eq(2));
        }
    }

    @Test
    @DisplayName("审核非 APPLIED 状态的售后单应被拒绝")
    void approve_nonApplied_rejected() {
        appliedAfterSale.setStatus(AfterSaleStatus.COMPLETED.name());
        when(afterSaleMapper.findById(1L)).thenReturn(appliedAfterSale);

        assertThatThrownBy(() -> afterSaleService.approve(999L, 1L, ""))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("仅可审核售后中的申请");
    }

    // ==================== reject ====================

    @Test
    @DisplayName("管理员驳回，状态变为 REJECTED")
    void reject_success() {
        paidOrder.setStatus(OrderStatus.REFUNDING.name()); // 模拟已进入售后中状态

        when(afterSaleMapper.findById(1L)).thenReturn(appliedAfterSale);
        when(afterSaleMapper.updateStatus(eq(1L), eq("REJECTED"), anyString(), eq(999L), eq("APPLIED"))).thenAnswer(inv -> {
            appliedAfterSale.setStatus("REJECTED");
            return 1;
        });
        when(orderItemMapper.findByOrderId(1L)).thenReturn(List.of(normalItem));
        when(afterSaleMapper.findByOrderItemId(10L)).thenReturn(appliedAfterSale);
        when(orderMapper.findById(1L)).thenReturn(paidOrder);
        when(orderMapper.updateStatus(eq(1L), eq("PAID"), eq(OrderStatus.REFUNDING.name()))).thenReturn(1);

        try (var mockedSecurity = mockStatic(SecurityUtils.class)) {
            mockedSecurity.when(SecurityUtils::getCurrentUsername).thenReturn("admin");

            AfterSaleResponse response = afterSaleService.reject(999L, 1L, "不符合退款条件");

            assertThat(response.status()).isEqualTo("REJECTED");

            // 验证：驳回后不会退款/恢复库存
            verify(orderItemMapper, never()).updateRefundStatus(anyLong(), anyString());
            verify(productStockMapper, never()).restoreProductStock(anyLong(), anyInt());
        }
    }
}
