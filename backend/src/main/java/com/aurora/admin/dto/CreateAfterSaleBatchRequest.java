package com.aurora.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateAfterSaleBatchRequest(
    @NotNull(message = "订单ID不能为空")
    Long orderId,

    @NotBlank(message = "售后类型不能为空")
    String type,

    String reason
) {}
