package com.aurora.admin.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 库存操作 Mapper，提供乐观锁扣减库存能力。
 * 不修改原有 ProductMapper / ProductSkuMapper，仅在此扩展库存相关操作。
 */
@Mapper
public interface ProductStockMapper {

    /**
     * 乐观锁扣减商品库存
     *
     * @return 受影响行数（0 表示库存不足或商品不存在）
     */
    @Update("UPDATE t_product SET stock = stock - #{quantity} WHERE id = #{id} AND stock >= #{quantity} AND is_deleted = 0")
    int deductProductStock(@Param("id") Long id, @Param("quantity") Integer quantity);

    /**
     * 乐观锁扣减 SKU 库存
     *
     * @return 受影响行数（0 表示库存不足或 SKU 不存在）
     */
    @Update("UPDATE t_product_sku SET stock = stock - #{quantity} WHERE id = #{id} AND stock >= #{quantity} AND is_deleted = 0")
    int deductSkuStock(@Param("id") Long id, @Param("quantity") Integer quantity);

    /**
     * 重新计算商品总库存 = 所有 SKU 库存之和，并更新 t_product.stock
     * 应在扣减/增加 SKU 库存后调用
     *
     * @return 受影响行数
     */
    @Update("UPDATE t_product SET stock = (" +
            "  SELECT COALESCE(SUM(stock), 0) FROM t_product_sku WHERE product_id = #{productId} AND is_deleted = 0" +
            ") WHERE id = #{productId}")
    int refreshProductStock(@Param("productId") Long productId);

    /**
     * 恢复 SKU 库存（取消订单时调用）
     *
     * @return 受影响行数
     */
    @Update("UPDATE t_product_sku SET stock = stock + #{quantity} WHERE id = #{id} AND is_deleted = 0")
    int restoreSkuStock(@Param("id") Long id, @Param("quantity") Integer quantity);

    /**
     * 恢复商品库存（取消订单时调用，无 SKU 的商品）
     *
     * @return 受影响行数
     */
    @Update("UPDATE t_product SET stock = stock + #{quantity} WHERE id = #{id} AND is_deleted = 0")
    int restoreProductStock(@Param("id") Long id, @Param("quantity") Integer quantity);
}
