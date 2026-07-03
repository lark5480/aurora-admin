package com.aurora.admin.controller;

import com.aurora.admin.dto.ApiResponse;
import com.aurora.admin.entity.Dept;
import com.aurora.admin.service.DeptService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * 部门管理控制器。提供部门树查询、列表查询、增删改等 REST 接口。
 * 创建、修改、删除操作需要 ADMIN 或 SUPER_ADMIN 权限。
 */
@RestController
@RequestMapping("/api/depts")
public class DeptController {

    @Autowired
    private DeptService deptService;

    /**
     * 获取部门树结构。按 parentId 分组后返回层级嵌套的部门树。
     */
    @GetMapping("/tree")
    public ApiResponse getTree() {
        List<Dept> tree = deptService.findTree();
        return ApiResponse.success(tree);
    }

    /**
     * 获取全部部门列表。返回扁平列表，不做树形组装。
     */
    @GetMapping
    public ApiResponse getAll() {
        List<Dept> depts = deptService.findAll();
        return ApiResponse.success(depts);
    }

    /**
     * 根据 ID 查询部门详情。部门不存在时返回 404 错误。
     *
     * @param id 部门 ID
     */
    @GetMapping("/{id}")
    public ApiResponse getById(@PathVariable Long id) {
        Dept dept = deptService.findById(id);
        if (dept == null) {
            return ApiResponse.error(404, "部门不存在");
        }
        return ApiResponse.success(dept);
    }

    /**
     * 创建新部门。部门名称不能为空。需要 ADMIN 或 SUPER_ADMIN 权限。
     *
     * @param dept 部门信息（JSON 请求体）
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ApiResponse create(@RequestBody Dept dept) {
        if (dept.getName() == null || dept.getName().isBlank()) {
            return ApiResponse.error(400, "部门名称不能为空");
        }
        Dept created = deptService.create(dept);
        return ApiResponse.success(created);
    }

    /**
     * 更新指定部门信息。需要 ADMIN 或 SUPER_ADMIN 权限。
     *
     * @param id   部门 ID
     * @param dept 部门更新信息（JSON 请求体）
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ApiResponse update(@PathVariable Long id, @RequestBody Dept dept) {
        dept.setId(id);
        deptService.update(id, dept);
        return ApiResponse.success("更新成功");
    }

    /**
     * 删除指定部门。存在子部门时拒绝删除并返回错误信息。需要 ADMIN 或 SUPER_ADMIN 权限。
     *
     * @param id 部门 ID
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ApiResponse delete(@PathVariable Long id) {
        try {
            deptService.delete(id);
            return ApiResponse.success("删除成功");
        } catch (RuntimeException e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }
}
