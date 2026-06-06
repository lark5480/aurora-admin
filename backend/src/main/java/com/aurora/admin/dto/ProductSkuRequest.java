package com.aurora.admin.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;

/**
 * 商品SKU请求 DTO
 */
public record ProductSkuRequest(
    String specName,
    String specCode,

    @DecimalMin(value = "0.01", message = "SKU价格必须大于0")
    BigDecimal price,

    @Min(value = 0, message = "SKU库存不能小于0")
    Integer stock
) {
}
