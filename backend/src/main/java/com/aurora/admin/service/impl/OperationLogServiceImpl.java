package com.aurora.admin.service.impl;

import com.aurora.admin.entity.OperationLog;
import com.aurora.admin.mapper.OperationLogMapper;
import com.aurora.admin.service.OperationLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class OperationLogServiceImpl implements OperationLogService {

    @Autowired
    private OperationLogMapper operationLogMapper;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public void saveLog(OperationLog log) {
        operationLogMapper.insert(log);
    }

    @Override
    public List<OperationLog> getPage(int page, int size) {
        int offset = (page - 1) * size;
        return operationLogMapper.findPage(offset, size);
    }

    @Override
    public long getTotalCount() {
        return operationLogMapper.count();
    }

    @Override
    public List<OperationLog> getPageByDateRange(int page, int size, String startDate, String endDate) {
        int offset = (page - 1) * size;
        return operationLogMapper.findPageByDateRange(offset, size, startDate, endDate);
    }

    @Override
    public long getCountByDateRange(String startDate, String endDate) {
        return operationLogMapper.countByDateRange(startDate, endDate);
    }

    @Override
    public void cleanOldLogs(int days) {
        LocalDateTime beforeTime = LocalDateTime.now().minusDays(days);
        operationLogMapper.deleteBeforeTime(beforeTime.format(FORMATTER));
    }

    @Override
    public void deleteByIds(List<Long> ids) {
        operationLogMapper.deleteByIds(ids);
    }
}
