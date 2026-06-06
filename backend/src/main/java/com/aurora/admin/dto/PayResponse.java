package com.aurora.admin.dto;

import java.math.BigDecimal;

/**
 * 支付结果响应
 */
public record PayResponse(
    String orderNo,
    BigDecimal amount,
    String status,
    String message
) {}
