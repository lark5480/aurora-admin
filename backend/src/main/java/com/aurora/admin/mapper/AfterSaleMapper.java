package com.aurora.admin.mapper;

import com.aurora.admin.annotation.DataScope;
import com.aurora.admin.entity.AfterSale;
import org.apache.ibatis.annotations.*;

import java.util.Collection;
import java.util.List;

@Mapper
public interface AfterSaleMapper {

    @Insert("INSERT INTO t_after_sale(after_sale_no, order_id, order_item_id, user_id, type, reason, refund_amount, original_order_status, status) " +
            "VALUES(#{afterSaleNo}, #{orderId}, #{orderItemId}, #{userId}, #{type}, #{reason}, #{refundAmount}, #{originalOrderStatus}, #{status})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(AfterSale afterSale);

    @Select("SELECT a.*, o.order_no AS order_no FROM t_after_sale a " +
            "LEFT JOIN t_order o ON a.order_id = o.id AND o.is_deleted = 0 " +
            "WHERE a.id = #{id} AND a.is_deleted = 0")
    AfterSale findById(Long id);

    @Select("SELECT * FROM t_after_sale WHERE order_item_id = #{orderItemId} AND is_deleted = 0 ORDER BY create_time DESC LIMIT 1")
    AfterSale findByOrderItemId(Long orderItemId);

    @Select("SELECT * FROM t_after_sale WHERE order_item_id = #{orderItemId} AND status = 'APPLIED' AND is_deleted = 0 LIMIT 1")
    AfterSale findAppliedByOrderItemId(Long orderItemId);

    @Update("UPDATE t_after_sale SET status = #{status}, review_remark = #{reviewRemark}, " +
            "review_time = NOW(), reviewer_id = #{reviewerId} " +
            "WHERE id = #{id} AND status = #{expectedStatus} AND is_deleted = 0")
    int updateStatus(@Param("id") Long id, @Param("status") String status,
                     @Param("reviewRemark") String reviewRemark,
                     @Param("reviewerId") Long reviewerId,
                     @Param("expectedStatus") String expectedStatus);

    // ========== 批量查询 ==========

    @Select("<script>SELECT * FROM t_after_sale WHERE is_deleted = 0 AND status = 'APPLIED' AND order_item_id IN " +
            "<foreach collection='orderItemIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>" +
            "</script>")
    List<AfterSale> findAppliedByOrderItemIds(@Param("orderItemIds") Collection<Long> ids);

    @Select("<script>SELECT * FROM t_after_sale WHERE is_deleted = 0 AND order_item_id IN " +
            "<foreach collection='orderItemIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>" +
            " ORDER BY create_time DESC</script>")
    List<AfterSale> findByOrderItemIds(@Param("orderItemIds") Collection<Long> ids);

    @Select("<script>SELECT * FROM t_after_sale WHERE is_deleted = 0 AND status = 'APPLIED' AND order_id IN " +
            "<foreach collection='orderIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>" +
            "</script>")
    List<AfterSale> findAppliedByOrderIds(@Param("orderIds") Collection<Long> orderIds);

    // ========== 动态查询（XML: mapper/AfterSaleMapper.xml） ==========

    @DataScope(userColumn = "a.user_id")
    List<AfterSale> findPage(@Param("offset") int offset, @Param("size") int size,
                             @Param("userId") Long userId, @Param("orderId") Long orderId,
                             @Param("status") String status,
                             @Param("afterSaleNo") String afterSaleNo,
                             @Param("orderNo") String orderNo);

    @DataScope(userColumn = "a.user_id")
    long countFiltered(@Param("userId") Long userId, @Param("orderId") Long orderId,
                       @Param("status") String status,
                       @Param("afterSaleNo") String afterSaleNo,
                       @Param("orderNo") String orderNo);

    /** 查询超过24小时未审核的售后单（APPLIED 状态且 create_time < now - 24h） */
    List<AfterSale> findPendingOver24h();
}
