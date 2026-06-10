package com.aurora.admin.service.impl;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.aurora.admin.document.ProductDocument;
import com.aurora.admin.entity.Product;
import com.aurora.admin.entity.ProductCategory;
import com.aurora.admin.mapper.ProductCategoryMapper;
import com.aurora.admin.mapper.ProductMapper;
import com.aurora.admin.mapper.ProductSearchMapper;
import com.aurora.admin.repository.ProductSearchRepository;
import com.aurora.admin.service.ProductSearchService;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductSearchServiceImpl implements ProductSearchService {

    private final ProductSearchRepository productSearchRepository;
    private final ElasticsearchOperations elasticsearchOperations;
    private final ProductMapper productMapper;
    private final ProductCategoryMapper productCategoryMapper;
    private final ProductSearchMapper productSearchMapper;

    @Override
    public Page<ProductDocument> search(String keyword, Long categoryId, String status, int page, int size) {
        Query esQuery = Query.of(q -> q
                .bool(b -> {
                    // 关键词匹配 name 或 description
                    if (StringUtils.hasText(keyword)) {
                        b.should(s -> s.match(m -> m.field("name").query(keyword)));
                        b.should(s -> s.match(m -> m.field("description").query(keyword)));
                    } else {
                        b.must(m -> m.matchAll(ma -> ma));
                    }
                    // 分类筛选
                    if (categoryId != null) {
                        b.filter(f -> f.term(t -> t.field("categoryId").value(categoryId)));
                    }
                    // 状态筛选（防御性编程，虽然 ES 目前只存上架商品）
                    if (StringUtils.hasText(status)) {
                        b.filter(f -> f.term(t -> t.field("status").value(status)));
                    }
                    return b;
                })
        );

        var nativeQuery = NativeQuery.builder()
                .withQuery(esQuery)
                .withSort(Sort.by(Sort.Direction.DESC, "createTime"))
                .withPageable(PageRequest.of(Math.max(0, page - 1), size))
                .build();

        var searchHits = elasticsearchOperations.search(nativeQuery, ProductDocument.class);
        long total = searchHits.getTotalHits();
        List<ProductDocument> documents = searchHits.stream()
                .map(SearchHit::getContent)
                .toList();

        return new PageImpl<>(documents, PageRequest.of(Math.max(0, page - 1), size), total);
    }

    @Override
    public void index(ProductDocument doc) {
        productSearchRepository.save(doc);
    }

    @Override
    public void indexProduct(Long productId) {
        Product product = productMapper.findById(productId);
        if (product == null) {
            log.error("ES索引失败，MySQL中商品不存在: id={}", productId);
            return;
        }
        String categoryName = resolveCategoryName(product.getCategoryId());
        ProductDocument doc = ProductDocument.from(product, categoryName);
        productSearchRepository.save(doc);
        log.debug("商品索引完成: id={}", productId);
    }

    @Override
    public void reindexAll() {
        // 先清空索引，避免残留下架/已删除的旧文档
        long deletedCount = elasticsearchOperations.count(NativeQuery.builder()
                        .withQuery(Query.of(q -> q.matchAll(m -> m)))
                        .build(),
                ProductDocument.class);
        if (deletedCount > 0) {
            NativeQuery deleteAllQuery = NativeQuery.builder()
                    .withQuery(Query.of(q -> q.matchAll(m -> m)))
                    .build();
            elasticsearchOperations.delete(deleteAllQuery, ProductDocument.class);
            log.info("[ES] 已清空 {} 条旧文档", deletedCount);
        }

        List<Product> products = productSearchMapper.findAllOnSaleProducts();
        if (products.isEmpty()) {
            log.info("没有上架商品需要重建索引");
            return;
        }

        List<ProductDocument> documents = products.stream()
                .map(p -> ProductDocument.from(p, resolveCategoryName(p.getCategoryId())))
                .toList();

        productSearchRepository.saveAll(documents);
        log.info("全量索引重建完成，共 {} 条", documents.size());
    }

    @Override
    public void delete(Long productId) {
        productSearchRepository.deleteById(productId);
        log.debug("商品索引已删除: id={}", productId);
    }

    @Override
    public void deleteIndex() {
        try {
            var indexOps = elasticsearchOperations.indexOps(ProductDocument.class);
            if (indexOps.exists()) {
                indexOps.delete();
                log.info("[ES] products 索引已删除");
            } else {
                log.warn("[ES] products 索引不存在");
            }
        } catch (Exception e) {
            log.error("[ES] 删除索引失败", e);
            throw new RuntimeException("删除索引失败: " + e.getMessage(), e);
        }
    }

    private String resolveCategoryName(Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        ProductCategory category = productCategoryMapper.findById(categoryId);
        return category != null ? category.getName() : null;
    }
}
