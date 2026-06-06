package com.aurora.admin.dto;

import java.math.BigDecimal;

/**
 * 支付记录响应
 */
public record PaymentResponse(
    Long id,
    String orderNo,
    String transactionNo,
    BigDecimal amount,
    String payMethod,
    String status
) {}
