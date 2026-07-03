package com.aurora.admin.service;

import com.aurora.admin.dto.ProductQuery;
import com.aurora.admin.dto.ProductRequest;
import com.aurora.admin.dto.ProductResponse;
import com.aurora.admin.dto.PageResult;

import java.util.List;

public interface ProductService {

    /**
     * 分页查询商品列表。支持按关键词模糊搜索、分类筛选、状态筛选。
     *
     * @param query 查询条件，包含页码、每页条数、关键词、分类 ID、状态
     * @return 分页结果，包含商品列表及总记录数
     */
    PageResult<ProductResponse> getPage(ProductQuery query);

    /**
     * 查询商品详情（含 SKU 列表）。
     *
     * @param id 商品 ID
     * @return 商品详情，包含商品基本信息及所有 SKU
     */
    ProductResponse getById(Long id);

    /**
     * 创建商品及其 SKU。
     *
     * @param request 商品创建请求，包含商品基本信息及 SKU 列表
     * @return 创建成功的商品详情（含 SKU）
     */
    ProductResponse create(ProductRequest request);

    /**
     * 更新商品及其 SKU。先删除原 SKU 再插入新 SKU（全量替换）。
     *
     * @param id      商品 ID
     * @param request 商品更新请求，包含需更新的字段及新 SKU 列表
     * @return 更新后的商品详情（含 SKU）
     */
    ProductResponse update(Long id, ProductRequest request);

    /**
     * 更新单个商品上下架状态。
     *
     * @param id     商品 ID
     * @param status 目标状态：ON_SALE（上架）或 OFF_SHELF（下架）
     */
    void updateStatus(Long id, String status);

    /**
     * 批量更新商品的上下架状态。
     *
     * @param ids    商品 ID 列表，非空
     * @param status 目标状态：ON_SALE（上架）或 OFF_SHELF（下架）
     */
    void batchUpdateStatus(List<Long> ids, String status);

    /**
     * 软删除商品。将商品标记为已删除而非物理移除。
     *
     * @param id 商品 ID
     */
    void delete(Long id);
}
