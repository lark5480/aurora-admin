package com.aurora.admin.controller;

import com.aurora.admin.dto.ApiResponse;
import com.aurora.admin.entity.Menu;
import com.aurora.admin.service.MenuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * 菜单管理控制器。提供菜单的增删改查、树形结构查询以及当前登录用户的菜单/权限查询接口。
 * 路径前缀：/api/menus
 */
@RestController
@RequestMapping("/api/menus")
public class MenuController {

    @Autowired
    private MenuService menuService;

    /**
     * 获取完整菜单树。返回所有菜单的树形结构，供管理端使用。
     */
    @GetMapping("/tree")
    public ApiResponse getTree() {
        List<Menu> tree = menuService.findTree();
        return ApiResponse.success(tree);
    }

    /**
     * 获取当前登录用户的菜单树。根据用户角色过滤，仅返回有权限的菜单。
     */
    @GetMapping("/my")
    public ApiResponse getMyMenus() {
        List<Menu> tree = menuService.findTreeForCurrentUser();
        return ApiResponse.success(tree);
    }

    /**
     * 获取当前登录用户的权限标识列表。用于前端按钮级鉴权。
     */
    @GetMapping("/my/permissions")
    public ApiResponse getMyPermissions() {
        List<String> permissions = menuService.findPermissionsForCurrentUser();
        return ApiResponse.success(permissions);
    }

    /**
     * 获取所有菜单列表（扁平结构）。用于管理端菜单配置。
     */
    @GetMapping
    public ApiResponse getAll() {
        List<Menu> menus = menuService.findAll();
        return ApiResponse.success(menus);
    }

    /**
     * 根据 ID 获取菜单详情。菜单不存在时返回 404。
     *
     * @param id 菜单 ID
     */
    @GetMapping("/{id}")
    public ApiResponse getById(@PathVariable Long id) {
        Menu menu = menuService.findById(id);
        if (menu == null) {
            return ApiResponse.error(404, "菜单不存在");
        }
        return ApiResponse.success(menu);
    }

    /**
     * 创建菜单。需要 ADMIN 或 SUPER_ADMIN 权限。
     *
     * @param menu 菜单信息（名称和类型为必填）
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ApiResponse create(@RequestBody Menu menu) {
        if (menu.getName() == null || menu.getName().isBlank()) {
            return ApiResponse.error(400, "菜单名称不能为空");
        }
        if (menu.getMenuType() == null) {
            return ApiResponse.error(400, "菜单类型不能为空");
        }
        Menu created = menuService.create(menu);
        return ApiResponse.success(created);
    }

    /**
     * 更新菜单。需要 ADMIN 或 SUPER_ADMIN 权限。
     *
     * @param id   菜单 ID
     * @param menu 更新的菜单信息
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ApiResponse update(@PathVariable Long id, @RequestBody Menu menu) {
        try {
            Menu updated = menuService.update(id, menu);
            return ApiResponse.success(updated);
        } catch (RuntimeException e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    /**
     * 删除菜单。需要 ADMIN 或 SUPER_ADMIN 权限。存在子菜单时不允许删除。
     *
     * @param id 菜单 ID
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ApiResponse delete(@PathVariable Long id) {
        try {
            menuService.delete(id);
            return ApiResponse.success("删除成功");
        } catch (RuntimeException e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }
}
