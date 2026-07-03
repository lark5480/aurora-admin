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

/**
 * 商品分类控制器。提供商品分类的树形结构查询、创建、更新、删除等 REST 接口，路径前缀为 /api/categories。
 */
@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class ProductCategoryController {

    private final ProductCategoryService productCategoryService;

    /**
     * 获取分类树。递归查询全部分类，按 sortOrder 排序，以父子层级的树形结构返回。无需鉴权。
     */
    @GetMapping("/tree")
    public ApiResponse getTree() {
        List<ProductCategory> tree = productCategoryService.getTree();
        return ApiResponse.success(tree);
    }

    /**
     * 创建分类。新增商品分类，仅 ADMIN/SUPER_ADMIN 可操作。限流 10 次/用户/60 秒。
     *
     * @param request 分类请求体，包含名称、父分类 ID、排序值
     * @return 新建的分类实体
     */
    @RateLimit(key = KeyType.USER, limit = 10, duration = 60)
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ApiResponse create(@RequestBody CategoryRequest request) {
        ProductCategory category = productCategoryService.create(request);
        return ApiResponse.success(category);
    }

    /**
     * 更新分类。修改商品分类的名称、排序或父分类，仅 ADMIN/SUPER_ADMIN 可操作。限流 10 次/用户/60 秒。
     *
     * @param id      分类 ID
     * @param request 分类请求体，包含名称、排序值及可选的父分类 ID
     * @return 更新后的分类实体
     */
    @RateLimit(key = KeyType.USER, limit = 10, duration = 60)
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ApiResponse update(@PathVariable Long id, @RequestBody CategoryRequest request) {
        ProductCategory category = productCategoryService.update(id, request);
        return ApiResponse.success(category);
    }

    /**
     * 删除分类。仅 ADMIN/SUPER_ADMIN 可操作。若该分类下存在子分类则拒绝删除，需先清除子分类。限流 10 次/用户/60 秒。
     *
     * @param id 分类 ID
     * @return 操作结果消息
     */
    @RateLimit(key = KeyType.USER, limit = 10, duration = 60)
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ApiResponse delete(@PathVariable Long id) {
        productCategoryService.delete(id);
        return ApiResponse.success("删除成功");
    }
}
