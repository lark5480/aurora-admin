package com.aurora.admin.dto;

import java.math.BigDecimal;

/**
 * SKU 选项（购物车/订单选择用）
 */
public record SkuOption(
    Long id,
    String specName,
    BigDecimal price,
    Integer stock
) {}
