package com.aurora.admin.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;

/**
 * 创建订单请求
 */
public record CreateOrderRequest(
    @NotEmpty(message = "请选择结算商品")
    List<Long> cartItemIds,

    @NotBlank(message = "收货人不能为空")
    String receiverName,

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "请输入正确的11位手机号")
    String receiverPhone,

    @NotBlank(message = "收货地址不能为空")
    String receiverAddress,

    String remark
) {
}
