package com.aurora.admin.controller;

import java.util.Map;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.aurora.admin.dto.*;
import com.aurora.admin.service.AfterSaleService;
import com.aurora.admin.util.SecurityUtils;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * 售后管理控制器。提供售后申请的创建、批量提交、分页查询、详情查看、审核通过与驳回等接口。
 * 所有接口需要用户已认证，审核相关接口需要 ADMIN / SUPER_ADMIN 角色。
 * 基础路径：/api/after-sales
 */
@RestController
@RequestMapping("/api/after-sales")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class AfterSaleController {

    private final AfterSaleService afterSaleService;

    /**
     * 创建售后申请。用户对指定订单明细提交售后申请（退款/退货），状态初始为 APPLIED，等待管理员审核。
     *
     * @param request 售后申请参数，包含订单明细ID、售后类型、原因等
     * @return 创建的售后记录详情
     */
    @PostMapping
    public ApiResponse createAfterSale(@Valid @RequestBody CreateAfterSaleRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        AfterSaleResponse result = afterSaleService.createAfterSale(userId, request);
        return ApiResponse.success(result);
    }

    /**
     * 批量提交售后申请。对订单内所有未退款的明细行发起售后，返回成功创建的记录数。
     *
     * @param request 批量售后参数，包含订单ID、售后类型、原因等
     * @return 成功创建的售后申请数量
     */
    @PostMapping("/batch")
    public ApiResponse createAfterSaleBatch(@Valid @RequestBody CreateAfterSaleBatchRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        int count = afterSaleService.createAfterSaleBatch(userId, request);
        return ApiResponse.success("成功创建 " + count + " 笔售后申请");
    }

    /**
     * 分页查询售后记录。支持按订单ID、售后单号、订单号、状态筛选；管理员可查看全部，普通用户仅查看自己的记录。
     *
     * @param page       页码，默认 1
     * @param size       每页条数，默认 10
     * @param orderId    订单ID（可选）
     * @param status     售后状态（可选）
     * @param afterSaleNo 售后单号（可选）
     * @param orderNo    订单号（可选）
     * @return 售后记录分页结果
     */
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

    /**
     * 查询售后详情。按售后记录ID获取单条详情，管理员可查看全部，普通用户仅查看自己的记录。
     *
     * @param id 售后记录ID
     * @return 售后记录详情
     */
    @GetMapping("/{id}")
    public ApiResponse getAfterSaleDetail(@PathVariable Long id) {
        Long userId = SecurityUtils.isCurrentUserAdmin() ? null : SecurityUtils.getCurrentUserId();
        AfterSaleResponse result = afterSaleService.getAfterSaleDetail(userId, id);
        return ApiResponse.success(result);
    }

    /**
     * 审核通过售后申请。管理员执行退款并恢复库存，需 ADMIN / SUPER_ADMIN 角色。
     *
     * @param id   售后记录ID
     * @param body 请求体，可包含 remark（审核备注）
     * @return 更新后的售后记录详情
     */
    @PatchMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ApiResponse approve(@PathVariable Long id, @RequestBody Map<String, String> body) {
        Long adminId = SecurityUtils.getCurrentUserId();
        String remark = body.getOrDefault("remark", "");
        AfterSaleResponse result = afterSaleService.approve(adminId, id, remark);
        return ApiResponse.success(result);
    }

    /**
     * 驳回售后申请。管理员驳回用户的售后申请，需 ADMIN / SUPER_ADMIN 角色。
     *
     * @param id   售后记录ID
     * @param body 请求体，可包含 remark（驳回原因）
     * @return 更新后的售后记录详情
     */
    @PatchMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ApiResponse reject(@PathVariable Long id, @RequestBody Map<String, String> body) {
        Long adminId = SecurityUtils.getCurrentUserId();
        String remark = body.getOrDefault("remark", "");
        AfterSaleResponse result = afterSaleService.reject(adminId, id, remark);
        return ApiResponse.success(result);
    }
}
