package com.aurora.admin.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 支付请求
 */
public record PayRequest(
    @NotBlank(message = "订单号不能为空")
    String orderNo,

    @NotBlank(message = "支付方式不能为空")
    String payMethod
) {
}
