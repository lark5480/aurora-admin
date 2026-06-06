package com.aurora.admin.dto;

import com.aurora.admin.entity.UserAddress;

/**
 * 收货地址响应 DTO
 */
public record AddressResponse(
    Long id,
    String receiverName,
    String receiverPhone,
    String province,
    String city,
    String district,
    String detail,
    Boolean isDefault
) {
    public static AddressResponse from(UserAddress addr) {
        if (addr == null) {
            return null;
        }
        return new AddressResponse(
            addr.getId(),
            addr.getReceiverName(),
            addr.getReceiverPhone(),
            addr.getProvince(),
            addr.getCity(),
            addr.getDistrict(),
            addr.getDetail(),
            addr.getIsDefault() != null && addr.getIsDefault() == 1
        );
    }
}
