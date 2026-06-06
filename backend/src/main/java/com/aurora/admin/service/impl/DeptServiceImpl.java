package com.aurora.admin.service.impl;

import com.aurora.admin.entity.Dept;
import com.aurora.admin.entity.OperationLog;
import com.aurora.admin.entity.Role;
import com.aurora.admin.entity.User;
import com.aurora.admin.mapper.DeptMapper;
import com.aurora.admin.mapper.OperationLogMapper;
import com.aurora.admin.mapper.RoleMapper;
import com.aurora.admin.mapper.UserMapper;
import com.aurora.admin.service.DeptService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DeptServiceImpl implements DeptService {

    @Autowired
    private DeptMapper deptMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private OperationLogMapper operationLogMapper;

    @Autowired
    private RoleMapper roleMapper;

    @Override
    public List<Dept> findAll() {
        return deptMapper.findAll();
    }

    @Override
    public List<Dept> findTree() {
        List<Dept> allDepts = deptMapper.findAll();
        Map<Long, List<Dept>> grouped = allDepts.stream()
                .collect(Collectors.groupingBy(Dept::getParentId));

        List<Dept> rootDepts = grouped.getOrDefault(0L, new ArrayList<>());
        rootDepts.sort((a, b) -> {
            int sortA = a.getSortOrder() != null ? a.getSortOrder() : 0;
            int sortB = b.getSortOrder() != null ? b.getSortOrder() : 0;
            return sortA - sortB;
        });

        for (Dept dept : rootDepts) {
            buildChildren(dept, grouped);
        }

        return rootDepts;
    }

    private void buildChildren(Dept parent, Map<Long, List<Dept>> grouped) {
        List<Dept> children = grouped.getOrDefault(parent.getId(), new ArrayList<>());
        children.sort((a, b) -> {
            int sortA = a.getSortOrder() != null ? a.getSortOrder() : 0;
            int sortB = b.getSortOrder() != null ? b.getSortOrder() : 0;
            return sortA - sortB;
        });
        parent.setChildren(children);
        for (Dept child : children) {
            buildChildren(child, grouped);
        }
    }

    @Override
    public Dept findById(Long id) {
        return deptMapper.findById(id);
    }

    @Override
    public Dept create(Dept dept) {
        if (dept.getParentId() == null) {
            dept.setParentId(0L);
        }
        if (dept.getCode() == null || dept.getCode().isBlank()) {
            dept.setCode("DEPT_" + System.currentTimeMillis());
        }
        if (dept.getSortOrder() == null) {
            dept.setSortOrder(0);
        }
        if (dept.getStatus() == null) {
            dept.setStatus(1);
        }
        syncLeader(dept);
        deptMapper.insert(dept);
        if (dept.getLeaderUserId() != null) {
            userMapper.updateDeptId(dept.getLeaderUserId(), dept.getId());
        }
        return dept;
    }

    @Override
    public Dept update(Long id, Dept dept) {
        dept.setId(id);
        syncLeader(dept);
        deptMapper.update(dept);
        return dept;
    }

    private void syncLeader(Dept dept) {
        if (dept.getLeaderName() == null || dept.getLeaderName().isBlank()) {
            dept.setLeaderUserId(null);
            return;
        }
        String name = dept.getLeaderName().trim();
        User user = userMapper.findByNickname(name);
        if (user == null) {
            user = userMapper.findByUsername(name);
        }
        if (user == null) {
            user = userMapper.findByUsernameIncludeDeleted(name);
            if (user != null && user.getIsDeleted() != null && user.getIsDeleted() == 1) {
                user.setIsDeleted(0);
            }
        }
        if (user != null) {
            user.setNickname(name);
            user.setEmail(dept.getEmail());
            user.setDeptName(dept.getName());
            if (dept.getId() != null) {
                user.setDeptId(dept.getId());
            }
            userMapper.update(user);
            dept.setLeaderUserId(user.getId());
        } else {
            User newUser = new User();
            newUser.setUsername(name);
            newUser.setNickname(name);
            newUser.setPassword(passwordEncoder.encode("123456"));
            newUser.setRole("user");
            newUser.setStatus(1);
            newUser.setEmail(dept.getEmail());
            newUser.setDeptName(dept.getName());
            if (dept.getId() != null) {
                newUser.setDeptId(dept.getId());
            }
            userMapper.insert(newUser);
            Role userRole = roleMapper.findByCode("USER");
            if (userRole != null) {
                roleMapper.insertUserRole(newUser.getId(), userRole.getId());
            }
            dept.setLeaderUserId(newUser.getId());
        }
    }

    @Override
    public void delete(Long id) {
        int childCount = deptMapper.countByParentId(id);
        if (childCount > 0) {
            throw new RuntimeException("请先删除子部门");
        }
        Dept dept = deptMapper.findById(id);
        String resourceDesc = dept != null ? "id=" + dept.getId() + ", name=" + dept.getName() : "id=" + id;

        deptMapper.delete(id);

        OperationLog operationLog = new OperationLog();
        operationLog.setUserId(getCurrentUserId());
        operationLog.setUsername(getCurrentUsername());
        operationLog.setOperation("删除部门");
        operationLog.setMethod("DELETE");
        operationLog.setUrl("/api/depts/" + id);
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
