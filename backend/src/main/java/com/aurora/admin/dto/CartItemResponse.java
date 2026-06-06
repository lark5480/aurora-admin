package com.aurora.admin.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * 购物车商品响应（含商品信息）
 */
public record CartItemResponse(
    Long id,
    Long productId,
    Long skuId,
    String productName,
    String coverImage,
    BigDecimal price,
    Integer stock,
    String specName,
    Integer quantity,
    String status,
    List<SkuOption> availableSkus
) {
}
