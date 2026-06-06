package com.aurora.admin.service.impl;

import com.aurora.admin.dto.CategoryRequest;
import com.aurora.admin.entity.ProductCategory;
import com.aurora.admin.exception.NotFoundException;
import com.aurora.admin.mapper.ProductCategoryMapper;
import com.aurora.admin.service.ProductCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductCategoryServiceImpl implements ProductCategoryService {

    private final ProductCategoryMapper productCategoryMapper;

    @Override
    public List<ProductCategory> getTree() {
        List<ProductCategory> all = productCategoryMapper.findAll();
        Map<Long, List<ProductCategory>> grouped = all.stream()
                .collect(Collectors.groupingBy(ProductCategory::getParentId));

        List<ProductCategory> roots = grouped.getOrDefault(0L, new ArrayList<>());
        roots.sort((a, b) -> {
            int sortA = a.getSortOrder() != null ? a.getSortOrder() : 0;
            int sortB = b.getSortOrder() != null ? b.getSortOrder() : 0;
            return Integer.compare(sortA, sortB);
        });

        for (ProductCategory root : roots) {
            buildChildren(root, grouped);
        }

        return roots;
    }

    private void buildChildren(ProductCategory parent, Map<Long, List<ProductCategory>> grouped) {
        List<ProductCategory> children = grouped.getOrDefault(parent.getId(), new ArrayList<>());
        children.sort((a, b) -> {
            int sortA = a.getSortOrder() != null ? a.getSortOrder() : 0;
            int sortB = b.getSortOrder() != null ? b.getSortOrder() : 0;
            return Integer.compare(sortA, sortB);
        });
        parent.setChildren(children);
        for (ProductCategory child : children) {
            buildChildren(child, grouped);
        }
    }

    @Override
    @Transactional
    public ProductCategory create(CategoryRequest request) {
        ProductCategory category = new ProductCategory();
        category.setParentId(request.parentId() != null ? request.parentId() : 0L);
        category.setName(request.name());
        category.setSortOrder(request.sortOrder() != null ? request.sortOrder() : 0);
        category.setCreateTime(LocalDateTime.now());
        productCategoryMapper.insert(category);
        return category;
    }

    @Override
    @Transactional
    public ProductCategory update(Long id, CategoryRequest request) {
        ProductCategory category = productCategoryMapper.findById(id);
        if (category == null) {
            throw new NotFoundException("商品分类", id);
        }
        // 只有当 request.parentId() 不为 null 时才更新 parentId，保护原有层级关系
        if (request.parentId() != null) {
            category.setParentId(request.parentId());
        }
        category.setName(request.name());
        category.setSortOrder(request.sortOrder() != null ? request.sortOrder() : 0);
        productCategoryMapper.update(category);
        return category;
    }

    @Override
    @Transactional
    public void delete(Long id) {
        ProductCategory category = productCategoryMapper.findById(id);
        if (category == null) {
            throw new NotFoundException("商品分类", id);
        }
        int childCount = productCategoryMapper.countByParentId(id);
        if (childCount > 0) {
            throw new IllegalStateException("请先删除子分类");
        }
        productCategoryMapper.deleteById(id);
    }
}
