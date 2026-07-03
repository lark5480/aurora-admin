package com.aurora.admin.service;

import com.aurora.admin.entity.SystemConfig;

import java.util.List;

public interface SystemConfigService {

    /**
     * 获取所有可见的系统配置项（isVisible = 1）。
     *
     * @return 可见配置项列表
     */
    List<SystemConfig> getAllVisible();

    /**
     * 获取全部系统配置项（含隐藏项）。
     *
     * @return 全部配置项列表
     */
    List<SystemConfig> getAll();

    /**
     * 根据配置键查询配置项。
     *
     * @param key 配置键
     * @return 配置项，不存在返回 null
     */
    SystemConfig getByKey(String key);

    /**
     * 更新指定配置项的值。如果键不存在则自动创建新配置项。
     *
     * @param key   配置键
     * @param value 配置值
     * @return 成功返回 true
     */
    boolean updateByKey(String key, String value);

    /**
     * 全量更新指定配置项（含描述、类型等全部字段）。键不存在则自动创建。
     *
     * @param key       配置键
     * @param newConfig 新的配置内容
     * @return 成功返回 true
     */
    boolean updateByKeyFull(String key, SystemConfig newConfig);

    /**
     * 创建新的系统配置项。
     *
     * @param config 配置项
     * @return 成功返回 true
     */
    boolean create(SystemConfig config);

    /**
     * 根据配置键删除配置项。
     *
     * @param key 配置键
     * @return 成功返回 true
     */
    boolean deleteByKey(String key);
}
