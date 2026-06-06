package com.aurora.admin.service;

import com.aurora.admin.entity.Menu;
import com.aurora.admin.entity.OperationLog;
import com.aurora.admin.mapper.MenuMapper;
import com.aurora.admin.mapper.OperationLogMapper;
import com.aurora.admin.mapper.RoleMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MenuServiceImpl implements MenuService {

    private static final Logger log = LoggerFactory.getLogger(MenuServiceImpl.class);

    @Autowired
    private MenuMapper menuMapper;

    @Autowired
    private RoleMapper roleMapper;

    @Autowired
    private OperationLogMapper operationLogMapper;

    @Override
    public List<Menu> findAll() {
        return menuMapper.findAll();
    }

    @Override
    public Menu findById(Long id) {
        return menuMapper.findById(id);
    }

    @Override
    public List<Menu> findTree() {
        List<Menu> allMenus = menuMapper.findAll();
        return buildTree(allMenus, 0L);
    }

    @Override
    public List<Menu> findTreeForCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        log.debug("findTreeForCurrentUser called, auth: {}", auth);
        log.debug("Principal: {}, isAuthenticated: {}", auth != null ? auth.getPrincipal() : "null",
                auth != null ? auth.isAuthenticated() : "false");

        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            log.warn("User not authenticated or is anonymous, returning empty menu list");
            return List.of();
        }

        List<String> roleCodes = auth.getAuthorities().stream()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .collect(Collectors.toList());
        log.debug("User roles from SecurityContext: {}", roleCodes);

        if (roleCodes.contains("SUPER_ADMIN")) {
            List<Menu> allMenus = menuMapper.findAll();
            log.debug("SUPER_ADMIN: returning all {} menus", allMenus.size());
            return buildTree(allMenus, 0L);
        }

        if (roleCodes.isEmpty()) {
            log.warn("No roles found for user, returning empty menu list");
            return List.of();
        }

        List<Long> roleIds = roleMapper.findIdsByCodes(roleCodes);
        log.debug("Role IDs from database: {}", roleIds);

        if (roleIds.isEmpty()) {
            log.warn("No role IDs found for codes: {}, returning empty menu list", roleCodes);
            return List.of();
        }

        List<Menu> userMenus = menuMapper.findByRoleIds(roleIds);
        log.debug("Menus found for user: {}", userMenus.size());
        return buildTree(userMenus, 0L);
    }

    private List<Menu> buildTree(List<Menu> menus, Long parentId) {
        return menus.stream()
                .filter(m -> m.getParentId().equals(parentId))
                .peek(m -> m.setChildren(buildTree(menus, m.getId())))
                .collect(Collectors.toList());
    }

    @Override
    public List<String> findPermissionsForCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return List.of();
        }

        List<String> roleCodes = auth.getAuthorities().stream()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .collect(Collectors.toList());

        if (roleCodes.contains("SUPER_ADMIN")) {
            List<Menu> allMenus = menuMapper.findAll();
            return allMenus.stream()
                    .filter(m -> m.getPermission() != null && !m.getPermission().isBlank())
                    .map(Menu::getPermission)
                    .distinct()
                    .collect(Collectors.toList());
        }

        if (roleCodes.isEmpty()) {
            return List.of();
        }

        List<Long> roleIds = roleMapper.findIdsByCodes(roleCodes);
        if (roleIds.isEmpty()) {
            return List.of();
        }

        List<Menu> userMenus = menuMapper.findByRoleIds(roleIds);
        return userMenus.stream()
                .filter(m -> m.getPermission() != null && !m.getPermission().isBlank())
                .map(Menu::getPermission)
                .distinct()
                .collect(Collectors.toList());
    }

    @Override
    public List<Menu> findByParentId(Long parentId) {
        return menuMapper.findByParentId(parentId);
    }

    @Override
    public List<Menu> findByRoleId(Long roleId) {
        return menuMapper.findByRoleId(roleId);
    }

    @Override
    public Menu create(Menu menu) {
        menuMapper.insert(menu);
        return menu;
    }

    @Override
    public Menu update(Long id, Menu menu) {
        Menu existing = menuMapper.findById(id);
        if (existing == null) {
            throw new RuntimeException("菜单不存在");
        }
        menu.setId(id);
        menuMapper.update(menu);
        return menuMapper.findById(id);
    }

    @Override
    public void delete(Long id) {
        Menu menu = menuMapper.findById(id);
        if (menu == null) {
            throw new RuntimeException("菜单不存在");
        }
        int childCount = menuMapper.countByParentId(id);
        if (childCount > 0) {
            throw new RuntimeException("请先删除子菜单");
        }
        String resourceDesc = "id=" + menu.getId() + ", name=" + menu.getName();

        menuMapper.delete(id);

        OperationLog operationLog = new OperationLog();
        operationLog.setUserId(getCurrentUserId());
        operationLog.setUsername(getCurrentUsername());
        operationLog.setOperation("删除菜单");
        operationLog.setMethod("DELETE");
        operationLog.setUrl("/api/menus/" + id);
        operationLog.setParams(resourceDesc);
        operationLog.setStatus(1);
        operationLog.setCreateTime(LocalDateTime.now());
        operationLogMapper.insert(operationLog);
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }
        Object credentials = auth.getCredentials();
        if (credentials instanceof Long id) {
            return id;
        }
        if (credentials instanceof Number n) {
            return n.longValue();
        }
        return null;
    }

    private String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return "unknown";
        }
        return auth.getName();
    }
}
