package com.aurora.admin.service;

import com.aurora.admin.entity.OperationLog;
import com.aurora.admin.entity.Role;
import com.aurora.admin.mapper.OperationLogMapper;
import com.aurora.admin.mapper.RoleMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 角色管理服务实现。处理角色的增删改查、系统内置角色保护及菜单权限分配。
 * 系统内置角色（isSystem=1）禁止修改和删除，权限分配使用全量覆盖策略。
 */
@Service
public class RoleServiceImpl implements RoleService {

    @Autowired
    private RoleMapper roleMapper;

    @Autowired
    private OperationLogMapper operationLogMapper;

    @Override
    public List<Role> findAll() {
        return roleMapper.findAll();
    }

    @Override
    public List<Role> findByKeyword(String keyword, int offset, int size) {
        return roleMapper.findByKeyword(keyword, offset, size);
    }

    @Override
    public long countByKeyword(String keyword) {
        return roleMapper.countByKeyword(keyword);
    }

    @Override
    public Role findById(Long id) {
        return roleMapper.findById(id);
    }

    @Override
    public Role create(Role role) {
        roleMapper.insert(role);
        return role;
    }

    @Override
    public Role update(Long id, Role role) {
        Role existing = roleMapper.findById(id);
        if (existing == null) {
            throw new RuntimeException("角色不存在");
        }
        // 系统内置角色禁止修改
        if (existing.getIsSystem() != null && existing.getIsSystem() == 1) {
            throw new RuntimeException("系统内置角色不可修改");
        }
        role.setId(id);
        roleMapper.update(role);
        return roleMapper.findById(id);
    }

    @Override
    public void delete(Long id) {
        Role role = roleMapper.findById(id);
        if (role == null) {
            throw new RuntimeException("角色不存在");
        }
        // 系统内置角色禁止删除
        if (role.getIsSystem() != null && role.getIsSystem() == 1) {
            throw new RuntimeException("系统内置角色不可删除");
        }
        String resourceDesc = "id=" + role.getId() + ", name=" + role.getName();

        roleMapper.delete(id);

        OperationLog operationLog = new OperationLog();
        operationLog.setUserId(getCurrentUserId());
        operationLog.setUsername(getCurrentUsername());
        operationLog.setOperation("删除角色");
        operationLog.setMethod("DELETE");
        operationLog.setUrl("/api/roles/" + id);
        operationLog.setParams(resourceDesc);
        operationLog.setStatus(1);
        operationLog.setCreateTime(LocalDateTime.now());
        operationLogMapper.insert(operationLog);
    }

    @Override
    @Transactional
    public void assignMenus(Long roleId, List<Long> menuIds) {
        Role role = roleMapper.findById(roleId);
        if (role == null) {
            throw new RuntimeException("角色不存在");
        }
        // 系统内置角色禁止修改权限
        if (role.getIsSystem() != null && role.getIsSystem() == 1) {
            throw new RuntimeException("系统内置角色不可修改权限");
        }
        roleMapper.deleteRoleMenus(roleId);
        if (menuIds != null && !menuIds.isEmpty()) {
            roleMapper.insertRoleMenus(roleId, menuIds);
        }
    }

    @Override
    public List<Long> findMenuIdsByRoleId(Long roleId) {
        return roleMapper.findMenuIdsByRoleId(roleId);
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
