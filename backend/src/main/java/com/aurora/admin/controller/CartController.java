package com.aurora.admin.controller;

import com.aurora.admin.annotation.RateLimit;
import com.aurora.admin.annotation.RateLimit.KeyType;
import com.aurora.admin.dto.AddToCartRequest;
import com.aurora.admin.dto.ApiResponse;
import com.aurora.admin.dto.CartItemResponse;
import com.aurora.admin.dto.UpdateCartQuantityRequest;
import com.aurora.admin.service.ShoppingCartService;
import com.aurora.admin.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 购物车控制器。提供购物车商品列表查询、添加商品、修改数量、切换 SKU、删除单品及清空购物车接口。
 * 所有接口需要登录态（isAuthenticated()），添加购物车接口额外受限流保护（10次/分钟）。
 */
@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class CartController {

    private final ShoppingCartService shoppingCartService;

    /**
     * 获取当前用户的购物车列表。返回购物车内所有商品项及 SKU 信息。
     */
    @GetMapping
    public ApiResponse getCart() {
        Long userId = SecurityUtils.getCurrentUserId();
        List<CartItemResponse> cart = shoppingCartService.getCart(userId);
        return ApiResponse.success(cart);
    }

    /**
     * 添加商品到购物车。限流 10次/分钟。已存在的商品 SKU 会自动增加数量。
     *
     * @param request 添加购物车请求体（包含商品 ID、SKU ID、数量）
     */
    @RateLimit(key = KeyType.USER, limit = 10, duration = 60)
    @PostMapping
    public ApiResponse addToCart(@Valid @RequestBody AddToCartRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        shoppingCartService.addToCart(userId, request);
        return ApiResponse.success("添加成功");
    }

    /**
     * 修改购物车中指定商品项的数量。
     *
     * @param id      购物车商品项 ID
     * @param request 更新数量请求体（包含新数量）
     */
    @PutMapping("/{id}")
    public ApiResponse updateQuantity(@PathVariable Long id, @Valid @RequestBody UpdateCartQuantityRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        shoppingCartService.updateQuantity(userId, id, request.quantity());
        return ApiResponse.success("更新成功");
    }

    /**
     * 切换购物车中指定商品项的 SKU（如更换规格）。
     *
     * @param id   购物车商品项 ID
     * @param body 请求体，key 为 "skuId"，value 为目标 SKU ID
     */
    @PatchMapping("/{id}/sku")
    public ApiResponse switchSku(@PathVariable Long id, @RequestBody Map<String, Long> body) {
        Long userId = SecurityUtils.getCurrentUserId();
        Long skuId = body.get("skuId");
        shoppingCartService.switchSku(userId, id, skuId);
        return ApiResponse.success("切换成功");
    }

    /**
     * 从购物车中移除指定商品项。
     *
     * @param id 购物车商品项 ID
     */
    @DeleteMapping("/{id}")
    public ApiResponse removeFromCart(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        shoppingCartService.removeFromCart(userId, id);
        return ApiResponse.success("删除成功");
    }

    /**
     * 清空当前用户的整个购物车。
     */
    @DeleteMapping
    public ApiResponse clearCart() {
        Long userId = SecurityUtils.getCurrentUserId();
        shoppingCartService.clearCart(userId);
        return ApiResponse.success("已清空");
    }
}
