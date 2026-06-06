package com.aurora.admin.controller;

import com.aurora.admin.dto.ApiResponse;
import com.aurora.admin.entity.Dept;
import com.aurora.admin.service.DeptService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/depts")
public class DeptController {

    @Autowired
    private DeptService deptService;

    @GetMapping("/tree")
    public ApiResponse getTree() {
        List<Dept> tree = deptService.findTree();
        return ApiResponse.success(tree);
    }

    @GetMapping
    public ApiResponse getAll() {
        List<Dept> depts = deptService.findAll();
        return ApiResponse.success(depts);
    }

    @GetMapping("/{id}")
    public ApiResponse getById(@PathVariable Long id) {
        Dept dept = deptService.findById(id);
        if (dept == null) {
            return ApiResponse.error(404, "部门不存在");
        }
        return ApiResponse.success(dept);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ApiResponse create(@RequestBody Dept dept) {
        if (dept.getName() == null || dept.getName().isBlank()) {
            return ApiResponse.error(400, "部门名称不能为空");
        }
        Dept created = deptService.create(dept);
        return ApiResponse.success(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ApiResponse update(@PathVariable Long id, @RequestBody Dept dept) {
        dept.setId(id);
        deptService.update(id, dept);
        return ApiResponse.success("更新成功");
    }

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
