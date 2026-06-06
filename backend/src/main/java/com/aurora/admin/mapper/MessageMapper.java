package com.aurora.admin.mapper;

import com.aurora.admin.entity.Message;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface MessageMapper {

    @Select("SELECT * FROM t_message WHERE recipient_id = #{recipientId} AND (#{isRead} IS NULL OR is_read = #{isRead}) AND is_deleted = 0 ORDER BY create_time DESC LIMIT #{offset}, #{size}")
    List<Message> findByRecipientId(@Param("recipientId") Long recipientId, @Param("offset") int offset, @Param("size") int size, @Param("isRead") Integer isRead);

    @Select("SELECT COUNT(*) FROM t_message WHERE recipient_id = #{recipientId} AND (#{isRead} IS NULL OR is_read = #{isRead}) AND is_deleted = 0")
    int countByRecipientId(@Param("recipientId") Long recipientId, @Param("isRead") Integer isRead);

    @Select("SELECT * FROM t_message WHERE (#{isRead} IS NULL OR is_read = #{isRead}) AND is_deleted = 0 ORDER BY create_time DESC LIMIT #{offset}, #{size}")
    List<Message> findAll(@Param("offset") int offset, @Param("size") int size, @Param("isRead") Integer isRead);

    @Select("SELECT COUNT(*) FROM t_message WHERE (#{isRead} IS NULL OR is_read = #{isRead}) AND is_deleted = 0")
    int countAll(@Param("isRead") Integer isRead);

    @Select("SELECT COUNT(*) FROM t_message WHERE recipient_id = #{recipientId} AND is_read = 0 AND is_deleted = 0")
    int countUnreadByRecipientId(@Param("recipientId") Long recipientId);

    @Select("SELECT * FROM t_message WHERE id = #{id} AND is_deleted = 0")
    Message findById(Long id);

    @Insert("INSERT INTO t_message(title, content, type, priority, sender_id, sender_name, recipient_id, is_read, create_time) " +
            "VALUES(#{title}, #{content}, #{type}, #{priority}, #{senderId}, #{senderName}, #{recipientId}, 0, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Message message);

    @Update("UPDATE t_message SET is_read = 1, read_time = NOW() WHERE id = #{id} AND is_deleted = 0")
    int markAsRead(Long id);

    @Update("UPDATE t_message SET is_read = 1, read_time = NOW() WHERE recipient_id = #{recipientId} AND is_read = 0 AND is_deleted = 0")
    int markAllAsRead(Long recipientId);

    @Update("UPDATE t_message SET is_read = 1, read_time = NOW() WHERE is_read = 0 AND is_deleted = 0")
    int markAllAsReadForAdmin();

    @Update("UPDATE t_message SET is_deleted = 1 WHERE id = #{id} AND is_deleted = 0")
    int delete(Long id);
}
