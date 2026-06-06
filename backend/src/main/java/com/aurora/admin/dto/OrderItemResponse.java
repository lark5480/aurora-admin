package com.aurora.admin.dto;

import java.math.BigDecimal;

/**
 * 订单明细响应
 */
public record OrderItemResponse(
    Long id,
    String productName,
    String specName,
    BigDecimal price,
    Integer quantity,
    BigDecimal subtotal,
    String coverImage,
    String refundStatus
) {
    public OrderItemResponse(String productName, String specName, BigDecimal price, Integer quantity) {
        this(null, productName, specName, price, quantity,
             price != null && quantity != null ? price.multiply(BigDecimal.valueOf(quantity)) : BigDecimal.ZERO,
             null, null);
    }

    public OrderItemResponse withCoverImage(String coverImage) {
        return new OrderItemResponse(id, productName, specName, price, quantity, subtotal, coverImage, refundStatus);
    }
}
