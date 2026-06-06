package com.aurora.admin.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 创建用户请求 DTO（管理员创建用户）
 * 替代直接暴露 Entity 到 Controller 层
 */
@Data
public class CreateUserRequest {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 20, message = "用户名长度必须在3-20个字符之间")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 50, message = "密码长度必须在6-50个字符之间")
    private String password;

    @Email(message = "邮箱格式不正确")
    private String email;

    private String nickname;

    /**
     * 用户角色标识，如 "admin"、"user"
     */
    private String role;
}
