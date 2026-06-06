package com.aurora.admin.dto;

import jakarta.validation.constraints.Email;
import lombok.Data;

/**
 * 更新用户请求 DTO（管理员更新用户信息）
 * 只允许更新安全的字段，防止越权篡改 role/status 等
 */
@Data
public class UpdateUserRequest {

    private String nickname;

    @Email(message = "邮箱格式不正确")
    private String email;

    private String avatar;

    private Long deptId;
}
