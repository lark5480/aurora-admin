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

/**
 * 商品管理控制器。提供商品的分页查询、详情查看、创建、更新、状态修改及删除功能。
 * 管理端操作（增、改、状态、删）需要 ADMIN 或 SUPER_ADMIN 权限，并受速率限制 (10次/60秒)。
 * 映射路径：/api/products
 */
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    /**
     * 分页查询商品列表。支持按关键词、分类、状态过滤。
     *
     * @param page      页码，从 1 开始，默认 1
     * @param size      每页条数，默认 10
     * @param keyword   搜索关键词（商品名称模糊匹配），默认空字符串
     * @param categoryId 分类 ID，可选
     * @param status    商品状态（ON_SALE / OFF_SHELF），默认空字符串（全部）
     * @return 分页结果，包含商品列表及总记录数
     */
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

    /**
     * 根据 ID 获取商品详情。
     *
     * @param id 商品 ID
     * @return 商品详情响应
     */
    @GetMapping("/{id}")
    public ApiResponse getById(@PathVariable Long id) {
        ProductResponse product = productService.getById(id);
        return ApiResponse.success(product);
    }

    /**
     * 创建商品。仅 ADMIN 或 SUPER_ADMIN 可操作，限流 10次/60秒。
     *
     * @param request 商品创建请求，包含名称、价格、分类、描述等信息，需通过 Bean Validation
     * @return 创建成功的商品详情
     */
    @RateLimit(key = KeyType.USER, limit = 10, duration = 60)
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ApiResponse create(@Valid @RequestBody ProductRequest request) {
        ProductResponse product = productService.create(request);
        return ApiResponse.success(product);
    }

    /**
     * 更新指定商品的完整信息。仅 ADMIN 或 SUPER_ADMIN 可操作，限流 10次/60秒。
     *
     * @param id      商品 ID
     * @param request 商品更新请求，包含需要修改的字段，需通过 Bean Validation
     * @return 更新后的商品详情
     */
    @RateLimit(key = KeyType.USER, limit = 10, duration = 60)
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ApiResponse update(@PathVariable Long id, @Valid @RequestBody ProductRequest request) {
        ProductResponse product = productService.update(id, request);
        return ApiResponse.success(product);
    }

    /**
     * 更新单个商品的状态。支持 "ON"/"ON_SALE" → 上架，"OFF"/"OFF_SHELF" → 下架。
     * 仅 ADMIN 或 SUPER_ADMIN 可操作，限流 10次/60秒。
     *
     * @param id   商品 ID
     * @param body 请求体，需包含 "status" 字段（ON / ON_SALE / OFF / OFF_SHELF）
     * @return 更新结果
     */
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

    /**
     * 批量更新商品状态。支持 "ON"/"ON_SALE" → 上架，"OFF"/"OFF_SHELF" → 下架。
     * 仅 ADMIN 或 SUPER_ADMIN 可操作，限流 10次/60秒。
     *
     * @param body 请求体，需包含 "ids"（商品 ID 列表）和 "status" 字段
     * @return 批量操作结果
     */
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

    /**
     * 删除指定商品。仅 ADMIN 或 SUPER_ADMIN 可操作，限流 10次/60秒。
     *
     * @param id 商品 ID
     * @return 删除结果
     */
    @RateLimit(key = KeyType.USER, limit = 10, duration = 60)
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ApiResponse delete(@PathVariable Long id) {
        productService.delete(id);
        return ApiResponse.success("删除成功");
    }
}
