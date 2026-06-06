package com.aurora.admin.service;

import com.aurora.admin.dto.NoticeRequest;
import com.aurora.admin.dto.NoticeResponse;
import com.aurora.admin.dto.PageResult;

public interface NoticeService {
    /**
     * 管理列表 - 管理员查看所有公告，不受权限筛选
     */
    PageResult<NoticeResponse> findAllForManage(int page, int size, String status, String keyword);

    /**
     * 获取公告详情
     */
    NoticeResponse findById(Long id);

    /**
     * 创建公告
     */
    NoticeResponse create(NoticeRequest request, Long createBy);

    /**
     * 更新公告
     */
    NoticeResponse update(Long id, NoticeRequest request);

    /**
     * 删除公告
     */
    void delete(Long id);

    /**
     * 发布公告（含定时）
     */
    NoticeResponse publish(Long id);

    /**
     * 撤回公告
     */
    NoticeResponse withdraw(Long id);

    /**
     * 定时任务：处理定时发布和过期
     */
    void processScheduledNotices();

    /**
     * 获取当前用户可见的公告列表
     */
    java.util.List<NoticeResponse> getVisibleNotices(Long userId, Long deptId);
}
