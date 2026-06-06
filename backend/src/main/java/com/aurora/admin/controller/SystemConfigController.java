package com.aurora.admin.controller;

import com.aurora.admin.annotation.RateLimit;
import com.aurora.admin.annotation.RateLimit.KeyType;
import com.aurora.admin.config.ConfigCache;
import com.aurora.admin.dto.ApiResponse;
import com.aurora.admin.entity.SystemConfig;
import com.aurora.admin.service.SystemConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/config")
public class SystemConfigController {

    @Autowired
    private SystemConfigService systemConfigService;

    @Autowired
    private ConfigCache configCache;

    @GetMapping
    public ApiResponse getAllVisible() {
        List<SystemConfig> configs = systemConfigService.getAllVisible();
        return ApiResponse.success(configs);
    }

    @GetMapping("/all")
    public ApiResponse getAll() {
        List<SystemConfig> configs = systemConfigService.getAll();
        return ApiResponse.success(configs);
    }

    @GetMapping("/{key}")
    public ApiResponse getByKey(@PathVariable String key) {
        SystemConfig config = systemConfigService.getByKey(key);
        if (config == null) {
            return ApiResponse.error(404, "配置不存在");
        }
        return ApiResponse.success(config);
    }

    @RateLimit(key = KeyType.USER, limit = 10, duration = 60, message = "配置更新过于频繁，请稍后再试")
    @PutMapping("/{key}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ApiResponse updateByKey(@PathVariable String key, @RequestBody SystemConfig config) {
        if (config.getDescription() != null) {
            boolean success = systemConfigService.updateByKeyFull(key, config);
            if (success) {
                configCache.refresh();
                return ApiResponse.success("更新成功");
            }
            return ApiResponse.error(400, "更新失败");
        }
        boolean success = systemConfigService.updateByKey(key, config.getConfigValue());
        if (success) {
            configCache.refresh();
            return ApiResponse.success("更新成功");
        }
        return ApiResponse.error(400, "更新失败");
    }

    @RateLimit(key = KeyType.USER, limit = 5, duration = 60, message = "创建配置过于频繁，请稍后再试")
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ApiResponse create(@RequestBody SystemConfig config) {
        boolean success = systemConfigService.create(config);
        if (success) {
            configCache.refresh();
            return ApiResponse.success("创建成功");
        }
        return ApiResponse.error(400, "创建失败");
    }

    @RateLimit(key = KeyType.USER, limit = 3, duration = 60, message = "删除配置过于频繁，请稍后再试")
    @DeleteMapping("/{key}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ApiResponse deleteByKey(@PathVariable String key) {
        boolean success = systemConfigService.deleteByKey(key);
        if (success) {
            configCache.refresh();
            return ApiResponse.success("删除成功");
        }
        return ApiResponse.error(400, "删除失败");
    }

    @GetMapping("/public")
    public ApiResponse getPublicConfigs() {
        List<SystemConfig> configs = systemConfigService.getAllVisible();
        Map<String, String> result = new HashMap<>();
        for (SystemConfig config : configs) {
            if (config.getConfigValue() != null) {
                result.put(config.getConfigKey(), config.getConfigValue());
            }
        }
        return ApiResponse.success(result);
    }
}
