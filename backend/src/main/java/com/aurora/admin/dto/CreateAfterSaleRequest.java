package com.aurora.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateAfterSaleRequest(
    @NotNull(message = "订单明细ID不能为空")
    Long orderItemId,

    @NotBlank(message = "售后类型不能为空")
    String type,

    String reason
) {}
