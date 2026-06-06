package com.aurora.admin.exception;

import lombok.Getter;

/**
 * 业务异常基类
 * 用于在 Service 层抛出可预期的业务错误，
 * 由 GlobalExceptionHandler 统一拦截并返回结构化错误响应。
 */
@Getter
public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public BusinessException(String message) {
        this(400, message);
    }
}
