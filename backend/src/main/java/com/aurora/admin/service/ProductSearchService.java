package com.aurora.admin.service;

import org.springframework.data.domain.Page;

import com.aurora.admin.document.ProductDocument;

public interface ProductSearchService {

    /**
     * 搜索商品：关键词匹配 name/description，支持分类和状态筛选
     */
    Page<ProductDocument> search(String keyword, Long categoryId, String status, int page, int size);

    /**
     * 索引单个文档
     */
    void index(ProductDocument doc);

    /**
     * 根据商品ID索引（查 MySQL 后同步到 ES）
     */
    void indexProduct(Long productId);

    /**
     * 全量重建索引（查 MySQL 所有上架商品，批量写入 ES）
     */
    void reindexAll();

    /**
     * 删除指定商品索引
     */
    void delete(Long productId);

    /**
     * 删除整个 products 索引
     */
    void deleteIndex();
}
