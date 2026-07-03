package com.aurora.admin.service.impl;

import com.aurora.admin.entity.OperationLog;
import com.aurora.admin.mapper.OperationLogMapper;
import com.aurora.admin.service.OperationLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 操作日志服务实现类。基于 MyBatis-Plus Mapper 完成日志的 CRUD 操作，
 * 支持的查询维度：全量分页查询和按日期范围筛选。
 */
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

    /**
     * 清理日志：计算当前时间向前 N 天的截止时间点，删除该时间之前的所有日志。
     * 格式化为 yyyy-MM-dd HH:mm:ss 后传给 Mapper 执行物理删除。
     *
     * @param days 保留最近 N 天的日志
     */
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
