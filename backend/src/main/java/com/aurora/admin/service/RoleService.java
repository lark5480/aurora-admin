package com.aurora.admin.service;

import com.aurora.admin.entity.Role;
import java.util.List;

public interface RoleService {
    List<Role> findAll();
    List<Role> findByKeyword(String keyword, int offset, int size);
    long countByKeyword(String keyword);
    Role findById(Long id);
    Role create(Role role);
    Role update(Long id, Role role);
    void delete(Long id);
    void assignMenus(Long roleId, List<Long> menuIds);
    List<Long> findMenuIdsByRoleId(Long roleId);
}
