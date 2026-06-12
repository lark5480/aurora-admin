package com.aurora.admin.service.impl;

import com.aurora.admin.dto.CreateOrderRequest;
import com.aurora.admin.dto.OrderMessage;
import com.aurora.admin.dto.OrderQuery;
import com.aurora.admin.dto.OrderResponse;
import com.aurora.admin.dto.PageResult;
import com.aurora.admin.entity.*;
import com.aurora.admin.exception.BusinessException;
import com.aurora.admin.exception.ForbiddenException;
import com.aurora.admin.exception.NotFoundException;
import com.aurora.admin.mapper.*;
import com.aurora.admin.service.MessageProducer;
import com.aurora.admin.util.SecurityUtils;
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
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderService 单元测试")
class OrderServiceImplTest {

    @Mock private OrderMapper orderMapper;
    @Mock private OrderItemMapper orderItemMapper;
    @Mock private ShoppingCartMapper shoppingCartMapper;
    @Mock private ProductMapper productMapper;
    @Mock private ProductSkuMapper productSkuMapper;
    @Mock private ProductStockMapper productStockMapper;
    @Mock private MessageProducer messageProducer;
    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOps;

    @InjectMocks
    private OrderServiceImpl orderService;

    private Product onSaleProduct;
    private ShoppingCart cartItem;

    @BeforeEach
    void setUp() {
        onSaleProduct = new Product();
        onSaleProduct.setId(1L);
        onSaleProduct.setName("测试商品");
        onSaleProduct.setPrice(new BigDecimal("99.00"));
        onSaleProduct.setStock(100);
        onSaleProduct.setStatus("ON_SALE");
        onSaleProduct.setCoverImage("cover.jpg");

        cartItem = new ShoppingCart();
        cartItem.setId(10L);
        cartItem.setUserId(1L);
        cartItem.setProductId(1L);
        cartItem.setSkuId(null);
        cartItem.setQuantity(2);
    }

