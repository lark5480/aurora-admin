package com.aurora.admin.mapper;

import com.aurora.admin.entity.Product;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ProductMapper {

    // ========== 单条 CRUD（注解） ==========

    @Select("SELECT p.*, c.name AS category_name FROM t_product p " +
            "LEFT JOIN t_product_category c ON p.category_id = c.id AND c.is_deleted = 0 " +
            "WHERE p.id = #{id} AND p.is_deleted = 0")
    Product findById(Long id);

    @Select("SELECT p.*, c.name AS category_name FROM t_product p " +
            "LEFT JOIN t_product_category c ON p.category_id = c.id AND c.is_deleted = 0 " +
            "WHERE p.is_deleted = 0 ORDER BY p.create_time DESC " +
            "LIMIT #{offset}, #{size}")
    List<Product> findPage(@Param("offset") int offset, @Param("size") int size);

    @Insert("INSERT INTO t_product(category_id, name, description, cover_image, price, stock, status, create_time) " +
            "VALUES(#{categoryId}, #{name}, #{description}, #{coverImage}, #{price}, #{stock}, #{status}, #{createTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Product product);

    @Update("UPDATE t_product SET category_id = #{categoryId}, name = #{name}, description = #{description}, " +
            "cover_image = #{coverImage}, price = #{price}, stock = #{stock}, status = #{status} WHERE id = #{id}")
    int update(Product product);

    @Update("UPDATE t_product SET status = #{status} WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") String status);

    @Update("UPDATE t_product SET is_deleted = 1 WHERE id = #{id}")
    int deleteById(Long id);

    @Update("UPDATE t_product SET stock = #{stock} WHERE id = #{id}")
    int updateStockById(@Param("id") Long id, @Param("stock") int stock);

    // ========== 动态查询 / 批量操作（XML: mapper/ProductMapper.xml） ==========

    List<Product> findPageWithFilter(@Param("offset") int offset, @Param("size") int size,
                                     @Param("keyword") String keyword,
                                     @Param("categoryId") Long categoryId,
                                     @Param("status") String status);

    long countFiltered(@Param("keyword") String keyword,
                       @Param("categoryId") Long categoryId,
                       @Param("status") String status);

    int batchUpdateStatus(@Param("ids") List<Long> ids, @Param("status") String status);
}
