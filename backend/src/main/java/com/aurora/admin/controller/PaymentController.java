package com.aurora.admin.controller;

import com.aurora.admin.annotation.RateLimit;
import com.aurora.admin.annotation.RateLimit.KeyType;
import com.aurora.admin.dto.ApiResponse;
import com.aurora.admin.dto.PayRequest;
import com.aurora.admin.dto.PayResponse;
import com.aurora.admin.dto.PaymentResponse;
import com.aurora.admin.service.PaymentService;
import com.aurora.admin.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 支付模块 REST 控制器。提供支付下单、按订单 ID/订单号查询支付记录等接口。
 * 所有接口需要登录后才能访问。
 */
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * 提交支付。根据支付请求参数执行扣款操作，限流 3次/用户/60秒。
     *
     * @param request 支付请求体（含订单号、支付方式、金额等）
     * @return 支付结果，包含交易流水号、支付状态等信息
     */
    @RateLimit(key = KeyType.USER, limit = 3, duration = 60)
    @PostMapping
    public ApiResponse pay(@Valid @RequestBody PayRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        PayResponse response = paymentService.pay(userId, request);
        return ApiResponse.success(response);
    }

    /**
     * 按订单 ID 查询关联的支付记录。一个订单可能对应多笔支付（如部分退款重付）。
     *
     * @param orderId 订单 ID
     * @return 该订单的支付记录列表
     */
    @GetMapping("/order/{orderId}")
    public ApiResponse getByOrderId(@PathVariable Long orderId) {
        List<PaymentResponse> payments = paymentService.getPaymentByOrderId(orderId);
        return ApiResponse.success(payments);
    }

    /**
     * 按订单号查询关联的支付记录。
     *
     * @param orderNo 订单号（字符串格式）
     * @return 该订单的支付记录列表
     */
    @GetMapping("/orderNo/{orderNo}")
    public ApiResponse getByOrderNo(@PathVariable String orderNo) {
        List<PaymentResponse> payments = paymentService.getPaymentByOrderNo(orderNo);
        return ApiResponse.success(payments);
    }
}
