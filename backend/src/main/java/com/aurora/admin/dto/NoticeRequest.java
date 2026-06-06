package com.aurora.admin.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 公告创建/更新请求 DTO
 */
public record NoticeRequest(
    String title,
    String content,
    String targetType,
    List<Long> targetIds,
    LocalDateTime publishTime,
    LocalDateTime expireTime
) {
}