    private void mockRedisIdempotentSuccess() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);
    }

    private CreateOrderRequest buildRequest() {
        return new CreateOrderRequest(List.of(10L), "张三", "13800138000", "北京市朝阳区", "请尽快发货");
    }

    // ==================== createOrder ====================

    @Nested
    @DisplayName("创建订单")
    class CreateOrder {

        @Test
        @DisplayName("正常创建订单成功")
        void createOrder_success() {
            try (var mockedSecurity = mockStatic(SecurityUtils.class)) {
                mockedSecurity.when(SecurityUtils::getCurrentUsername).thenReturn("testuser");
                mockRedisIdempotentSuccess();

                when(shoppingCartMapper.findByUserId(1L)).thenReturn(List.of(cartItem));
                when(productMapper.findById(1L)).thenReturn(onSaleProduct);
                when(orderMapper.findByOrderNo(anyString())).thenReturn(null);
                when(orderMapper.insert(any(Order.class))).thenAnswer(inv -> {
                    Order o = inv.getArgument(0);
                    o.setId(100L);
                    o.setCreateTime(LocalDateTime.now());
                    return 1;
                });
                when(productStockMapper.deductProductStock(1L, 2)).thenReturn(1);

                OrderResponse response = orderService.createOrder(1L, buildRequest());

                assertThat(response.orderNo()).isNotNull();
                assertThat(response.status()).isEqualTo("PENDING");
                assertThat(response.totalAmount()).isEqualByComparingTo(new BigDecimal("198.00"));
                assertThat(response.orderItems()).hasSize(1);

                verify(orderMapper).insert(any(Order.class));
                verify(orderItemMapper).insertBatch(anyList());
                verify(productStockMapper).deductProductStock(1L, 2);
                verify(shoppingCartMapper).deleteById(10L);
                verify(messageProducer).sendOrderNotification(any(OrderMessage.class));
            }
        }

        @Test
        @DisplayName("幂等校验：重复提交抛异常")
        void createOrder_idempotent_rejected() {
            when(redisTemplate.opsForValue()).thenReturn(valueOps);
            when(valueOps.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(false);

            assertThatThrownBy(() -> orderService.createOrder(1L, buildRequest()))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("请勿重复提交");
        }

        @Test
        @DisplayName("购物车为空抛异常")
        void createOrder_emptyCart() {
            mockRedisIdempotentSuccess();
            when(shoppingCartMapper.findByUserId(1L)).thenReturn(Collections.emptyList());

            assertThatThrownBy(() -> orderService.createOrder(1L, buildRequest()))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("购物车为空");

            verify(redisTemplate).delete(anyString());
        }

        @Test
        @DisplayName("商品已下架抛异常")
        void createOrder_productOffSale() {
            mockRedisIdempotentSuccess();
            onSaleProduct.setStatus("OFF_SHELF");
            when(shoppingCartMapper.findByUserId(1L)).thenReturn(List.of(cartItem));
            when(productMapper.findById(1L)).thenReturn(onSaleProduct);

            assertThatThrownBy(() -> orderService.createOrder(1L, buildRequest()))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("商品已下架");

            verify(redisTemplate).delete(anyString());
        }

        @Test
        @DisplayName("库存不足抛异常")
        void createOrder_insufficientStock() {
            mockRedisIdempotentSuccess();
            onSaleProduct.setStock(1); // 需要 2，只有 1
            when(shoppingCartMapper.findByUserId(1L)).thenReturn(List.of(cartItem));
            when(productMapper.findById(1L)).thenReturn(onSaleProduct);

            assertThatThrownBy(() -> orderService.createOrder(1L, buildRequest()))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("库存不足");

            verify(redisTemplate).delete(anyString());
        }

        @Test
        @DisplayName("乐观锁扣库存失败抛异常")
        void createOrder_stockDeductFailed() {
            try (var mockedSecurity = mockStatic(SecurityUtils.class)) {
                mockedSecurity.when(SecurityUtils::getCurrentUsername).thenReturn("testuser");
                mockRedisIdempotentSuccess();

                when(shoppingCartMapper.findByUserId(1L)).thenReturn(List.of(cartItem));
                when(productMapper.findById(1L)).thenReturn(onSaleProduct);
                when(orderMapper.findByOrderNo(anyString())).thenReturn(null);
                when(orderMapper.insert(any(Order.class))).thenAnswer(inv -> {
                    Order o = inv.getArgument(0);
                    o.setId(100L);
                    return 1;
                });
                when(productStockMapper.deductProductStock(1L, 2)).thenReturn(0); // 乐观锁失败

                assertThatThrownBy(() -> orderService.createOrder(1L, buildRequest()))
                        .isInstanceOf(BusinessException.class)
                        .hasMessageContaining("库存不足");
            }
        }
    }

    // ==================== cancelOrder ====================

    @Nested
    @DisplayName("取消订单")
    class CancelOrder {

        @Test
        @DisplayName("正常取消 PENDING 订单成功，库存恢复")
        void cancelOrder_success() {
            try (var mockedSecurity = mockStatic(SecurityUtils.class)) {
                mockedSecurity.when(SecurityUtils::isCurrentUserAdmin).thenReturn(false);
                mockedSecurity.when(SecurityUtils::getCurrentUsername).thenReturn("testuser");

                Order order = new Order();
                order.setId(1L);
                order.setOrderNo("20260601120000123456");
                order.setUserId(1L);
                order.setStatus("PENDING");

                OrderItem item = new OrderItem();
                item.setProductId(1L);
                item.setSkuId(null);
                item.setQuantity(2);

                when(orderMapper.findById(1L)).thenReturn(order);
                when(orderMapper.updateStatus(1L, "CANCELLED", "PENDING")).thenReturn(1);
                when(orderItemMapper.findByOrderId(1L)).thenReturn(List.of(item));
                when(productStockMapper.restoreProductStock(1L, 2)).thenReturn(1);

                orderService.cancelOrder(1L, 1L);

                verify(orderMapper).updateStatus(1L, "CANCELLED", "PENDING");
                verify(productStockMapper).restoreProductStock(1L, 2);
                verify(messageProducer).sendOrderNotification(any(OrderMessage.class));
            }
        }

        @Test
        @DisplayName("订单不存在抛 NotFoundException")
        void cancelOrder_notFound() {
            when(orderMapper.findById(999L)).thenReturn(null);

            assertThatThrownBy(() -> orderService.cancelOrder(1L, 999L))
                    .isInstanceOf(NotFoundException.class);
        }

        @Test
        @DisplayName("非本人非管理员取消抛 ForbiddenException")
        void cancelOrder_forbidden() {
            try (var mockedSecurity = mockStatic(SecurityUtils.class)) {
                mockedSecurity.when(SecurityUtils::isCurrentUserAdmin).thenReturn(false);

                Order order = new Order();
                order.setId(1L);
                order.setUserId(2L); // 别人的订单
                order.setStatus("PENDING");

                when(orderMapper.findById(1L)).thenReturn(order);

                assertThatThrownBy(() -> orderService.cancelOrder(1L, 1L))
                        .isInstanceOf(ForbiddenException.class)
                        .hasMessageContaining("无权操作");
            }
        }

        @Test
        @DisplayName("非 PENDING 状态不允许取消")
        void cancelOrder_wrongStatus() {
            try (var mockedSecurity = mockStatic(SecurityUtils.class)) {
                mockedSecurity.when(SecurityUtils::isCurrentUserAdmin).thenReturn(true);

                Order order = new Order();
                order.setId(1L);
                order.setUserId(1L);
                order.setStatus("PAID");

                when(orderMapper.findById(1L)).thenReturn(order);

                assertThatThrownBy(() -> orderService.cancelOrder(1L, 1L))
                        .isInstanceOf(BusinessException.class)
                        .hasMessageContaining("不允许取消");
            }
        }

        @Test
        @DisplayName("取消 SKU 订单时恢复 SKU 库存并刷新商品总库存")
        void cancelOrder_skuStockRestored() {
            try (var mockedSecurity = mockStatic(SecurityUtils.class)) {
                mockedSecurity.when(SecurityUtils::isCurrentUserAdmin).thenReturn(true);
                mockedSecurity.when(SecurityUtils::getCurrentUsername).thenReturn("admin");

                Order order = new Order();
                order.setId(1L);
                order.setOrderNo("NO123");
                order.setUserId(1L);
                order.setStatus("PENDING");

                OrderItem item = new OrderItem();
                item.setProductId(1L);
                item.setSkuId(5L);
                item.setQuantity(3);

                when(orderMapper.findById(1L)).thenReturn(order);
                when(orderMapper.updateStatus(1L, "CANCELLED", "PENDING")).thenReturn(1);
                when(orderItemMapper.findByOrderId(1L)).thenReturn(List.of(item));

                orderService.cancelOrder(1L, 1L);

                verify(productStockMapper).restoreSkuStock(5L, 3);
                verify(productStockMapper).refreshProductStock(1L);
                verify(productStockMapper, never()).restoreProductStock(anyLong(), anyInt());
            }
        }
    }

    // ==================== shipOrder ====================

    @Nested
    @DisplayName("发货")
    class ShipOrder {

        @Test
        @DisplayName("PAID 订单发货成功，生成运单号")
        void shipOrder_success() {
            try (var mockedSecurity = mockStatic(SecurityUtils.class)) {
                mockedSecurity.when(SecurityUtils::getCurrentUsername).thenReturn("admin");

                Order order = new Order();
                order.setId(1L);
                order.setOrderNo("NO123");
                order.setUserId(1L);
                order.setStatus("PAID");

                when(orderMapper.findById(1L)).thenReturn(order);
                when(orderMapper.updateStatus(1L, "SHIPPED", "PAID")).thenReturn(1);

                orderService.shipOrder(1L);

                verify(orderMapper).updateStatus(1L, "SHIPPED", "PAID");
                verify(orderMapper).updateTrackingNumber(eq(1L), argThat(tn -> tn.startsWith("SF")));
                verify(messageProducer).sendOrderNotification(any(OrderMessage.class));
            }
        }

        @Test
        @DisplayName("非 PAID 状态不允许发货")
        void shipOrder_wrongStatus() {
            Order order = new Order();
            order.setId(1L);
            order.setStatus("PENDING");

            when(orderMapper.findById(1L)).thenReturn(order);

            assertThatThrownBy(() -> orderService.shipOrder(1L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("不允许发货");
        }

        @Test
        @DisplayName("订单不存在抛 NotFoundException")
        void shipOrder_notFound() {
            when(orderMapper.findById(999L)).thenReturn(null);

            assertThatThrownBy(() -> orderService.shipOrder(999L))
                    .isInstanceOf(NotFoundException.class);
        }
    }

    // ==================== confirmOrder ====================

    @Nested
    @DisplayName("确认收货")
    class ConfirmOrder {

        @Test
        @DisplayName("SHIPPED 订单确认收货成功")
        void confirmOrder_success() {
            try (var mockedSecurity = mockStatic(SecurityUtils.class)) {
                mockedSecurity.when(SecurityUtils::isCurrentUserAdmin).thenReturn(false);
                mockedSecurity.when(SecurityUtils::getCurrentUsername).thenReturn("testuser");

                Order order = new Order();
                order.setId(1L);
                order.setOrderNo("NO123");
                order.setUserId(1L);
                order.setStatus("SHIPPED");

                when(orderMapper.findById(1L)).thenReturn(order);
                when(orderMapper.updateStatus(1L, "COMPLETED", "SHIPPED")).thenReturn(1);

                orderService.confirmOrder(1L, 1L);

                verify(orderMapper).updateStatus(1L, "COMPLETED", "SHIPPED");
                verify(messageProducer).sendOrderNotification(any(OrderMessage.class));
            }
        }

        @Test
        @DisplayName("非本人非管理员确认抛 ForbiddenException")
        void confirmOrder_forbidden() {
            try (var mockedSecurity = mockStatic(SecurityUtils.class)) {
                mockedSecurity.when(SecurityUtils::isCurrentUserAdmin).thenReturn(false);

                Order order = new Order();
                order.setId(1L);
                order.setUserId(2L);
                order.setStatus("SHIPPED");

                when(orderMapper.findById(1L)).thenReturn(order);

                assertThatThrownBy(() -> orderService.confirmOrder(1L, 1L))
                        .isInstanceOf(ForbiddenException.class);
            }
        }

        @Test
        @DisplayName("非 SHIPPED 状态不允许确认收货")
        void confirmOrder_wrongStatus() {
            try (var mockedSecurity = mockStatic(SecurityUtils.class)) {
                mockedSecurity.when(SecurityUtils::isCurrentUserAdmin).thenReturn(true);

                Order order = new Order();
                order.setId(1L);
                order.setUserId(1L);
                order.setStatus("PAID");

                when(orderMapper.findById(1L)).thenReturn(order);

                assertThatThrownBy(() -> orderService.confirmOrder(1L, 1L))
                        .isInstanceOf(BusinessException.class)
                        .hasMessageContaining("不允许确认收货");
            }
        }
    }

    // ==================== getOrderDetail ====================

    @Nested
    @DisplayName("订单详情")
    class GetOrderDetail {

        @Test
        @DisplayName("正常获取订单详情")
        void getOrderDetail_success() {
            Order order = new Order();
            order.setId(1L);
            order.setOrderNo("NO123");
            order.setUserId(1L);
            order.setStatus("PENDING");
            order.setTotalAmount(new BigDecimal("198.00"));

            OrderItem item = new OrderItem();
            item.setId(10L);
            item.setProductId(1L);
            item.setProductName("测试商品");
            item.setPrice(new BigDecimal("99.00"));
            item.setQuantity(2);

            when(orderMapper.findById(1L)).thenReturn(order);
            when(orderItemMapper.findByOrderId(1L)).thenReturn(List.of(item));
            when(productMapper.findById(1L)).thenReturn(onSaleProduct);

            OrderResponse response = orderService.getOrderDetail(1L, 1L);

            assertThat(response.orderNo()).isEqualTo("NO123");
            assertThat(response.orderItems()).hasSize(1);
        }

        @Test
        @DisplayName("订单不存在抛 NotFoundException")
        void getOrderDetail_notFound() {
            when(orderMapper.findById(999L)).thenReturn(null);

            assertThatThrownBy(() -> orderService.getOrderDetail(1L, 999L))
                    .isInstanceOf(NotFoundException.class);
        }

        @Test
        @DisplayName("userId 不匹配时抛 NotFoundException（防信息泄漏）")
        void getOrderDetail_wrongOwner() {
            Order order = new Order();
            order.setId(1L);
            order.setUserId(2L);

            when(orderMapper.findById(1L)).thenReturn(order);

            assertThatThrownBy(() -> orderService.getOrderDetail(1L, 1L))
                    .isInstanceOf(NotFoundException.class);
        }
    }

    // ==================== batchDeleteCancelled ====================

    @Nested
    @DisplayName("批量删除已取消订单")
    class BatchDeleteCancelled {

        @Test
        @DisplayName("空列表返回 0")
        void batchDelete_empty() {
            assertThat(orderService.batchDeleteCancelled(Collections.emptyList(), 1L)).isZero();
            assertThat(orderService.batchDeleteCancelled(null, 1L)).isZero();
        }

        @Test
        @DisplayName("非本人订单数量不匹配抛 ForbiddenException")
        void batchDelete_forbidden() {
            when(orderMapper.countByIdsAndUser(List.of(1L, 2L), 1L)).thenReturn(1); // 只有 1 个是自己的

            assertThatThrownBy(() -> orderService.batchDeleteCancelled(List.of(1L, 2L), 1L))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessageContaining("只能删除自己的订单");
        }

        @Test
        @DisplayName("管理员删除跳过归属校验")
        void batchDelete_admin() {
            when(orderMapper.batchDeleteCancelled(List.of(1L, 2L))).thenReturn(2);

            int result = orderService.batchDeleteCancelled(List.of(1L, 2L), null);

            assertThat(result).isEqualTo(2);
            verify(orderMapper, never()).countByIdsAndUser(anyList(), anyLong());
        }
    }
}
