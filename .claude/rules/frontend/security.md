---
paths:
  - "**/*.ts"
  - "**/*.tsx"
  - "**/*.js"
  - "**/*.jsx"
  - "**/*.vue"
---
# TypeScript/JavaScript Security

> This file extends [common/security.md](../common/security.md) with TypeScript/JavaScript specific content.

## XSS Prevention

`v-html` 和 `innerHTML` 是 XSS 入口，必须避免直接渲染用户输入：

```vue
<!-- DANGEROUS — 用户输入直接插入 HTML -->
<div v-html="user.bio"></div>

<!-- SAFE — 使用文本插值，自动转义 -->
<div>{{ user.bio }}</div>
```

如果必须渲染 HTML（如富文本），使用 DOMPurify 消毒：

```typescript
import DOMPurify from 'dompurify'

const sanitized = DOMPurify.sanitize(user.bio)
```

## Token Storage

JWT Token 存储选择及理由：

| 方案 | XSS 风险 | CSRF 风险 | 适用场景 |
|------|---------|----------|---------|
| `sessionStorage` | 低（标签页隔离） | 无 | 当前项目默认 |
| `localStorage` | 高（持久化，所有标签页可读） | 无 | 不推荐 |
| `httpOnly cookie` | 无（JS 不可读） | 需 CSRF 防护 | 高安全需求 |

当前项目使用 `sessionStorage`，关闭标签页即清除，每个标签页独立。

```typescript
// stores/user.ts
const token = ref(sessionStorage.getItem('token') || '')

function setUser(data: { token: string }) {
  token.value = data.token
  sessionStorage.setItem('token', data.token)
}

function logout() {
  token.value = ''
  sessionStorage.removeItem('token')
}
```

## Route Guards

前端路由守卫拦截未认证访问：

```typescript
// router/index.ts
import { useUserStore } from '@/stores/user'

router.beforeEach((to, from, next) => {
  const userStore = useUserStore()

  if (to.meta.requiresAuth && !userStore.isLoggedIn) {
    next({ path: '/login', query: { redirect: to.fullPath } })
  } else {
    next()
  }
})
```

路由 meta 标记需要认证的页面：

```typescript
{
  path: '/dashboard',
  component: () => import('@/views/Dashboard.vue'),
  meta: { requiresAuth: true }
}
```

## Input Sanitization

在提交到后端之前，对用户输入做基本清理：

```typescript
function sanitizeInput(input: string): string {
  return input
    .trim()
    .replace(/[<>]/g, '')  // 移除 HTML 标签字符
}
```

## Secrets Management

```typescript
// NEVER — 硬编码
const apiKey = "sk-proj-xxxxx"

// ALWAYS — 环境变量（Vite）
const apiKey = import.meta.env.VITE_API_KEY

if (!apiKey) {
  throw new Error('VITE_API_KEY not configured')
}
```

Vite 环境变量必须以 `VITE_` 为前缀才能暴露给客户端。敏感 key 放在后端，前端不持有。

## Content Security Policy

后端响应头配置 CSP 限制脚本来源，防止 XSS 执行：

```text
Content-Security-Policy: default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'
```

由后端 `filter/` 或 Spring Security 统一配置，前端无需处理。

## CSRF Protection

JWT Bearer Token 方案天然防 CSRF（Token 不在 Cookie 中，浏览器不会自动附带）。如果切换到 Cookie 方案，需要同步添加 CSRF Token。

## Console and Logging

- 禁止 `console.log` 输出敏感信息（Token、密码、用户数据）
- 生产构建移除 console（Vite 默认 `drop_console: true`）

## Third-Party Dependencies

- 定期 `npm audit` 检查已知漏洞
- 锁定依赖版本（`package-lock.json` 提交到仓库）
- 避免引入不必要的依赖

## References

See skill: `security-review` for comprehensive security audits.
