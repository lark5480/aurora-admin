package com.aurora.admin.mapper;

import com.aurora.admin.entity.Product;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ProductSearchMapper {

    @Select("SELECT * FROM t_product WHERE is_deleted = 0 AND status = 'ON_SALE'")
    List<Product> findAllOnSaleProducts();
}
