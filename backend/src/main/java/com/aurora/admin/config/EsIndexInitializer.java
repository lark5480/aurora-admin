package com.aurora.admin.config;

import com.aurora.admin.document.ProductDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.stereotype.Component;

/**
 * 应用启动时检查并创建 ES 索引，无需手动调接口。
 * <p>
 * 如果索引不存在则自动创建；已存在则跳过。
 * 创建完成后立即全量同步 MySQL 上架商品到 ES。
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

            if (!indexOps.exists()) {
                log.info("[ES] 索引 products 不存在，正在创建...");
                indexOps.create();
                indexOps.putMapping(indexOps.createMapping(ProductDocument.class));
                log.info("[ES] 索引 products 创建成功，开始全量同步商品...");

                if (productSearchService != null) {
                    try {
                        productSearchService.reindexAll();
                        log.info("[ES] 全量同步完成");
                    } catch (Exception e) {
                        log.warn("[ES] 全量同步失败（可能商品表为空）: {}", e.getMessage());
                    }
                }
            } else {
                log.info("[ES] 索引 products 已存在，跳过创建");
            }
        } catch (Exception e) {
            log.warn("[ES] ES 不可用，跳过索引初始化: {}", e.getMessage());
        }
    }
}
