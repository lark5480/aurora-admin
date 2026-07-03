package com.aurora.admin.service;

import com.aurora.admin.dto.PayRequest;
import com.aurora.admin.dto.PayResponse;
import com.aurora.admin.dto.PaymentResponse;

import java.util.List;

public interface PaymentService {

    /**
     * 支付（含幂等处理）
     *
     * @param userId  当前用户ID
     * @param request 支付请求
     * @return 支付结果
     */
    PayResponse pay(Long userId, PayRequest request);

    /**
     * 根据订单ID查询支付记录
     *
     * @param orderId 订单 ID
     * @return 该订单的支付记录列表
     */
    List<PaymentResponse> getPaymentByOrderId(Long orderId);

    /**
     * 根据订单号查询支付记录
     *
     * @param orderNo 订单号（字符串格式）
     * @return 该订单的支付记录列表
     */
    List<PaymentResponse> getPaymentByOrderNo(String orderNo);
}
