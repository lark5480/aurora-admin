package com.aurora.admin.controller;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.aurora.admin.annotation.RateLimit;
import com.aurora.admin.annotation.RateLimit.KeyType;
import com.aurora.admin.dto.ApiResponse;
import com.aurora.admin.dto.CreateOrderRequest;
import com.aurora.admin.dto.OrderQuery;
import com.aurora.admin.dto.OrderResponse;
import com.aurora.admin.dto.PageResult;
import com.aurora.admin.service.OrderService;
import com.aurora.admin.util.SecurityUtils;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class OrderController {

    private final OrderService orderService;

    @RateLimit(key = KeyType.USER, limit = 5, duration = 60)
    @PostMapping
    public ApiResponse createOrder(@Valid @RequestBody CreateOrderRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        OrderResponse order = orderService.createOrder(userId, request);
        return ApiResponse.success(order);
    }

    @GetMapping
    public ApiResponse getOrderPage(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) String username) {
        Long userId = SecurityUtils.isCurrentUserAdmin() ? null : SecurityUtils.getCurrentUserId();
        OrderQuery query = new OrderQuery(page, size, status, orderNo, username);
        PageResult<OrderResponse> result = orderService.getOrderPage(userId, query);
        return ApiResponse.success(result);
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportOrders(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) String username) {
        Long userId = SecurityUtils.isCurrentUserAdmin() ? null : SecurityUtils.getCurrentUserId();
        OrderQuery query = new OrderQuery(1, Integer.MAX_VALUE, status, orderNo, username);
        byte[] excel = orderService.exportOrders(userId, query);
        String filename = URLEncoder.encode("订单导出.xlsx", StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + filename)
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excel);
    }

    @GetMapping("/{id}")
    public ApiResponse getOrderDetail(@PathVariable Long id) {
        Long userId = SecurityUtils.isCurrentUserAdmin() ? null : SecurityUtils.getCurrentUserId();
        OrderResponse order = orderService.getOrderDetail(userId, id);
        return ApiResponse.success(order);
    }

    @RateLimit(key = KeyType.USER, limit = 10, duration = 60)
    @PatchMapping("/{id}/cancel")
    public ApiResponse cancelOrder(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        orderService.cancelOrder(userId, id);
        return ApiResponse.success("取消成功");
    }

    // 支付统一走 PaymentController（POST /api/payments），此处不再提供 payOrder 入口

    @RateLimit(key = KeyType.USER, limit = 10, duration = 60)
    @PatchMapping("/{id}/ship")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ApiResponse shipOrder(@PathVariable Long id) {
        orderService.shipOrder(id);
        return ApiResponse.success("发货成功");
    }

    @RateLimit(key = KeyType.USER, limit = 10, duration = 60)
    @PatchMapping("/{id}/confirm")
    public ApiResponse confirmOrder(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        orderService.confirmOrder(userId, id);
        return ApiResponse.success("确认收货成功");
    }

    @RateLimit(key = KeyType.USER, limit = 10, duration = 60)
    @DeleteMapping("/batch")
    public ApiResponse batchDelete(@RequestBody Map<String, Object> body) {
        Object rawIds = body.get("ids");
        if (!(rawIds instanceof java.util.List<?> rawList) || rawList.isEmpty()) {
            return ApiResponse.error("请选择订单");
        }
        java.util.List<Long> ids = rawList.stream()
                .map(o -> o instanceof Number n ? n.longValue() : Long.parseLong(o.toString()))
                .toList();
        Long userId = SecurityUtils.isCurrentUserAdmin() ? null : SecurityUtils.getCurrentUserId();
        int deleted = orderService.batchDeleteCancelled(ids, userId);
        if (deleted == 0) {
            return ApiResponse.error("所选订单中没有已取消状态的订单，无法删除");
        }
        if (deleted < ids.size()) {
            return ApiResponse.success("成功删除 " + deleted + " 条，已跳过 " + (ids.size() - deleted) + " 条非已取消状态订单");
        }
        return ApiResponse.success("成功删除 " + deleted + " 条订单");
    }

}
