package com.aurora.admin.dto;

/**
 * 分类创建/更新请求 DTO
 */
public record CategoryRequest(
    String name,
    Long parentId,
    Integer sortOrder
) {
}
