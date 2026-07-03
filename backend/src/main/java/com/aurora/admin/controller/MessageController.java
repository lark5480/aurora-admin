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

/**
 * 消息管理控制器。提供系统内部消息的发送、查询、标记已读、删除等功能，
 * 支持区分管理员和普通用户的查询范围，并对发送接口进行限流。
 * REST 路径：/api/messages
 */
@RestController
@RequestMapping("/api/messages")
public class MessageController {

    @Autowired
    private MessageService messageService;

    /**
     * 发送消息（限流 10次/分钟）。将消息写入数据库并通过 WebSocket 推送给接收方。
     *
     * @param message 消息内容，包含接收方 ID、标题、正文等字段
     * @return 发送成功提示
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

    /**
     * 分页查询消息列表。管理员返回全部消息，普通用户仅返回自己收到的消息。
     *
     * @param page   页码，默认 1
     * @param size   每页条数，默认 10
     * @param isRead 可选的已读状态过滤（1 已读，0 未读），不传则查询全部
     * @return 包含消息列表、总数、页码、每页条数的分页数据
     */
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

    /**
     * 获取当前用户的未读消息数量。
     *
     * @return 包含未读计数的数据（key: count）
     */
    @GetMapping("/unread-count")
    public ApiResponse getUnreadCount() {
        Long userId = getCurrentUserId();
        int count = messageService.countUnread(userId);
        Map<String, Object> data = new HashMap<>();
        data.put("count", count);
        return ApiResponse.success(data);
    }

    /**
     * 将指定消息标记为已读。仅管理员或消息接收方有权操作。
     *
     * @param id 消息 ID
     * @return 标记成功返回 200，消息不存在或无权限返回 404
     */
    @PutMapping("/{id}/read")
    public ResponseEntity<ApiResponse> markAsRead(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        boolean success = messageService.markAsRead(id, userId);
        if (success) {
            return ResponseEntity.ok(ApiResponse.success("标记已读成功"));
        }
        return ResponseEntity.status(404).body(ApiResponse.error(404, "消息不存在或无权限"));
    }

    /**
     * 将当前用户所有未读消息标记为已读。管理员可标记全部用户的消息。
     *
     * @return 包含实际标记条数的数据（key: count）
     */
    @PutMapping("/read-all")
    public ApiResponse markAllAsRead() {
        Long userId = getCurrentUserId();
        int count = messageService.markAllAsRead(userId);
        Map<String, Object> data = new HashMap<>();
        data.put("count", count);
        return ApiResponse.success(data);
    }

    /**
     * 删除指定消息。仅管理员或消息接收方有权操作。
     *
     * @param id 消息 ID
     * @return 删除成功返回 200，消息不存在或无权限返回 404
     */
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
