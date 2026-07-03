package com.aurora.admin.service.impl;

import com.aurora.admin.entity.SystemConfig;
import com.aurora.admin.mapper.SystemConfigMapper;
import com.aurora.admin.service.SystemConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 系统配置服务实现类。基于 MyBatis-Plus Mapper 完成系统配置的 CRUD 操作，
 * 支持按可见性查询、按键精确查找，以及更新时自动创建不存在的配置。
 */
@Service
public class SystemConfigServiceImpl implements SystemConfigService {

    @Autowired
    private SystemConfigMapper systemConfigMapper;

    @Override
    public List<SystemConfig> getAllVisible() {
        return systemConfigMapper.findAllVisible();
    }

    @Override
    public List<SystemConfig> getAll() {
        return systemConfigMapper.findAll();
    }

    @Override
    public SystemConfig getByKey(String key) {
        return systemConfigMapper.findByKey(key);
    }

    @Override
    public boolean updateByKey(String key, String value) {
        SystemConfig config = systemConfigMapper.findByKey(key);
        if (config == null) {
            SystemConfig newConfig = new SystemConfig();
            newConfig.setConfigKey(key);
            newConfig.setConfigValue(value);
            newConfig.setConfigType("string");
            newConfig.setConfigName(key);
            newConfig.setConfigGroup("default");
            newConfig.setDescription("");
            newConfig.setIsVisible(1);
            return systemConfigMapper.insert(newConfig) > 0;
        }
        return systemConfigMapper.updateByKey(key, value) > 0;
    }

    public boolean updateByKeyFull(String key, SystemConfig newConfig) {
        SystemConfig config = systemConfigMapper.findByKey(key);
        if (config == null) {
            newConfig.setConfigKey(key);
            newConfig.setIsVisible(1);
            return systemConfigMapper.insert(newConfig) > 0;
        }
        return systemConfigMapper.updateByKeyFull(key, newConfig) > 0;
    }

    @Override
    public boolean create(SystemConfig config) {
        return systemConfigMapper.insert(config) > 0;
    }

    @Override
    @Transactional
    public boolean deleteByKey(String key) {
        // 操作日志由 OperationLogAspect 切面统一记录，Service 层不再手动插入
        return systemConfigMapper.deleteByKey(key) > 0;
    }
}
