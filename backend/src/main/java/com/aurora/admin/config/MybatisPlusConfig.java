package com.aurora.admin.config;

import com.aurora.admin.mapper.RoleMapper;
import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

@Configuration
public class MybatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor(@Lazy RoleMapper roleMapper) {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 数据权限拦截器（在分页之前）
        interceptor.addInnerInterceptor(new DataScopeInterceptor(roleMapper));
        // 分页拦截器
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }
}
