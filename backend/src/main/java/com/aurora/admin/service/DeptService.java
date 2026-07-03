package com.aurora.admin.service;

import com.aurora.admin.entity.Dept;
import java.util.List;

public interface DeptService {
    /**
     * 获取全部部门列表（扁平结构）。
     *
     * @return 部门列表
     */
    List<Dept> findAll();

    /**
     * 获取部门树结构。按 parentId 分组后递归组装为层级树。
     *
     * @return 部门层级树（根节点列表）
     */
    List<Dept> findTree();

    /**
     * 根据 ID 查询部门。
     *
     * @param id 部门 ID
     * @return 部门信息，不存在时返回 null
     */
    Dept findById(Long id);

    /**
     * 创建新部门。自动填充默认值（parentId、code、sortOrder、status），
     * 并同步部门负责人信息（不存在时自动创建用户）。
     *
     * @param dept 部门信息
     * @return 创建后的部门（含自增 ID）
     */
    Dept create(Dept dept);

    /**
     * 更新部门信息。同步更新部门负责人信息。
     *
     * @param id   部门 ID
     * @param dept 更新后的部门信息
     * @return 更新后的部门
     */
    Dept update(Long id, Dept dept);

    /**
     * 删除指定部门。存在子部门时抛出异常。删除后记录操作日志。
     *
     * @param id 部门 ID
     * @throws RuntimeException 存在子部门时抛出
     */
    void delete(Long id);
}
