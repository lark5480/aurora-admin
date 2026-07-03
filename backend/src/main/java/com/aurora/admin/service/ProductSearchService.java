package com.aurora.admin.service;

import org.springframework.data.domain.Page;

import com.aurora.admin.document.ProductDocument;

public interface ProductSearchService {

    /**
     * 搜索商品：关键词匹配 name/description，支持分类和状态筛选
     *
     * @param keyword    搜索关键词，为空时返回全部商品
     * @param categoryId 分类 ID，可选，传 null 则不按分类过滤
     * @param status     商品状态，可选，如 ON_SALE
     * @param page       页码，从 1 开始
     * @param size       每页条数
     * @return 商品文档分页结果，按创建时间降序
     */
    Page<ProductDocument> search(String keyword, Long categoryId, String status, int page, int size);

    /**
     * 索引单个文档到 ES
     *
     * @param doc 商品文档对象
     */
    void index(ProductDocument doc);

    /**
     * 根据商品ID索引（查 MySQL 后同步到 ES）
     *
     * @param productId 商品 ID，MySQL 中必须存在该记录
     */
    void indexProduct(Long productId);

    /**
     * 全量重建索引（查 MySQL 所有上架商品，批量写入 ES）
     */
    void reindexAll();

    /**
     * 删除指定商品索引
     *
     * @param productId 要删除索引的商品 ID
     */
    void delete(Long productId);

    /**
     * 删除整个 products 索引
     */
    void deleteIndex();
}
