package com.aurora.admin.service;

import com.aurora.admin.entity.Role;
import java.util.List;

public interface RoleService {
    /**
     * 查询所有角色列表。
     *
     * @return 全部角色列表
     */
    List<Role> findAll();
    /**
     * 按关键字模糊查询角色列表，支持分页。关键字匹配角色编码和名称。
     *
     * @param keyword 关键字（可选，传空字符串则查询全部）
     * @param offset  偏移量
     * @param size    每页条数
     * @return 角色列表
     */
    List<Role> findByKeyword(String keyword, int offset, int size);
    /**
     * 按关键字统计角色总数。关键字匹配角色编码和名称。
     *
     * @param keyword 关键字（可选，传空字符串则统计全部）
     * @return 角色总数
     */
    long countByKeyword(String keyword);
    /**
     * 根据ID查询角色。
     *
     * @param id 角色ID
     * @return 角色对象，不存在时返回 null
     */
    Role findById(Long id);
    /**
     * 创建新角色。
     *
     * @param role 待创建的角色对象
     * @return 创建成功后的角色信息（含自动生成的ID）
     */
    Role create(Role role);
    /**
     * 更新角色信息。
     *
     * @param id   角色ID
     * @param role 新的角色信息
     * @return 更新后的角色对象
     * @throws RuntimeException 角色不存在时抛出异常
     */
    Role update(Long id, Role role);
    /**
     * 删除指定角色。
     *
     * @param id 角色ID
     * @throws RuntimeException 角色不存在或有关联数据时抛出异常
     */
    void delete(Long id);
    /**
     * 为角色分配菜单权限（全量覆盖）。先清除原有菜单权限，再插入新的关联。
     *
     * @param roleId 角色ID
     * @param menuIds 菜单权限ID列表
     * @throws RuntimeException 角色不存在时抛出异常
     */
    void assignMenus(Long roleId, List<Long> menuIds);
    /**
     * 查询角色已分配的菜单权限ID列表。
     *
     * @param roleId 角色ID
     * @return 菜单权限ID列表
     */
    List<Long> findMenuIdsByRoleId(Long roleId);
}
