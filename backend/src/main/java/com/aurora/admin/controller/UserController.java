package com.aurora.admin.controller;

import com.aurora.admin.annotation.RateLimit;
import com.aurora.admin.annotation.RateLimit.KeyType;
import com.aurora.admin.dto.ApiResponse;
import com.aurora.admin.dto.ChangePasswordRequest;
import com.aurora.admin.dto.CreateUserRequest;
import com.aurora.admin.dto.PageResult;
import com.aurora.admin.dto.UpdateProfileRequest;
import com.aurora.admin.dto.UpdateUserRequest;
import com.aurora.admin.dto.UserVO;
import com.aurora.admin.entity.Role;
import com.aurora.admin.entity.User;
import com.aurora.admin.exception.NotFoundException;
import com.aurora.admin.service.UserService;
import com.aurora.admin.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @RateLimit(key = KeyType.USER, limit = 5, duration = 60, message = "创建用户过于频繁，请稍后再试")
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ApiResponse createUser(@RequestBody @Valid CreateUserRequest request) {
        User created = userService.create(request);
        return ApiResponse.success(UserVO.from(created));
    }

    @GetMapping
    public ApiResponse getUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "") String keyword) {
        PageResult<UserVO> result = userService.findAll(page, size, keyword);
        return ApiResponse.success(result);
    }

    @GetMapping("/{id}")
    public ApiResponse getUserById(@PathVariable Long id) {
        User user = userService.findById(id);
        if (user == null) {
            throw new NotFoundException("用户", id);
        }
        return ApiResponse.success(UserVO.from(user));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ApiResponse updateUser(@PathVariable Long id, @RequestBody @Valid UpdateUserRequest request) {
        userService.update(id, request);
        return ApiResponse.success("更新成功");
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ApiResponse updateStatus(@PathVariable Long id, @RequestParam int status) {
        userService.updateStatus(id, status);
        return ApiResponse.success("状态更新成功");
    }

    @RateLimit(key = KeyType.USER, limit = 3, duration = 60, message = "删除用户过于频繁，请稍后再试")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ApiResponse deleteUser(@PathVariable Long id) {
        userService.delete(id);
        return ApiResponse.success("删除成功");
    }

    @GetMapping("/info")
    public ApiResponse getUserInfo() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) {
            throw new NotFoundException("用户");
        }
        String username = auth.getPrincipal().toString();
        User user = userService.findByUsername(username);
        return ApiResponse.success(UserVO.from(user));
    }

    @GetMapping("/findByEmail")
    public ApiResponse findByEmail(@RequestParam String email) {
        User user = userService.findByEmail(email);
        if (user == null) {
            throw new NotFoundException("用户");
        }
        return ApiResponse.success(UserVO.from(user));
    }

    @GetMapping("/{id}/roles")
    public ApiResponse getUserRoles(@PathVariable Long id) {
        List<Role> roles = userService.findRolesByUserId(id);
        return ApiResponse.success(roles);
    }

    @RateLimit(key = KeyType.USER, limit = 5, duration = 60, message = "角色分配过于频繁，请稍后再试")
    @PutMapping("/{id}/roles")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ApiResponse assignRoles(@PathVariable Long id, @RequestBody List<Long> roleIds) {
        userService.assignRoles(id, roleIds);
        return ApiResponse.success("角色分配成功");
    }

    @GetMapping("/profile")
    public ApiResponse getProfile() {
        User user = userService.getProfile();
        if (user == null) {
            throw new NotFoundException("用户");
        }
        return ApiResponse.success(UserVO.from(user));
    }

    @PutMapping("/profile")
    public ApiResponse updateProfile(@RequestBody @Valid UpdateProfileRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        userService.updateProfile(userId, request.getNickname(), request.getEmail(), request.getAvatar());
        return ApiResponse.success("更新成功");
    }

    @PutMapping("/password")
    public ApiResponse changePassword(@RequestBody @Valid ChangePasswordRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        userService.changePassword(userId, request.getOldPassword(), request.getNewPassword());
        return ApiResponse.success("密码修改成功");
    }
}
