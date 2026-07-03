package com.aurora.admin.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.aurora.admin.dto.ApiResponse;
import com.aurora.admin.service.ProductSearchService;

import lombok.RequiredArgsConstructor;

/**
 * 商品搜索控制器。提供基于 Elasticsearch 的商品全文搜索、索引重建和索引删除功能。
 * 搜索接口对所有登录用户开放，索引管理接口需要 ADMIN 或 SUPER_ADMIN 角色。
 * 路径: /api/products/search
 */
@RestController
@RequestMapping("/api/products/search")
@RequiredArgsConstructor
public class ProductSearchController {
    
    private final ProductSearchService productSearchService;

    /**
     * 搜索商品。关键词匹配商品名称和描述，支持按分类和状态筛选。
     * 结果按创建时间降序排列，分页返回。
     *
     * @param keyword    搜索关键词（为空时返回全部商品）
     * @param categoryId 分类 ID（可选，指定后仅返回该分类下商品）
     * @param status     商品状态（可选，如 ON_SALE）
     * @param page       页码，从 1 开始，默认 1
     * @param size       每页条数，默认 20
     * @return 商品搜索结果分页数据
     */
    @GetMapping
    public ApiResponse search(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "") String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(productSearchService.search(keyword, categoryId, status, page, size));
    }

    /**
     * 全量重建 ES 商品索引。从 MySQL 读取所有上架商品后批量写入 Elasticsearch，
     * 重建前会清空旧索引数据。需要 ADMIN 或 SUPER_ADMIN 权限。
     *
     * @return 操作提示信息
     */
    @PostMapping("/reindex")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ApiResponse reindex() {
        productSearchService.reindexAll();
        return ApiResponse.success("全量索引重建中");
    }

    /**
     * 删除 ES 中的 products 索引。删除后搜索功能不可用，需调用 /reindex 重建。
     * 需要 ADMIN 或 SUPER_ADMIN 权限。
     *
     * @return 操作提示信息
     */
    @PostMapping("/delete-index")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ApiResponse deleteIndex() {
        productSearchService.deleteIndex();
        return ApiResponse.success("索引已删除，请调用 /reindex 重建");
    }
}
