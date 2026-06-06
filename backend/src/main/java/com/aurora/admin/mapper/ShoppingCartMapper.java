package com.aurora.admin.mapper;

import com.aurora.admin.entity.ShoppingCart;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ShoppingCartMapper {

    @Select("SELECT * FROM t_shopping_cart WHERE id = #{id} AND is_deleted = 0")
    ShoppingCart findById(Long id);

    @Select("SELECT * FROM t_shopping_cart WHERE user_id = #{userId} AND is_deleted = 0 ORDER BY create_time DESC")
    List<ShoppingCart> findByUserId(Long userId);

    @Select("SELECT * FROM t_shopping_cart WHERE user_id = #{userId} AND product_id = #{productId} AND sku_id <=> #{skuId} AND is_deleted = 0")
    ShoppingCart findByUserAndProduct(@Param("userId") Long userId, @Param("productId") Long productId, @Param("skuId") Long skuId);

    @Insert("INSERT INTO t_shopping_cart(user_id, product_id, sku_id, quantity) VALUES(#{userId}, #{productId}, #{skuId}, #{quantity})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ShoppingCart cart);

    @Update("UPDATE t_shopping_cart SET quantity = #{quantity} WHERE id = #{id} AND is_deleted = 0")
    int updateQuantity(@Param("id") Long id, @Param("quantity") Integer quantity);

    @Update("UPDATE t_shopping_cart SET sku_id = #{skuId} WHERE id = #{id} AND is_deleted = 0")
    int updateSku(@Param("id") Long id, @Param("skuId") Long skuId);

    @Delete("DELETE FROM t_shopping_cart WHERE id = #{id}")
    int deleteById(@Param("id") Long id);

    @Delete("DELETE FROM t_shopping_cart WHERE user_id = #{userId}")
    int deleteByUserId(Long userId);
}
