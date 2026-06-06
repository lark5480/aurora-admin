package com.aurora.admin.util;

import com.aurora.admin.exception.UnauthorizedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 安全上下文工具类
 * 统一从 SecurityContext 中获取当前登录用户信息，
 * 避免在各 Controller/Service 中重复编写相同逻辑。
 */
public final class SecurityUtils {

    private static final String ROLE_SUPER_ADMIN = "ROLE_SUPER_ADMIN";
    private static final String ROLE_ADMIN = "ROLE_ADMIN";

    private SecurityUtils() {
    }

    /**
     * 获取当前登录用户 ID
     *
     * @throws UnauthorizedException 未登录或无法获取用户ID时抛出
     */
    public static Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new UnauthorizedException();
        }
        Object credentials = auth.getCredentials();
        if (credentials instanceof Long id) {
            return id;
        }
        if (credentials instanceof Number n) {
            return n.longValue();
        }
        throw new UnauthorizedException("无法获取用户ID");
    }

    /**
     * 获取当前登录用户名
     */
    public static String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return "unknown";
        }
        return auth.getName();
    }

    /**
     * 判断当前用户是否为管理员（SUPER_ADMIN 或 ADMIN）
     */
    public static boolean isCurrentUserAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return false;
        }
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority -> ROLE_SUPER_ADMIN.equals(authority) || ROLE_ADMIN.equals(authority));
    }
}
