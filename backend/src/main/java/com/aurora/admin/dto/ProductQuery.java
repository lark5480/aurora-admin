package com.aurora.admin.dto;

/**
 * 商品分页查询 DTO
 */
public record ProductQuery(
    Integer page,
    Integer size,
    String keyword,
    Long categoryId,
    String status
) {

    public int getPage() {
        return page != null && page > 0 ? page : 1;
    }

    public int getSize() {
        return size != null && size > 0 ? size : 10;
    }
}
