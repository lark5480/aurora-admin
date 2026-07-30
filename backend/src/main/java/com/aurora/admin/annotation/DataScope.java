package com.aurora.admin.annotation;

import java.lang.annotation.*;

/**
 * 数据权限注解。标注在 Mapper 方法上，拦截器自动追加数据过滤条件。
 * Phase 1 仅支持 dataScope=1（全部）和 dataScope=4（仅本人）。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DataScope {
    /** 用户 ID 字段名（含表别名时使用，如 "o.user_id"） */
    String userColumn() default "user_id";
}
