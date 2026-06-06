package com.aurora.admin.dto;

import com.aurora.admin.entity.ProductSku;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 商品详情响应 DTO
 */
public record ProductResponse(
    Long id,
    Long categoryId,
    String categoryName,
    String name,
    String description,
    String coverImage,
    BigDecimal price,
    Integer stock,
    String status,
    LocalDateTime createTime,
    LocalDateTime updateTime,
    List<ProductSku> skus
) {}
