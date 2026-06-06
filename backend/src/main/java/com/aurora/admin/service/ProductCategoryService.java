package com.aurora.admin.service;

import com.aurora.admin.dto.CategoryRequest;
import com.aurora.admin.entity.ProductCategory;

import java.util.List;

public interface ProductCategoryService {

    /**
     * 获取分类树（递归，一级分类下挂二级）
     */
    List<ProductCategory> getTree();

    /**
     * 创建分类
     */
    ProductCategory create(CategoryRequest request);

    /**
     * 更新分类
     */
    ProductCategory update(Long id, CategoryRequest request);

    /**
     * 删除分类
     */
    void delete(Long id);
}
