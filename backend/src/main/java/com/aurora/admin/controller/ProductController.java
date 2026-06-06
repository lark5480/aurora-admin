package com.aurora.admin.controller;

import java.util.Map;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.aurora.admin.annotation.RateLimit;
import com.aurora.admin.annotation.RateLimit.KeyType;
import com.aurora.admin.dto.ApiResponse;
import com.aurora.admin.dto.PageResult;
import com.aurora.admin.dto.ProductQuery;
import com.aurora.admin.dto.ProductRequest;
import com.aurora.admin.dto.ProductResponse;
import com.aurora.admin.service.ProductService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public ApiResponse getPage(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "") String status) {
        ProductQuery query = new ProductQuery(page, size, keyword, categoryId, status);
        PageResult<ProductResponse> result = productService.getPage(query);
        return ApiResponse.success(result);
    }

    @GetMapping("/{id}")
    public ApiResponse getById(@PathVariable Long id) {
        ProductResponse product = productService.getById(id);
        return ApiResponse.success(product);
    }

    @RateLimit(key = KeyType.USER, limit = 10, duration = 60)
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ApiResponse create(@Valid @RequestBody ProductRequest request) {
        ProductResponse product = productService.create(request);
        return ApiResponse.success(product);
    }

    @RateLimit(key = KeyType.USER, limit = 10, duration = 60)
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ApiResponse update(@PathVariable Long id, @Valid @RequestBody ProductRequest request) {
        ProductResponse product = productService.update(id, request);
        return ApiResponse.success(product);
    }

    @RateLimit(key = KeyType.USER, limit = 10, duration = 60)
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ApiResponse updateStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String rawStatus = body.get("status");
        String status = normalizeProductStatus(rawStatus);
        if (status == null) {
            return ApiResponse.error("无效的商品状态: " + rawStatus);
        }
        productService.updateStatus(id, status);
        return ApiResponse.success("更新成功");
    }

    @RateLimit(key = KeyType.USER, limit = 10, duration = 60)
    @PatchMapping("/batch-status")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ApiResponse batchUpdateStatus(@RequestBody Map<String, Object> body) {
        Object rawIds = body.get("ids");
        if (!(rawIds instanceof java.util.List<?> rawList) || rawList.isEmpty()) {
            return ApiResponse.error("请选择商品");
        }
        // JSON 反序列化数字默认为 Integer，需要安全转换为 Long
        java.util.List<Long> ids = rawList.stream()
                .map(o -> o instanceof Number n ? n.longValue() : Long.parseLong(o.toString()))
                .toList();

        String rawStatus = (String) body.get("status");
        String status = normalizeProductStatus(rawStatus);
        if (status == null) {
            return ApiResponse.error("无效的商品状态: " + rawStatus);
        }
        productService.batchUpdateStatus(ids, status);
        return ApiResponse.success("批量操作成功");
    }

    /**
     * 统一处理前端传入的商品状态：
     *   "ON" / "ON_SALE"   → "ON_SALE"
     *   "OFF" / "OFF_SHELF" → "OFF_SHELF"
     */
    private String normalizeProductStatus(String raw) {
        if (raw == null) return null;
        return switch (raw) {
            case "ON", "ON_SALE"       -> "ON_SALE";
            case "OFF", "OFF_SHELF"     -> "OFF_SHELF";
            default -> null;
        };
    }

    @RateLimit(key = KeyType.USER, limit = 10, duration = 60)
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ApiResponse delete(@PathVariable Long id) {
        productService.delete(id);
        return ApiResponse.success("删除成功");
    }
}
