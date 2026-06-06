package com.aurora.admin.mapper;

import com.aurora.admin.entity.ProductCategory;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ProductCategoryMapper {

    @Select("SELECT * FROM t_product_category WHERE is_deleted = 0 ORDER BY sort_order")
    List<ProductCategory> findAll();

    @Select("SELECT * FROM t_product_category WHERE id = #{id} AND is_deleted = 0")
    ProductCategory findById(Long id);

    @Select("SELECT * FROM t_product_category WHERE parent_id = #{parentId} AND is_deleted = 0 ORDER BY sort_order")
    List<ProductCategory> findByParentId(Long parentId);

    @Insert("INSERT INTO t_product_category(parent_id, name, sort_order) VALUES(#{parentId}, #{name}, #{sortOrder})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ProductCategory category);

    @Update("UPDATE t_product_category SET parent_id = #{parentId}, name = #{name}, sort_order = #{sortOrder} WHERE id = #{id} AND is_deleted = 0")
    int update(ProductCategory category);

    @Update("UPDATE t_product_category SET is_deleted = 1 WHERE id = #{id}")
    int deleteById(Long id);

    @Select("SELECT COUNT(*) FROM t_product_category WHERE parent_id = #{parentId} AND is_deleted = 0")
    int countByParentId(Long parentId);
}
