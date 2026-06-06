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

@RestController
@RequestMapping("/api/logs")
public class LogController {

    @Autowired
    private OperationLogService operationLogService;

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

    @RateLimit(key = KeyType.USER, limit = 1, duration = 300, message = "日志清理过于频繁，请5分钟后再试")
    @DeleteMapping("/clean")
    public ApiResponse cleanLogs(@RequestParam(defaultValue = "30") int days) {
        operationLogService.cleanOldLogs(days);
        return ApiResponse.success("清理成功");
    }

    @RateLimit(key = KeyType.USER, limit = 3, duration = 60, message = "批量删除过于频繁，请稍后再试")
    @DeleteMapping("/batch")
    public ApiResponse batchDeleteLogs(@RequestBody BatchDeleteRequest request) {
        operationLogService.deleteByIds(request.ids());
        return ApiResponse.success("删除成功");
    }
}
