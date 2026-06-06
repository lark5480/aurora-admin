package com.aurora.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class LoginResponse {
    private String token;
    private String username;
    private String nickname;
    private String email;
    private String role;
    private String avatar;
    private List<String> permissions;
}
