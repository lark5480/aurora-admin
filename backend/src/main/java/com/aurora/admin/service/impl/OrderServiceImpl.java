package com.aurora.admin.service.impl;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aurora.admin.dto.CreateOrderRequest;
import com.aurora.admin.dto.OrderItemResponse;
import com.aurora.admin.dto.OrderMessage;
import com.aurora.admin.dto.OrderQuery;
import com.aurora.admin.dto.OrderResponse;
import com.aurora.admin.dto.PageResult;
import com.aurora.admin.entity.Order;
import com.aurora.admin.entity.OrderItem;
import com.aurora.admin.entity.OrderStatus;
import com.aurora.admin.entity.Product;
import com.aurora.admin.entity.ProductSku;
import com.aurora.admin.entity.ShoppingCart;
import com.aurora.admin.exception.BusinessException;
import com.aurora.admin.exception.ForbiddenException;
import com.aurora.admin.exception.NotFoundException;
import com.aurora.admin.mapper.OrderItemMapper;
import com.aurora.admin.mapper.OrderMapper;
import com.aurora.admin.mapper.ProductMapper;
import com.aurora.admin.mapper.ProductSkuMapper;
import com.aurora.admin.mapper.ProductStockMapper;
import com.aurora.admin.mapper.ShoppingCartMapper;
import com.aurora.admin.service.MessageProducer;
import com.aurora.admin.service.OrderService;
import com.aurora.admin.util.SecurityUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 订单服务实现。提供订单创建、查询、取消、发货、确认收货、批量删除及导出功能。
 * 创建订单时包含 Redis 幂等校验、乐观锁扣库存、购物车清理及 MQ 异步通知等内部逻辑。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final ShoppingCartMapper shoppingCartMapper;
    private final ProductMapper productMapper;
    private final ProductSkuMapper productSkuMapper;
    private final ProductStockMapper productStockMapper;
    private final MessageProducer messageProducer;
    private final org.springframework.data.redis.core.StringRedisTemplate redisTemplate;

    /**
     * 创建订单。内部包含 Redis 幂等检查防止重复提交、乐观锁扣减库存、
     * 清理购物车已结算商品、异步 MQ 通知等逻辑。异常时自动删除幂等 Key 以允许重试。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderResponse createOrder(Long userId, CreateOrderRequest request, String idempotentKey) {
        // 0. 幂等检查：防止重复提交订单（Idempotent-Key 由客户端传入）
        String redisKey = "order:idempotent:" + userId + ":" + idempotentKey;
        Boolean acquired = redisTemplate.opsForValue()
                .setIfAbsent(redisKey, "1", Duration.ofMinutes(10));
        if (Boolean.FALSE.equals(acquired)) {
            throw new BusinessException("请勿重复提交订单");
        }

        try {
        // 1. 获取所选购物车商品
        // JSON 反序列化数字默认为 Integer，需安全转换为 Long 再做集合匹配
        java.util.Set<Long> selectedIdSet = request.cartItemIds().stream()
                .<Long>mapMulti((id, sink) -> {
                    if (id instanceof Number n) sink.accept(n.longValue());
                })
                .collect(java.util.stream.Collectors.toSet());
        List<ShoppingCart> allCartItems = shoppingCartMapper.findByUserId(userId);
        if (allCartItems.isEmpty()) {
            throw new BusinessException("购物车为空");
        }
        List<ShoppingCart> cartItems = allCartItems.stream()
                .filter(c -> selectedIdSet.contains(c.getId()))
                .toList();
        if (cartItems.isEmpty()) {
            throw new BusinessException("未选择结算商品");
        }

        // 2. 校验库存并准备订单明细
        List<OrderItem> orderItems = new ArrayList<>();
        Map<Long, String> coverImageMap = new HashMap<>();
        BigDecimal total = BigDecimal.ZERO;

        for (ShoppingCart cart : cartItems) {
            Product product = productMapper.findById(cart.getProductId());
            if (product == null) {
                throw new BusinessException("商品不存在: " + cart.getProductId());
            }

            // 预校验：商品是否已上架
            if (!"ON_SALE".equals(product.getStatus())) {
                throw new BusinessException("商品已下架，无法结算: " + product.getName());
            }

            coverImageMap.put(cart.getProductId(), product.getCoverImage());

            // 预校验商品库存
            if (product.getStock() < cart.getQuantity()) {
                throw new BusinessException("商品库存不足: " + product.getName());
            }

            String specName = "";
            BigDecimal price = product.getPrice();

            if (cart.getSkuId() != null) {
                List<ProductSku> skus = productSkuMapper.findByProductId(cart.getProductId());
                ProductSku sku = skus.stream()
                        .filter(s -> s.getId().equals(cart.getSkuId()))
                        .findFirst()
                        .orElseThrow(() -> new BusinessException("SKU不存在: " + cart.getSkuId()));

                // 预校验 SKU 库存
                if (sku.getStock() < cart.getQuantity()) {
                    throw new BusinessException("SKU库存不足: " + product.getName() + " - " + sku.getSpecName());
                }

                specName = sku.getSpecName();
                if (sku.getPrice() != null) {
                    price = sku.getPrice();
                }
            }

            OrderItem item = new OrderItem();
            item.setProductId(cart.getProductId());
            item.setSkuId(cart.getSkuId());
            item.setProductName(product.getName());
            item.setSpecName(specName);
            item.setPrice(price);
            item.setQuantity(cart.getQuantity());
            orderItems.add(item);

            total = total.add(price.multiply(BigDecimal.valueOf(cart.getQuantity())));
        }

        // 3. 生成订单号（含碰撞重试）
        String orderNo = null;
        for (int attempt = 0; attempt < 3; attempt++) {
            String candidate = generateOrderNo();
            if (orderMapper.findByOrderNo(candidate) == null) {
                orderNo = candidate;
                break;
            }
        }
        if (orderNo == null) {
            throw new BusinessException("系统繁忙，请稍后重试");
        }

        // 4. 插入订单
        Order order = new Order();
        order.setOrderNo(orderNo);
        order.setUserId(userId);
        order.setTotalAmount(total);
        order.setStatus(OrderStatus.PENDING.name());
        order.setReceiverName(request.receiverName());
        order.setReceiverPhone(request.receiverPhone());
        order.setReceiverAddress(request.receiverAddress());
        order.setRemark(request.remark());
        orderMapper.insert(order);

        // 5. 批量插入订单明细
        Long orderId = order.getId();
        for (OrderItem item : orderItems) {
            item.setOrderId(orderId);
        }
        orderItemMapper.insertBatch(orderItems);

        // 6. 乐观锁扣库存
        for (ShoppingCart cart : cartItems) {
            if (cart.getSkuId() != null) {
                // 有 SKU：只扣 SKU 库存，然后刷新商品总库存
                int affected = productStockMapper.deductSkuStock(cart.getSkuId(), cart.getQuantity());
                if (affected == 0) {
                    throw new BusinessException("SKU库存不足，订单已取消");
                }
                productStockMapper.refreshProductStock(cart.getProductId());
            } else {
                // 无 SKU：直接扣减商品库存
                int affected = productStockMapper.deductProductStock(cart.getProductId(), cart.getQuantity());
                if (affected == 0) {
                    throw new BusinessException("商品库存不足，订单已取消");
                }
            }
        }

        // 7. 移除已结算的购物车商品
        for (Long cartId : selectedIdSet) {
            shoppingCartMapper.deleteById(cartId);
        }

        // 8. 标记幂等 Key 为已完成（防止超时后重复提交）
        redisTemplate.opsForValue().set(redisKey, "success", Duration.ofMinutes(10));

        // 9. 异步发送订单通知
        sendOrderMq(order, "ORDER_CREATED", "订单创建成功，金额：" + total);

        // 10. 构建响应
        List<OrderItemResponse> itemResponses = orderItems.stream()
                .map(item -> new OrderItemResponse(
                        null,
                        item.getProductName(),
                        item.getSpecName(),
                        item.getPrice(),
                        item.getQuantity(),
                        item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())),
                        coverImageMap.get(item.getProductId()),
                        null
                ))
                .toList();

        return new OrderResponse(orderId, orderNo, SecurityUtils.getCurrentUsername(),
                total, OrderStatus.PENDING.name(),
                request.receiverName(), request.receiverPhone(), request.receiverAddress(),
                request.remark(), null, order.getCreateTime(), itemResponses);
        } catch (Exception e) {
            // 异常时删除幂等 Key，允许用户重试
            redisTemplate.delete(redisKey);
            throw e;
        }
    }

    @Override
    public PageResult<OrderResponse> getOrderPage(Long userId, OrderQuery query) {
        int page = query.getPage();
        int size = query.getSize();
        int offset = (page - 1) * size;

        long total = orderMapper.countFiltered(userId, query.status(), query.orderNo(), query.username());
        if (total == 0) {
            return PageResult.of(Collections.emptyList(), 0, page, size);
        }

        List<Order> orders = orderMapper.findPage(offset, size, userId, query.status(),
                query.orderNo(), query.username());
        List<OrderResponse> records = orders.stream()
                .map(this::toOrderResponse)
                .toList();

        return PageResult.of(records, total, page, size);
    }

    @Override
    public OrderResponse getOrderDetail(Long userId, Long orderId) {
        Order order = orderMapper.findById(orderId);
        if (order == null) {
            throw new NotFoundException("订单", orderId);
        }
        if (userId != null && !order.getUserId().equals(userId)) {
            throw new NotFoundException("订单", orderId);
        }
        return toOrderResponse(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelOrder(Long userId, Long orderId) {
        Order order = orderMapper.findById(orderId);
        if (order == null) {
            throw new NotFoundException("订单", orderId);
        }
        if (!SecurityUtils.isCurrentUserAdmin() && !order.getUserId().equals(userId)) {
            throw new ForbiddenException("无权操作该订单");
        }
        if (!OrderStatus.PENDING.name().equals(order.getStatus())) {
            throw new BusinessException("当前订单状态不允许取消");
        }

        int affected = orderMapper.updateStatus(orderId, OrderStatus.CANCELLED.name(), OrderStatus.PENDING.name());
        if (affected == 0) {
            throw new BusinessException("取消失败，订单状态已变更");
        }

        // 取消订单，恢复库存
        restoreStock(orderId);

        sendOrderMq(order, "ORDER_CANCELLED", "订单已取消");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void shipOrder(Long orderId) {
        Order order = orderMapper.findById(orderId);
        if (order == null) {
            throw new NotFoundException("订单", orderId);
        }
        if (!OrderStatus.PAID.name().equals(order.getStatus())) {
            throw new BusinessException("当前订单状态不允许发货");
        }

        int affected = orderMapper.updateStatus(orderId, OrderStatus.SHIPPED.name(), OrderStatus.PAID.name());
        if (affected == 0) {
            throw new BusinessException("发货失败，订单状态已变更");
        }

        // 生成运单号
        String trackingNo = "SF" + System.currentTimeMillis();
        orderMapper.updateTrackingNumber(orderId, trackingNo);

        sendOrderMq(order, "ORDER_SHIPPED", "订单已发货，运单号：" + trackingNo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmOrder(Long userId, Long orderId) {
        Order order = orderMapper.findById(orderId);
        if (order == null) {
            throw new NotFoundException("订单", orderId);
        }
        if (!SecurityUtils.isCurrentUserAdmin() && !order.getUserId().equals(userId)) {
            throw new ForbiddenException("无权操作该订单");
        }
        if (!OrderStatus.SHIPPED.name().equals(order.getStatus())) {
            throw new BusinessException("当前订单状态不允许确认收货");
        }

        int affected = orderMapper.updateStatus(orderId, OrderStatus.COMPLETED.name(), OrderStatus.SHIPPED.name());
        if (affected == 0) {
            throw new BusinessException("确认收货失败，订单状态已变更");
        }

        sendOrderMq(order, "ORDER_COMPLETED", "订单已完成");
    }

    /**
     * 取消订单时恢复库存
     */
    private void restoreStock(Long orderId) {
        List<OrderItem> items = orderItemMapper.findByOrderId(orderId);
        for (OrderItem item : items) {
            if (item.getSkuId() != null) {
                productStockMapper.restoreSkuStock(item.getSkuId(), item.getQuantity());
                productStockMapper.refreshProductStock(item.getProductId());
            } else {
                productStockMapper.restoreProductStock(item.getProductId(), item.getQuantity());
            }
        }
    }

    /**
     * 将 Order 转换为 OrderResponse（含明细）
     */
    private OrderResponse toOrderResponse(Order order) {
        List<OrderItem> items = orderItemMapper.findByOrderId(order.getId());

        // 批量加载商品封面
        Set<Long> productIds = items.stream().map(OrderItem::getProductId).collect(Collectors.toSet());
        Map<Long, String> coverImageMap = new HashMap<>();
        for (Long pid : productIds) {
            Product product = productMapper.findById(pid);
            if (product != null && product.getCoverImage() != null) {
                coverImageMap.put(pid, product.getCoverImage());
            }
        }

        List<OrderItemResponse> itemResponses = items.stream()
                .map(item -> new OrderItemResponse(
                        item.getId(),
                        item.getProductName(),
                        item.getSpecName(),
                        item.getPrice(),
                        item.getQuantity(),
                        item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())),
                        coverImageMap.get(item.getProductId()),
                        item.getRefundStatus()
                ))
                .toList();
        return new OrderResponse(
                order.getId(),
                order.getOrderNo(),
                order.getUsername(),
                order.getTotalAmount(),
                order.getStatus(),
                order.getReceiverName(),
                order.getReceiverPhone(),
                order.getReceiverAddress(),
                order.getRemark(),
                order.getTrackingNumber(),
                order.getCreateTime(),
                itemResponses
        );
    }

    private void sendOrderMq(Order order, String type, String message) {
        try {
            messageProducer.sendOrderNotification(new OrderMessage(
                    order.getOrderNo(), order.getUserId(),
                    SecurityUtils.getCurrentUsername(), type, message,
                    LocalDateTime.now().toString()
            ));
        } catch (Exception e) {
            log.warn("MQ消息发送失败: orderNo={}, type={}", order.getOrderNo(), type, e);
        }
    }

    /**
     * 生成订单号：yyyyMMddHHmmss + 6 位随机数
     */
    private String generateOrderNo() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        // nextInt 遵循左闭右开规则 [origin, bound)
        // int random = ThreadLocalRandom.current().nextInt(100000, 999999);
        int random = ThreadLocalRandom.current().nextInt(100000, 1000000);
        return timestamp + random;
    }

    private static final int MAX_EXPORT_ROWS = 100000;

    @Override
    public byte[] exportOrders(Long userId, OrderQuery query) {
        List<Order> orders = orderMapper.findForExport(userId,
                query.status() != null && !query.status().isEmpty() ? query.status() : null,
                query.orderNo() != null && !query.orderNo().isEmpty() ? query.orderNo() : null,
                query.username() != null && !query.username().isEmpty() ? query.username() : null,
                MAX_EXPORT_ROWS);

        List<com.aurora.admin.dto.OrderExportVO> exportList = orders.stream()
                .map(com.aurora.admin.dto.OrderExportVO::from)
                .toList();

        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        com.alibaba.excel.EasyExcel.write(out, com.aurora.admin.dto.OrderExportVO.class)
                .sheet("订单导出")
                .doWrite(exportList);
        return out.toByteArray();
    }

    @Override
    public int batchDeleteCancelled(java.util.List<Long> ids, Long userId) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        // 普通用户：校验订单归属
        if (userId != null) {
            int ownedCount = orderMapper.countByIdsAndUser(ids, userId);
            if (ownedCount != ids.size()) {
                throw new ForbiddenException("只能删除自己的订单");
            }
        }
        return orderMapper.batchDeleteCancelled(ids);
    }

}
