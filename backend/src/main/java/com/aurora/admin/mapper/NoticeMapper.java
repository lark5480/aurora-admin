package com.aurora.admin.mapper;

import java.time.LocalDateTime;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.aurora.admin.entity.Notice;

@Mapper
public interface NoticeMapper extends BaseMapper<Notice> {
    
    @Select("SELECT * FROM t_notice WHERE status = #{status} AND publish_time <= #{now} AND publish_time IS NOT NULL AND is_deleted = 0")
    List<Notice> findByStatusAndPublishTimeBefore(@Param("status") String status, @Param("now") LocalDateTime now);

    @Select("SELECT * FROM t_notice WHERE status = #{status} AND expire_time <= #{now} AND expire_time IS NOT NULL AND is_deleted = 0")
    List<Notice> findByStatusAndExpireTimeBefore(@Param("status") String status, @Param("now") LocalDateTime now);
}
