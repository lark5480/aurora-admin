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

/**
 * 系统配置控制器。提供系统配置项的查询、新增、修改、删除接口。
 * 默认路径 /api/config，部分接口需要 ADMIN / SUPER_ADMIN 角色。
 */
@RestController
@RequestMapping("/api/config")
public class SystemConfigController {

    @Autowired
    private SystemConfigService systemConfigService;

    @Autowired
    private ConfigCache configCache;

    /**
     * 获取所有可见配置项。返回 isVisible = 1 的配置列表，无需登录。
     */
    @GetMapping
    public ApiResponse getAllVisible() {
        List<SystemConfig> configs = systemConfigService.getAllVisible();
        return ApiResponse.success(configs);
    }

    /**
     * 获取全部配置项（含隐藏项）。需要 ADMIN / SUPER_ADMIN 权限。
     */
    @GetMapping("/all")
    public ApiResponse getAll() {
        List<SystemConfig> configs = systemConfigService.getAll();
        return ApiResponse.success(configs);
    }

    /**
     * 根据配置键查询配置项。键不存在时返回 404。
     *
     * @param key 配置键
     */
    @GetMapping("/{key}")
    public ApiResponse getByKey(@PathVariable String key) {
        SystemConfig config = systemConfigService.getByKey(key);
        if (config == null) {
            return ApiResponse.error(404, "配置不存在");
        }
        return ApiResponse.success(config);
    }

    /**
     * 更新指定配置项的值。请求体携带 description 时为全量更新，否则仅更新 configValue。
     * 需要 ADMIN / SUPER_ADMIN 权限，限流 10 次/用户/分钟。
     *
     * @param key    配置键
     * @param config 配置内容（可部分更新）
     */
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

    /**
     * 创建新的系统配置项。需要 ADMIN / SUPER_ADMIN 权限，限流 5 次/用户/分钟。
     *
     * @param config 配置项信息
     */
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

    /**
     * 根据配置键删除配置项。需要 ADMIN / SUPER_ADMIN 权限，限流 3 次/用户/分钟。
     *
     * @param key 配置键
     */
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

    /**
     * 获取公开配置（键值对形式）。将可见配置列表转换为 Map<String, String> 返回，无需登录。
     */
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
