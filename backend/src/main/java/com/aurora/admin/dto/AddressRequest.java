package com.aurora.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 创建/更新收货地址请求 DTO
 */
public record AddressRequest(
    @NotBlank(message = "收货人不能为空")
    String receiverName,

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "请输入正确的11位手机号")
    String receiverPhone,

    String province,

    String city,

    String district,

    @NotBlank(message = "详细地址不能为空")
    String detail,

    Boolean isDefault
) {}
