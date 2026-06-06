package com.aurora.admin.entity;

public enum AfterSaleStatus {
    /** 售后中（等待审核） */
    APPLIED,
    /** 已退款（审核通过） */
    COMPLETED,
    /** 已驳回（审核不通过） */
    REJECTED
}
