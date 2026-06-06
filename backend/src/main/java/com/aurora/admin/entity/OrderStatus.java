package com.aurora.admin.entity;

public enum OrderStatus {
    /** 待支付 */
    PENDING,
    /** 已支付 */
    PAID,
    /** 已发货 */
    SHIPPED,
    /** 已完成 */
    COMPLETED,
    /** 已取消 */
    CANCELLED,
    /** 售后中 */
    REFUNDING,
    /** 已退款 */
    REFUNDED
}
