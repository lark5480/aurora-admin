package com.aurora.admin.exception;

/**
 * 禁止访问异常（403）
 */
public class ForbiddenException extends BusinessException {

    public ForbiddenException(String message) {
        super(403, message);
    }
}
