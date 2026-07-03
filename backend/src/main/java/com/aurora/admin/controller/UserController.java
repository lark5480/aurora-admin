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

/**
 * 用户管理控制器。提供用户 CRUD、角色分配、个人资料修改等功能。
 *
 * <ul>
 *   <li>管理员接口（创建/更新/删除/分配角色）需 ADMIN 或 SUPER_ADMIN 角色。</li>
 *   <li>个人资料接口（profile/password）由当前登录用户操作本人数据。</li>
 *   <li>公共查询接口（列表/详情）登录即可访问。</li>
 *   <li>部分接口通过 {@code @RateLimit} 注解限流。</li>
 * </ul>
 *
 * @see UserService
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 创建用户。仅 ADMIN / SUPER_ADMIN 可操作，限流 5 次/分钟（按用户维度）。
     *
     * @param request 创建用户请求（用户名、密码、邮箱等）
     * @return 创建成功的用户视图对象
     */
    @RateLimit(key = KeyType.USER, limit = 5, duration = 60, message = "创建用户过于频繁，请稍后再试")
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ApiResponse createUser(@RequestBody @Valid CreateUserRequest request) {
        User created = userService.create(request);
        return ApiResponse.success(UserVO.from(created));
    }

    /**
     * 分页查询用户列表。支持按关键字模糊搜索用户名和邮箱，登录即可访问。
     *
     * @param page    页码，从 1 开始，默认 1
     * @param size    每页条数，默认 10
     * @param keyword 搜索关键字（模糊匹配用户名、邮箱），默认空字符串查询全部
     * @return 分页结果，包含用户视图对象列表
     */
    @GetMapping
    public ApiResponse getUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "") String keyword) {
        PageResult<UserVO> result = userService.findAll(page, size, keyword);
        return ApiResponse.success(result);
    }

    /**
     * 根据 ID 查询单个用户。登录即可访问。
     *
     * @param id 用户 ID
     * @return 用户视图对象；不存在时抛出 {@link NotFoundException}
     */
    @GetMapping("/{id}")
    public ApiResponse getUserById(@PathVariable Long id) {
        User user = userService.findById(id);
        if (user == null) {
            throw new NotFoundException("用户", id);
        }
        return ApiResponse.success(UserVO.from(user));
    }

    /**
     * 更新用户信息。仅 ADMIN / SUPER_ADMIN 可操作。
     *
     * @param id      用户 ID
     * @param request 更新请求（可修改用户名、邮箱、状态等）
     * @return 操作成功提示
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ApiResponse updateUser(@PathVariable Long id, @RequestBody @Valid UpdateUserRequest request) {
        userService.update(id, request);
        return ApiResponse.success("更新成功");
    }

    /**
     * 更新用户状态（启用/禁用）。仅 ADMIN / SUPER_ADMIN 可操作。
     *
     * @param id     用户 ID
     * @param status 目标状态值（0=禁用，1=启用）
     * @return 操作成功提示
     */
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ApiResponse updateStatus(@PathVariable Long id, @RequestParam int status) {
        userService.updateStatus(id, status);
        return ApiResponse.success("状态更新成功");
    }

    /**
     * 删除用户。仅 ADMIN / SUPER_ADMIN 可操作，限流 3 次/分钟。
     *
     * @param id 用户 ID
     * @return 操作成功提示
     */
    @RateLimit(key = KeyType.USER, limit = 3, duration = 60, message = "删除用户过于频繁，请稍后再试")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ApiResponse deleteUser(@PathVariable Long id) {
        userService.delete(id);
        return ApiResponse.success("删除成功");
    }

    /**
     * 获取当前登录用户的信息。从 SecurityContext 中提取用户名后查询。
     *
     * @return 当前用户的视图对象；用户不存在时抛出 {@link NotFoundException}
     */
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

    /**
     * 根据邮箱查询用户。登录即可访问。
     *
     * @param email 用户邮箱
     * @return 用户视图对象；不存在时抛出 {@link NotFoundException}
     */
    @GetMapping("/findByEmail")
    public ApiResponse findByEmail(@RequestParam String email) {
        User user = userService.findByEmail(email);
        if (user == null) {
            throw new NotFoundException("用户");
        }
        return ApiResponse.success(UserVO.from(user));
    }

    /**
     * 查询用户已关联的角色列表。登录即可访问。
     *
     * @param id 用户 ID
     * @return 角色列表
     */
    @GetMapping("/{id}/roles")
    public ApiResponse getUserRoles(@PathVariable Long id) {
        List<Role> roles = userService.findRolesByUserId(id);
        return ApiResponse.success(roles);
    }

    /**
     * 为用户分配角色。仅 ADMIN / SUPER_ADMIN 可操作，限流 5 次/分钟。
     * 会覆盖用户原有的角色关联（先清空后插入）。
     *
     * @param id      用户 ID
     * @param roleIds 角色 ID 列表
     * @return 操作成功提示
     */
    @RateLimit(key = KeyType.USER, limit = 5, duration = 60, message = "角色分配过于频繁，请稍后再试")
    @PutMapping("/{id}/roles")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ApiResponse assignRoles(@PathVariable Long id, @RequestBody List<Long> roleIds) {
        userService.assignRoles(id, roleIds);
        return ApiResponse.success("角色分配成功");
    }

    /**
     * 获取当前登录用户的个人资料。从安全上下文中提取当前用户 ID。
     *
     * @return 用户视图对象
     */
    @GetMapping("/profile")
    public ApiResponse getProfile() {
        User user = userService.getProfile();
        if (user == null) {
            throw new NotFoundException("用户");
        }
        return ApiResponse.success(UserVO.from(user));
    }

    /**
     * 修改当前登录用户的个人资料（昵称、邮箱、头像）。
     *
     * @param request 修改请求（昵称、邮箱、头像）
     * @return 操作成功提示
     */
    @PutMapping("/profile")
    public ApiResponse updateProfile(@RequestBody @Valid UpdateProfileRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        userService.updateProfile(userId, request.getNickname(), request.getEmail(), request.getAvatar());
        return ApiResponse.success("更新成功");
    }

    /**
     * 修改当前登录用户的密码。需要校验旧密码正确性。
     *
     * @param request 密码修改请求（旧密码、新密码）
     * @return 操作成功提示
     */
    @PutMapping("/password")
    public ApiResponse changePassword(@RequestBody @Valid ChangePasswordRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        userService.changePassword(userId, request.getOldPassword(), request.getNewPassword());
        return ApiResponse.success("密码修改成功");
    }
}
