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

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class CartController {

    private final ShoppingCartService shoppingCartService;

    @GetMapping
    public ApiResponse getCart() {
        Long userId = SecurityUtils.getCurrentUserId();
        List<CartItemResponse> cart = shoppingCartService.getCart(userId);
        return ApiResponse.success(cart);
    }

    @RateLimit(key = KeyType.USER, limit = 10, duration = 60)
    @PostMapping
    public ApiResponse addToCart(@Valid @RequestBody AddToCartRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        shoppingCartService.addToCart(userId, request);
        return ApiResponse.success("添加成功");
    }

    @PutMapping("/{id}")
    public ApiResponse updateQuantity(@PathVariable Long id, @Valid @RequestBody UpdateCartQuantityRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        shoppingCartService.updateQuantity(userId, id, request.quantity());
        return ApiResponse.success("更新成功");
    }

    @PatchMapping("/{id}/sku")
    public ApiResponse switchSku(@PathVariable Long id, @RequestBody Map<String, Long> body) {
        Long userId = SecurityUtils.getCurrentUserId();
        Long skuId = body.get("skuId");
        shoppingCartService.switchSku(userId, id, skuId);
        return ApiResponse.success("切换成功");
    }

    @DeleteMapping("/{id}")
    public ApiResponse removeFromCart(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        shoppingCartService.removeFromCart(userId, id);
        return ApiResponse.success("删除成功");
    }

    @DeleteMapping
    public ApiResponse clearCart() {
        Long userId = SecurityUtils.getCurrentUserId();
        shoppingCartService.clearCart(userId);
        return ApiResponse.success("已清空");
    }
}
