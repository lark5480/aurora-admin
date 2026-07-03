package com.aurora.admin.controller;

import com.aurora.admin.annotation.RateLimit;
import com.aurora.admin.annotation.RateLimit.KeyType;
import com.aurora.admin.dto.AddressRequest;
import com.aurora.admin.dto.ApiResponse;
import com.aurora.admin.service.UserAddressService;
import com.aurora.admin.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/addresses")
@RequiredArgsConstructor
/**
 * 用户地址管理控制器。
 * 提供当前登录用户收货地址的增删改查及默认地址设置功能。
 * 所有接口要求用户已登录（{@code isAuthenticated()}）。
 */
public class UserAddressController {

    private final UserAddressService addressService;

    /**
     * 获取当前用户的所有收货地址列表。
     *
     * @return 地址列表
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ApiResponse getAddresses() {
        Long userId = SecurityUtils.getCurrentUserId();
        return ApiResponse.success(addressService.getAddresses(userId));
    }

    /**
     * 新增收货地址。限流 10次/60秒。
     *
     * @param request 地址信息（收件人、电话、地区、详细地址等）
     * @return 新增的地址信息
     */
    @RateLimit(key = KeyType.USER, limit = 10, duration = 60)
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ApiResponse create(@RequestBody @Valid AddressRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ApiResponse.success(addressService.create(userId, request));
    }

    /**
     * 更新指定收货地址。仅允许更新当前用户自己的地址。
     *
     * @param id      地址 ID
     * @param request 更新的地址信息
     * @return 更新后的地址信息
     */
    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse update(@PathVariable Long id, @RequestBody @Valid AddressRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ApiResponse.success(addressService.update(userId, id, request));
    }

    /**
     * 删除指定收货地址。仅允许删除当前用户自己的地址。
     *
     * @param id 地址 ID
     * @return 操作结果
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse delete(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        addressService.delete(userId, id);
        return ApiResponse.success("删除成功");
    }

    /**
     * 设置指定地址为默认收货地址。仅允许设置当前用户自己的地址。
     *
     * @param id 地址 ID
     * @return 操作结果
     */
    @PatchMapping("/{id}/default")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse setDefault(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        addressService.setDefault(userId, id);
        return ApiResponse.success("设置成功");
    }
}
