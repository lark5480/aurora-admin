package com.aurora.admin.controller;

import com.aurora.admin.annotation.RateLimit;
import com.aurora.admin.annotation.RateLimit.KeyType;
import com.aurora.admin.dto.ApiResponse;
import com.aurora.admin.dto.CategoryRequest;
import com.aurora.admin.entity.ProductCategory;
import com.aurora.admin.service.ProductCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class ProductCategoryController {

    private final ProductCategoryService productCategoryService;

    @GetMapping("/tree")
    public ApiResponse getTree() {
        List<ProductCategory> tree = productCategoryService.getTree();
        return ApiResponse.success(tree);
    }

    @RateLimit(key = KeyType.USER, limit = 10, duration = 60)
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ApiResponse create(@RequestBody CategoryRequest request) {
        ProductCategory category = productCategoryService.create(request);
        return ApiResponse.success(category);
    }

    @RateLimit(key = KeyType.USER, limit = 10, duration = 60)
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ApiResponse update(@PathVariable Long id, @RequestBody CategoryRequest request) {
        ProductCategory category = productCategoryService.update(id, request);
        return ApiResponse.success(category);
    }

    @RateLimit(key = KeyType.USER, limit = 10, duration = 60)
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ApiResponse delete(@PathVariable Long id) {
        productCategoryService.delete(id);
        return ApiResponse.success("删除成功");
    }
}
