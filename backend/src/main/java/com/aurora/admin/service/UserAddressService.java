package com.aurora.admin.service;

import com.aurora.admin.dto.AddressRequest;
import com.aurora.admin.dto.AddressResponse;

import java.util.List;

public interface UserAddressService {

    /**
     * 查询用户的所有收货地址，默认地址排最前
     */
    List<AddressResponse> getAddresses(Long userId);

    /**
     * 根据ID查询地址并校验归属
     */
    AddressResponse getById(Long userId, Long id);

    /**
     * 创建收货地址
     */
    AddressResponse create(Long userId, AddressRequest request);

    /**
     * 更新收货地址
     */
    AddressResponse update(Long userId, Long id, AddressRequest request);

    /**
     * 删除收货地址
     */
    void delete(Long userId, Long id);

    /**
     * 设为默认地址
     */
    void setDefault(Long userId, Long id);
}
