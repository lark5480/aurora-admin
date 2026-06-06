package com.aurora.admin.service;

import com.aurora.admin.dto.AddToCartRequest;
import com.aurora.admin.dto.CartItemResponse;

import java.util.List;

public interface ShoppingCartService {

    /**
     * 获取用户购物车列表
     */
    List<CartItemResponse> getCart(Long userId);

    /**
     * 添加商品到购物车（已存在则增加数量）
     */
    void addToCart(Long userId, AddToCartRequest request);

    /**
     * 更新购物车商品数量
     */
    void updateQuantity(Long userId, Long cartId, Integer quantity);

    /**
     * 切换购物车商品的 SKU 规格
     */
    void switchSku(Long userId, Long cartId, Long skuId);

    /**
     * 从购物车删除商品
     */
    void removeFromCart(Long userId, Long cartId);

    /**
     * 清空购物车
     */
    void clearCart(Long userId);
}
