package com.aurora.admin.controller;

import com.aurora.admin.dto.ApiResponse;
import com.aurora.admin.entity.Menu;
import com.aurora.admin.service.MenuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/menus")
public class MenuController {

    @Autowired
    private MenuService menuService;

    @GetMapping("/tree")
    public ApiResponse getTree() {
        List<Menu> tree = menuService.findTree();
        return ApiResponse.success(tree);
    }

    @GetMapping("/my")
    public ApiResponse getMyMenus() {
        List<Menu> tree = menuService.findTreeForCurrentUser();
        return ApiResponse.success(tree);
    }

    @GetMapping("/my/permissions")
    public ApiResponse getMyPermissions() {
        List<String> permissions = menuService.findPermissionsForCurrentUser();
        return ApiResponse.success(permissions);
    }

    @GetMapping
    public ApiResponse getAll() {
        List<Menu> menus = menuService.findAll();
        return ApiResponse.success(menus);
    }

    @GetMapping("/{id}")
    public ApiResponse getById(@PathVariable Long id) {
        Menu menu = menuService.findById(id);
        if (menu == null) {
            return ApiResponse.error(404, "菜单不存在");
        }
        return ApiResponse.success(menu);
    }

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
