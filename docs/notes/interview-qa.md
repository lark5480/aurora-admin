# Aurora Admin 电商后台 — 知识笔记与面试 Q&A

> 基于项目实际代码实现整理，涵盖消息队列、限流、权限、WebSocket、售后状态机、搜索引擎、并发控制、数据权限、审计、性能优化、序列化、定时任务、容器化、测试等核心知识点。

---
GitHub:[lark5480/aurora-admin: Spring Boot 3 + Vue 3 电商后台管理系统，集成 RabbitMQ 发件箱、Redis 多层降级、ES 中文搜索、ShedLock 分布式任务](https://github.com/lark5480/aurora-admin)
## 一、可靠消息投递（发件箱模式 + 补偿 + DLQ）

### 知识概述

Aurora Admin 采用「本地消息表 + Publisher Confirm + 定时补偿」三层机制保障 MQ 消息可靠投递。消息先写入 `t_mq_message_log`（status=0），发送成功后通过 Confirm 回调更新为 status=1，失败标记 status=2 由 `MqCompensationTask` 每 2 分钟扫描重发（最多 5 次）。消费端通过 Redis `mq:consumed:{messageId}` 实现幂等去重，消息处理失败时 `basicNack` 不重入队，交由死信队列 `order.notify.dlq` 接管。

### 面试 Q&A

**Q1: 如何保证消息不丢失？从生产端到消费端完整链路是什么？**

**A:** 三层保障：(1) 生产端：消息先落库 `t_mq_message_log`(status=0)，通过 `CorrelationData` 实现 Publisher Confirm 回调，Broker 确认后更新 status=1，拒绝则标记 status=2 等补偿。(2) 补偿层：`MqCompensationTask` 每 2 分钟扫描 status=2 且 retry_count < max_retry(5) 的消息重发，失败后 next_retry_time 延迟 5 分钟。(3) 消费端：Redis `mq:consumed:{messageId}` (TTL 24h) 做幂等去重，处理成功后 `basicAck`，失败 `basicNack(requeue=false)` 进入死信队列 `order.notify.dlq`。

**Q2: 为什么用 INSERT IGNORE 写消息表？message_id 的 UNIQUE 约束起什么作用？**

**A:** `message_id` 设为 `VARCHAR(64) NOT NULL UNIQUE`，`INSERT IGNORE` 保证同一 messageId 重复写入时被静默忽略而非报错。这在补偿任务重发场景下很关键：`MqCompensationTask` 调用 `messageProducer.sendOrderNotification(msg)` 重发时，`MessageProducer` 内部会再次执行 INSERT IGNORE，由于消息已存在不会重复插入，只会更新状态。

**Q3: 死信队列是如何配置的？什么场景下消息会进入 DLQ？**

**A:** 在 `RabbitMQConfig.java` 中，主队列 `order.notify` 通过 `QueueBuilder.durable().withArgument("x-dead-letter-exchange", DLX_EXCHANGE).withArgument("x-dead-letter-routing-key", DLX_ROUTING_KEY)` 绑定死信交换机。当消费者 `basicNack(requeue=false)` 时（如 `OrderNotificationListener` 处理异常），消息被路由到 `order.notify.dlq` 队列。典型场景：DB 写入失败、WebSocket 推送异常等。

**Q4: 补偿任务如何防止多实例重复执行？**

**A:** 使用 ShedLock 分布式锁，`@SchedulerLock(name="mqCompensation", lockAtMostFor="PT5M", lockAtLeastFor="PT30S")`。锁基于 Redis 实现（`RedisLockProvider`），多实例部署时同一时刻只有一个实例执行补偿。`lockAtMostFor` 防止持锁实例宕机后锁永不释放，`lockAtLeastFor` 防止任务执行过快导致下一周期重复触发。

---

## 二、多层降级限流（Redis Lua + Bucket4j 本地降级）

### 知识概述

基于自定义 `@RateLimit` 注解 + AOP 切面实现声明式限流。Redis 可用时通过 Lua 脚本原子执行 `INCR + EXPIRE`（固定窗口计数），不可用时自动降级到本地 Bucket4j 令牌桶。支持 IP / USER / IP_METHOD 三种限流维度，`@Order(1)` 确保在操作日志切面之前执行，被限流的请求不记录日志。

### 面试 Q&A

**Q1: Lua 脚本中为什么只在 count==1 时设置 EXPIRE？**

**A:** 这是固定窗口限流的核心设计。第一次 INCR 返回 1 说明是新窗口的第一个请求，此时设置 EXPIRE 定义窗口长度。后续请求只 INCR 不刷新 TTL，保证窗口边界一致。如果对每次请求都 EXPIRE，会导致滑动窗口效果——只要持续有请求，key 永不过期，限流窗口永远不会重置。

**Q2: Redis 不可用时如何降级？Bucket4j 和 Redis 限流有什么区别？**

**A:** `RateLimitAspect` 在 `@PostConstruct` 时探测 Redis 连通性，设置 `redisAvailable` 标志。运行时 Lua 脚本执行失败也会 catch 异常后 fallback。降级到 Bucket4j 本地令牌桶（`ConcurrentHashMap<String, Bucket>`），使用 `Refill.intervally` 间隔补充策略。区别：Redis 是分布式全局计数（多实例共享），Bucket4j 是 JVM 本地计数（每实例独立，实际限流阈值 = limit × 实例数）。

**Q3: `@Order(1)` 有什么作用？**

**A:** 确保 `RateLimitAspect` 在 `OperationLogAspect` 之前执行。当请求被限流拦截（抛出 `RateLimitException`）时，不会进入 `OperationLogAspect` 记录操作日志，避免无意义的日志记录。

---

## 三、RBAC 权限体系（JWT + Spring Security + 动态路由 + v-permission）

### 知识概述

认证层采用 JWT Bearer Token + `JwtAuthenticationFilter`(OncePerRequestFilter)，无状态会话。鉴权层后端使用 `@EnableMethodSecurity` + `@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")` 方法级权限，前端通过 `v-permission` 指令实现按钮级鉴权。动态路由基于后端返回的 `menuTree` 在 `router.beforeEach` 中调用 `router.addRoute('home', route)` 注册子路由。

### 面试 Q&A

**Q1: JWT 中存了什么信息？为什么 roles 放在 token 而不是每次查库？**

**A:** JWT payload 包含 `subject`(username)、`userId`(Long)、`roles`(List<String>)、`issuedAt`、`expiration`。roles 缓存在 token 中避免每次请求都查数据库获取角色，减少 DB 压力。`JwtAuthenticationFilter` 从 token 解析 roles 后加 `ROLE_` 前缀构造 `SimpleGrantedAuthority`，直接注入 `SecurityContext`。Token 黑名单通过 Redis `blacklist:{token}` 实现，支持主动注销。

**Q2: 前端 v-permission 指令的实现原理？为什么用 removeChild 而不是 v-if？**

**A:** `v-permission` 在 `mounted` 钩子中获取绑定的权限值（支持字符串或数组），调用 `userStore.hasPermission(p)` 检查用户权限列表。无权限时通过 `el.parentNode?.removeChild(el)` 直接移除 DOM 元素。与 `v-if` 的区别：`v-if` 在模板编译时确定，权限数据变化后需要重新渲染；`v-permission` 是运行时指令，更适合从后端动态获取的权限标识，且移除后不可恢复（比隐藏更安全）。

**Q3: 动态路由是如何实现的？为什么用 `router.addRoute('home', ...)` 而不是 `router.addRoute(route)`？**

**A:** 登录后 `router.beforeEach` 检测到 `menuTree` 为空时，调用 `menuApi.myMenus()` 获取后端菜单树，存入 Pinia store 后调用 `setupRouter()`。该函数遍历 `menuType===2` 的菜单项，通过 `componentMap` 映射到前端组件的懒加载函数，然后 `router.addRoute('home', route)` 将路由注册为 `/home` 的子路由。使用命名父路由 `'home'` 确保动态路由正确嵌套在 `AppLayout` 布局下，而非作为顶层路由。

**Q4: `SecurityConfig` 中 userId 是如何传递到 Controller 层的？**

**A:** `JwtAuthenticationFilter` 解析 token 后创建 `UsernamePasswordAuthenticationToken(username, userId, authorities)`，其中 `principal=username`, `credentials=userId`。Controller/Service 层通过 `SecurityContextHolder.getContext().getAuthentication().getCredentials()` 获取 userId（Long 类型）。这在 `OrderServiceImpl`、`AfterSaleServiceImpl` 等多处使用。

---

## 四、WebSocket 实时推送（STOMP + SockJS + JWT 鉴权）

### 知识概述

基于 Spring WebSocket + STOMP 协议 + SockJS 降级实现实时消息推送。连接端点 `/ws/message` 通过 `ChannelInterceptor` 在 STOMP CONNECT 帧中提取 JWT token 完成认证，Principal name 设为 `userId.toString()` 以匹配 `convertAndSendToUser` 的路由。消息流：业务事件 → MQ → `OrderNotificationListener` → `SimpMessagingTemplate.convertAndSendToUser(userId, "/queue/messages", data)`。

### 面试 Q&A

**Q1: WebSocket 连接如何做 JWT 鉴权？为什么 Principal name 必须是 userId.toString()？**

**A:** WebSocket 不经过 HTTP Filter 链，因此在 `WebSocketConfig.configureClientInboundChannel()` 中注册 `ChannelInterceptor`，拦截 STOMP CONNECT 命令，从 native header `Authorization` 中提取 Bearer token（或从 SockJS 的 session attribute 中取 query param `token`），调用 `jwtUtil.validateToken()` 验证后设置 `accessor.setUser(authentication)`。Principal name 必须设为 `userId.toString()` 因为 `convertAndSendToUser` 的第一个参数 `recipientId` 会与 `SimpUserRegistry` 中的 Principal name 匹配，不一致则消息无法路由到目标用户。

**Q2: 消息推送的完整链路是什么？为什么经过 MQ 而不是直接推送？**

**A:** 链路：业务操作 → `MessageProducer.sendOrderNotification()` → RabbitMQ `order.exchange` → `OrderNotificationListener` 消费 → 写入 `t_message` 表 → `SimpMessagingTemplate.convertAndSendToUser()` → 前端 WebSocket 接收。经过 MQ 的好处：(1) 解耦业务与推送，业务不关心推送是否成功；(2) MQ 保证消息不丢，即使 WebSocket 断开，消息已持久化到 DB，用户上线后可查历史；(3) 消费端做幂等去重，避免重复推送。

**Q3: SockJS 的作用是什么？**

**A:** SockJS 提供 WebSocket 降级方案。当浏览器或网络环境不支持 WebSocket 时（如某些企业代理），SockJS 自动降级到 long-polling 或 iframe 等传输方式，API 保持不变。通过 `.withSockJS()` 启用。

---

## 五、售后状态机（状态流转 + 库存回滚 + 订单金额扣减）

### 知识概述

售后流程采用显式状态机：`APPLIED`(待审核) → `COMPLETED`(通过) / `REJECTED`(驳回)。创建时校验订单状态决定售后类型（PAID→仅退款，SHIPPED/COMPLETED→退货退款），审核通过后执行库存恢复（SKU→刷新总库存 / 直接恢复商品库存）、订单金额扣减、订单状态联动（全部退款→REFUNDED，无待审核→恢复原状态）。支持 24 小时自动审核超时机制。

### 面试 Q&A

**Q1: 售后审核通过后库存是如何恢复的？为什么要区分 SKU？**

**A:** `restoreStock()` 方法判断 `item.getSkuId() != null`：有 SKU 时先恢复 SKU 库存 `restoreSkuStock(skuId, quantity)`（`UPDATE t_product_sku SET stock = stock + #{quantity}`），再调用 `refreshProductStock(productId)` 重新计算商品总库存（`SELECT COALESCE(SUM(stock),0) FROM t_product_sku WHERE product_id=?`）。无 SKU 时直接 `restoreProductStock`。区分 SKU 是因为商品总库存应等于所有 SKU 库存之和，不能直接加回商品表。

**Q2: 售后状态流转中如何保证并发安全？**

**A:** 使用乐观锁模式，`afterSaleMapper.updateStatus(id, newStatus, remark, adminId, expectedOldStatus)` 在 SQL 中 WHERE 条件包含 `status = #{expectedOldStatus}`，返回受影响行数。`affected == 0` 说明状态已被其他请求修改，抛出 `BusinessException("审核失败，售后状态已变更")`。订单状态变更同理。

**Q3: 部分退款后订单状态如何处理？**

**A:** 审核通过后检查该订单所有 OrderItem 是否均已 REFUNDED：(1) 全部退款 → 订单状态更新为 REFUNDED；(2) 部分退款 → 调用 `restoreOrderIfNoPending()`，批量查询该订单所有明细行的 APPLIED 状态售后记录，如果没有待审核的售后单，则将订单从 REFUNDING 恢复到 `originalOrderStatus`（从售后记录中保存的原始状态），否则保持 REFUNDING 不变。

---

## 六、Elasticsearch 中文检索（IK 分词 + 启动建索引 + 降级）

### 知识概述

基于 Spring Data Elasticsearch 8 + IK 中文分词器实现商品全文搜索。`EsIndexInitializer` 实现 `ApplicationRunner` 在启动时检查索引是否存在/是否有数据，自动创建索引并全量同步。`ProductDocument` 中 name 使用 `ik_smart`（粗粒度），description 使用 `ik_max_word`（细粒度提高召回率）。ES 不可用时启动初始化静默跳过，不阻塞应用启动。

### 面试 Q&A

**Q1: name 用 ik_smart、description 用 ik_max_word 的原因是什么？**

**A:** `ik_smart` 做最粗粒度分词（如"中华人民共和国"→"中华人民共和国"），适合短文本精确搜索，减少噪音；`ik_max_word` 做最细粒度分词（如"中华人民共和国"→"中华人民共和国/中华人民/中华/华人/人民/共和国/..."），提高长文本召回率。name 字段索引和搜索均用 `ik_smart`；description 字段索引时用 `ik_max_word`（细粒度提高召回率），搜索时用 `ik_smart`（粗粒度减少噪声），这是"索引细粒度 + 搜索粗粒度"的经典策略——保证用户输入的搜索词粗粒度切分后与索引中的细粒度词项匹配。

**Q2: 启动时如何保证 ES 索引和数据就绪？**

**A:** `EsIndexInitializer` 实现 `ApplicationRunner`，覆盖四种场景：(1) 首次启动索引不存在 → `create()` + `putMapping()` + `reindexAll()` 全量同步；(2) ES 启动比后端慢 → 连接异常 catch 后仅 warn，下次重启重试；(3) MySQL 重建后索引存在但无数据 → `docCount==0` 时触发 `reindexAll()`；(4) 正常重启 → 索引和数据均存在，跳过。`ProductSearchService` 设为 `@Autowired(required=false)` 避免 ES 不可用时启动失败。

**Q3: 搜索查询是如何构建的？为什么用 bool 查询而不是 query_string？**

**A:** 使用 ES bool 查询构建器：keyword 非空时用 `should(match name)` + `should(match description)` 实现 OR 语义匹配两个字段；keyword 为空时用 `must(matchAll)` 返回全部。分类和状态用 `filter(term)` 精确匹配（不计算评分，有缓存优势）。相比 `query_string`，bool 查询可以精确控制每个字段的匹配逻辑和评分权重，且 filter 上下文不计算 _score 性能更好。

---

## 七、幂等与并发控制（Redis setIfAbsent + 乐观锁 + 雪花 ID）

### 知识概述

下单和支付接口通过 Redis `setIfAbsent(key, value, TTL)` 实现分布式幂等锁，防止重复提交。库存扣减使用 SQL 级乐观锁 `WHERE stock >= #{quantity}` 保证不超卖。订单号/售后单号使用 MyBatis-Plus `IdWorker` 雪花算法生成全局唯一 ID，碰撞概率极低但保留 1 次重试兜底。

### 面试 Q&A

**Q1: 下单幂等和支付幂等的实现有什么区别？为什么支付需要两阶段状态？**

**A:** 下单幂等：key 格式为 `order:idempotent:{userId}:{idempotentKey}`，`setIfAbsent("1", 10min)`，包含 userId 维度防止不同用户的 idempotentKey 互相冲突。成功获得锁后执行业务，异常时 delete key 允许重试，成功时 set 为 "success" 继续防重。支付幂等采用两阶段：先 `setIfAbsent("processing", 5min)` 获得锁，业务成功后更新值为 "success"。重复请求到来时：值为 "success" → 幂等返回成功结果（不重复扣款）；值为 "processing" → 拒绝并提示"处理中"。两阶段设计是因为支付成功后需要幂等返回结果，而下单只需防重。

**Q2: 乐观锁扣库存为什么用 `stock >= #{quantity}` 而不是 `@Version`？**

**A:** 使用 `UPDATE ... SET stock = stock - #{quantity} WHERE id = #{id} AND stock >= #{quantity}` 是数据库层面的条件更新，返回受影响行数 0 表示库存不足。相比 MyBatis-Plus `@Version` 乐观锁（需要额外 version 字段和重试循环），这种方式更简洁：(1) 无需额外字段；(2) 一次 SQL 完成检查+扣减，无 CAS 重试；(3) 天然防超卖，并发安全。

**Q3: 雪花 ID 生成订单号如何防碰撞？**

**A:** `IdWorker.getIdStr()` 基于 MyBatis-Plus 内置雪花算法，生成全局递增的 Long 型 ID（时间戳+机器ID+序列号），碰撞概率极低。代码中额外做了 1 次重试兜底：生成候选 orderNo 后查库 `orderMapper.findByOrderNo(candidate)`，若已存在则重新生成，最多重试 2 次，仍碰撞则抛异常"系统繁忙"。

---

## 八、数据权限插件（InnerInterceptor + jsqlparser SQL 改写）

### 知识概述

基于 MyBatis-Plus `InnerInterceptor` 实现数据权限拦截器，在 SQL 执行前通过 jsqlparser 解析并改写 SELECT 语句，自动追加 `WHERE user_id = ?` 条件。`@DataScope` 注解标注在 Mapper 方法上指定用户列名。Role 实体定义了 4 个值：1=全部、2=本部门及下级、3=本部门、4=仅本人。当前 Phase 1 实现了 1（全部，不追加条件）和 4（仅本人，追加 `WHERE user_id = ?`），2 和 3 为占位预留。多角色用户取 `MIN(data_scope)` 即最宽权限。

### 面试 Q&A

**Q1: DataScopeInterceptor 是如何工作的？为什么不直接在 Mapper XML 中写条件？**

**A:** 工作流程：(1) 从 `MappedStatement` 反射获取 Mapper 方法上的 `@DataScope` 注解；(2) 通过 `SecurityUtils.getCurrentUserId()` 获取当前用户 ID，查 `RoleMapper.findMinDataScopeByUserId()` 获取最宽权限；(3) scope=4 时用 jsqlparser 解析原始 SQL，在 WHERE 子句中追加 `AND userColumn = currentUserId`；(4) 通过 `PluginUtils.mpBoundSql(boundSql).sql()` 替换 SQL。不在 Mapper 中硬编码是因为数据权限是横切关注点，与业务逻辑无关，通过拦截器统一处理避免每个查询重复编写。

**Q2: SQL 解析失败时为什么抛异常而不是跳过？**

**A:** 代码中 `catch (Exception e)` 后 `throw new SQLException("数据权限 SQL 解析失败")`，这是安全优先的设计。如果解析失败时跳过权限追加，等于给了用户全部数据的访问权限，造成数据泄露。抛出异常中断查询，宁可功能不可用也不能泄露数据。

**Q3: `findMinDataScopeByUserId` 为什么用 MIN 而不是 MAX？**

**A:** `data_scope` 数值越小权限越大（1=全部, 4=仅本人）。一个用户可能有多个角色，取 `MIN(data_scope)` 得到数值最小的 = 权限最宽的角色。例如用户同时是 ADMIN(scope=1) 和 USER(scope=4)，MIN=1 即拥有全部数据权限。这符合"多角色取最宽权限"的设计原则。

---

## 九、AOP 操作审计

### 知识概述

`OperationLogAspect` 通过 `@Around` 切面拦截所有 Controller 方法（`execution(* com.aurora.admin.controller..*.*(..))`），自动记录操作人、HTTP method、URL、IP、参数、耗时等信息到 `t_operation_log` 表。日志记录失败不影响主业务流程。

### 面试 Q&A

**Q1: 操作日志切面如何保证不影响主流程？**

**A:** 两层保护：(1) `saveOperationLog()` 调用被 `try-catch(Exception)` 包裹，日志记录失败时静默处理不抛出异常；(2) 切面在 `joinPoint.proceed()` 之后执行日志记录，即使日志写入失败，业务方法已经成功执行并返回结果。

**Q2: 限流切面和日志切面的执行顺序如何控制？**

**A:** `RateLimitAspect` 标注 `@Order(1)`，`OperationLogAspect` 未标注 `@Order`（默认 `Ordered.LOWEST_PRECEDENCE`，值最大）。Spring AOP 按 Order 值升序执行，因此限流切面先执行。当请求被限流拦截抛出异常时，不会进入 Controller 方法，也不会触发日志切面的日志记录逻辑。

---

## 十、N+1 查询优化（批量预加载 + Map 组装）

### 知识概述

项目严格遵循"禁止循环中单条查询"的约定，Mapper 层提供 `findByIds` / `findByProductIds` / `findByOrderIds` 等批量查询方法（`@Select` + `<script>` + `<foreach>`），Service 层先批量加载到 Map，再遍历组装响应，彻底消除 N+1 问题。

### 面试 Q&A

**Q1: 项目中如何系统性解决 N+1 查询问题？**

**A:** 约定 + 工具双管齐下。约定：Mapper 层必须提供 `findByIds` / `findByProductIds` / `findByOrderIds` 等批量方法，使用 `@Select("<script>SELECT ... WHERE id IN <foreach>...</foreach></script>")`。Service 层遵循"先批量加载 → 构建 Map → 遍历组装"模式。例如 `OrderServiceImpl.createOrder()` 中，先从购物车提取所有 productId 集合，一次性 `productMapper.findByIds(productIds)` 加载为 `Map<Long, Product>`，然后遍历购物车时从 Map 取值，避免 N 次单条查询。

**Q2: 批量查询时如何处理空集合？**

**A:** 所有批量查询前都做空集合保护：`Set<Long> productIds = ...; Map<Long, Product> productMap = productIds.isEmpty() ? Collections.emptyMap() : productMapper.findByIds(productIds).stream().collect(...)`。避免传入空集合导致 SQL `IN ()` 语法错误。

---

## 十一、Jackson 序列化（Long→String 全局防精度丢失）

### 知识概述

`JacksonConfig` 全局注册 `ToStringSerializer` 将 `Long` 包装类型序列化为 String，防止前端 JavaScript `Number.MAX_SAFE_INTEGER`（2^53 - 1）精度丢失导致 ID 不准确。同时配置 `LocalDateTime` 格式化为 `yyyy-MM-dd HH:mm:ss`。

### 面试 Q&A

**Q1: 为什么 Long 要序列化为 String？primitive long 受影响吗？**

**A:** JavaScript 的 `Number` 类型使用 IEEE 754 双精度浮点，安全整数范围为 -(2^53-1) 到 2^53-1。Java Long 最大值为 2^63-1，超出 JS 安全范围后前端收到的 ID 会丢失精度（末几位变为 0）。通过 `ToStringSerializer` 将 `Long` 包装类型序列化为 JSON String，前端以字符串形式接收不受精度限制。`long` 原始类型不受影响（`longModule.addSerializer(Long.class, ...)` 仅注册了包装类型的序列化器），这也是设计意图——Entity ID 字段使用 `Long` 包装类型，自动获得 String 序列化；而统计数量等 `long` 字段保持数字类型。

---

## 十二、分布式定时任务（ShedLock + Redis）

### 知识概述

使用 ShedLock 框架 + Redis 分布式锁防止多实例重复执行定时任务。`ShedLockConfig` 配置 `RedisLockProvider`，所有定时任务标注 `@SchedulerLock(name, lockAtMostFor, lockAtLeastFor)`。当前有 5 个定时任务：MQ 补偿(2min)、售后自动审核(10min)、公告处理(60s)、日志清理(每月1号)、每日统计(凌晨1点，含7天回填容错)。

### 面试 Q&A

**Q1: ShedLock 的 lockAtMostFor 和 lockAtLeastFor 分别解决什么问题？**

**A:** `lockAtMostFor` 解决持锁实例宕机问题——如果执行任务的实例在执行过程中崩溃，锁会在 `lockAtMostFor` 后自动释放，其他实例可以接管。`lockAtLeastFor` 防止任务执行过快导致下一调度周期重复执行——例如任务 2 秒完成但 `lockAtLeastFor="PT30S"`，则 30 秒内其他实例不会重复执行，保证最小间隔。

**Q2: 为什么用 Redis 实现分布式锁而不是数据库？**

**A:** 项目已有 Redis 基础设施（限流、缓存、黑名单），使用 `RedisLockProvider` 零额外运维成本。相比数据库锁，Redis 锁性能更好（内存操作，无磁盘 IO），且 ShedLock 的 Redis 实现已处理锁续期、过期等边界情况。

---

## 十三、Docker Compose 编排（健康检查 + 多阶段构建）

### 知识概述

`docker-compose.yml` 编排 6 个服务（MySQL 8 / Redis 7 / RabbitMQ 3 / ES 8 / Spring Boot / Vue+Nginx）。所有中间件配置健康检查，后端通过 `depends_on.condition: service_healthy` 确保中间件就绪后才启动。前后端均采用多阶段构建 Dockerfile 减小镜像体积。另有 `docker/elasticsearch/Dockerfile` 基于官方 ES 8 镜像安装 IK 中文分词插件。

### 面试 Q&A

**Q1: 多阶段构建 Dockerfile 的好处是什么？**

**A:** 后端 Dockerfile 分两阶段：Stage 1 使用 `maven:3.9-eclipse-temurin-21-alpine` 构建 JAR（包含完整 JDK 和 Maven），Stage 2 仅使用 `eclipse-temurin:21-jre-alpine` 运行（仅 JRE，约 200MB vs 完整 JDK 约 500MB+）。前端同理：Stage 1 `node:20-alpine` 构建，Stage 2 `nginx:alpine` 托管静态文件。好处：最终镜像不含编译工具链，体积大幅减小；减少攻击面（无 Maven/npm/Node 等不必要组件）。

**Q2: `depends_on: condition: service_healthy` 解决了什么问题？**

**A:** 确保后端容器只在所有中间件健康检查通过后才启动。例如 MySQL 容器启动后需要几秒初始化，如果后端立即连接可能失败。通过 `condition: service_healthy`，Docker Compose 等待 MySQL 的 `mysqladmin ping` 成功后才启动后端，避免启动时连接失败。

**Q3: Redis 配置了哪些优化参数？**

**A:** `redis-server --appendonly yes --maxmemory 256mb --maxmemory-policy allkeys-lru`：(1) AOF 持久化保障数据不丢失；(2) 256MB 内存上限防止 OOM；(3) `allkeys-lru` 淘汰策略——内存满时淘汰最近最少使用的 key，适合缓存场景。

---

## 十四、单元测试（Mockito + JUnit 5）

### 知识概述

测试覆盖 12 个文件，涵盖 Controller 层（AuthController、UserController）、Service 层（Order、Payment、AfterSale、Product、ShoppingCart、Notice、File、User、MessageProducer）和 DTO 层。使用 Mockito `@ExtendWith(MockitoExtension.class)` + `@Mock` / `@InjectMocks` 隔离依赖，关键场景包括幂等防重、乐观锁失败、权限校验等。

### 面试 Q&A

**Q1: 订单服务的单元测试覆盖了哪些边界场景？**

**A:** `OrderServiceImplTest`（544 行）覆盖：(1) 正常下单流程（mock 购物车→商品校验→乐观锁扣库存→MQ 通知）；(2) 幂等检查 — `setIfAbsent` 返回 false 时抛 `BusinessException("请勿重复提交订单")`；(3) 乐观锁扣库存失败 — `deductProductStock` 返回 0 时抛异常；(4) 商品已下架 — status != "ON_SALE" 时拒绝；(5) SKU 库存不足；(6) 购物车为空/未选择商品；(7) 异常时幂等 key 删除。

**Q2: 测试如何隔离外部依赖（Redis、MQ、DB）？**

**A:** 使用 Mockito `@Mock` 注入所有外部依赖：`StringRedisTemplate`、`ValueOperations`、`MessageProducer`、各 Mapper。例如测试幂等时 `when(valueOps.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true)` 模拟 Redis 返回成功。MQ 发送 `verify(messageProducer).sendOrderNotification(argThat(...))` 验证参数。所有测试不依赖真实中间件，纯内存运行。

---

## 十五、统一响应与前后端解包（ApiResponse + axios）

### 知识概述

后端所有 Controller 统一返回 `ApiResponse(code, message, data)`，其中 `code=200` 表示成功，`data` 字段携带业务数据。前端 `utils/request.ts` 配置 axios 实例（baseURL `/api`，超时 15s），响应拦截器自动解包：`code === 200 || code === 0` 时返回 `res.data`（即 ApiResponse 的 data 字段），业务层无需重复解包。请求拦截器从 Pinia store 获取 token 附加 `Authorization: Bearer ${token}` 头。

### 面试 Q&A

**Q1: 前后端响应格式是如何对齐的？为什么前端要解包？**

**A:** 后端 Controller 统一返回 `ApiResponse.success(data)` → `{code: 200, message: "success", data: ...}`。前端 axios 响应拦截器判断 `res.code === 200` 后直接 `return res.data`，因此业务层拿到的就是 data 字段的内容（如订单对象、分页列表等），无需手动 `response.data.data` 解包。错误场景（code 非 200）时 `Promise.reject(new Error(res.message))`，由 UI 层统一 catch 处理。

**Q2: 401 未授权场景前后端如何协作？**

**A:** 两种触发路径：(1) 业务层返回 `ApiResponse.error(401, "未登录或登录已过期")`，前端响应拦截器调用 `userStore.logout()` 清除本地 token 并跳转 `/login`；(2) HTTP 层面 401（如 Spring Security 拒绝），前端拦截器同样执行 logout + 跳转。双重保障确保 token 失效或过期后用户被正确引导重新登录。

---

## 十六、数据库初始化（schema.sql + INSERT IGNORE）

### 知识概述

项目通过 `spring.sql.init.mode=always` 自动执行 `schema.sql`（首次启动或 schema 变更时）。所有建表语句使用 `CREATE TABLE IF NOT EXISTS`，初始化数据全部使用 `INSERT IGNORE`，保证重复执行不报错。初始化数据涵盖：系统配置（7 条）、角色（3 条）、默认用户（2 条，BCrypt 加密）、部门（1 条）、菜单（22 条）、角色-菜单关联、公告（3 条）、商品分类（4 条）、商品（10 条）、SKU 规格（8 条）。

### 面试 Q&A

**Q1: 为什么用 INSERT IGNORE 而不是 INSERT？与 MQ 消息表的 INSERT IGNORE 有何共性？**

**A:** `INSERT IGNORE` 在遇到主键/唯一键冲突时静默忽略而非报错。schema.sql 可能因应用重启、多实例并发启动等场景被多次执行，`INSERT IGNORE` 保证幂等。这与 MQ 消息表的 `message_id` UNIQUE 约束 + `INSERT IGNORE` 的设计思路一致：都是通过"唯一约束 + 忽略冲突"实现幂等写入，无需先查后插的额外逻辑。

**Q2: 初始化数据中哪些是业务数据、哪些是配置数据？如何区分维护？**

**A:** 配置数据（`t_system_config`）：站点名称、Logo、上传限制、注册开关等，可通过管理后台动态修改。基础数据（角色、菜单、默认用户、部门）：系统运行的前置条件，通常部署后不再变更。业务种子数据（商品、分类、公告）：仅用于开发和演示，生产环境应通过管理后台录入或清空。

---

## 十七、乐观锁 + 碰撞重试（库存扣减 + 订单号生成）

### 知识概述

项目有两处乐观锁应用：(1) 库存扣减：`UPDATE t_product SET stock = stock - #{quantity} WHERE id = #{id} AND stock >= #{quantity}`，affected=0 时直接抛异常，**不做重试**（库存不足是业务失败，非碰撞）；(2) 订单号生成：雪花算法 `IdWorker.getIdStr()` 生成候选订单号后查库验证唯一性，碰撞时重试 1 次（共 2 次尝试），仍碰撞则抛 `BusinessException("系统繁忙，请稍后重试")`。

### 面试 Q&A

**Q1: 库存扣减为什么不做重试？与售后状态机的乐观锁有什么区别？**

**A:** 库存扣减 `affected=0` 意味着**库存不足**（业务失败），而非并发冲突。即使确实是并发导致的超卖防护，重试也无法解决（库存确实不够）。售后状态机的乐观锁 `affected=0` 意味着**状态已被其他请求修改**（并发冲突），抛出异常让调用方决定重试策略，但代码中也没有自动重试——因为售后审核是低频操作，并发冲突概率极低。两者都是"检测冲突 → 抛异常"模式，区别在于业务语义不同。

**Q2: 订单号生成为什么需要重试？雪花算法不是全局唯一吗？**

**A:** 雪花算法在单实例内全局唯一，但多实例部署时（不同 machineId）理论上仍可能碰撞（时钟回拨、配置错误等）。代码中保留 1 次重试作为兜底：生成候选 orderNo 后查库 `orderMapper.findByOrderNo(candidate)`，若已存在则重新生成。这是"理论风险极低 + 极小成本兜底"的典型实践。

---

## 十八、文件上传安全（类型白名单 + 路径穿越防护）

### 知识概述

`FileServiceImpl` 实现多层文件上传安全防护：(1) **类型白名单**：从配置 `upload.allowed_types` 读取允许的扩展名列表，上传文件扩展名转小写后匹配，不匹配则拒绝；(2) **文件名随机化**：落盘文件名使用 `UUID.randomUUID() + 扩展名`，原始文件名仅存入数据库，杜绝路径穿越；(3) **路径隔离**：落盘路径 `{uploadDir}/{yyyy/MM/dd}/{UUID.ext}`，上传时检查 `relativePath.contains("..")`；(4) **下载防护**：双重验证——检查 `filePath.contains("..")` + `getCanonicalPath()` 解析后验证必须以 `uploadDir` 开头；(5) **文件大小限制**：默认 10MB，可配置；(6) **孤儿文件清理**：DB 写入失败时自动删除已落盘的文件。

### 面试 Q&A

**Q1: 为什么不直接用原始文件名？UUID 重命名有什么好处？**

**A:** 原始文件名可能包含特殊字符（`../`、`%00` 等），直接落盘存在路径穿越风险。UUID 重命名后：(1) 彻底消除路径穿越可能；(2) 文件名唯一，不会覆盖已有文件；(3) 扩展名保留，便于浏览器识别 MIME 类型。原始文件名存入数据库 `file_name` 字段供下载时展示。

**Q2: 下载时为什么需要双重防护（contains + canonicalPath）？**

**A:** `contains("..")` 是第一层快速拦截，但可能被编码绕过（如 `%2e%2e%2f`）。`getCanonicalPath()` 解析规范路径后验证是否以 `uploadDir` 开头，是最终的确定性检查。两层结合兼顾性能和安全性——大部分非法请求在第一层被拦截，少数绕过第一层的在第二层被捕获。
