package com.aurora.admin.controller;

import com.aurora.admin.dto.ApiResponse;
import com.aurora.admin.entity.Role;
import com.aurora.admin.service.RoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/roles")
public class RoleController {

    @Autowired
    private RoleService roleService;

    @GetMapping
    public ApiResponse getAll(@RequestParam(defaultValue = "") String keyword,
                              @RequestParam(defaultValue = "1") int page,
                              @RequestParam(defaultValue = "10") int size) {
        int offset = (page - 1) * size;
        List<Role> roles = roleService.findByKeyword(keyword, offset, size);
        long total = roleService.countByKeyword(keyword);
        Map<String, Object> data = new HashMap<>();
        data.put("list", roles);
        data.put("total", total);
        return ApiResponse.success(data);
    }

    @GetMapping("/{id}")
    public ApiResponse getById(@PathVariable Long id) {
        Role role = roleService.findById(id);
        if (role == null) {
            return ApiResponse.error(404, "角色不存在");
        }
        Map<String, Object> data = new HashMap<>();
        data.put("role", role);
        data.put("menuIds", roleService.findMenuIdsByRoleId(id));
        return ApiResponse.success(data);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ApiResponse create(@RequestBody Role role) {
        if (role.getCode() == null || role.getCode().isBlank()) {
            return ApiResponse.error(400, "角色编码不能为空");
        }
        if (role.getName() == null || role.getName().isBlank()) {
            return ApiResponse.error(400, "角色名称不能为空");
        }
        Role created = roleService.create(role);
        return ApiResponse.success(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ApiResponse update(@PathVariable Long id, @RequestBody Role role) {
        try {
            Role updated = roleService.update(id, role);
            return ApiResponse.success(updated);
        } catch (RuntimeException e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ApiResponse delete(@PathVariable Long id) {
        try {
            roleService.delete(id);
            return ApiResponse.success("删除成功");
        } catch (RuntimeException e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @GetMapping("/{id}/menus")
    public ApiResponse getMenus(@PathVariable Long id) {
        List<Long> menuIds = roleService.findMenuIdsByRoleId(id);
        return ApiResponse.success(menuIds);
    }

    @PutMapping("/{id}/menus")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ApiResponse assignMenus(@PathVariable Long id, @RequestBody Map<String, List<Long>> request) {
        try {
            List<Long> menuIds = request.get("menuIds");
            roleService.assignMenus(id, menuIds);
            return ApiResponse.success("分配成功");
        } catch (RuntimeException e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }
}
