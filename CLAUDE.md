# CLAUDE.md

## 技术栈
- 后端：Java 21, Spring Boot 3, MyBatis-Plus 3, MySQL 8, Redis (Lettuce), RabbitMQ, Elasticsearch 7
- 前端：Vue 3, TypeScript, Vite, Pinia, Element Plus, ECharts

## 项目结构
```
backend/  src/main/java/com/aurora/admin/
  controller/  service/  mapper/  entity/  dto/  config/  filter/
  exception/   task/     aspect/  util/    document/  annotation/
  listener/    repository/
frontend/  src/
  views/  stores/  api/  router/  components/  directives/  services/  utils/  assets/
docs/          # 项目文档（环境搭建指南等）
```

## 开发命令
```bash
cd backend && mvn spring-boot:run    # 后端 8080
cd frontend && npm install && npm run dev   # 前端 3001
```

## 架构要点
- **认证**：JWT Bearer Token + Spring Security + `JwtAuthenticationFilter`（OncePerRequestFilter），`sessionStorage` 存 token
- **鉴权**：`@PreAuthorize("hasRole('ADMIN')")` 方法级权限，前端 `v-permission` 指令（`directives/permission.ts`）按钮级鉴权
- **状态**：Pinia Setup Store 语法（`defineStore('x', () => { ... })`），7 个 store：user / message / config / product / order / address / afterSale
- **路由**：动态路由 — 登录后 `useUserStore().menuTree` 驱动 `router.addRoute('home', ...)` 注册子路由
- **响应**：`ApiResponse<T>` (code, message, data)，`utils/request.ts` 的 axios 响应拦截器自动解包
- **限流**：Bucket4j（登录 5次/15分钟，注册 3次/小时），依赖 Redis
- **消息队列**：RabbitMQ，`MessageProducer` 发送，`@RabbitListener` 消费（订单创建通知、ES 索引重建）
- **搜索引擎**：Elasticsearch 7，`ProductSearchService` 全文搜索，`EsIndexInitializer` 启动时建索引
- **分布式锁**：ShedLock `@SchedulerLock`，定时任务防重

## 配置体系
3 层配置文件：`application.yml`（公共）→ `application-dev.yml`（默认激活，本地开发）→ `application-prod.yml`（线上）

开发环境中间件默认值：MySQL `localhost:3306` / Redis 单机 `localhost:6379` / RabbitMQ `localhost:5672`(admin/admin123) / ES `localhost:9200`。Docker 搭建详见 `docs/00-环境搭建指南.md`。

## 关键约定
- Entity：必须 `@TableName` + `@TableId(type = IdType.AUTO)`，使用 `@Getter @Setter` 不用 `@Data`
- 数据库：`schema.sql` 自动执行（`spring.sql.init.mode=always`），MyBatis-Plus `#{}` 参数化查询
- DI：新代码用构造器注入，旧代码 `@Autowired` 字段注入可接受
- 前端：`<script setup lang="ts">`，interface 定义 props/emits，`import type` 导类型
- 样式：赛博朋克暗色主题（#ff00ff #00ffff #39ff14），`assets/theme.css` + `assets/table.css`

## 调试
- 账号：`admin` / `admin123`（`schema.sql` 首次建库自动创建，SUPER_ADMIN 角色），`user` / `123456`（普通用户）
- 常用检查：`cd frontend && npx tsc --noEmit` | `cd backend && mvn test` | `cd frontend && npx playwright test`
- RabbitMQ 面板：`http://localhost:15672` | ES 验证：`curl http://localhost:9200`

> 完整页面/Controller/Entity/Store/API 清单已抽取到 memory，需要时我会自动调用。
