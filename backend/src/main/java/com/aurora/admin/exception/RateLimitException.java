package com.aurora.admin.exception;

/**
 * 接口限流异常，由 {@link com.aurora.admin.aspect.RateLimitAspect} 抛出，
 * {@link GlobalExceptionHandler} 统一处理返回 HTTP 429。
 */
public class RateLimitException extends RuntimeException {

    public RateLimitException(String message) {
        super(message);
    }
}
