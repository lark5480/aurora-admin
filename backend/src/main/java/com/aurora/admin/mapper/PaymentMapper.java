package com.aurora.admin.mapper;

import com.aurora.admin.entity.Payment;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface PaymentMapper {

    @Insert("INSERT INTO t_payment(order_id, order_no, transaction_no, amount, pay_method, status) " +
            "VALUES(#{orderId}, #{orderNo}, #{transactionNo}, #{amount}, #{payMethod}, #{status})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Payment payment);

    @Select("SELECT * FROM t_payment WHERE id = #{id} AND is_deleted = 0")
    Payment findById(Long id);

    @Select("SELECT * FROM t_payment WHERE order_id = #{orderId} AND is_deleted = 0 ORDER BY create_time DESC")
    List<Payment> findByOrderId(Long orderId);

    @Select("SELECT * FROM t_payment WHERE order_no = #{orderNo} AND is_deleted = 0 ORDER BY create_time DESC")
    List<Payment> findByOrderNo(String orderNo);

    @Update("UPDATE t_payment SET status = #{status}, transaction_no = #{transactionNo} WHERE id = #{id} AND is_deleted = 0")
    int updateStatus(@Param("id") Long id, @Param("status") String status, @Param("transactionNo") String transactionNo);
}
