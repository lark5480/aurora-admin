package com.aurora.admin.mapper;

import com.aurora.admin.entity.DailyStats;
import org.apache.ibatis.annotations.*;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface DailyStatsMapper {

    /**
     * 插入或更新每日统计记录（基于 stat_date + stat_type 唯一键）
     */
    @Insert("INSERT INTO t_daily_stats(stat_date, stat_type, stat_count) " +
            "VALUES(#{statDate}, #{statType}, #{statCount}) " +
            "ON DUPLICATE KEY UPDATE stat_count = #{statCount}")
    int upsert(DailyStats stats);

    /**
     * 批量查询日期范围内的统计记录
     */
    @Select("SELECT * FROM t_daily_stats WHERE stat_date >= #{startDate} AND stat_date <= #{endDate} AND is_deleted = 0 ORDER BY stat_date, stat_type")
    List<DailyStats> findByDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /**
     * 查询指定日期、指定类型的统计记录
     */
    @Select("SELECT * FROM t_daily_stats WHERE stat_date = #{date} AND stat_type = #{type} AND is_deleted = 0")
    DailyStats findByDateAndType(@Param("date") LocalDate date, @Param("type") String type);
}
