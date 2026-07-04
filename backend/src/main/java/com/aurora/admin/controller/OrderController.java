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
import org.springframework.web.bind.annotation.RequestHeader;
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

/**
 * 订单管理控制器。提供订单的创建、分页查询、Excel 导出、详情查询、取消、发货、
 * 确认收货、批量删除等接口。所有接口需登录访问，部分接口要求 ADMIN/SUPER_ADMIN 权限。
 */
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class OrderController {

    private final OrderService orderService;

    /**
     * 创建订单。从购物车选中商品创建新订单，限流 5次/分钟。
     * 客户端需传入 Idempotent-Key（UUID）用于防重复提交。
     *
     * @param request       创建订单请求（含商品 SKU、数量等信息）
     * @param idempotentKey 幂等 Key（UUID），由客户端生成，用于防止重复提交
     * @return 创建的订单详情
     */
    @RateLimit(key = KeyType.USER, limit = 5, duration = 60)
    @PostMapping
    public ApiResponse createOrder(
            @Valid @RequestBody CreateOrderRequest request,
            @RequestHeader("Idempotent-Key") String idempotentKey) {
        Long userId = SecurityUtils.getCurrentUserId();
        OrderResponse order = orderService.createOrder(userId, request, idempotentKey);
        return ApiResponse.success(order);
    }

    /**
     * 分页查询订单列表。管理员可查全部订单，普通用户仅查自己的订单。
     *
     * @param page     页码（默认 1）
     * @param size     每页条数（默认 10）
     * @param status   订单状态筛选（可选）
     * @param orderNo  订单号筛选（可选）
     * @param username 用户名筛选（可选，仅管理员有效）
     * @return 分页订单结果
     */
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

    /**
     * 导出订单 Excel 文件。根据筛选条件导出，管理员可导出全部订单。
     *
     * @param status   订单状态筛选（可选）
     * @param orderNo  订单号筛选（可选）
     * @param username 用户名筛选（可选，仅管理员有效）
     * @return Excel 文件字节流响应（Content-Disposition 附件下载）
     */
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

    /**
     * 获取订单详情。根据订单 ID 查询，普通用户仅可查自己的订单。
     *
     * @param id 订单 ID
     * @return 订单详情
     */
    @GetMapping("/{id}")
    public ApiResponse getOrderDetail(@PathVariable Long id) {
        Long userId = SecurityUtils.isCurrentUserAdmin() ? null : SecurityUtils.getCurrentUserId();
        OrderResponse order = orderService.getOrderDetail(userId, id);
        return ApiResponse.success(order);
    }

    /**
     * 取消订单。用户取消自己的订单，仅 PENDING 状态允许操作，限流 10次/分钟。
     *
     * @param id 订单 ID
     * @return 取消成功提示
     */
    @RateLimit(key = KeyType.USER, limit = 10, duration = 60)
    @PatchMapping("/{id}/cancel")
    public ApiResponse cancelOrder(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        orderService.cancelOrder(userId, id);
        return ApiResponse.success("取消成功");
    }

    // 支付统一走 PaymentController（POST /api/payments），此处不再提供 payOrder 入口

    /**
     * 发货。管理员权限，将订单标记为已发货，限流 10次/分钟。
     *
     * @param id 订单 ID
     * @return 发货成功提示
     */
    @RateLimit(key = KeyType.USER, limit = 10, duration = 60)
    @PatchMapping("/{id}/ship")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ApiResponse shipOrder(@PathVariable Long id) {
        orderService.shipOrder(id);
        return ApiResponse.success("发货成功");
    }

    /**
     * 确认收货。用户确认自己的订单已收货，仅 SHIPPED 状态允许操作，限流 10次/分钟。
     *
     * @param id 订单 ID
     * @return 确认收货成功提示
     */
    @RateLimit(key = KeyType.USER, limit = 10, duration = 60)
    @PatchMapping("/{id}/confirm")
    public ApiResponse confirmOrder(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        orderService.confirmOrder(userId, id);
        return ApiResponse.success("确认收货成功");
    }

    /**
     * 批量删除已取消的订单。仅删除 CANCELLED 状态的订单，其他状态自动跳过，
     * 管理员可删除全部用户的订单，普通用户仅可删除自己的订单，限流 10次/分钟。
     *
     * @param body 请求体（含 ids 数组，如 {"ids": [1, 2, 3]}）
     * @return 删除结果提示（含实际删除数与跳过的数量）
     */
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
