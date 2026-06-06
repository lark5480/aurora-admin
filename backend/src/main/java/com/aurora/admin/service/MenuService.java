package com.aurora.admin.service;

import com.aurora.admin.entity.Menu;
import java.util.List;

public interface MenuService {
    List<Menu> findAll();
    Menu findById(Long id);
    List<Menu> findTree();
    List<Menu> findTreeForCurrentUser();
    List<String> findPermissionsForCurrentUser();
    List<Menu> findByParentId(Long parentId);
    List<Menu> findByRoleId(Long roleId);
    Menu create(Menu menu);
    Menu update(Long id, Menu menu);
    void delete(Long id);
}
