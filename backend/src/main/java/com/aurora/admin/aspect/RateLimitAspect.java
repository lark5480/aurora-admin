package com.aurora.admin.aspect;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.aurora.admin.annotation.RateLimit;
import com.aurora.admin.exception.RateLimitException;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;

/**
 * 接口限流 AOP 切面。
 *
 * <p>拦截所有标注 {@link RateLimit} 的方法，按配置维度（IP/用户/IP+方法）计数，
 * Redis 可用时使用 INCR + EXPIRE 分布式计数，否则回退到本地 Bucket4j 令牌桶。
 *
 * <p>执行顺序：{@code @Order(1)} 确保在 {@link OperationLogAspect} 之前执行，
 * 请求被限流拦截后不再记录操作日志。
 */
@Aspect
@Component
@Order(1)
public class RateLimitAspect {

    private static final Logger log = LoggerFactory.getLogger(RateLimitAspect.class);

    private static final String REDIS_KEY_PREFIX = "rate_limit:";

    /**
     * Lua 脚本：原子执行 INCR + EXPIRE。
     * 首次 INCR 返回 1 时设置 TTL，后续 INCR 不刷新过期时间，保证窗口一致性。
     * 适用于 Redis 单机/哨兵/集群所有拓扑。
     */
    private static final RedisScript<Long> RATE_LIMIT_SCRIPT = new DefaultRedisScript<>(
        "local count = redis.call('INCR', KEYS[1])\n" +
        "if count == 1 then\n" +
        "    redis.call('EXPIRE', KEYS[1], tonumber(ARGV[1]))\n" +
        "end\n" +
        "return count",
        Long.class
    );

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    private boolean redisAvailable = false;

    /** 本地令牌桶，Redis 不可用时兜底 */
    private final ConcurrentHashMap<String, Bucket> localBuckets = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        if (redisTemplate != null) {
            try {
                redisTemplate.opsForValue().get("rate_limit:test");
                redisAvailable = true;
                log.info("[RateLimitAspect] Redis available, using distributed rate limiting");
            } catch (Exception e) {
                log.warn("[RateLimitAspect] Redis unavailable, using local buckets");
                redisAvailable = false;
            }
        } else {
            log.info("[RateLimitAspect] RedisTemplate not configured, using local buckets");
        }
    }

    @Pointcut("@annotation(com.aurora.admin.annotation.RateLimit)")
    public void rateLimitPointcut() {
    }

    @Around("rateLimitPointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        RateLimit annotation = method.getAnnotation(RateLimit.class);
        String methodName = method.getName();

        String key = resolveKey(annotation, methodName);
        int limit = annotation.limit();
        int duration = annotation.duration();

        if (isRateLimited(key, limit, duration)) {
            log.warn("[RateLimit] 触发限流 key={} limit={} duration={}s", key, limit, duration);
            throw new RateLimitException(annotation.message());
        }

        return joinPoint.proceed();
    }

    /**
     * 根据注解配置解析限流 key。
     */
    private String resolveKey(RateLimit annotation, String methodName) {
        String type = annotation.key().name().toLowerCase();

        return switch (annotation.key()) {
            case USER -> {
                Long userId = getCurrentUserId();
                String val = userId != null ? String.valueOf(userId) : getClientIp();
                yield REDIS_KEY_PREFIX + type + ":" + methodName + ":" + val;
            }
            case IP_METHOD -> REDIS_KEY_PREFIX + type + ":" + methodName + ":" + getClientIp();
            case IP -> REDIS_KEY_PREFIX + type + ":" + getClientIp();
        };
    }

    /**
     * 判断是否超限。先尝试 Redis INCR，失败则走本地令牌桶。
     */
    /**
     * 判断是否超限。
     *
     * <p>Redis 可用时通过 Lua 脚本原子执行 INCR + EXPIRE，适用于单机/哨兵/集群所有拓扑。
     * Redis 不可用时回退本地 Bucket4j 令牌桶。
     */
    private boolean isRateLimited(String key, int limit, int durationSeconds) {
        if (redisAvailable) {
            try {
                Long count = redisTemplate.execute(
                    RATE_LIMIT_SCRIPT,
                    java.util.List.of(key),
                    String.valueOf(durationSeconds)
                );
                return count != null && count > limit;
            } catch (Exception e) {
                log.warn("[RateLimitAspect] Redis Lua script failed, fallback to local bucket", e);
            }
        }
        return isLimitedByLocalBucket(key, limit, durationSeconds);
    }

    /**
     * 本地 Bucket4j 令牌桶计数。
     */
    private boolean isLimitedByLocalBucket(String key, int limit, int durationSeconds) {
        Bucket bucket = localBuckets.computeIfAbsent(key, k -> {
            Bandwidth bandwidth = Bandwidth.classic(limit,
                    Refill.intervally(limit, Duration.ofSeconds(durationSeconds)));
            return Bucket.builder().addLimit(bandwidth).build();
        });
        return !bucket.tryConsume(1);
    }

    /**
     * 从 RequestContextHolder 提取客户端 IP。
     */
    private String getClientIp() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) {
                return "unknown";
            }
            HttpServletRequest request = attrs.getRequest();
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                return forwarded.split(",")[0].trim();
            }
            String realIp = request.getHeader("X-Real-IP");
            if (realIp != null && !realIp.isBlank()) {
                return realIp.trim();
            }
            return request.getRemoteAddr();
        } catch (Exception e) {
            return "unknown";
        }
    }

    /**
     * 从 SecurityContext 提取当前登录用户 ID，未认证返回 null。
     */
    private Long getCurrentUserId() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && auth.getCredentials() instanceof Long id) {
                return id;
            }
        } catch (Exception ignored) {
        }
        return null;
    }
}
