package com.aurora.admin.exception;

/**
 * 未授权异常（401）
 */
public class UnauthorizedException extends BusinessException {

    public UnauthorizedException() {
        super(401, "未登录或登录已过期");
    }

    public UnauthorizedException(String message) {
        super(401, message);
    }
}
