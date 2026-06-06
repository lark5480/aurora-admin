package com.aurora.admin.config;

import com.aurora.admin.entity.Menu;
import com.aurora.admin.mapper.MenuMapper;
import com.aurora.admin.mapper.RoleMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 应用启动时的数据维护。
 * 基础数据（角色、用户、菜单、部门）由 schema.sql 负责初始化，
 * 本类只处理需要在运行时做增量同步的逻辑。
 */
@Component
public class DataMigration implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataMigration.class);

    @Autowired
    private RoleMapper roleMapper;

    @Autowired
    private MenuMapper menuMapper;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        syncSuperAdminMenus();
    }

    /**
     * 确保 SUPER_ADMIN 拥有全部菜单权限。
     * 场景：schema.sql INSERT IGNORE 只在建库时生效，
     * 后续新增菜单不会自动分配给 SUPER_ADMIN，这里兜底同步。
     */
    private void syncSuperAdminMenus() {
        var superAdmin = roleMapper.findByCode("SUPER_ADMIN");
        if (superAdmin == null) {
            log.info("SUPER_ADMIN 角色不存在，跳过菜单同步");
            return;
        }

        List<Menu> allMenus = menuMapper.findAll();
        if (allMenus.isEmpty()) {
            return;
        }

        List<Long> allMenuIds = allMenus.stream().map(Menu::getId).toList();
        List<Long> existingMenuIds = roleMapper.findMenuIdsByRoleId(superAdmin.getId());

        if (existingMenuIds.size() == allMenuIds.size() && existingMenuIds.containsAll(allMenuIds)) {
            return;
        }

        roleMapper.deleteRoleMenus(superAdmin.getId());
        roleMapper.insertRoleMenus(superAdmin.getId(), allMenuIds);
        log.info("SUPER_ADMIN 菜单权限已同步: {} 个菜单", allMenuIds.size());
    }
}
