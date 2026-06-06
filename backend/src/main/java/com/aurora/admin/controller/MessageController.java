package com.aurora.admin.controller;

import com.aurora.admin.annotation.RateLimit;
import com.aurora.admin.annotation.RateLimit.KeyType;
import com.aurora.admin.dto.ApiResponse;
import com.aurora.admin.entity.Message;
import com.aurora.admin.service.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/messages")
public class MessageController {

    @Autowired
    private MessageService messageService;

    /**
     * 发送消息
     */
    @RateLimit(key = KeyType.USER, limit = 10, duration = 60, message = "发送消息过于频繁，请稍后再试")
    @PostMapping
    public ApiResponse sendMessage(@RequestBody Message message) {
        Long userId = getCurrentUserId();
        message.setSenderId(userId);
        if (message.getSenderName() == null || message.getSenderName().isEmpty()) {
            message.setSenderName(getCurrentUsername());
        }
        messageService.sendMessage(message);
        return ApiResponse.success("发送成功");
    }

    @GetMapping
    public ApiResponse getMessages(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer isRead) {
        Map<String, Object> data = new HashMap<>();
        if (isCurrentUserAdmin()) {
            List<Message> messages = messageService.findAll(page, size, isRead);
            int total = messageService.countAll(isRead);
            data.put("list", messages);
            data.put("total", total);
        } else {
            Long userId = getCurrentUserId();
            List<Message> messages = messageService.findByRecipientId(userId, page, size, isRead);
            int total = messageService.countByRecipientId(userId, isRead);
            data.put("list", messages);
            data.put("total", total);
        }
        data.put("page", page);
        data.put("size", size);
        return ApiResponse.success(data);
    }

    @GetMapping("/unread-count")
    public ApiResponse getUnreadCount() {
        Long userId = getCurrentUserId();
        int count = messageService.countUnread(userId);
        Map<String, Object> data = new HashMap<>();
        data.put("count", count);
        return ApiResponse.success(data);
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<ApiResponse> markAsRead(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        boolean success = messageService.markAsRead(id, userId);
        if (success) {
            return ResponseEntity.ok(ApiResponse.success("标记已读成功"));
        }
        return ResponseEntity.status(404).body(ApiResponse.error(404, "消息不存在或无权限"));
    }

    @PutMapping("/read-all")
    public ApiResponse markAllAsRead() {
        Long userId = getCurrentUserId();
        int count = messageService.markAllAsRead(userId);
        Map<String, Object> data = new HashMap<>();
        data.put("count", count);
        return ApiResponse.success(data);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> deleteMessage(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        boolean success = messageService.delete(id, userId);
        if (success) {
            return ResponseEntity.ok(ApiResponse.success("删除成功"));
        }
        return ResponseEntity.status(404).body(ApiResponse.error(404, "消息不存在或无权限"));
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getCredentials() != null) {
            return (Long) auth.getCredentials();
        }
        return 0L;
    }

    private String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getName() != null) {
            return auth.getName();
        }
        return "";
    }

    private boolean isCurrentUserAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return false;
        }
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority -> "ROLE_SUPER_ADMIN".equals(authority) || "ROLE_ADMIN".equals(authority));
    }
}
