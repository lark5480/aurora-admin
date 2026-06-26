package com.aurora.admin.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductService 单元测试")
class ProductServiceImplTest {
    
    @Mock private ProductMapper productMapper;
    @Mock private ProductSkuMapper productSkuMapper;
    @Mock private ProductSearchService productSearchService;

    @InjectMocks
    private ProductServiceImpl productService;

    private Product offShelfProduct;
    private Product onSaleProduct;

    @BeforeEach
    void setUp() {
        offShelfProduct = new Product();
        offShelfProduct.setId(1L);
        offShelfProduct.setCategoryId(10L);
        offShelfProduct.setName("下架商品");
        offShelfProduct.setDescription("描述");
        offShelfProduct.setCoverImage("cover.jpg");
        offShelfProduct.setPrice(new BigDecimal("99.00"));
        offShelfProduct.setStock(50);
        offShelfProduct.setStatus("OFF_SHELF");

        onSaleProduct = new Product();
        onSaleProduct.setId(2L);
        onSaleProduct.setCategoryId(10L);
        onSaleProduct.setName("上架商品");
        onSaleProduct.setDescription("描述");
        onSaleProduct.setCoverImage("cover.jpg");
        onSaleProduct.setPrice(new BigDecimal("199.00"));
        onSaleProduct.setStock(100);
        onSaleProduct.setStatus("ON_SALE");
    }

    // ==================== create ====================

    @Nested
    @DisplayName("创建商品")
    class Create {

        @Test
        @DisplayName("无 SKU 创建商品成功")
        void create_noSku_success() {
            ProductRequest request = new ProductRequest(
                    "新商品", 10L, "描述", "cover.jpg",
                    new BigDecimal("50.00"), 20, "ON_SALE", null);

            when(productMapper.insert(any(Product.class))).thenAnswer(inv -> {
                Product p = inv.getArgument(0);
                p.setId(1L);
                return 1;
            });

            ProductResponse response = productService.create(request);

            assertThat(response.name()).isEqualTo("新商品");
            assertThat(response.stock()).isEqualTo(20);
            verify(productMapper).insert(any(Product.class));
            verify(productSearchService).indexProduct(1L);
        }

        @Test
        @DisplayName("有 SKU 时库存为 SKU 库存之和")
        void create_withSkus_stockSummed() {
            List<ProductSkuRequest> skuRequests = List.of(
                    new ProductSkuRequest("红色", "RED", new BigDecimal("50.00"), 10),
                    new ProductSkuRequest("蓝色", "BLUE", new BigDecimal("60.00"), 20)
            );
            ProductRequest request = new ProductRequest(
                    "SKU商品", 10L, "描述", "cover.jpg",
                    new BigDecimal("50.00"), 0, "ON_SALE", skuRequests);

            when(productMapper.insert(any(Product.class))).thenAnswer(inv -> {
                Product p = inv.getArgument(0);
                p.setId(2L);
                return 1;
            });
            when(productSkuMapper.insert(any(ProductSku.class))).thenReturn(1);

            ProductResponse response = productService.create(request);

            assertThat(response.stock()).isEqualTo(30); // 10 + 20
            verify(productMapper).updateStockById(2L, 30);
            verify(productSkuMapper, times(2)).insert(any(ProductSku.class));
        }

        @Test
        @DisplayName("默认状态为 ON_SALE")
        void create_defaultStatusOnSale() {
            ProductRequest request = new ProductRequest(
                    "商品", 10L, "描述", null,
                    new BigDecimal("10.00"), 5, null, null);

            when(productMapper.insert(any(Product.class))).thenAnswer(inv -> {
                Product p = inv.getArgument(0);
                p.setId(3L);
                return 1;
            });

            ProductResponse response = productService.create(request);

            verify(productMapper).insert(argThat(p -> "ON_SALE".equals(p.getStatus())));
        }
    }

    // ==================== update ====================

    @Nested
    @DisplayName("更新商品")
    class Update {

