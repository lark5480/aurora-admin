package com.aurora.admin.service;

import com.aurora.admin.dto.CreateUserRequest;
import com.aurora.admin.dto.PageResult;
import com.aurora.admin.dto.UpdateUserRequest;
import com.aurora.admin.dto.UserVO;
import com.aurora.admin.entity.Role;
import com.aurora.admin.entity.User;

import java.util.List;

public interface UserService {
    User findByUsername(String username);
    User register(String username, String password, String email);
    User create(CreateUserRequest request);
    User create(User user);
    User findById(Long id);
    User findByEmail(String email);
    PageResult<UserVO> findAll(int page, int size, String keyword);
    void update(User user);
    void update(Long id, UpdateUserRequest request);
    void updateStatus(Long id, int status);
    void delete(Long id);
    List<Role> findRolesByUserId(Long userId);
    void assignRoles(Long userId, List<Long> roleIds);

    User getProfile();
    void updateProfile(Long userId, String nickname, String email, String avatar);
    void changePassword(Long userId, String oldPassword, String newPassword);
}
