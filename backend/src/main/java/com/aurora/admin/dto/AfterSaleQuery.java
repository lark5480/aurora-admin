package com.aurora.admin.dto;

public record AfterSaleQuery(
    Integer page,
    Integer size,
    Long orderId,
    String status,
    String afterSaleNo,
    String orderNo
) {
    public int getPage() {
        return page != null && page > 0 ? page : 1;
    }

    public int getSize() {
        return size != null && size > 0 ? size : 10;
    }
}
