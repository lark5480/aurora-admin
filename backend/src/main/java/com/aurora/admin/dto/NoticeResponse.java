package com.aurora.admin.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 公告响应 DTO
 */
public record NoticeResponse(
    Long id,
    String title,
    String content,
    String targetType,
    List<Long> targetIds,
    LocalDateTime publishTime,
    LocalDateTime expireTime,
    String status,
    LocalDateTime createTime
) {}
