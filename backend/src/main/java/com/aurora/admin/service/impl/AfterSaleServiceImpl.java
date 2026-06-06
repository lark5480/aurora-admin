package com.aurora.admin.service.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aurora.admin.dto.*;
import com.aurora.admin.entity.*;
import com.aurora.admin.exception.BusinessException;
import com.aurora.admin.exception.ForbiddenException;
import com.aurora.admin.exception.NotFoundException;
import com.aurora.admin.mapper.AfterSaleMapper;
import com.aurora.admin.mapper.OrderItemMapper;
import com.aurora.admin.mapper.OrderMapper;
import com.aurora.admin.mapper.ProductStockMapper;
import com.aurora.admin.service.AfterSaleService;
import com.aurora.admin.service.MessageProducer;
import com.aurora.admin.util.SecurityUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AfterSaleServiceImpl implements AfterSaleService {

    private final AfterSaleMapper afterSaleMapper;
    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final ProductStockMapper productStockMapper;
    private final MessageProducer messageProducer;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AfterSaleResponse createAfterSale(Long userId, CreateAfterSaleRequest request) {
        // 1. 查 OrderItem
        OrderItem item = orderItemMapper.findById(request.orderItemId());
        if (item == null) {
            throw new NotFoundException("订单明细", request.orderItemId());
        }

        // 2. 查 Order
        Order order = orderMapper.findById(item.getOrderId());
        if (order == null) {
            throw new NotFoundException("订单", item.getOrderId());
        }

        // 3. 校验归属
        if (!SecurityUtils.isCurrentUserAdmin() && !userId.equals(order.getUserId())) {
            throw new ForbiddenException("无权操作该订单");
        }

        // 4. 校验订单状态 → 确定售后类型
        String orderStatus = order.getStatus();
        if ("PAID".equals(orderStatus)) {
            if (!"REFUND".equals(request.type())) {
                throw new BusinessException("已支付未发货的订单仅支持退款");
            }
        } else if ("SHIPPED".equals(orderStatus) || "COMPLETED".equals(orderStatus)) {
            if (!"RETURN".equals(request.type())) {
                throw new BusinessException("已发货/已完成的订单仅支持退货退款");
            }
        } else {
            throw new BusinessException("当前订单状态不支持售后");
        }

        // 5. 校验同一明细行是否已有进行中的售后
        if ("REFUNDED".equals(item.getRefundStatus())) {
            throw new BusinessException("该商品已退款，不可重复申请");
        }
        AfterSale existing = afterSaleMapper.findAppliedByOrderItemId(item.getId());
        if (existing != null && "APPLIED".equals(existing.getStatus())) {
            throw new BusinessException("该商品已有售后申请正在审核中");
        }

        // 6. 计算退款金额
        if (item.getPrice() == null || item.getQuantity() == null) {
            throw new BusinessException("订单明细数据异常：价格或数量为空");
        }
        BigDecimal refundAmount = item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));

        // 7. 生成售后单号
        String afterSaleNo = generateAfterSaleNo();

        // 8. 插入 AfterSale 记录（APPLIED，等待审核）
        AfterSale afterSale = new AfterSale();
        afterSale.setAfterSaleNo(afterSaleNo);
        afterSale.setOrderId(order.getId());
        afterSale.setOrderItemId(item.getId());
        afterSale.setUserId(userId);
        afterSale.setType(request.type());
        afterSale.setReason(request.reason());
        afterSale.setRefundAmount(refundAmount);
        afterSale.setOriginalOrderStatus(orderStatus);
        afterSale.setStatus(AfterSaleStatus.APPLIED.name());
        afterSaleMapper.insert(afterSale);

        // 9. 订单状态 → REFUNDING（第一次售后申请时）
        if (!OrderStatus.REFUNDING.name().equals(orderStatus)
                && !OrderStatus.REFUNDED.name().equals(orderStatus)) {
            orderMapper.updateStatus(order.getId(), OrderStatus.REFUNDING.name(), orderStatus);
        }

        // 10. 发 MQ 消息
        sendAfterSaleMq(order.getOrderNo(), userId, request.type(),
                "售后申请已提交，等待审核，退款金额：" + refundAmount);

        return toResponse(afterSale);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int createAfterSaleBatch(Long userId, CreateAfterSaleBatchRequest request) {
        Order order = orderMapper.findById(request.orderId());
        if (order == null) {
            throw new NotFoundException("订单", request.orderId());
        }
        if (!SecurityUtils.isCurrentUserAdmin() && !userId.equals(order.getUserId())) {
            throw new ForbiddenException("无权操作该订单");
        }

        String orderStatus = order.getStatus();
        boolean isRefund = "REFUND".equals(request.type());
        boolean isReturn = "RETURN".equals(request.type());
        if ("PAID".equals(orderStatus) && !isRefund) {
            throw new BusinessException("已支付未发货的订单仅支持退款");
        }
        if (("SHIPPED".equals(orderStatus) || "COMPLETED".equals(orderStatus)) && !isReturn) {
            throw new BusinessException("已发货/已完成的订单仅支持退货退款");
        }
        if (!isRefund && !isReturn) {
            throw new BusinessException("当前订单状态不支持售后");
        }

        List<OrderItem> items = orderItemMapper.findByOrderId(order.getId());
        List<OrderItem> refundableItems = items.stream()
                .filter(i -> !"REFUNDED".equals(i.getRefundStatus()))
                .filter(i -> {
                    AfterSale existing = afterSaleMapper.findAppliedByOrderItemId(i.getId());
                    return existing == null || !"APPLIED".equals(existing.getStatus());
                })
                .toList();

        if (refundableItems.isEmpty()) {
            throw new BusinessException("该订单没有可售后的商品");
        }

        int count = 0;
        for (OrderItem item : refundableItems) {
            if (item.getPrice() == null || item.getQuantity() == null) {
                continue;
            }
            BigDecimal refundAmount = item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            String afterSaleNo = generateAfterSaleNo();

            AfterSale afterSale = new AfterSale();
            afterSale.setAfterSaleNo(afterSaleNo);
            afterSale.setOrderId(order.getId());
            afterSale.setOrderItemId(item.getId());
            afterSale.setUserId(userId);
            afterSale.setType(request.type());
            afterSale.setReason(request.reason());
            afterSale.setRefundAmount(refundAmount);
            afterSale.setOriginalOrderStatus(orderStatus);
            afterSale.setStatus(AfterSaleStatus.APPLIED.name());
            afterSaleMapper.insert(afterSale);
            count++;
        }

        // 订单状态 → REFUNDING
        if (!OrderStatus.REFUNDING.name().equals(orderStatus)
                && !OrderStatus.REFUNDED.name().equals(orderStatus)) {
            orderMapper.updateStatus(order.getId(), OrderStatus.REFUNDING.name(), orderStatus);
        }

        sendAfterSaleMq(order.getOrderNo(), userId, request.type(),
                "批量售后申请已提交，共" + count + "件商品，等待审核");

        return count;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AfterSaleResponse approve(Long adminId, Long id, String remark) {
        AfterSale afterSale = afterSaleMapper.findById(id);
        if (afterSale == null) {
            throw new NotFoundException("售后记录", id);
        }
        if (!AfterSaleStatus.APPLIED.name().equals(afterSale.getStatus())) {
            throw new BusinessException("当前状态不允许审核，仅可审核售后中的申请");
        }

        // 查 OrderItem + Order
        OrderItem item = orderItemMapper.findById(afterSale.getOrderItemId());
        if (item == null) {
            throw new BusinessException("关联的订单明细不存在");
        }
        Order order = orderMapper.findById(afterSale.getOrderId());
        if (order == null) {
            throw new BusinessException("关联的订单不存在");
        }

        // 更新售后状态为 COMPLETED
        int affected = afterSaleMapper.updateStatus(id, AfterSaleStatus.COMPLETED.name(),
                remark, adminId, AfterSaleStatus.APPLIED.name());
        if (affected == 0) {
            throw new BusinessException("审核失败，售后状态已变更");
        }

        // 标记 OrderItem 已退款
        orderItemMapper.updateRefundStatus(item.getId(), "REFUNDED");

        // 扣减订单金额
        int deducted = orderMapper.deductTotalAmount(order.getId(), afterSale.getRefundAmount());
        if (deducted == 0) {
            throw new BusinessException("扣减订单金额失败，订单金额不足或状态异常");
        }

        // 检查该订单所有明细行是否均已退款
        List<OrderItem> allItems = orderItemMapper.findByOrderId(order.getId());
        boolean allRefunded = allItems.stream()
                .allMatch(i -> "REFUNDED".equals(i.getRefundStatus()));
        if (allRefunded) {
            int statusAffected = orderMapper.updateStatus(order.getId(),
                    OrderStatus.REFUNDED.name(), order.getStatus());
            if (statusAffected == 0) {
                throw new BusinessException("订单状态已变更，请刷新后重试");
            }
        } else {
            // 该订单还有商品未退款，但若无其他待审核售后单则退出 REFUNDING
            restoreOrderIfNoPending(order.getId(), afterSale.getType());
        }

        // 恢复库存
        restoreStock(item);

        // MQ 通知
        sendAfterSaleMq(order.getOrderNo(), afterSale.getUserId(), afterSale.getType(),
                "售后审核通过，退款金额：" + afterSale.getRefundAmount());

        // 重新加载以获取最新状态
        AfterSale fresh = afterSaleMapper.findById(id);
        return toResponse(fresh != null ? fresh : afterSale);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AfterSaleResponse reject(Long adminId, Long id, String remark) {
        AfterSale afterSale = afterSaleMapper.findById(id);
        if (afterSale == null) {
            throw new NotFoundException("售后记录", id);
        }
        if (!AfterSaleStatus.APPLIED.name().equals(afterSale.getStatus())) {
            throw new BusinessException("当前状态不允许审核，仅可审核售后中的申请");
        }

        // 更新售后状态为 REJECTED
        int affected = afterSaleMapper.updateStatus(id, AfterSaleStatus.REJECTED.name(),
                remark, adminId, AfterSaleStatus.APPLIED.name());
        if (affected == 0) {
            throw new BusinessException("驳回失败，售后状态已变更");
        }

        // 若没有其他待审核售后单，则恢复订单原状态
        restoreOrderIfNoPending(afterSale.getOrderId(), afterSale.getType());

        // MQ 通知
        sendAfterSaleMq(afterSale.getOrderNo(), afterSale.getUserId(), afterSale.getType(),
                "售后申请被驳回，原因：" + (remark != null ? remark : "无"));

        // 重新加载以获取最新状态
        AfterSale fresh = afterSaleMapper.findById(id);
        return toResponse(fresh != null ? fresh : afterSale);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int autoApproveExpired() {
        List<AfterSale> pendingList = afterSaleMapper.findPendingOver24h();
        if (pendingList.isEmpty()) {
            return 0;
        }

        int count = 0;
        for (AfterSale afterSale : pendingList) {
            try {
                // 用系统管理员ID=1自动审核
                approve(1L, afterSale.getId(), "超过24小时未审核，系统自动通过");
                count++;
            } catch (Exception e) {
                log.warn("自动审核售后单失败: afterSaleId={}, reason={}", afterSale.getId(), e.getMessage());
            }
        }
        if (count > 0) {
            log.info("自动审核完成: {} / {} 笔", count, pendingList.size());
        }
        return count;
    }

    @Override
    public PageResult<AfterSaleResponse> getAfterSalePage(Long userId, AfterSaleQuery query) {
        int page = query.getPage();
        int size = query.getSize();
        int offset = (page - 1) * size;

        long total = afterSaleMapper.countFiltered(userId, query.orderId(), query.status(),
                query.afterSaleNo(), query.orderNo());
        if (total == 0) {
            return PageResult.of(Collections.emptyList(), 0, page, size);
        }

        List<AfterSale> list = afterSaleMapper.findPage(offset, size, userId, query.orderId(),
                query.status(), query.afterSaleNo(), query.orderNo());
        List<AfterSaleResponse> records = list.stream()
                .map(this::toResponse)
                .toList();

        return PageResult.of(records, total, page, size);
    }

    @Override
    public AfterSaleResponse getAfterSaleDetail(Long userId, Long id) {
        AfterSale afterSale = afterSaleMapper.findById(id);
        if (afterSale == null) {
            throw new NotFoundException("售后记录", id);
        }
        if (userId != null && !afterSale.getUserId().equals(userId)
                && !SecurityUtils.isCurrentUserAdmin()) {
            throw new ForbiddenException("无权查看该售后记录");
        }
        return toResponse(afterSale);
    }

    // ==================== private helpers ====================

    private AfterSaleResponse toResponse(AfterSale a) {
        OrderItem item = orderItemMapper.findById(a.getOrderItemId());
        String productName = item != null ? item.getProductName() : "";
        String specName = item != null ? item.getSpecName() : "";
        return new AfterSaleResponse(
                a.getId(),
                a.getAfterSaleNo(),
                a.getOrderId(),
                a.getOrderNo(),
                a.getOrderItemId(),
                a.getType(),
                a.getReason(),
                a.getRefundAmount(),
                a.getStatus(),
                productName,
                specName,
                a.getReviewRemark(),
                a.getReviewTime(),
                a.getCreateTime()
        );
    }

    private void restoreStock(OrderItem item) {
        if (item.getSkuId() != null) {
            productStockMapper.restoreSkuStock(item.getSkuId(), item.getQuantity());
            productStockMapper.refreshProductStock(item.getProductId());
        } else {
            productStockMapper.restoreProductStock(item.getProductId(), item.getQuantity());
        }
    }

    private void sendAfterSaleMq(String orderNo, Long userId, String type, String message) {
        try {
            messageProducer.sendOrderNotification(new OrderMessage(
                    orderNo, userId,
                    SecurityUtils.getCurrentUsername(),
                    "AFTER_SALE_" + type,
                    message,
                    LocalDateTime.now().toString()
            ));
        } catch (Exception e) {
            log.warn("售后MQ消息发送失败: orderNo={}, type={}", orderNo, type, e);
        }
    }

    private String generateAfterSaleNo() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        int random = ThreadLocalRandom.current().nextInt(100000, 1000000);
        return "AS" + timestamp + random;
    }

    /**
     * 检查指定订单是否还有进行中的售后单，若没有则从 REFUNDING 恢复到原状态
     */
    private void restoreOrderIfNoPending(Long orderId, String afterSaleType) {
        List<OrderItem> allItems = orderItemMapper.findByOrderId(orderId);
        boolean hasApplied = allItems.stream().anyMatch(i -> {
            AfterSale as = afterSaleMapper.findAppliedByOrderItemId(i.getId());
            return as != null;
        });
        if (hasApplied) {
            return; // 还有待审核的，保持 REFUNDING
        }

        Order order = orderMapper.findById(orderId);
        if (order == null || !OrderStatus.REFUNDING.name().equals(order.getStatus())) {
            return;
        }

        // 从已处理的售后记录中取原始订单状态
        String originalStatus = allItems.stream()
                .map(i -> afterSaleMapper.findByOrderItemId(i.getId()))
                .filter(as -> as != null && as.getOriginalOrderStatus() != null)
                .findFirst()
                .map(AfterSale::getOriginalOrderStatus)
                .orElse("REFUND".equals(afterSaleType) ? "PAID" : "SHIPPED");
        int affected = orderMapper.updateStatus(orderId, originalStatus, OrderStatus.REFUNDING.name());
        if (affected > 0) {
            log.info("订单状态恢复: orderId={}, status={}", orderId, originalStatus);
        }
    }
}
