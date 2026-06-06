package com.aurora.admin.service;

import com.aurora.admin.dto.ProductQuery;
import com.aurora.admin.dto.ProductRequest;
import com.aurora.admin.dto.ProductResponse;
import com.aurora.admin.dto.PageResult;

import java.util.List;

public interface ProductService {

    /**
     * 分页查询，支持关键词搜索、分类筛选、状态筛选
     */
    PageResult<ProductResponse> getPage(ProductQuery query);

    /**
     * 查询商品详情（含 SKU 列表）
     */
    ProductResponse getById(Long id);

    /**
     * 创建商品+SKU
     */
    ProductResponse create(ProductRequest request);

    /**
     * 更新商品+SKU（删旧SKU再插新SKU）
     */
    ProductResponse update(Long id, ProductRequest request);

    /**
     * 上下架
     */
    void updateStatus(Long id, String status);

    /**
     * 批量上下架
     */
    void batchUpdateStatus(List<Long> ids, String status);

    /**
     * 软删除
     */
    void delete(Long id);
}
