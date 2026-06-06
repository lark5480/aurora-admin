package com.aurora.admin.dto;

public record OrderMessage(
    String messageId,
    String orderNo,
    Long userId,
    String username,
    String type,
    String message,
    String timestamp
) {
    public OrderMessage(String orderNo, Long userId, String username, String type, String message, String timestamp) {
        this(java.util.UUID.randomUUID().toString().replace("-", ""), orderNo, userId, username, type, message, timestamp);
    }
}
