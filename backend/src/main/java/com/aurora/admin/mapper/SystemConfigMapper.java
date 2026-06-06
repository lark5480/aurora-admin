package com.aurora.admin.mapper;

import com.aurora.admin.entity.SystemConfig;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface SystemConfigMapper {

    @Select("SELECT * FROM t_system_config WHERE is_visible = 1 AND is_deleted = 0")
    List<SystemConfig> findAllVisible();

    @Select("SELECT * FROM t_system_config WHERE is_deleted = 0")
    List<SystemConfig> findAll();

    @Select("SELECT * FROM t_system_config WHERE config_key = #{key} AND is_deleted = 0")
    SystemConfig findByKey(String key);

    @Insert("INSERT INTO t_system_config(config_key, config_value, config_type, config_name, config_group, description, is_visible) " +
            "VALUES(#{configKey}, #{configValue}, #{configType}, #{configName}, #{configGroup}, #{description}, #{isVisible})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(SystemConfig config);

    @Update("UPDATE t_system_config SET config_value = #{configValue}, update_time = NOW() WHERE config_key = #{configKey}")
    int updateByKey(@Param("configKey") String configKey, @Param("configValue") String configValue);

    @Update("UPDATE t_system_config SET config_value = #{config.configValue}, config_name = #{config.configName}, " +
            "config_type = #{config.configType}, description = #{config.description}, " +
            "config_group = #{config.configGroup}, is_visible = #{config.isVisible}, " +
            "update_time = NOW() WHERE config_key = #{configKey}")
    int updateByKeyFull(@Param("configKey") String configKey, @Param("config") SystemConfig config);

    @Update("UPDATE t_system_config SET is_deleted = 1 WHERE config_key = #{key}")
    int deleteByKey(String key);
}
