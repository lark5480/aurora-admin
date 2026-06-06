package com.aurora.admin.service;

import com.aurora.admin.entity.SystemConfig;

import java.util.List;

public interface SystemConfigService {
    List<SystemConfig> getAllVisible();
    List<SystemConfig> getAll();
    SystemConfig getByKey(String key);
    boolean updateByKey(String key, String value);
    boolean updateByKeyFull(String key, SystemConfig newConfig);
    boolean create(SystemConfig config);
    boolean deleteByKey(String key);
}
