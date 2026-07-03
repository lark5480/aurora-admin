package com.aurora.admin.service;

import com.aurora.admin.dto.CreateUserRequest;
import com.aurora.admin.dto.PageResult;
import com.aurora.admin.dto.UpdateUserRequest;
import com.aurora.admin.dto.UserVO;
import com.aurora.admin.entity.Role;
import com.aurora.admin.entity.User;

import java.util.List;

public interface UserService {
    /**
     * 根据用户名查询用户。
     *
     * @param username 用户名
     * @return 用户实体，不存在时返回 null
     */
    User findByUsername(String username);

    /**
     * 用户注册。创建用户并分配默认角色 ROLE_USER。
     *
     * @param username 用户名
     * @param password 明文密码（方法内加密）
     * @param email    邮箱
     * @return 新建的用户实体
     */
    User register(String username, String password, String email);

    /**
     * 管理员创建用户。根据 DTO 创建用户并保存。
     *
     * @param request 创建用户请求 DTO
     * @return 新建的用户实体
     */
    User create(CreateUserRequest request);

    /**
     * 直接保存用户实体到数据库。
     *
     * @param user 已构造好的用户实体
     * @return 保存后的用户实体（含自动生成 ID）
     */
    User create(User user);

    /**
     * 根据 ID 查询用户。
     *
     * @param id 用户 ID
     * @return 用户实体，不存在时返回 null
     */
    User findById(Long id);

    /**
     * 根据邮箱查询用户。
     *
     * @param email 用户邮箱
     * @return 用户实体，不存在时返回 null
     */
    User findByEmail(String email);

    /**
     * 分页查询用户列表。支持按关键字模糊搜索用户名和邮箱。
     *
     * @param page    页码（从 1 开始）
     * @param size    每页条数
     * @param keyword 搜索关键字，为空字符串时查询全部
     * @return 分页结果（用户视图对象列表）
     */
    PageResult<UserVO> findAll(int page, int size, String keyword);

    /**
     * 更新用户实体（已过时，请使用 {@link #update(Long, UpdateUserRequest)}）。
     *
     * @param user 含更新字段的用户实体
     */
    void update(User user);

    /**
     * 根据 ID 更新用户信息，使用 DTO 传递更新字段。
     *
     * @param id      用户 ID
     * @param request 更新请求 DTO
     */
    void update(Long id, UpdateUserRequest request);

    /**
     * 更新用户状态（启用/禁用）。
     *
     * @param id     用户 ID
     * @param status 目标状态值（0=禁用，1=启用）
     */
    void updateStatus(Long id, int status);

    /**
     * 删除用户。物理删除用户及其角色关联记录。
     *
     * @param id 用户 ID
     */
    void delete(Long id);

    /**
     * 查询用户关联的角色列表。
     *
     * @param userId 用户 ID
     * @return 角色列表
     */
    List<Role> findRolesByUserId(Long userId);

    /**
     * 为用户分配角色。先清空原有角色关联，再插入新关联。
     *
     * @param userId 用户 ID
     * @param roleIds 角色 ID 列表
     */
    void assignRoles(Long userId, List<Long> roleIds);

    /**
     * 获取当前登录用户的个人资料。
     *
     * @return 当前用户实体
     */
    User getProfile();

    /**
     * 修改当前登录用户的个人资料。
     *
     * @param userId   当前用户 ID
     * @param nickname 新昵称
     * @param email    新邮箱
     * @param avatar   新头像 URL
     */
    void updateProfile(Long userId, String nickname, String email, String avatar);

    /**
     * 修改当前登录用户的密码。
     *
     * @param userId      当前用户 ID
     * @param oldPassword 旧密码（需验证正确性）
     * @param newPassword 新密码
     */
    void changePassword(Long userId, String oldPassword, String newPassword);
}
