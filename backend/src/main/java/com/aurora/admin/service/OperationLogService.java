package com.aurora.admin.service;

import com.aurora.admin.entity.OperationLog;

import java.util.List;

public interface OperationLogService {

    /**
     * 保存操作日志。记录用户操作轨迹，包括操作人、操作类型、请求参数等。
     *
     * @param log 操作日志实体
     */
    void saveLog(OperationLog log);

    /**
     * 分页查询全部操作日志，按创建时间降序排列。
     *
     * @param page 页码，从 1 开始
     * @param size 每页条数
     * @return 当前页的日志列表
     */
    List<OperationLog> getPage(int page, int size);

    /**
     * 获取操作日志总条数。
     *
     * @return 日志总数
     */
    long getTotalCount();

    /**
     * 按日期范围分页查询操作日志（含起止日期），按创建时间降序排列。
     *
     * @param page      页码，从 1 开始
     * @param size      每页条数
     * @param startDate 起始日期，格式 yyyy-MM-dd
     * @param endDate   截止日期，格式 yyyy-MM-dd
     * @return 当前页的日志列表
     */
    List<OperationLog> getPageByDateRange(int page, int size, String startDate, String endDate);

    /**
     * 统计指定日期范围内的操作日志数量（含起止日期）。
     *
     * @param startDate 起始日期，格式 yyyy-MM-dd
     * @param endDate   截止日期，格式 yyyy-MM-dd
     * @return 日志数量
     */
    long getCountByDateRange(String startDate, String endDate);

    /**
     * 清理指定天数之前的所有操作日志（物理删除）。
     *
     * @param days 保留最近 N 天的日志，之前的将被删除
     */
    void cleanOldLogs(int days);

    /**
     * 批量删除指定 ID 的操作日志。
     *
     * @param ids 待删除的日志 ID 列表
     */
    void deleteByIds(List<Long> ids);
}
