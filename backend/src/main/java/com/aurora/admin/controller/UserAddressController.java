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
public class UserAddressController {

    private final UserAddressService addressService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ApiResponse getAddresses() {
        Long userId = SecurityUtils.getCurrentUserId();
        return ApiResponse.success(addressService.getAddresses(userId));
    }

    @RateLimit(key = KeyType.USER, limit = 10, duration = 60)
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ApiResponse create(@RequestBody @Valid AddressRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ApiResponse.success(addressService.create(userId, request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse update(@PathVariable Long id, @RequestBody @Valid AddressRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ApiResponse.success(addressService.update(userId, id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse delete(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        addressService.delete(userId, id);
        return ApiResponse.success("删除成功");
    }

    @PatchMapping("/{id}/default")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse setDefault(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        addressService.setDefault(userId, id);
        return ApiResponse.success("设置成功");
    }
}
