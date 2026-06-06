package com.aurora.admin.dto;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;

/**
 * 创建/更新商品请求 DTO
 */
public record ProductRequest(
    String name,
    Long categoryId,
    String description,
    String coverImage,

    @DecimalMin(value = "0.01", message = "价格必须大于0")
    BigDecimal price,

    @Min(value = 0, message = "库存不能小于0")
    Integer stock,

    String status,
    List<ProductSkuRequest> skus
) {
}
