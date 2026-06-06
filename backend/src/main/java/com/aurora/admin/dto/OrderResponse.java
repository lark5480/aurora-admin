package com.aurora.admin.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单响应
 */
public record OrderResponse(
    Long id,
    String orderNo,
    String username,
    BigDecimal totalAmount,
    String status,
    String receiverName,
    String receiverPhone,
    String receiverAddress,
    String remark,
    String trackingNumber,
    LocalDateTime createTime,
    List<OrderItemResponse> orderItems
) {}
