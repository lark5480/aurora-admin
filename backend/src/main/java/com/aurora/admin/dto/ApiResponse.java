package com.aurora.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ApiResponse {
    private int code;
    private String message;
    private Object data;

    public static ApiResponse success() {
        return new ApiResponse(200, "success", null);
    }

    public static ApiResponse success(Object data) {
        return new ApiResponse(200, "success", data);
    }

    public static ApiResponse success(String message) {
        // message 和 data 都设为相同值，避免前端 axios 解包后收到 null
        return new ApiResponse(200, "success", message);
    }

    public static ApiResponse error(String message) {
        return new ApiResponse(500, message, null);
    }

    public static ApiResponse error(int code, String message) {
        return new ApiResponse(code, message, null);
    }
}
