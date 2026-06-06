package com.aurora.admin.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 接口限流注解，配合 {@link com.aurora.admin.aspect.RateLimitAspect} 使用。
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * // IP 限流：同一 IP 每分钟最多 10 次
 * @RateLimit(key = KeyType.IP, limit = 10, duration = 60)
 *
 * // 用户限流：同一用户每 5 分钟最多 1 次（危险操作）
 * @RateLimit(key = KeyType.USER, limit = 1, duration = 300)
 * }</pre>
 *
 * <p>Redis 可用时使用 INCR + EXPIRE 分布式计数，否则回退到本地 Bucket4j 令牌桶。</p>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {

    /** 限流维度 */
    KeyType key() default KeyType.IP;

    /** 时间窗口内最大请求数 */
    int limit() default 10;

    /** 时间窗口（秒） */
    int duration() default 60;

    /** 超限提示信息 */
    String message() default "请求过于频繁，请稍后再试";

    enum KeyType {
        /** 按客户端 IP 限流 */
        IP,
        /** 按当前登录用户 ID 限流，未认证则回退到 IP */
        USER,
        /** 按 IP + 方法名限流 */
        IP_METHOD
    }
}
