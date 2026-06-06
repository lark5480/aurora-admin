package com.aurora.admin.aspect;

import com.aurora.admin.entity.OperationLog;
import com.aurora.admin.service.OperationLogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;

/**
 * 操作日志 AOP 切面
 * <p>
 * 统一拦截所有 Controller 方法，自动记录操作日志。
 * Service 层不再需要手动插入日志。
 */
@Aspect
@Component
@RequiredArgsConstructor
public class OperationLogAspect {

    private final OperationLogService operationLogService;

    @Pointcut("execution(* com.aurora.admin.controller..*.*(..))")
    public void controllerPointcut() {
    }

    @Around("controllerPointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();

        Object result = joinPoint.proceed();

        long durationMs = System.currentTimeMillis() - startTime;

        try {
            saveOperationLog(joinPoint, durationMs);
        } catch (Exception e) {
            // 日志记录失败不影响主流程
        }

        return result;
    }

    private void saveOperationLog(ProceedingJoinPoint joinPoint, long durationMs) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        OperationLog opLog = new OperationLog();
        opLog.setOperation(method.getName());
        opLog.setMethod(getHttpMethod());
        opLog.setUrl(getRequestUrl());
        opLog.setIp(getClientIp());
        opLog.setParams(getParams(joinPoint));
        opLog.setStatus(1);
        opLog.setDurationMs((int) durationMs);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            opLog.setUsername(auth.getPrincipal().toString());
            if (auth.getCredentials() != null) {
                opLog.setUserId((Long) auth.getCredentials());
            }
        }

        operationLogService.saveLog(opLog);
    }

    private String getHttpMethod() {
        HttpServletRequest request = getRequest();
        return request != null ? request.getMethod() : "";
    }

    private String getRequestUrl() {
        HttpServletRequest request = getRequest();
        return request != null ? request.getRequestURI() : "";
    }

    private String getClientIp() {
        HttpServletRequest request = getRequest();
        if (request == null) {
            return "";
        }
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

    private String getParams(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String[] paramNames = signature.getParameterNames();
        Object[] args = joinPoint.getArgs();

        if (paramNames == null || args == null) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < paramNames.length; i++) {
            if (i > 0) {
                sb.append("&");
            }
            sb.append(paramNames[i]).append("=").append(args[i] != null ? args[i].toString() : "null");
        }
        return sb.toString();
    }

    private HttpServletRequest getRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes != null ? attributes.getRequest() : null;
    }
}
