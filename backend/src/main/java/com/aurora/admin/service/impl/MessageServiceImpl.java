package com.aurora.admin.service.impl;

import com.aurora.admin.dto.ApiResponse;
import com.aurora.admin.entity.Message;
import com.aurora.admin.entity.OperationLog;
import com.aurora.admin.mapper.MessageMapper;
import com.aurora.admin.mapper.OperationLogMapper;
import com.aurora.admin.service.MessageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 消息管理服务实现。提供消息的增删改查、已读状态管理功能，
 * 通过 WebSocket 实时推送新消息和状态变更通知，删除操作自动记录操作日志。
 */
@Service
public class MessageServiceImpl implements MessageService {

    private static final Logger log = LoggerFactory.getLogger(MessageServiceImpl.class);

    @Autowired
    private MessageMapper messageMapper;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private OperationLogMapper operationLogMapper;

    @Override
    public List<Message> findByRecipientId(Long recipientId, int page, int size, Integer isRead) {
        int offset = (page - 1) * size;
        return messageMapper.findByRecipientId(recipientId, offset, size, isRead);
    }

    @Override
    public int countByRecipientId(Long recipientId, Integer isRead) {
        return messageMapper.countByRecipientId(recipientId, isRead);
    }

    @Override
    public List<Message> findAll(int page, int size, Integer isRead) {
        int offset = (page - 1) * size;
        return messageMapper.findAll(offset, size, isRead);
    }

    @Override
    public int countAll(Integer isRead) {
        return messageMapper.countAll(isRead);
    }

    @Override
    public int countUnread(Long recipientId) {
        return messageMapper.countUnreadByRecipientId(recipientId);
    }

    @Override
    public Message findById(Long id) {
        return messageMapper.findById(id);
    }

    @Override
    public void sendMessage(Message message) {
        messageMapper.insert(message);
        try {
            messagingTemplate.convertAndSendToUser(
                    message.getRecipientId().toString(),
                    "/queue/messages",
                    ApiResponse.success(message)
            );
        } catch (Exception e) {
            log.warn("WebSocket push failed for user {}, reason: {}", message.getRecipientId(), e.getMessage());
        }
    }

    @Override
    public boolean markAsRead(Long id, Long recipientId) {
        Message message = messageMapper.findById(id);
        if (message == null) {
            return false;
        }
        if (!isCurrentUserAdmin() && !message.getRecipientId().equals(recipientId)) {
            return false;
        }
        boolean updated = messageMapper.markAsRead(id) > 0;
        if (updated) {
            try {
                Map<String, Object> data = new HashMap<>();
                data.put("messageId", id);
                data.put("unreadCount", messageMapper.countUnreadByRecipientId(recipientId));
                messagingTemplate.convertAndSendToUser(
                        recipientId.toString(),
                        "/queue/messages",
                        ApiResponse.success(data)
                );
            } catch (Exception e) {
                log.warn("WebSocket push failed for user {}, reason: {}", recipientId, e.getMessage());
            }
        }
        return updated;
    }

    @Override
    public int markAllAsRead(Long recipientId) {
        int count;
        if (isCurrentUserAdmin()) {
            count = messageMapper.markAllAsReadForAdmin();
        } else {
            count = messageMapper.markAllAsRead(recipientId);
        }
        if (count > 0) {
            try {
                Map<String, Object> data = new HashMap<>();
                data.put("unreadCount", 0);
                messagingTemplate.convertAndSendToUser(
                        recipientId.toString(),
                        "/queue/messages",
                        ApiResponse.success(data)
                );
            } catch (Exception e) {
                log.warn("WebSocket push failed for user {}, reason: {}", recipientId, e.getMessage());
            }
        }
        return count;
    }

    @Override
    public boolean delete(Long id, Long recipientId) {
        Message message = messageMapper.findById(id);
        if (message == null) {
            return false;
        }
        if (!isCurrentUserAdmin() && !message.getRecipientId().equals(recipientId)) {
            return false;
        }
        boolean deleted = messageMapper.delete(id) > 0;
        if (deleted) {
            String resourceDesc = "id=" + message.getId() + ", recipientId=" + message.getRecipientId();
            OperationLog operationLog = new OperationLog();
            operationLog.setUserId(getCurrentUserId());
            operationLog.setUsername(getCurrentUsername());
            operationLog.setOperation("删除消息");
            operationLog.setMethod("DELETE");
            operationLog.setUrl("/api/messages/" + id);
            operationLog.setParams(resourceDesc);
            operationLog.setStatus(1);
            operationLog.setCreateTime(LocalDateTime.now());
            operationLogMapper.insert(operationLog);
        }
        return deleted;
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

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }
        Object credentials = auth.getCredentials();
        if (credentials instanceof Long id) {
            return id;
        }
        if (credentials instanceof Number n) {
            return n.longValue();
        }
        return null;
    }

    private String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return "unknown";
        }
        return auth.getName();
    }
}
