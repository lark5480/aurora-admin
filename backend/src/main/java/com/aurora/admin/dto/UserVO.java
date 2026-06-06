package com.aurora.admin.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户信息 VO（View Object）
 * 返回给前端的用户数据，排除 password 等敏感字段
 */
@Data
public class UserVO {

    private Long id;
    private String username;
    private String email;
    private String nickname;
    private String role;
    private Long deptId;
    private String deptName;
    private String avatar;
    private Integer status;
    private LocalDateTime createTime;
    private List<String> roles;

    public static UserVO from(com.aurora.admin.entity.User user) {
        if (user == null) {
            return null;
        }
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setEmail(user.getEmail());
        vo.setNickname(user.getNickname());
        vo.setRole(user.getRole());
        vo.setDeptId(user.getDeptId());
        vo.setDeptName(user.getDeptName());
        vo.setAvatar(user.getAvatar());
        vo.setStatus(user.getStatus());
        vo.setCreateTime(user.getCreateTime());
        return vo;
    }
}
