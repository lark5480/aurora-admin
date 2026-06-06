package com.aurora.admin.service;

import com.aurora.admin.entity.OperationLog;

import java.util.List;

public interface OperationLogService {
    void saveLog(OperationLog log);
    List<OperationLog> getPage(int page, int size);
    long getTotalCount();
    List<OperationLog> getPageByDateRange(int page, int size, String startDate, String endDate);
    long getCountByDateRange(String startDate, String endDate);
    void cleanOldLogs(int days);
    void deleteByIds(List<Long> ids);
}
