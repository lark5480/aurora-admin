package com.aurora.admin.dto;

/**
 * 订单分页查询 DTO
 */
public record OrderQuery(
    Integer page,
    Integer size,
    String status,
    String orderNo,
    String username
) {
    public int getPage() {
        return page != null && page > 0 ? page : 1;
    }

    public int getSize() {
        return size != null && size > 0 ? size : 10;
    }
}
