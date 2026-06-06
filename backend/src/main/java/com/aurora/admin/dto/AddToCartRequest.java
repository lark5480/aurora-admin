package com.aurora.admin.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 添加购物车请求
 */
public record AddToCartRequest(
    @NotNull(message = "商品ID不能为空")
    Long productId,

    Long skuId,

    @NotNull(message = "数量不能为空")
    @Min(value = 1, message = "数量不能小于1")
    Integer quantity
) {
}
