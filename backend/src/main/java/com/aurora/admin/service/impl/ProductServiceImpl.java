package com.aurora.admin.service.impl;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aurora.admin.dto.PageResult;
import com.aurora.admin.dto.ProductQuery;
import com.aurora.admin.dto.ProductRequest;
import com.aurora.admin.dto.ProductResponse;
import com.aurora.admin.dto.ProductSkuRequest;
import com.aurora.admin.entity.Product;
import com.aurora.admin.entity.ProductSku;
import com.aurora.admin.exception.BusinessException;
import com.aurora.admin.exception.NotFoundException;
import com.aurora.admin.mapper.ProductMapper;
import com.aurora.admin.mapper.ProductSkuMapper;
import com.aurora.admin.service.ProductSearchService;
import com.aurora.admin.service.ProductService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private static final String STATUS_ON_SALE = "ON_SALE";
    private static final String STATUS_OFF_SHELF = "OFF_SHELF";

    private final ProductMapper productMapper;
    private final ProductSkuMapper productSkuMapper;
    private final ProductSearchService productSearchService;

    @Override
    public PageResult<ProductResponse> getPage(ProductQuery query) {
        int page = query.getPage();
        int size = query.getSize();
        int offset = (page - 1) * size;

        long total = productMapper.countFiltered(query.keyword(), query.categoryId(), query.status());
        if (total == 0) {
            return PageResult.of(Collections.emptyList(), 0, page, size);
        }

        List<Product> products = productMapper.findPageWithFilter(offset, size,
                query.keyword(), query.categoryId(), query.status());

        List<ProductResponse> records = products.stream()
                .map(this::toSimpleResponse)
                .toList();

        return PageResult.of(records, total, page, size);
    }

    @Override
    public ProductResponse getById(Long id) {
        Product product = productMapper.findById(id);
        if (product == null) {
            throw new NotFoundException("商品", id);
        }
        List<ProductSku> skus = productSkuMapper.findByProductId(id);
        return toDetailResponse(product, skus);
    }

    @Override
    @Transactional
    public ProductResponse create(ProductRequest request) {
        Product product = new Product();
        setProductFields(product, request);
        product.setStatus(request.status() != null ? request.status() : STATUS_ON_SALE);
        product.setCreateTime(LocalDateTime.now());

        productMapper.insert(product);

        List<ProductSku> skus = insertSkus(product.getId(), request.skus());
        // 有 SKU 时，商品库存 = 各 SKU 库存之和（覆盖传入的 stock）
        if (skus != null && !skus.isEmpty()) {
            int totalStock = skus.stream().mapToInt(ProductSku::getStock).sum();
            productMapper.updateStockById(product.getId(), totalStock);
            product.setStock(totalStock);
        } else {
            if (product.getStock() == null) {
                product.setStock(0);
            }
        }

        syncToEs(product.getId());

        return toDetailResponse(product, skus);
    }

    @Override
    @Transactional
    public ProductResponse update(Long id, ProductRequest request) {
        Product product = productMapper.findById(id);
        if (product == null) {
            throw new NotFoundException("商品", id);
        }
        if (STATUS_ON_SALE.equals(product.getStatus())) {
            throw new BusinessException("上架商品不能编辑，请先下架");
        }

        setProductFields(product, request);
        if (request.status() != null) {
            product.setStatus(request.status());
        }

        productMapper.update(product);

        // 删旧 SKU 再插新 SKU
        productSkuMapper.deleteByProductId(id);
        List<ProductSku> skus = insertSkus(product.getId(), request.skus());

        // 有 SKU 时，商品库存 = 各 SKU 库存之和（覆盖传入的 stock）
        if (skus != null && !skus.isEmpty()) {
            int totalStock = skus.stream().mapToInt(ProductSku::getStock).sum();
            productMapper.updateStockById(id, totalStock);
            product.setStock(totalStock);
        } else {
            if (product.getStock() == null) {
                product.setStock(0);
            }
        }

        syncToEs(id);

        return toDetailResponse(product, skus);
    }

    @Override
    @Transactional
    public void updateStatus(Long id, String status) {
        Product product = productMapper.findById(id);
        if (product == null) {
            throw new NotFoundException("商品", id);
        }
        if (!STATUS_ON_SALE.equals(status) && !STATUS_OFF_SHELF.equals(status)) {
            throw new IllegalArgumentException("无效的商品状态: " + status);
        }
        productMapper.updateStatus(id, status);

        // 上架时同步到 ES，下架时从 ES 删除
        if (STATUS_ON_SALE.equals(status)) {
            syncToEs(id);
        } else {
            syncDeleteFromEs(id);
        }
    }

    @Override
    @Transactional
    public void batchUpdateStatus(java.util.List<Long> ids, String status) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        if (!STATUS_ON_SALE.equals(status) && !STATUS_OFF_SHELF.equals(status)) {
            throw new IllegalArgumentException("无效的商品状态: " + status);
        }
        productMapper.batchUpdateStatus(ids, status);
        
        // 批量处理 ES 同步
        for (Long id : ids) {
            if (STATUS_ON_SALE.equals(status)) {
                syncToEs(id);
            } else {
                syncDeleteFromEs(id);
            }
        }
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Product product = productMapper.findById(id);
        if (product == null) {
            throw new NotFoundException("商品", id);
        }
        if (STATUS_ON_SALE.equals(product.getStatus())) {
            throw new BusinessException("上架商品不能删除，请先下架");
        }
        productMapper.deleteById(id);

        syncDeleteFromEs(id);
    }

    private void syncToEs(Long productId) {
        try {
            productSearchService.indexProduct(productId);
        } catch (Exception e) {
            log.error("ES索引同步失败: productId={}", productId, e);
        }
    }

    private void syncDeleteFromEs(Long productId) {
        try {
            productSearchService.delete(productId);
        } catch (Exception e) {
            log.error("ES索引删除失败: productId={}", productId, e);
        }
    }

    private void setProductFields(Product product, ProductRequest request) {
        product.setCategoryId(request.categoryId());
        product.setName(request.name());
        product.setDescription(request.description());
        product.setCoverImage(request.coverImage());
        product.setPrice(request.price());
        product.setStock(request.stock() != null ? request.stock() : 0);
    }

    private List<ProductSku> insertSkus(Long productId, List<ProductSkuRequest> skuRequests) {
        if (skuRequests == null || skuRequests.isEmpty()) {
            return Collections.emptyList();
        }
        return skuRequests.stream().map(skuReq -> {
            ProductSku sku = new ProductSku();
            sku.setProductId(productId);
            sku.setSpecName(skuReq.specName());
            sku.setSpecCode(skuReq.specCode());
            sku.setPrice(skuReq.price());
            sku.setStock(skuReq.stock() != null ? skuReq.stock() : 0);
            productSkuMapper.insert(sku);
            return sku;
        }).toList();
    }

    private ProductResponse toSimpleResponse(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getCategoryId(),
                product.getCategoryName(),
                product.getName(),
                product.getDescription(),
                product.getCoverImage(),
                product.getPrice(),
                product.getStock(),
                product.getStatus(),
                product.getCreateTime(),
                product.getUpdateTime(),
                Collections.emptyList()
        );
    }

    private ProductResponse toDetailResponse(Product product, List<ProductSku> skus) {
        return new ProductResponse(
                product.getId(),
                product.getCategoryId(),
                product.getCategoryName(),
                product.getName(),
                product.getDescription(),
                product.getCoverImage(),
                product.getPrice(),
                product.getStock(),
                product.getStatus(),
                product.getCreateTime(),
                product.getUpdateTime(),
                skus != null ? skus : Collections.emptyList()
        );
    }
}
