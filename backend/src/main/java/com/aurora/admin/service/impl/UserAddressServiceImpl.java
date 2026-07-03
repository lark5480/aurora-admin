package com.aurora.admin.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aurora.admin.dto.AddressRequest;
import com.aurora.admin.dto.AddressResponse;
import com.aurora.admin.entity.UserAddress;
import com.aurora.admin.exception.NotFoundException;
import com.aurora.admin.mapper.UserAddressMapper;
import com.aurora.admin.service.UserAddressService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
/**
 * 用户地址服务实现类。
 * 管理当前登录用户的收货地址，包括新增、更新、删除及默认地址设置，
 * 所有操作均校验地址归属，确保只能操作当前用户自己的地址。
 */
public class UserAddressServiceImpl implements UserAddressService {

    private final UserAddressMapper addressMapper;
    
    @Override
    public List<AddressResponse> getAddresses(Long userId) {
        return addressMapper.findByUserId(userId).stream()
                .map(AddressResponse::from)
                .toList();
    }

    @Override
    public AddressResponse getById(Long userId, Long id) {
        UserAddress address = addressMapper.findById(id);
        if (address == null || !address.getUserId().equals(userId)) {
            throw new NotFoundException("收货地址", id);
        }
        return AddressResponse.from(address);
    }

    @Override
    @Transactional
    public AddressResponse create(Long userId, AddressRequest request) {
        if (Boolean.TRUE.equals(request.isDefault())) {
            addressMapper.clearDefault(userId);
        }

        UserAddress address = new UserAddress();
        address.setUserId(userId);
        address.setReceiverName(request.receiverName());
        address.setReceiverPhone(request.receiverPhone());
        address.setProvince(request.province());
        address.setCity(request.city());
        address.setDistrict(request.district());
        address.setDetail(request.detail());
        address.setIsDefault(Boolean.TRUE.equals(request.isDefault()) ? 1 : 0);

        addressMapper.insert(address);
        return AddressResponse.from(address);
    }

    @Override
    @Transactional
    public AddressResponse update(Long userId, Long id, AddressRequest request) {
        UserAddress address = addressMapper.findById(id);
        if (address == null || !address.getUserId().equals(userId)) {
            throw new NotFoundException("收货地址", id);
        }

        if (Boolean.TRUE.equals(request.isDefault())) {
            addressMapper.clearDefault(userId);
        }

        address.setReceiverName(request.receiverName());
        address.setReceiverPhone(request.receiverPhone());
        address.setProvince(request.province());
        address.setCity(request.city());
        address.setDistrict(request.district());
        address.setDetail(request.detail());
        address.setIsDefault(Boolean.TRUE.equals(request.isDefault()) ? 1 : 0);

        addressMapper.update(address);
        return AddressResponse.from(address);
    }

    @Override
    @Transactional
    public void delete(Long userId, Long id) {
        UserAddress address = addressMapper.findById(id);
        if (address == null || !address.getUserId().equals(userId)) {
            throw new NotFoundException("收货地址", id);
        }
        addressMapper.deleteById(id);
    }

    @Override
    @Transactional
    public void setDefault(Long userId, Long id) {
        UserAddress address = addressMapper.findById(id);
        if (address == null || !address.getUserId().equals(userId)) {
            throw new NotFoundException("收货地址", id);
        }
        addressMapper.clearDefault(userId);
        addressMapper.setDefault(id);
    }
}
