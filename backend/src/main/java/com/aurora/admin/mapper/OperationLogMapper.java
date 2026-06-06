package com.aurora.admin.mapper;

import com.aurora.admin.entity.OperationLog;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface OperationLogMapper {

    @Insert("INSERT INTO t_operation_log(user_id, username, operation, method, url, ip, params, status, duration_ms) " +
            "VALUES(#{userId}, #{username}, #{operation}, #{method}, #{url}, #{ip}, #{params}, #{status}, #{durationMs})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(OperationLog log);

    @Select("SELECT * FROM t_operation_log WHERE is_deleted = 0 ORDER BY create_time DESC LIMIT #{offset}, #{limit}")
    List<OperationLog> findPage(@Param("offset") int offset, @Param("limit") int limit);

    @Select("SELECT COUNT(*) FROM t_operation_log WHERE is_deleted = 0")
    long count();

    @Select("SELECT * FROM t_operation_log WHERE create_time < #{beforeTime} AND is_deleted = 0 ORDER BY create_time DESC")
    List<OperationLog> findBeforeTime(String beforeTime);

    @Update("UPDATE t_operation_log SET is_deleted = 1 WHERE create_time < #{beforeTime}")
    int deleteBeforeTime(String beforeTime);

    @Select("SELECT * FROM t_operation_log WHERE user_id = #{userId} AND is_deleted = 0 ORDER BY create_time DESC LIMIT #{offset}, #{limit}")
    List<OperationLog> findByUserId(@Param("userId") Long userId, @Param("offset") int offset, @Param("limit") int limit);

    @Select("SELECT * FROM t_operation_log WHERE create_time >= #{startDate} AND create_time < DATE_ADD(#{endDate}, INTERVAL 1 DAY) AND is_deleted = 0 ORDER BY create_time DESC LIMIT #{offset}, #{limit}")
    List<OperationLog> findPageByDateRange(@Param("offset") int offset, @Param("limit") int limit,
                                            @Param("startDate") String startDate, @Param("endDate") String endDate);

    @Select("SELECT COUNT(*) FROM t_operation_log WHERE create_time >= #{startDate} AND create_time < DATE_ADD(#{endDate}, INTERVAL 1 DAY) AND is_deleted = 0")
    long countByDateRange(@Param("startDate") String startDate, @Param("endDate") String endDate);

    @Update("<script>UPDATE t_operation_log SET is_deleted = 1 WHERE id IN <foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach></script>")
    int deleteByIds(@Param("ids") List<Long> ids);
}
