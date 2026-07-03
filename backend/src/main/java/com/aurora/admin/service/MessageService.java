package com.aurora.admin.service;

import com.aurora.admin.entity.Message;
import java.util.List;

public interface MessageService {
    /**
     * 分页查询指定接收方收到的消息列表，可按已读状态过滤。
     *
     * @param recipientId 接收方用户 ID
     * @param page        页码
     * @param size        每页条数
     * @param isRead      可选的已读状态过滤（1 已读，0 未读），null 表示全部
     * @return 消息列表
     */
    List<Message> findByRecipientId(Long recipientId, int page, int size, Integer isRead);

    /**
     * 统计指定接收方的消息总数，可按已读状态过滤。
     *
     * @param recipientId 接收方用户 ID
     * @param isRead      可选的已读状态过滤，null 表示全部
     * @return 消息总数
     */
    int countByRecipientId(Long recipientId, Integer isRead);

    /**
     * 分页查询全部消息（管理员用），可按已读状态过滤。
     *
     * @param page   页码
     * @param size   每页条数
     * @param isRead 可选的已读状态过滤，null 表示全部
     * @return 消息列表
     */
    List<Message> findAll(int page, int size, Integer isRead);

    /**
     * 统计全部消息总数（管理员用），可按已读状态过滤。
     *
     * @param isRead 可选的已读状态过滤，null 表示全部
     * @return 消息总数
     */
    int countAll(Integer isRead);

    /**
     * 查询指定用户的未读消息数量。
     *
     * @param recipientId 接收方用户 ID
     * @return 未读消息数
     */
    int countUnread(Long recipientId);

    /**
     * 根据 ID 查询单条消息。
     *
     * @param id 消息 ID
     * @return 消息实体，不存在时返回 null
     */
    Message findById(Long id);

    /**
     * 发送消息。将消息持久化到数据库，并通过 WebSocket 推送给接收方。
     *
     * @param message 待发送的消息实体
     */
    void sendMessage(Message message);

    /**
     * 将指定消息标记为已读。仅管理员或消息接收方有权操作。
     *
     * @param id          消息 ID
     * @param recipientId 当前用户 ID（用于权限校验）
     * @return 标记成功返回 true，消息不存在或无权限返回 false
     */
    boolean markAsRead(Long id, Long recipientId);

    /**
     * 将指定接收方的所有未读消息标记为已读。
     * 管理员调用时标记全部用户的消息。
     *
     * @param recipientId 接收方用户 ID
     * @return 实际标记的条数
     */
    int markAllAsRead(Long recipientId);

    /**
     * 删除指定消息。仅管理员或消息接收方有权操作，删除时记录操作日志。
     *
     * @param id          消息 ID
     * @param recipientId 当前用户 ID（用于权限校验）
     * @return 删除成功返回 true，消息不存在或无权限返回 false
     */
    boolean delete(Long id, Long recipientId);
}
