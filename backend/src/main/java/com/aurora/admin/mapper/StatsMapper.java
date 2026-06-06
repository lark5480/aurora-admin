package com.aurora.admin.mapper;

import org.apache.ibatis.annotations.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Mapper
public interface StatsMapper {

    @Select("SELECT COUNT(*) FROM t_user WHERE is_deleted = 0")
    long countUsers();

    @Select("SELECT COUNT(*) FROM t_file WHERE is_deleted = 0")
    long countFiles();

    @Select("SELECT COUNT(*) FROM t_message WHERE is_deleted = 0")
    long countMessages();

    @Select("SELECT COUNT(DISTINCT user_id) FROM t_operation_log WHERE DATE(create_time) = CURDATE() AND is_deleted = 0")
    long countActiveUsersToday();

    @Select("SELECT COUNT(*) FROM t_user WHERE DATE(create_time) = #{date} AND is_deleted = 0")
    long countUsersByDate(LocalDate date);

    @Select("SELECT COUNT(*) FROM t_file WHERE is_deleted = 0 AND DATE(create_time) = #{date}")
    long countFilesByDate(LocalDate date);

    @Select("SELECT COUNT(*) FROM t_message WHERE DATE(create_time) = #{date} AND is_deleted = 0")
    long countMessagesByDate(LocalDate date);

    @Select("SELECT COUNT(*) FROM t_file WHERE upload_user_id = #{userId} AND is_deleted = 0")
    long countFilesByUserId(Long userId);

    @Select("SELECT COUNT(*) FROM t_message WHERE recipient_id = #{userId} AND is_deleted = 0")
    long countMessagesByUserId(Long userId);

    @Select("SELECT COUNT(*) FROM t_operation_log WHERE user_id = #{userId} AND DATE(create_time) = CURDATE() AND is_deleted = 0")
    long countActiveOperationsTodayByUserId(Long userId);

    // ===== 订单统计 =====

    @Select("SELECT COUNT(*) FROM t_order WHERE is_deleted = 0")
    long countOrders();

    @Select("SELECT COUNT(*) FROM t_order WHERE DATE(create_time) = CURDATE() AND is_deleted = 0")
    long countOrdersToday();

    @Select("SELECT COUNT(*) FROM t_order WHERE DATE(create_time) = #{date} AND is_deleted = 0")
    long countOrdersByDate(LocalDate date);

    @Select("SELECT COALESCE(SUM(total_amount), 0) FROM t_order WHERE is_deleted = 0 AND status NOT IN ('CANCELLED', 'REFUNDED')")
    BigDecimal sumOrderAmount();

    @Select("SELECT COALESCE(SUM(total_amount), 0) FROM t_order WHERE DATE(create_time) = CURDATE() AND is_deleted = 0 AND status NOT IN ('CANCELLED', 'REFUNDED')")
    BigDecimal sumOrderAmountToday();

    @Select("SELECT status, COUNT(*) AS count FROM t_order WHERE is_deleted = 0 GROUP BY status")
    List<Map<String, Object>> countOrdersByStatus();
}
