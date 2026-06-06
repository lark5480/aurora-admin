package com.aurora.admin.service;

import com.aurora.admin.entity.Message;
import java.util.List;

public interface MessageService {
    List<Message> findByRecipientId(Long recipientId, int page, int size, Integer isRead);
    int countByRecipientId(Long recipientId, Integer isRead);
    List<Message> findAll(int page, int size, Integer isRead);
    int countAll(Integer isRead);
    int countUnread(Long recipientId);
    Message findById(Long id);
    void sendMessage(Message message);
    boolean markAsRead(Long id, Long recipientId);
    int markAllAsRead(Long recipientId);
    boolean delete(Long id, Long recipientId);
}
