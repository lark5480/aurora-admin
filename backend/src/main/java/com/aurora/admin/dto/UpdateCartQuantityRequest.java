package com.aurora.admin.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 更新购物车数量请求
 */
public record UpdateCartQuantityRequest(
    @NotNull(message = "数量不能为空")
    @Min(value = 1, message = "数量不能小于1")
    Integer quantity
) {
}
