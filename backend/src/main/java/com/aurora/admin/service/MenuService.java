package com.aurora.admin.service;

import com.aurora.admin.entity.Menu;
import java.util.List;

public interface MenuService {

    /**
     * 获取所有菜单列表（扁平结构），不包含层级关系。
     *
     * @return 所有菜单列表
     */
    List<Menu> findAll();
    /**
     * 根据 ID 查询菜单。可能返回 null。
     *
     * @param id 菜单 ID
     * @return 菜单对象，不存在时返回 null
     */
    Menu findById(Long id);
    /**
     * 查询完整菜单树。将所有菜单按父子关系组装为树形结构返回。
     *
     * @return 菜单树
     */
    List<Menu> findTree();
    /**
     * 查询当前登录用户有权限的菜单树。根据用户角色过滤；SUPER_ADMIN 返回所有菜单。
     *
     * @return 当前用户的菜单树
     */
    List<Menu> findTreeForCurrentUser();
    /**
     * 查询当前登录用户的权限标识列表。返回所有可用菜单的 permission 字段，去重。
     *
     * @return 权限标识列表
     */
    List<String> findPermissionsForCurrentUser();
    /**
     * 根据父级 ID 查询子菜单列表。
     *
     * @param parentId 父菜单 ID
     * @return 子菜单列表
     */
    List<Menu> findByParentId(Long parentId);
    /**
     * 根据角色 ID 查询该角色关联的菜单列表。
     *
     * @param roleId 角色 ID
     * @return 菜单列表
     */
    List<Menu> findByRoleId(Long roleId);
    /**
     * 创建菜单。插入数据库后返回含自增 ID 的菜单对象。
     *
     * @param menu 待创建的菜单
     * @return 已持久化的菜单
     */
    Menu create(Menu menu);
    /**
     * 更新菜单。根据 ID 查找并覆盖已有数据；菜单不存在时抛出异常。
     *
     * @param id   菜单 ID
     * @param menu 更新的菜单内容
     * @return 更新后的菜单
     * @throws RuntimeException 菜单不存在时抛出
     */
    Menu update(Long id, Menu menu);
    /**
     * 删除菜单。存在子菜单时不允许删除；删除成功后会记录操作日志。
     *
     * @param id 菜单 ID
     * @throws RuntimeException 菜单不存在或存在子菜单时抛出
     */
    void delete(Long id);
}
