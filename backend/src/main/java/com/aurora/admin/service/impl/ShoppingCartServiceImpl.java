package com.aurora.admin.service.impl;

import com.aurora.admin.dto.AddToCartRequest;
import com.aurora.admin.dto.CartItemResponse;
import com.aurora.admin.dto.SkuOption;
import com.aurora.admin.entity.Product;
import com.aurora.admin.entity.ProductSku;
import com.aurora.admin.entity.ShoppingCart;
import com.aurora.admin.exception.BusinessException;
import com.aurora.admin.exception.NotFoundException;
import com.aurora.admin.mapper.ProductMapper;
import com.aurora.admin.mapper.ProductSkuMapper;
import com.aurora.admin.mapper.ShoppingCartMapper;
import com.aurora.admin.service.ShoppingCartService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 购物车服务实现。提供购物车增删改查、SKU 切换及合并逻辑。
 * 操作前校验商品状态（下架/库存不足禁止加购）、校验购物车记录归属权。
 * SKU 切换时自动合并同一商品+新 SKU 的已有记录数量，避免重复。
 */
@Service
@RequiredArgsConstructor
public class ShoppingCartServiceImpl implements ShoppingCartService {

    private final ShoppingCartMapper shoppingCartMapper;
    private final ProductMapper productMapper;
    private final ProductSkuMapper productSkuMapper;

    @Override
    public List<CartItemResponse> getCart(Long userId) {
        List<ShoppingCart> cartItems = shoppingCartMapper.findByUserId(userId);
        if (cartItems.isEmpty()) {
            return Collections.emptyList();
        }

        // 批量预加载 Product
        Set<Long> productIds = cartItems.stream().map(ShoppingCart::getProductId).collect(Collectors.toSet());
        Map<Long, Product> productMap = productIds.isEmpty()
                ? Collections.emptyMap()
                : productMapper.findByIds(productIds).stream()
                        .collect(Collectors.toMap(Product::getId, Function.identity()));

        // 批量预加载 SKU
        Map<Long, List<ProductSku>> skusByProductId = productIds.isEmpty()
                ? Collections.emptyMap()
                : productSkuMapper.findByProductIds(productIds).stream()
                        .collect(Collectors.groupingBy(ProductSku::getProductId));

        return cartItems.stream()
                .map(cart -> toCartItemResponse(cart, productMap, skusByProductId))
                .toList();
    }

    @Override
    @Transactional
    public void addToCart(Long userId, AddToCartRequest request) {
        Product product = productMapper.findById(request.productId());
        if (product == null) {
            throw new NotFoundException("商品", request.productId());
        }
        if (!"ON_SALE".equals(product.getStatus())) {
            throw new BusinessException("商品已下架，无法加购");
        }
        if (product.getStock() == null || product.getStock() <= 0) {
            throw new BusinessException("商品库存不足，无法加购");
        }

        // 未传 skuId 但商品有 SKU 时，默认选第一个规格
        Long actualSkuId = request.skuId();
        if (actualSkuId == null) {
            List<ProductSku> skus = productSkuMapper.findByProductId(product.getId());
            if (!skus.isEmpty()) {
                actualSkuId = skus.get(0).getId();
            }
        }

        ShoppingCart existing = shoppingCartMapper.findByUserAndProduct(userId, request.productId(), actualSkuId);
        if (existing != null) {
            int newQuantity = existing.getQuantity() + request.quantity();
            shoppingCartMapper.updateQuantity(existing.getId(), newQuantity);
            return;
        }

        ShoppingCart cart = new ShoppingCart();
        cart.setUserId(userId);
        cart.setProductId(request.productId());
        cart.setSkuId(actualSkuId);
        cart.setQuantity(request.quantity());
        shoppingCartMapper.insert(cart);
    }

    @Override
    @Transactional
    public void updateQuantity(Long userId, Long cartId, Integer quantity) {
        ShoppingCart cart = shoppingCartMapper.findById(cartId);
        if (cart == null || !cart.getUserId().equals(userId)) {
            throw new NotFoundException("购物车记录", cartId);
        }
        shoppingCartMapper.updateQuantity(cartId, quantity);
    }

    @Override
    @Transactional
    public void switchSku(Long userId, Long cartId, Long skuId) {
        ShoppingCart cart = shoppingCartMapper.findById(cartId);
        if (cart == null || !cart.getUserId().equals(userId)) {
            throw new NotFoundException("购物车记录", cartId);
        }

        // 检查同一商品+新 SKU 是否已在购物车中，合并数量
        ShoppingCart duplicate = shoppingCartMapper.findByUserAndProduct(userId, cart.getProductId(), skuId);
        if (duplicate != null && !duplicate.getId().equals(cartId)) {
            int mergedQty = duplicate.getQuantity() + cart.getQuantity();
            shoppingCartMapper.updateQuantity(duplicate.getId(), mergedQty);
            shoppingCartMapper.deleteById(cartId);
            return;
        }

        shoppingCartMapper.updateSku(cartId, skuId);
    }

    @Override
    @Transactional
    public void removeFromCart(Long userId, Long cartId) {
        ShoppingCart cart = shoppingCartMapper.findById(cartId);
        if (cart == null || !cart.getUserId().equals(userId)) {
            throw new NotFoundException("购物车记录", cartId);
        }
        shoppingCartMapper.deleteById(cartId);
    }

    @Override
    @Transactional
    public void clearCart(Long userId) {
        shoppingCartMapper.deleteByUserId(userId);
    }

    private CartItemResponse toCartItemResponse(ShoppingCart cart,
                                                   Map<Long, Product> productMap,
                                                   Map<Long, List<ProductSku>> skusByProductId) {
        Product product = productMap.get(cart.getProductId());
        if (product == null) {
            throw new BusinessException("购物车商品不存在: " + cart.getProductId());
        }

        String specName = "";
        BigDecimal price = product.getPrice();
        List<SkuOption> availableSkus = Collections.emptyList();

        List<ProductSku> skus = skusByProductId.getOrDefault(cart.getProductId(), Collections.emptyList());
        if (!skus.isEmpty()) {
            availableSkus = skus.stream()
                    .map(s -> new SkuOption(s.getId(), s.getSpecName(), s.getPrice(), s.getStock()))
                    .toList();
        }

        if (cart.getSkuId() != null) {
            ProductSku matched = skus.stream()
                    .filter(s -> s.getId().equals(cart.getSkuId()))
                    .findFirst()
                    .orElse(null);
            if (matched != null) {
                specName = matched.getSpecName();
                price = matched.getPrice() != null ? matched.getPrice() : price;
            }
        }

        return new CartItemResponse(
                cart.getId(),
                cart.getProductId(),
                cart.getSkuId(),
                product.getName(),
                product.getCoverImage(),
                price,
                product.getStock(),
                specName,
                cart.getQuantity(),
                product.getStatus(),
                availableSkus
        );
    }
}
