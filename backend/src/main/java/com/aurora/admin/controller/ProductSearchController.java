package com.aurora.admin.controller;

import com.aurora.admin.dto.ApiResponse;
import com.aurora.admin.service.ProductSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/products/search")
@RequiredArgsConstructor
public class ProductSearchController {

    private final ProductSearchService productSearchService;

    @GetMapping
    public ApiResponse search(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "") String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(productSearchService.search(keyword, categoryId, status, page, size));
    }

    @PostMapping("/reindex")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ApiResponse reindex() {
        productSearchService.reindexAll();
        return ApiResponse.success("全量索引重建中");
    }
}
