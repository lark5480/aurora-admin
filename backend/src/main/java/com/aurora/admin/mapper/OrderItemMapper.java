package com.aurora.admin.mapper;

import com.aurora.admin.entity.OrderItem;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface OrderItemMapper {

    @Insert("<script>" +
            "INSERT INTO t_order_item(order_id, product_id, sku_id, product_name, spec_name, price, quantity) VALUES " +
            "<foreach collection='list' item='item' separator=','>" +
            "(#{item.orderId}, #{item.productId}, #{item.skuId}, #{item.productName}, #{item.specName}, #{item.price}, #{item.quantity})" +
            "</foreach>" +
            "</script>")
    int insertBatch(@Param("list") List<OrderItem> items);

    @Select("SELECT * FROM t_order_item WHERE order_id = #{orderId}")
    List<OrderItem> findByOrderId(Long orderId);

    @Select("SELECT * FROM t_order_item WHERE id = #{id}")
    OrderItem findById(Long id);

    @Update("UPDATE t_order_item SET refund_status = #{refundStatus} WHERE id = #{id}")
    int updateRefundStatus(@Param("id") Long id, @Param("refundStatus") String refundStatus);
}
