package com.aurora.admin.controller;

import com.aurora.admin.annotation.RateLimit;
import com.aurora.admin.annotation.RateLimit.KeyType;
import com.aurora.admin.dto.ApiResponse;
import com.aurora.admin.dto.NoticeRequest;
import com.aurora.admin.dto.NoticeResponse;
import com.aurora.admin.dto.PageResult;
import com.aurora.admin.entity.User;
import com.aurora.admin.exception.NotFoundException;
import com.aurora.admin.service.NoticeService;
import com.aurora.admin.service.UserService;
import com.aurora.admin.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 公告管理控制器。提供公告的增删改查、发布、撤回等 REST API。
 * 管理端接口需要 ADMIN/SUPER_ADMIN 权限，用户端接口仅需登录认证。
 * 基础路径：/api/notices
 */
@RestController
@RequestMapping("/api/notices")
@RequiredArgsConstructor
public class NoticeController {

    private final NoticeService noticeService;
    private final UserService userService;

    /**
     * 获取管理后台公告分页列表。支持按状态和关键字筛选，管理员可查看所有公告不受可见范围限制。
     *
     * @param page    页码，默认 1
     * @param size    每页条数，默认 10
     * @param status  公告状态筛选（DRAFT / PUBLISHED / EXPIRED / WITHDRAWN），为空则查询全部
     * @param keyword 标题关键字模糊搜索，为空则不限
     * @return 公告分页结果
     */
    @GetMapping("/manage")
    public ApiResponse getManageList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "") String status,
            @RequestParam(defaultValue = "") String keyword) {
        PageResult<NoticeResponse> result = noticeService.findAllForManage(page, size, status, keyword);
        return ApiResponse.success(result);
    }

    /**
     * 获取当前用户可见的公告列表。根据公告目标范围（ALL / DEPT / USER）过滤已发布且未过期的公告。
     *
     * @return 当前用户可见的公告列表
     */
    @GetMapping
    public ApiResponse getVisibleNotices() {
        Long userId = SecurityUtils.getCurrentUserId();
        User user = userService.findById(userId);
        if (user == null) {
            throw new NotFoundException("用户", userId);
        }
        List<NoticeResponse> list = noticeService.getVisibleNotices(userId, user.getDeptId());
        return ApiResponse.success(list);
    }

    /**
     * 根据 ID 获取公告详情。公告不存在则返回 404。
     *
     * @param id 公告 ID
     * @return 公告详情
     */
    @GetMapping("/{id}")
    public ApiResponse getById(@PathVariable Long id) {
        NoticeResponse notice = noticeService.findById(id);
        if (notice == null) {
            throw new NotFoundException("公告", id);
        }
        return ApiResponse.success(notice);
    }

    /**
     * 创建公告。新建公告默认为草稿状态，需要 ADMIN/SUPER_ADMIN 权限，限流 5次/分钟。
     *
     * @param request 公告创建请求（标题、内容、目标范围等）
     * @return 创建的公告信息
     */
    @RateLimit(key = KeyType.USER, limit = 5, duration = 60, message = "创建公告过于频繁，请稍后再试")
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ApiResponse create(@RequestBody NoticeRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        NoticeResponse notice = noticeService.create(request, userId);
        return ApiResponse.success(notice);
    }

    /**
     * 更新公告。仅可编辑草稿或已撤回状态的公告，已发布或已过期的公告不可编辑。需要 ADMIN/SUPER_ADMIN 权限。
     *
     * @param id      公告 ID
     * @param request 公告更新内容
     * @return 更新后的公告信息
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ApiResponse update(@PathVariable Long id, @RequestBody NoticeRequest request) {
        NoticeResponse notice = noticeService.update(id, request);
        return ApiResponse.success(notice);
    }

    /**
     * 删除公告。仅可删除非发布状态的公告（草稿、已撤回、已过期），已发布的公告不可删除。需要 ADMIN/SUPER_ADMIN 权限。
     *
     * @param id 公告 ID
     * @return 删除成功提示
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ApiResponse delete(@PathVariable Long id) {
        noticeService.delete(id);
        return ApiResponse.success("删除成功");
    }

    /**
     * 发布公告。将草稿或已撤回的公告设为已发布状态，若设置了定时发布时间则按计划执行。需要 ADMIN/SUPER_ADMIN 权限。
     *
     * @param id 公告 ID
     * @return 发布后的公告信息
     */
    @PutMapping("/{id}/publish")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ApiResponse publish(@PathVariable Long id) {
        NoticeResponse notice = noticeService.publish(id);
        return ApiResponse.success(notice);
    }

    /**
     * 撤回公告。将已发布的公告撤回为已撤回状态，撤回后可重新编辑和发布。需要 ADMIN/SUPER_ADMIN 权限。
     *
     * @param id 公告 ID
     * @return 撤回后的公告信息
     */
    @PutMapping("/{id}/withdraw")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ApiResponse withdraw(@PathVariable Long id) {
        NoticeResponse notice = noticeService.withdraw(id);
        return ApiResponse.success(notice);
    }
}
