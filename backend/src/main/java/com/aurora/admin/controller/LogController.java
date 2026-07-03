package com.aurora.admin.controller;

import com.aurora.admin.annotation.RateLimit;
import com.aurora.admin.annotation.RateLimit.KeyType;
import com.aurora.admin.dto.ApiResponse;
import com.aurora.admin.dto.BatchDeleteRequest;
import com.aurora.admin.entity.OperationLog;
import com.aurora.admin.service.OperationLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 操作日志管理控制器。提供日志查询、批量删除、定时清理等 REST 接口。
 * 日志记录用户操作轨迹，支持按日期范围筛选，超期日志自动清理。
 */
@RestController
@RequestMapping("/api/logs")
public class LogController {

    @Autowired
    private OperationLogService operationLogService;

    /**
     * 分页查询操作日志。支持按日期范围筛选；若未传日期参数则返回全部日志。
     *
     * @param page     页码，默认 1
     * @param size     每页条数，默认 10
     * @param startDate 起始日期（含），格式 yyyy-MM-dd，为空时不按起始日期过滤
     * @param endDate   截止日期（含），格式 yyyy-MM-dd，为空时不按截止日期过滤
     * @return 包含日志列表、总数、页码、每页条数的分页结果
     */
    @GetMapping
    public ApiResponse getLogs(@RequestParam(defaultValue = "1") int page,
                               @RequestParam(defaultValue = "10") int size,
                               @RequestParam(defaultValue = "") String startDate,
                               @RequestParam(defaultValue = "") String endDate) {
        List<OperationLog> logs;
        long total;
        if (!startDate.isEmpty() && !endDate.isEmpty()) {
            logs = operationLogService.getPageByDateRange(page, size, startDate, endDate);
            total = operationLogService.getCountByDateRange(startDate, endDate);
        } else {
            logs = operationLogService.getPage(page, size);
            total = operationLogService.getTotalCount();
        }

        Map<String, Object> result = new HashMap<>();
        result.put("list", logs);
        result.put("total", total);
        result.put("page", page);
        result.put("size", size);

        return ApiResponse.success(result);
    }

    /**
     * 清理指定天数之前的旧操作日志（物理删除）。限流：每用户每 5 分钟 1 次。
     *
     * @param days 保留最近 N 天的日志，之前的将被删除，默认 30 天
     * @return 清理成功消息
     */
    @RateLimit(key = KeyType.USER, limit = 1, duration = 300, message = "日志清理过于频繁，请5分钟后再试")
    @DeleteMapping("/clean")
    public ApiResponse cleanLogs(@RequestParam(defaultValue = "30") int days) {
        operationLogService.cleanOldLogs(days);
        return ApiResponse.success("清理成功");
    }

    /**
     * 批量删除指定 ID 的操作日志。限流：每用户每分钟 3 次。
     *
     * @param request 包含待删除日志 ID 列表的请求体
     * @return 删除成功消息
     */
    @RateLimit(key = KeyType.USER, limit = 3, duration = 60, message = "批量删除过于频繁，请稍后再试")
    @DeleteMapping("/batch")
    public ApiResponse batchDeleteLogs(@RequestBody BatchDeleteRequest request) {
        operationLogService.deleteByIds(request.ids());
        return ApiResponse.success("删除成功");
    }
}
