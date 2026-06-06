package com.aurora.admin.controller;

import java.util.Map;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.aurora.admin.dto.*;
import com.aurora.admin.service.AfterSaleService;
import com.aurora.admin.util.SecurityUtils;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/after-sales")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class AfterSaleController {

    private final AfterSaleService afterSaleService;

    @PostMapping
    public ApiResponse createAfterSale(@Valid @RequestBody CreateAfterSaleRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        AfterSaleResponse result = afterSaleService.createAfterSale(userId, request);
        return ApiResponse.success(result);
    }

    @PostMapping("/batch")
    public ApiResponse createAfterSaleBatch(@Valid @RequestBody CreateAfterSaleBatchRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        int count = afterSaleService.createAfterSaleBatch(userId, request);
        return ApiResponse.success("成功创建 " + count + " 笔售后申请");
    }

    @GetMapping
    public ApiResponse getAfterSalePage(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Long orderId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String afterSaleNo,
            @RequestParam(required = false) String orderNo) {
        Long userId = SecurityUtils.isCurrentUserAdmin() ? null : SecurityUtils.getCurrentUserId();
        AfterSaleQuery query = new AfterSaleQuery(page, size, orderId, status, afterSaleNo, orderNo);
        PageResult<AfterSaleResponse> result = afterSaleService.getAfterSalePage(userId, query);
        return ApiResponse.success(result);
    }

    @GetMapping("/{id}")
    public ApiResponse getAfterSaleDetail(@PathVariable Long id) {
        Long userId = SecurityUtils.isCurrentUserAdmin() ? null : SecurityUtils.getCurrentUserId();
        AfterSaleResponse result = afterSaleService.getAfterSaleDetail(userId, id);
        return ApiResponse.success(result);
    }

    @PatchMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ApiResponse approve(@PathVariable Long id, @RequestBody Map<String, String> body) {
        Long adminId = SecurityUtils.getCurrentUserId();
        String remark = body.getOrDefault("remark", "");
        AfterSaleResponse result = afterSaleService.approve(adminId, id, remark);
        return ApiResponse.success(result);
    }

    @PatchMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ApiResponse reject(@PathVariable Long id, @RequestBody Map<String, String> body) {
        Long adminId = SecurityUtils.getCurrentUserId();
        String remark = body.getOrDefault("remark", "");
        AfterSaleResponse result = afterSaleService.reject(adminId, id, remark);
        return ApiResponse.success(result);
    }
}
