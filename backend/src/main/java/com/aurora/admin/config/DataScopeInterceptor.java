package com.aurora.admin.config;

import com.aurora.admin.annotation.DataScope;
import com.aurora.admin.mapper.RoleMapper;
import com.aurora.admin.util.SecurityUtils;
import com.baomidou.mybatisplus.core.toolkit.PluginUtils;
import com.baomidou.mybatisplus.extension.plugins.inner.InnerInterceptor;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.sql.SQLException;

/**
 * 数据权限拦截器。
 * Phase 1 仅支持 dataScope=1（全部）和 dataScope=4（仅本人）。
 */
public class DataScopeInterceptor implements InnerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(DataScopeInterceptor.class);

    private final RoleMapper roleMapper;

    public DataScopeInterceptor(RoleMapper roleMapper) {
        this.roleMapper = roleMapper;
    }

    @Override
    public void beforeQuery(Executor executor, MappedStatement ms, Object parameter,
                            RowBounds rowBounds, ResultHandler resultHandler, BoundSql boundSql) throws SQLException {
        // 1. 获取 Mapper 方法上的 @DataScope 注解
        DataScope dataScope = getDataScopeAnnotation(ms);
        if (dataScope == null) {
            return; // 无注解，跳过
        }

        // 2. 获取当前用户的 dataScope（多角色取最严格 = 数值最大）
        int scope = getDataScopeForCurrentUser();
        if (scope == 1) {
            return; // 全部权限，不追加条件
        }

        // 3. 仅本人（dataScope=4）：追加 userColumn = currentUserId
        if (scope == 4) {
            Long userId;
            try {
                userId = SecurityUtils.getCurrentUserId();
            } catch (Exception e) {
                return; // 未登录，不追加（由 Security 层拦截）
            }
            if (userId == null) {
                return;
            }

            String originalSql = boundSql.getSql();
            String userColumn = dataScope.userColumn();

            try {
                Statement statement = CCJSqlParserUtil.parse(originalSql);
                if (statement instanceof Select select) {
                    if (select.getSelectBody() instanceof PlainSelect plainSelect) {
                        Expression condition = new EqualsTo(
                                new Column(userColumn),
                                new LongValue(userId)
                        );
                        Expression where = plainSelect.getWhere();
                        if (where == null) {
                            plainSelect.setWhere(condition);
                        } else {
                            plainSelect.setWhere(new AndExpression(where, condition));
                        }
                        PluginUtils.mpBoundSql(boundSql).sql(statement.toString());
                    }
                }
            } catch (Exception e) {
                log.error("[DataScope] SQL 解析失败，拒绝查询以防数据泄露: {}", e.getMessage());
                throw new SQLException("数据权限 SQL 解析失败", e);
            }
        }
    }

    /**
     * 获取当前用户的最小 dataScope（多角色取最严格 = 数值最大）。
     * 这里取 MIN 是因为 dataScope 数值越小权限越大，MIN 即最宽松的有效权限。
     * 但按照需求：SUPER_ADMIN=1, ADMIN=1, USER=4，MIN 即得到该用户的最宽权限。
     */
    private int getDataScopeForCurrentUser() {
        try {
            Long userId = SecurityUtils.getCurrentUserId();
            if (userId == null) {
                return 1;
            }
            Integer minScope = roleMapper.findMinDataScopeByUserId(userId);
            return minScope != null ? minScope : 1;
        } catch (Exception e) {
            log.warn("[DataScope] 获取 dataScope 失败，默认全部权限: {}", e.getMessage());
            return 1;
        }
    }

    /**
     * 从 MappedStatement 中解析 Mapper 方法上的 @DataScope 注解。
     */
    private DataScope getDataScopeAnnotation(MappedStatement ms) {
        try {
            String id = ms.getId();
            String className = id.substring(0, id.lastIndexOf('.'));
            String methodName = id.substring(id.lastIndexOf('.') + 1);
            // 处理 MyBatis-Plus 分页方法名（如 findPage_mp_count）
            if (methodName.contains("_mp_")) {
                methodName = methodName.substring(0, methodName.indexOf("_mp_"));
            }
            // 去掉可能的 < 后缀（MyBatis 内部格式）
            if (methodName.contains("<")) {
                methodName = methodName.substring(0, methodName.indexOf('<'));
            }

            Class<?> mapperClass = Class.forName(className);
            for (Method method : mapperClass.getMethods()) {
                if (method.getName().equals(methodName)) {
                    DataScope annotation = method.getAnnotation(DataScope.class);
                    if (annotation != null) {
                        return annotation;
                    }
                }
            }
        } catch (Exception e) {
            log.debug("[DataScope] 无法获取注解: {}", e.getMessage());
        }
        return null;
    }
}
