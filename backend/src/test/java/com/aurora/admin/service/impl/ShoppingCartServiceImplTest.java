package com.aurora.admin.service.impl;

import com.aurora.admin.dto.AddToCartRequest;
import com.aurora.admin.dto.CartItemResponse;
import com.aurora.admin.entity.Product;
import com.aurora.admin.entity.ProductSku;
import com.aurora.admin.entity.ShoppingCart;
import com.aurora.admin.exception.BusinessException;
import com.aurora.admin.exception.NotFoundException;
import com.aurora.admin.mapper.ProductMapper;
import com.aurora.admin.mapper.ProductSkuMapper;
import com.aurora.admin.mapper.ShoppingCartMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ShoppingCartService 单元测试")
class ShoppingCartServiceImplTest {

    @Mock private ShoppingCartMapper shoppingCartMapper;
    @Mock private ProductMapper productMapper;
    @Mock private ProductSkuMapper productSkuMapper;

    @InjectMocks
    private ShoppingCartServiceImpl cartService;

    private Product onSaleProduct;

    @BeforeEach
    void setUp() {
        onSaleProduct = new Product();
        onSaleProduct.setId(1L);
        onSaleProduct.setName("测试商品");
        onSaleProduct.setPrice(new BigDecimal("99.00"));
        onSaleProduct.setStock(50);
        onSaleProduct.setStatus("ON_SALE");
        onSaleProduct.setCoverImage("cover.jpg");
    }

    // ==================== addToCart ====================

    @Nested
    @DisplayName("加入购物车")
    class AddToCart {

        @Test
        @DisplayName("新商品加购成功")
        void addToCart_newItem() {
            when(productMapper.findById(1L)).thenReturn(onSaleProduct);
            when(productSkuMapper.findByProductId(1L)).thenReturn(Collections.emptyList());
            when(shoppingCartMapper.findByUserAndProduct(1L, 1L, null)).thenReturn(null);

            cartService.addToCart(1L, new AddToCartRequest(1L, null, 2));

            verify(shoppingCartMapper).insert(argThat(cart ->
                    cart.getUserId().equals(1L) &&
                    cart.getProductId().equals(1L) &&
                    cart.getQuantity() == 2
            ));
        }

        @Test
        @DisplayName("已存在相同商品+SKU，合并数量")
        void addToCart_mergeQuantity() {
            ShoppingCart existing = new ShoppingCart();
            existing.setId(10L);
            existing.setUserId(1L);
            existing.setProductId(1L);
            existing.setSkuId(null);
            existing.setQuantity(3);

            when(productMapper.findById(1L)).thenReturn(onSaleProduct);
            when(productSkuMapper.findByProductId(1L)).thenReturn(Collections.emptyList());
            when(shoppingCartMapper.findByUserAndProduct(1L, 1L, null)).thenReturn(existing);

            cartService.addToCart(1L, new AddToCartRequest(1L, null, 2));

            verify(shoppingCartMapper).updateQuantity(10L, 5); // 3 + 2
            verify(shoppingCartMapper, never()).insert(any());
        }

        @Test
        @DisplayName("商品不存在抛 NotFoundException")
        void addToCart_productNotFound() {
            when(productMapper.findById(999L)).thenReturn(null);

            assertThatThrownBy(() -> cartService.addToCart(1L, new AddToCartRequest(999L, null, 1)))
                    .isInstanceOf(NotFoundException.class);
        }

        @Test
        @DisplayName("商品已下架抛 BusinessException")
        void addToCart_productOffSale() {
            onSaleProduct.setStatus("OFF_SHELF");
            when(productMapper.findById(1L)).thenReturn(onSaleProduct);

            assertThatThrownBy(() -> cartService.addToCart(1L, new AddToCartRequest(1L, null, 1)))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("已下架");
        }

        @Test
        @DisplayName("商品库存为 0 抛 BusinessException")
        void addToCart_outOfStock() {
            onSaleProduct.setStock(0);
            when(productMapper.findById(1L)).thenReturn(onSaleProduct);

            assertThatThrownBy(() -> cartService.addToCart(1L, new AddToCartRequest(1L, null, 1)))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("库存不足");
        }

        @Test
        @DisplayName("未传 skuId 时自动选第一个 SKU")
        void addToCart_autoSelectFirstSku() {
            ProductSku sku = new ProductSku();
            sku.setId(5L);
            sku.setSpecName("红色");

            when(productMapper.findById(1L)).thenReturn(onSaleProduct);
            when(productSkuMapper.findByProductId(1L)).thenReturn(List.of(sku));
            when(shoppingCartMapper.findByUserAndProduct(1L, 1L, 5L)).thenReturn(null);

            cartService.addToCart(1L, new AddToCartRequest(1L, null, 1));

            verify(shoppingCartMapper).insert(argThat(cart -> cart.getSkuId().equals(5L)));
        }
    }

