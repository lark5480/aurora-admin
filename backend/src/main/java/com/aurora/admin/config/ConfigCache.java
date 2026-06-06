package com.aurora.admin.config;

import com.aurora.admin.entity.SystemConfig;
import com.aurora.admin.mapper.SystemConfigMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ConfigCache {

    private final SystemConfigMapper systemConfigMapper;
    private final ConcurrentHashMap<String, String> cache = new ConcurrentHashMap<>();

    public ConfigCache(SystemConfigMapper systemConfigMapper) {
        this.systemConfigMapper = systemConfigMapper;
    }

    @PostConstruct
    public void init() {
        refresh();
    }

    /** 从数据库重新加载所有配置 */
    public void refresh() {
        List<SystemConfig> configs = systemConfigMapper.findAll();
        for (SystemConfig config : configs) {
            if (config.getConfigValue() != null) {
                cache.put(config.getConfigKey(), config.getConfigValue());
            }
        }
    }

    public String getString(String key) {
        return cache.get(key);
    }

    public String getString(String key, String defaultValue) {
        return cache.getOrDefault(key, defaultValue);
    }

    public int getInt(String key) {
        String value = cache.get(key);
        if (value == null || value.isEmpty()) {
            return 0;
        }
        return Integer.parseInt(value);
    }

    public int getInt(String key, int defaultValue) {
        String value = cache.get(key);
        if (value == null || value.isEmpty()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public boolean getBoolean(String key) {
        String value = cache.get(key);
        return "true".equalsIgnoreCase(value) || "1".equals(value);
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        String value = cache.get(key);
        if (value == null || value.isEmpty()) {
            return defaultValue;
        }
        return "true".equalsIgnoreCase(value) || "1".equals(value);
    }
}
