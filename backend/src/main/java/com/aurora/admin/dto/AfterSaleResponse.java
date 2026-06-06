package com.aurora.admin.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AfterSaleResponse(
    Long id,
    String afterSaleNo,
    Long orderId,
    String orderNo,
    Long orderItemId,
    String type,
    String reason,
    BigDecimal refundAmount,
    String status,
    String productName,
    String specName,
    String reviewRemark,
    LocalDateTime reviewTime,
    LocalDateTime createTime
) {}
