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

/**
 * 角色管理控制器。提供角色的增删改查及菜单权限分配接口。
 * 路径：/api/roles
 */
@RestController
@RequestMapping("/api/roles")
public class RoleController {

    @Autowired
    private RoleService roleService;

    /**
     * 分页查询角色列表。支持按关键字模糊搜索角色编码和名称。
     *
     * @param keyword 搜索关键字（可选，默认为空）
     * @param page    页码（从1开始，默认为1）
     * @param size    每页条数（默认为10）
     * @return 包含角色列表和总数量的分页数据
     */
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

    /**
     * 根据ID查询角色详情，包含已分配的菜单权限ID列表。
     *
     * @param id 角色ID
     * @return 角色信息及关联菜单ID列表
     */
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

    /**
     * 创建新角色。需要 ADMIN 或 SUPER_ADMIN 权限。
     * 角色编码和名称不能为空。
     *
     * @param role 角色对象（code、name 必填）
     * @return 创建成功的角色信息
     */
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

    /**
     * 更新角色信息。需要 ADMIN 或 SUPER_ADMIN 权限。
     *
     * @param id   角色ID
     * @param role 新的角色信息
     * @return 更新后的角色信息
     */
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

    /**
     * 删除指定角色。需要 ADMIN 或 SUPER_ADMIN 权限。
     *
     * @param id 角色ID
     * @return 删除成功提示
     */
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

    /**
     * 查询指定角色已分配的菜单权限ID列表。
     *
     * @param id 角色ID
     * @return 菜单权限ID列表
     */
    @GetMapping("/{id}/menus")
    public ApiResponse getMenus(@PathVariable Long id) {
        List<Long> menuIds = roleService.findMenuIdsByRoleId(id);
        return ApiResponse.success(menuIds);
    }

    /**
     * 为指定角色分配菜单权限。需要 ADMIN 或 SUPER_ADMIN 权限。
     *
     * @param id      角色ID
     * @param request 请求体，包含菜单ID列表（menuIds）
     * @return 分配成功提示
     */
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