    // ==================== updateQuantity ====================

    @Nested
    @DisplayName("修改数量")
    class UpdateQuantity {

        @Test
        @DisplayName("正常修改数量")
        void updateQuantity_success() {
            ShoppingCart cart = new ShoppingCart();
            cart.setId(10L);
            cart.setUserId(1L);

            when(shoppingCartMapper.findById(10L)).thenReturn(cart);

            cartService.updateQuantity(1L, 10L, 5);

            verify(shoppingCartMapper).updateQuantity(10L, 5);
        }

        @Test
        @DisplayName("非本人购物车抛 NotFoundException")
        void updateQuantity_notOwner() {
            ShoppingCart cart = new ShoppingCart();
            cart.setId(10L);
            cart.setUserId(2L);

            when(shoppingCartMapper.findById(10L)).thenReturn(cart);

            assertThatThrownBy(() -> cartService.updateQuantity(1L, 10L, 5))
                    .isInstanceOf(NotFoundException.class);
        }
    }

    // ==================== switchSku ====================

    @Nested
    @DisplayName("切换 SKU")
    class SwitchSku {

        @Test
        @DisplayName("切换 SKU 成功（无重复）")
        void switchSku_noConflict() {
            ShoppingCart cart = new ShoppingCart();
            cart.setId(10L);
            cart.setUserId(1L);
            cart.setProductId(1L);
            cart.setSkuId(5L);
            cart.setQuantity(2);

            when(shoppingCartMapper.findById(10L)).thenReturn(cart);
            when(shoppingCartMapper.findByUserAndProduct(1L, 1L, 6L)).thenReturn(null);

            cartService.switchSku(1L, 10L, 6L);

            verify(shoppingCartMapper).updateSku(10L, 6L);
        }

        @Test
        @DisplayName("切换 SKU 时已有相同商品+SKU，合并后删除旧记录")
        void switchSku_mergeAndDelete() {
            ShoppingCart cart = new ShoppingCart();
            cart.setId(10L);
            cart.setUserId(1L);
            cart.setProductId(1L);
            cart.setSkuId(5L);
            cart.setQuantity(2);

            ShoppingCart duplicate = new ShoppingCart();
            duplicate.setId(20L);
            duplicate.setUserId(1L);
            duplicate.setProductId(1L);
            duplicate.setSkuId(6L);
            duplicate.setQuantity(3);

            when(shoppingCartMapper.findById(10L)).thenReturn(cart);
            when(shoppingCartMapper.findByUserAndProduct(1L, 1L, 6L)).thenReturn(duplicate);

            cartService.switchSku(1L, 10L, 6L);

            verify(shoppingCartMapper).updateQuantity(20L, 5); // 3 + 2
            verify(shoppingCartMapper).deleteById(10L);
            verify(shoppingCartMapper, never()).updateSku(anyLong(), anyLong());
        }
    }

    // ==================== removeFromCart ====================

    @Nested
    @DisplayName("移除购物车")
    class RemoveFromCart {

        @Test
        @DisplayName("正常移除")
        void remove_success() {
            ShoppingCart cart = new ShoppingCart();
            cart.setId(10L);
            cart.setUserId(1L);

            when(shoppingCartMapper.findById(10L)).thenReturn(cart);

            cartService.removeFromCart(1L, 10L);

            verify(shoppingCartMapper).deleteById(10L);
        }

        @Test
        @DisplayName("不存在抛 NotFoundException")
        void remove_notFound() {
            when(shoppingCartMapper.findById(999L)).thenReturn(null);

            assertThatThrownBy(() -> cartService.removeFromCart(1L, 999L))
                    .isInstanceOf(NotFoundException.class);
        }
    }

    // ==================== clearCart ====================

    @Test
    @DisplayName("清空购物车")
    void clearCart_success() {
        cartService.clearCart(1L);

        verify(shoppingCartMapper).deleteByUserId(1L);
    }

    // ==================== getCart ====================

    @Test
    @DisplayName("获取购物车列表")
    void getCart_success() {
        ShoppingCart cart = new ShoppingCart();
        cart.setId(10L);
        cart.setUserId(1L);
        cart.setProductId(1L);
        cart.setSkuId(null);
        cart.setQuantity(2);

        when(shoppingCartMapper.findByUserId(1L)).thenReturn(List.of(cart));
        when(productMapper.findByIds(org.mockito.ArgumentMatchers.<Set<Long>>any())).thenReturn(List.of(onSaleProduct));
        when(productSkuMapper.findByProductIds(org.mockito.ArgumentMatchers.<Set<Long>>any())).thenReturn(Collections.emptyList());

        List<CartItemResponse> items = cartService.getCart(1L);

        assertThat(items).hasSize(1);
        assertThat(items.get(0).productName()).isEqualTo("测试商品");
        assertThat(items.get(0).quantity()).isEqualTo(2);
    }
}
