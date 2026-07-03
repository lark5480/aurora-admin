package com.aurora.admin.service;

import com.aurora.admin.dto.AddToCartRequest;
import com.aurora.admin.dto.CartItemResponse;

import java.util.List;

public interface ShoppingCartService {

    /**
     * 获取用户购物车列表。返回当前用户购物车中所有商品项，含商品信息、SKU 规格和数量。
     *
     * @param userId 用户 ID
     * @return 购物车商品项列表
     */
    List<CartItemResponse> getCart(Long userId);

    /**
     * 添加商品到购物车。若该用户购物车中已有相同 SKU 的商品，则增加数量而非重复添加。
     *
     * @param userId  用户 ID
     * @param request 添加请求（含商品 ID、SKU ID、数量）
     */
    void addToCart(Long userId, AddToCartRequest request);

    /**
     * 更新购物车商品数量。校验该商品项属于当前用户后执行更新。
     *
     * @param userId   用户 ID
     * @param cartId   购物车商品项 ID
     * @param quantity 新数量
     */
    void updateQuantity(Long userId, Long cartId, Integer quantity);

    /**
     * 切换购物车商品的 SKU 规格。将指定购物车项更换为新的 SKU（如颜色、尺寸等规格变更）。
     *
     * @param userId 用户 ID
     * @param cartId 购物车商品项 ID
     * @param skuId  目标 SKU ID
     */
    void switchSku(Long userId, Long cartId, Long skuId);

    /**
     * 从购物车删除商品。校验该商品项属于当前用户后执行物理删除。
     *
     * @param userId 用户 ID
     * @param cartId 购物车商品项 ID
     */
    void removeFromCart(Long userId, Long cartId);

    /**
     * 清空购物车。删除当前用户购物车中的所有商品项。
     *
     * @param userId 用户 ID
     */
    void clearCart(Long userId);
}
