package com.aurora.admin.mapper;

import com.aurora.admin.annotation.DataScope;
import com.aurora.admin.entity.Order;
import org.apache.ibatis.annotations.*;

import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface OrderMapper {

    // ========== 单条 CRUD（注解） ==========

    @Insert("INSERT INTO t_order(order_no, user_id, total_amount, status, receiver_name, receiver_phone, receiver_address, remark) " +
            "VALUES(#{orderNo}, #{userId}, #{totalAmount}, #{status}, #{receiverName}, #{receiverPhone}, #{receiverAddress}, #{remark})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Order order);

    @Select("SELECT * FROM t_order WHERE id = #{id} AND is_deleted = 0")
    Order findById(Long id);

    @Select("SELECT * FROM t_order WHERE order_no = #{orderNo} AND is_deleted = 0")
    Order findByOrderNo(String orderNo);

    @Update("UPDATE t_order SET status = #{status} WHERE id = #{id} AND status = #{expectedStatus} AND is_deleted = 0")
    int updateStatus(@Param("id") Long id, @Param("status") String status, @Param("expectedStatus") String expectedStatus);

    @Update("UPDATE t_order SET tracking_number = #{trackingNumber} WHERE id = #{id} AND is_deleted = 0")
    int updateTrackingNumber(@Param("id") Long id, @Param("trackingNumber") String trackingNumber);

    @Update("UPDATE t_order SET total_amount = total_amount - #{amount} WHERE id = #{id} AND total_amount >= #{amount} AND is_deleted = 0")
    int deductTotalAmount(@Param("id") Long id, @Param("amount") BigDecimal amount);

    // ========== 动态查询 / 批量操作（XML: mapper/OrderMapper.xml） ==========

    @DataScope(userColumn = "o.user_id")
    List<Order> findPage(@Param("offset") int offset, @Param("size") int size,
                         @Param("userId") Long userId, @Param("status") String status,
                         @Param("orderNo") String orderNo, @Param("username") String username);

    @DataScope(userColumn = "o.user_id")
    long countFiltered(@Param("userId") Long userId, @Param("status") String status,
                       @Param("orderNo") String orderNo, @Param("username") String username);

    @DataScope(userColumn = "o.user_id")
    List<Order> findForExport(@Param("userId") Long userId, @Param("status") String status,
                              @Param("orderNo") String orderNo, @Param("username") String username,
                              @Param("limit") int limit);

    int countByIdsAndUser(@Param("ids") List<Long> ids, @Param("userId") Long userId);

    int batchDeleteCancelled(@Param("ids") List<Long> ids);
}
