package com.aurora.admin.config;

import com.aurora.admin.document.ProductDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.stereotype.Component;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;

/**
 * 应用启动时确保 ES 索引和数据就绪，新同学部署后搜索开箱即用。
 * <p>
 * 处理以下场景：
 * <ul>
 *   <li><b>首次启动</b>：索引不存在 → 自动创建 + 全量同步 MySQL → ES</li>
 *   <li><b>ES 启动比后端慢</b>：连接失败静默跳过，下次重启重试</li>
 *   <li><b>MySQL 重建后索引已存在但无数据</b>：自动重新同步</li>
 *   <li><b>正常重启</b>：索引和数据均存在，跳过</li>
 * </ul>
 */
@Component
public class EsIndexInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(EsIndexInitializer.class);

    @Autowired
    private ElasticsearchOperations elasticsearchOperations;

    @Autowired(required = false)
    private com.aurora.admin.service.ProductSearchService productSearchService;

    @Override
    public void run(ApplicationArguments args) {
        try {
            IndexOperations indexOps = elasticsearchOperations.indexOps(ProductDocument.class);
            boolean indexExists = indexOps.exists();

            if (!indexExists) {
                log.info("[ES] 索引 products 不存在，正在创建...");
                indexOps.create();
                indexOps.putMapping(indexOps.createMapping(ProductDocument.class));
                log.info("[ES] 索引 products 创建成功");
                reindexAll();
                return;
            }

            // 索引已存在，检查是否有文档数据
            long docCount = elasticsearchOperations.count(
                    NativeQuery.builder()
                            .withQuery(Query.of(q -> q.matchAll(m -> m)))
                            .build(),
                    ProductDocument.class);

            if (docCount == 0) {
                log.info("[ES] 索引 products 已存在但无数据，开始全量同步...");
                reindexAll();
            } else {
                log.info("[ES] 索引 products 已就绪，当前 {} 条文档", docCount);
            }

        } catch (Exception e) {
            log.warn("[ES] 连接失败，跳过索引初始化（可下次启动重试）: {}", e.getMessage());
        }
    }

    private void reindexAll() {
        if (productSearchService == null) {
            log.warn("[ES] ProductSearchService 不可用，跳过数据同步");
            return;
        }
        try {
            productSearchService.reindexAll();
            log.info("[ES] 全量同步完成");
        } catch (Exception e) {
            log.warn("[ES] 全量同步失败（可能商品表为空或无上架商品）: {}", e.getMessage());
        }
    }
}
