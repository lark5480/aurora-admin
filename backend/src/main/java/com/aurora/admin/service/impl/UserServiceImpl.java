package com.aurora.admin.service.impl;

import com.aurora.admin.dto.CreateUserRequest;
import com.aurora.admin.dto.PageResult;
import com.aurora.admin.dto.UpdateUserRequest;
import com.aurora.admin.dto.UserVO;
import com.aurora.admin.entity.Role;
import com.aurora.admin.entity.User;
import com.aurora.admin.exception.BusinessException;
import com.aurora.admin.exception.NotFoundException;
import com.aurora.admin.mapper.RoleMapper;
import com.aurora.admin.mapper.UserMapper;
import com.aurora.admin.service.UserService;
import com.aurora.admin.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 用户服务实现类。提供用户注册、查询、更新、删除、角色分配等业务逻辑。
 *
 * <ul>
 *   <li>注册时自动加密密码（BCrypt）并分配默认角色。</li>
 *   <li>更新操作仅修改非空字段，不影响其他字段值。</li>
 *   <li>角色分配采用先清空后插入的策略。</li>
 *   <li>修改密码需校验旧密码正确性。</li>
 * </ul>
 *
 * @see UserService
 * @see UserMapper
 * @see PasswordEncoder
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public User findByUsername(String username) {
        return userMapper.findByUsername(username);
    }

    @Override
    public User register(String username, String password, String email) {
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setEmail(email);
        user.setRole("user");
        user.setStatus(1);
        userMapper.insert(user);
        assignDefaultRole(user);
        return user;
    }

    @Override
    @Transactional
    public User create(CreateUserRequest request) {
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmail(request.getEmail());
        user.setNickname(request.getNickname());
        user.setRole(request.getRole() != null ? request.getRole() : "user");
        user.setStatus(1);
        userMapper.insert(user);
        assignDefaultRole(user);
        return user;
    }

    @Override
    @Transactional
    public User create(User user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        if (user.getStatus() == null) {
            user.setStatus(1);
        }
        userMapper.insert(user);
        assignDefaultRole(user);
        return user;
    }

    private void assignDefaultRole(User user) {
        String roleCode = "admin".equalsIgnoreCase(user.getRole()) ? "ADMIN" : "USER";
        Role role = roleMapper.findByCode(roleCode);
        if (role != null) {
            roleMapper.insertUserRole(user.getId(), role.getId());
        }
    }

    @Override
    public User findById(Long id) {
        return userMapper.findById(id);
    }

    @Override
    public User findByEmail(String email) {
        return userMapper.findByEmail(email);
    }

    @Override
    public PageResult<UserVO> findAll(int page, int size, String keyword) {
        List<User> users = userMapper.findAll((page - 1) * size, size, keyword);
        int total = userMapper.count(keyword);
        List<UserVO> voList = users.stream().map(UserVO::from).toList();
        return PageResult.of(voList, total, page, size);
    }

    @Override
    public void update(User user) {
        userMapper.update(user);
    }

    @Override
    @Transactional
    public void update(Long id, UpdateUserRequest request) {
        User user = userMapper.findById(id);
        if (user == null) {
            throw new NotFoundException("用户", id);
        }
        if (request.getNickname() != null) {
            user.setNickname(request.getNickname());
        }
        if (request.getEmail() != null) {
            user.setEmail(request.getEmail());
        }
        if (request.getAvatar() != null) {
            user.setAvatar(request.getAvatar());
        }
        if (request.getDeptId() != null) {
            user.setDeptId(request.getDeptId());
        }
        userMapper.update(user);
    }

    @Override
    public void updateStatus(Long id, int status) {
        userMapper.updateStatus(id, status);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        User user = userMapper.findById(id);
        if (user == null) {
            throw new NotFoundException("用户", id);
        }
        userMapper.delete(id);
    }

    @Override
    public List<Role> findRolesByUserId(Long userId) {
        return roleMapper.findByUserId(userId);
    }

    @Override
    @Transactional
    public void assignRoles(Long userId, List<Long> roleIds) {
        roleMapper.deleteUserRoles(userId);
        for (Long roleId : roleIds) {
            roleMapper.insertUserRole(userId, roleId);
        }
    }

    @Override
    public User getProfile() {
        String username = SecurityUtils.getCurrentUsername();
        return userMapper.findByUsername(username);
    }

    @Override
    public void updateProfile(Long userId, String nickname, String email, String avatar) {
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new NotFoundException("用户", userId);
        }
        if (nickname != null) {
            user.setNickname(nickname);
        }
        if (email != null) {
            user.setEmail(email);
        }
        if (avatar != null) {
            user.setAvatar(avatar);
        }
        userMapper.updateProfile(userId,
                nickname != null ? nickname : user.getNickname(),
                email != null ? email : user.getEmail(),
                avatar != null ? avatar : user.getAvatar());
    }

    @Override
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new NotFoundException("用户", userId);
        }
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new BusinessException("原密码不正确");
        }
        userMapper.updatePassword(userId, passwordEncoder.encode(newPassword));
    }
}