        @Test
        @DisplayName("下架商品可以编辑")
        void update_offShelf_success() {
            ProductRequest request = new ProductRequest(
                    "更新名称", 10L, "新描述", "new.jpg",
                    new BigDecimal("88.00"), 30, "OFF_SHELF", null);

            when(productMapper.findById(1L)).thenReturn(offShelfProduct);

            ProductResponse response = productService.update(1L, request);

            assertThat(response.name()).isEqualTo("更新名称");
            verify(productMapper).update(any(Product.class));
            verify(productSearchService).indexProduct(1L);
        }

        @Test
        @DisplayName("上架商品不能编辑")
        void update_onSale_rejected() {
            when(productMapper.findById(2L)).thenReturn(onSaleProduct);
            ProductRequest request = new ProductRequest(
                    "新名称", 10L, "描述", null,
                    new BigDecimal("99.00"), 10, null, null);

            assertThatThrownBy(() -> productService.update(2L, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("上架商品不能编辑");
        }

        @Test
        @DisplayName("商品不存在抛 NotFoundException")
        void update_notFound() {
            when(productMapper.findById(999L)).thenReturn(null);
            ProductRequest request = new ProductRequest(
                    "名称", 1L, "", null,
                    new BigDecimal("1.00"), 0, null, null);

            assertThatThrownBy(() -> productService.update(999L, request))
                    .isInstanceOf(NotFoundException.class);
        }

        @Test
        @DisplayName("更新时删除旧 SKU 并插入新 SKU")
        void update_replaceSkus() {
            List<ProductSkuRequest> newSkus = List.of(
                    new ProductSkuRequest("大号", "LG", new BigDecimal("99.00"), 15)
            );
            ProductRequest request = new ProductRequest(
                    "更新", 10L, "描述", null,
                    new BigDecimal("99.00"), 15, null, newSkus);

            when(productMapper.findById(1L)).thenReturn(offShelfProduct);
            when(productSkuMapper.insert(any(ProductSku.class))).thenReturn(1);

            productService.update(1L, request);

            verify(productSkuMapper).deleteByProductId(1L);
            verify(productSkuMapper).insert(any(ProductSku.class));
            verify(productMapper).updateStockById(1L, 15);
        }
    }

    // ==================== updateStatus ====================

    @Nested
    @DisplayName("上下架")
    class UpdateStatus {

        @Test
        @DisplayName("上架成功并同步 ES")
        void updateStatus_onSale() {
            when(productMapper.findById(1L)).thenReturn(offShelfProduct);

            productService.updateStatus(1L, "ON_SALE");

            verify(productMapper).updateStatus(1L, "ON_SALE");
            verify(productSearchService).indexProduct(1L);
        }

        @Test
        @DisplayName("下架成功并从 ES 删除")
        void updateStatus_offShelf() {
            when(productMapper.findById(2L)).thenReturn(onSaleProduct);

            productService.updateStatus(2L, "OFF_SHELF");

            verify(productMapper).updateStatus(2L, "OFF_SHELF");
            verify(productSearchService).delete(2L);
        }

        @Test
        @DisplayName("无效状态抛 IllegalArgumentException")
        void updateStatus_invalidStatus() {
            when(productMapper.findById(1L)).thenReturn(offShelfProduct);

            assertThatThrownBy(() -> productService.updateStatus(1L, "INVALID"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("无效的商品状态");
        }

        @Test
        @DisplayName("商品不存在抛 NotFoundException")
        void updateStatus_notFound() {
            when(productMapper.findById(999L)).thenReturn(null);

            assertThatThrownBy(() -> productService.updateStatus(999L, "ON_SALE"))
                    .isInstanceOf(NotFoundException.class);
        }
    }

    // ==================== delete ====================

    @Nested
    @DisplayName("删除商品")
    class Delete {

        @Test
        @DisplayName("下架商品可以删除")
        void delete_offShelf_success() {
            when(productMapper.findById(1L)).thenReturn(offShelfProduct);

            productService.delete(1L);

            verify(productMapper).deleteById(1L);
            verify(productSearchService).delete(1L);
        }

        @Test
        @DisplayName("上架商品不能删除")
        void delete_onSale_rejected() {
            when(productMapper.findById(2L)).thenReturn(onSaleProduct);

            assertThatThrownBy(() -> productService.delete(2L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("上架商品不能删除");

            verify(productMapper, never()).deleteById(anyLong());
        }

        @Test
        @DisplayName("商品不存在抛 NotFoundException")
        void delete_notFound() {
            when(productMapper.findById(999L)).thenReturn(null);

            assertThatThrownBy(() -> productService.delete(999L))
                    .isInstanceOf(NotFoundException.class);
        }
    }

    // ==================== batchUpdateStatus ====================

    @Nested
    @DisplayName("批量上下架")
    class BatchUpdateStatus {

        @Test
        @DisplayName("批量上架成功")
        void batchUpdate_onSale() {
            productService.batchUpdateStatus(List.of(1L, 2L, 3L), "ON_SALE");

            verify(productMapper).batchUpdateStatus(List.of(1L, 2L, 3L), "ON_SALE");
            verify(productSearchService, times(3)).indexProduct(anyLong());
        }

        @Test
        @DisplayName("批量下架并删除 ES 索引")
        void batchUpdate_offShelf() {
            productService.batchUpdateStatus(List.of(1L, 2L), "OFF_SHELF");

            verify(productMapper).batchUpdateStatus(List.of(1L, 2L), "OFF_SHELF");
            verify(productSearchService).delete(1L);
            verify(productSearchService).delete(2L);
        }

        @Test
        @DisplayName("无效状态抛异常")
        void batchUpdate_invalidStatus() {
            assertThatThrownBy(() -> productService.batchUpdateStatus(List.of(1L), "INVALID"))
                    .isInstanceOf(IllegalArgumentException.class);

            verify(productMapper, never()).batchUpdateStatus(anyList(), anyString());
        }

        @Test
        @DisplayName("空列表不执行任何操作")
        void batchUpdate_emptyList() {
            productService.batchUpdateStatus(Collections.emptyList(), "ON_SALE");
            productService.batchUpdateStatus(null, "ON_SALE");

            verify(productMapper, never()).batchUpdateStatus(anyList(), anyString());
        }
    }

    // ==================== getById ====================

    @Nested
    @DisplayName("商品详情")
    class GetById {

        @Test
        @DisplayName("获取详情含 SKU 列表")
        void getById_withSkus() {
            ProductSku sku = new ProductSku();
            sku.setId(1L);
            sku.setProductId(1L);
            sku.setSpecName("红色");

            when(productMapper.findById(1L)).thenReturn(offShelfProduct);
            when(productSkuMapper.findByProductId(1L)).thenReturn(List.of(sku));

            ProductResponse response = productService.getById(1L);

            assertThat(response.name()).isEqualTo("下架商品");
            assertThat(response.skus()).hasSize(1);
        }

        @Test
        @DisplayName("商品不存在抛 NotFoundException")
        void getById_notFound() {
            when(productMapper.findById(999L)).thenReturn(null);

            assertThatThrownBy(() -> productService.getById(999L))
                    .isInstanceOf(NotFoundException.class);
        }
    }

    // ==================== ES sync error handling ====================

    @Test
    @DisplayName("ES 同步失败不影响主流程")
    void esSync_failureDoesNotAffectMainFlow() {
        when(productMapper.findById(1L)).thenReturn(offShelfProduct);
        doThrow(new RuntimeException("ES connection error"))
                .when(productSearchService).indexProduct(1L);

        // 不应抛异常
        assertThatCode(() -> productService.updateStatus(1L, "ON_SALE"))
                .doesNotThrowAnyException();

        verify(productMapper).updateStatus(1L, "ON_SALE");
    }
}
