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

@RestController
@RequestMapping("/api/notices")
@RequiredArgsConstructor
public class NoticeController {

    private final NoticeService noticeService;
    private final UserService userService;

    @GetMapping("/manage")
    public ApiResponse getManageList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "") String status,
            @RequestParam(defaultValue = "") String keyword) {
        PageResult<NoticeResponse> result = noticeService.findAllForManage(page, size, status, keyword);
        return ApiResponse.success(result);
    }

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

    @GetMapping("/{id}")
    public ApiResponse getById(@PathVariable Long id) {
        NoticeResponse notice = noticeService.findById(id);
        if (notice == null) {
            throw new NotFoundException("公告", id);
        }
        return ApiResponse.success(notice);
    }

    @RateLimit(key = KeyType.USER, limit = 5, duration = 60, message = "创建公告过于频繁，请稍后再试")
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ApiResponse create(@RequestBody NoticeRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        NoticeResponse notice = noticeService.create(request, userId);
        return ApiResponse.success(notice);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ApiResponse update(@PathVariable Long id, @RequestBody NoticeRequest request) {
        NoticeResponse notice = noticeService.update(id, request);
        return ApiResponse.success(notice);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ApiResponse delete(@PathVariable Long id) {
        noticeService.delete(id);
        return ApiResponse.success("删除成功");
    }

    @PutMapping("/{id}/publish")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ApiResponse publish(@PathVariable Long id) {
        NoticeResponse notice = noticeService.publish(id);
        return ApiResponse.success(notice);
    }

    @PutMapping("/{id}/withdraw")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ApiResponse withdraw(@PathVariable Long id) {
        NoticeResponse notice = noticeService.withdraw(id);
        return ApiResponse.success(notice);
    }
}
