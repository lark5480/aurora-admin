package com.aurora.admin.mapper;

import com.aurora.admin.entity.ProductSku;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ProductSkuMapper {

    @Select("SELECT * FROM t_product_sku WHERE product_id = #{productId} AND is_deleted = 0")
    List<ProductSku> findByProductId(Long productId);

    @Insert("INSERT INTO t_product_sku(product_id, spec_name, spec_code, price, stock) " +
            "VALUES(#{productId}, #{specName}, #{specCode}, #{price}, #{stock})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ProductSku sku);

    @Update("UPDATE t_product_sku SET spec_name = #{specName}, spec_code = #{specCode}, " +
            "price = #{price}, stock = #{stock} WHERE id = #{id} AND is_deleted = 0")
    int update(ProductSku sku);

    @Update("UPDATE t_product_sku SET is_deleted = 1 WHERE id = #{id}")
    int deleteById(Long id);

    @Update("UPDATE t_product_sku SET is_deleted = 1 WHERE product_id = #{productId}")
    int deleteByProductId(Long productId);
}
