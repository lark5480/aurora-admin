# AGENTS.md

## 技术栈
- 后端：Java 21, Spring Boot 3, MyBatis-Plus 3, MySQL 8, Redis (Lettuce), RabbitMQ, Elasticsearch 8
- 前端：Vue 3, TypeScript, Vite, Pinia, Element Plus, ECharts

## 架构要点
- **认证**：JWT Bearer Token + Spring Security + `JwtAuthenticationFilter`（OncePerRequestFilter），`sessionStorage` 存 token
- **鉴权**：`@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")` 方法级权限，前端 `v-permission` 指令（`directives/permission.ts`）按钮级鉴权
- **数据权限**：`@DataScope` 注解 + `DataScopeInterceptor`（MyBatis-Plus InnerInterceptor），基于 `t_role.data_scope` 自动追加 SQL 过滤条件；Phase 1 支持 `1=全部` / `4=仅本人`，标注在 Mapper 分页/列表方法上
- **状态**：Pinia Setup Store 语法（`defineStore('x', () => { ... })`），7 个 store：user / message / config / product / order / address / afterSale
- **路由**：动态路由 — 登录后 `useUserStore().menuTree` 驱动 `router.addRoute('home', ...)` 注册子路由
- **响应**：`ApiResponse<T>` (code, message, data)，`utils/request.ts` 的 axios 响应拦截器自动解包
- **限流**：Bucket4j（登录 5次/15分钟，注册 3次/小时），依赖 Redis
- **消息队列**：RabbitMQ，`MessageProducer` 发送，`@RabbitListener` 消费（订单创建通知、ES 索引重建）
- **搜索引擎**：Elasticsearch 8，`ProductSearchService` 全文搜索，`EsIndexInitializer` 启动时建索引
- **分布式锁**：ShedLock `@SchedulerLock`，定时任务防重

## 配置体系
3 层配置文件：`application.yml`（公共）→ `application-dev.yml`（默认激活，本地开发）→ `application-prod.yml`（线上）

开发环境中间件默认值：MySQL `localhost:3306` / Redis 单机 `localhost:6379` / RabbitMQ `localhost:5672`(admin/admin123) / ES `localhost:9200`。Docker 搭建详见 `docs/00-环境搭建指南.md`。

## 关键约定
- Entity：必须 `@TableName` + `@TableId`，默认 `IdType.AUTO`（数据库自增），业务编号需防碰撞的场景用 `IdType.ASSIGN_ID`（雪花算法，如 Order / AfterSale）；使用 `@Getter @Setter` 不用 `@Data`
- 数据库：`schema.sql` 自动执行（`spring.sql.init.mode=always`），初始化数据用 `INSERT IGNORE` 避免重复报错
- 权限注解：统一用 `hasAnyRole('ADMIN','SUPER_ADMIN')`，不要只写 `hasRole('ADMIN')`
- DI：新代码用构造器注入，旧代码 `@Autowired` 字段注入可接受
- 前端：`<script setup lang="ts">`，interface 定义 props/emits，`import type` 导类型
- 样式：赛博朋克暗色主题（#ff00ff #00ffff #39ff14），`assets/theme.css` + `assets/table.css`；组件用 scoped style，弹窗等非组件内 DOM 用独立非 scoped style 块
- 改一个文件时，先读完整个文件再动手；新增字段/接口时同步更新 schema.sql
- 改完代码后主动编译验证（`mvn compile` / `vue-tsc --noEmit`）
- JSON 序列化：`JacksonConfig` 全局将 `Long` 包装类型序列化为 String（防前端 JS 精度丢失），`long` 原始类型不受影响；新增 Entity ID 字段无需额外注解
- 批量查询：Mapper 层提供 `findByIds` / `findByOrderIds` 等批量方法（`@Select` + `<script>` + `<foreach>`），Service 层禁止在循环中调用单条查询，必须用批量方法 + Map 组装

## 调试
- 账号：`admin` / `admin123`（SUPER_ADMIN），`user` / `123456`（普通用户）
- 常用检查：`cd frontend && npx tsc --noEmit` | `cd backend && mvn test` | `cd frontend && npx playwright test`
- RabbitMQ 面板：`http://localhost:15672` | ES 验证：`curl http://localhost:9200`

> 完整页面/Controller/Entity/Store/API 清单已抽取到 memory，需要时我会自动调用。
