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

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class PaymentController {

    private final PaymentService paymentService;

    @RateLimit(key = KeyType.USER, limit = 3, duration = 60)
    @PostMapping
    public ApiResponse pay(@Valid @RequestBody PayRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        PayResponse response = paymentService.pay(userId, request);
        return ApiResponse.success(response);
    }

    @GetMapping("/order/{orderId}")
    public ApiResponse getByOrderId(@PathVariable Long orderId) {
        List<PaymentResponse> payments = paymentService.getPaymentByOrderId(orderId);
        return ApiResponse.success(payments);
    }

    @GetMapping("/orderNo/{orderNo}")
    public ApiResponse getByOrderNo(@PathVariable String orderNo) {
        List<PaymentResponse> payments = paymentService.getPaymentByOrderNo(orderNo);
        return ApiResponse.success(payments);
    }
}
