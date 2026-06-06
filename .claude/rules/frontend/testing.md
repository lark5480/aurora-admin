---
paths:
  - "**/*.ts"
  - "**/*.tsx"
  - "**/*.js"
  - "**/*.jsx"
---
# TypeScript/JavaScript Testing

> This file extends [common/testing.md](../common/testing.md) with TypeScript/JavaScript specific content.

## Test Framework

- **Vitest** — Unit and integration testing (fast, Vite-native)
- **@vue/test-utils** — Vue component testing
- **Playwright** — E2E testing for critical user flows
- **MSW (Mock Service Worker)** — API mocking in tests

## Unit Test Pattern (Vitest)

```typescript
// utils/__tests__/format.test.ts
import { describe, it, expect } from 'vitest'
import { formatDate, formatCurrency } from '../format'

describe('formatDate', () => {
  it('formats date correctly', () => {
    const date = new Date('2024-01-15')
    expect(formatDate(date)).toBe('2024-01-15')
  })

  it('returns empty string for null', () => {
    expect(formatDate(null)).toBe('')
  })
})
```

## Component Test Pattern (@vue/test-utils)

```typescript
// components/__tests__/UserCard.test.ts
import { describe, it, expect, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import UserCard from '../UserCard.vue'

describe('UserCard', () => {
  it('displays user name', () => {
    const wrapper = mount(UserCard, {
      props: {
        user: { id: '1', name: 'Alice', email: 'alice@example.com' }
      }
    })

    expect(wrapper.text()).toContain('Alice')
  })

  it('emits select event with user id', async () => {
    const wrapper = mount(UserCard, {
      props: { user: { id: '1', name: 'Alice', email: 'alice@example.com' } }
    })

    await wrapper.find('button').trigger('click')

    expect(wrapper.emitted('select')).toBeTruthy()
    expect(wrapper.emitted('select')[0]).toEqual(['1'])
  })
})
```

## Pinia Store Test Pattern

```typescript
// stores/__tests__/user.test.ts
import { describe, it, expect, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useUserStore } from '../user'

describe('useUserStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('sets user and persists token', () => {
    const store = useUserStore()

    store.setUser({ token: 'abc', username: 'alice', role: 'admin' })

    expect(store.token).toBe('abc')
    expect(store.username).toBe('alice')
    expect(store.isAdmin).toBe(true)
  })

  it('clears user on logout', () => {
    const store = useUserStore()

    store.setUser({ token: 'abc', username: 'alice', role: 'admin' })
    store.logout()

    expect(store.token).toBe('')
    expect(store.username).toBe('')
  })
})
```

## API Integration Test Pattern (MSW)

```typescript
// api/__tests__/auth.test.ts
import { describe, it, expect, beforeAll, afterAll } from 'vitest'
import { setupServer } from 'msw/node'
import { http, HttpResponse } from 'msw'
import { authApi } from '../auth'

const server = setupServer(
  http.post('/api/auth/login', ({ request }) => {
    return HttpResponse.json({
      code: 200,
      data: { token: 'test-token', username: 'alice' }
    })
  })
)

beforeAll(() => server.listen())
afterAll(() => server.close())

describe('authApi', () => {
  it('login returns token on success', async () => {
    const result = await authApi.login({ username: 'alice', password: '123456' })

    expect(result.token).toBe('test-token')
  })
})
```

## E2E Test Pattern (Playwright)

```typescript
// e2e/login.spec.ts
import { test, expect } from '@playwright/test'

test.describe('Login Flow', () => {
  test('successful login redirects to dashboard', async ({ page }) => {
    await page.goto('/login')

    await page.fill('[placeholder="用户名"]', 'admin')
    await page.fill('[placeholder="密码"]', 'admin123')
    await page.click('button:has-text("登录")')

    await expect(page).toHaveURL('/dashboard')
    await expect(page.locator('text=欢迎回来')).toBeVisible()
  })

  test('failed login shows error message', async ({ page }) => {
    await page.goto('/login')

    await page.fill('[placeholder="用户名"]', 'wrong')
    await page.fill('[placeholder="密码"]', 'wrong')
    await page.click('button:has-text("登录")')

    await expect(page.locator('text=用户名或密码错误')).toBeVisible()
  })
})
```

## Test File Organization

```
frontend/src/
  api/
    __tests__/           # API integration tests
  components/
    __tests__/           # Component unit tests
  composables/
    __tests__/           # Composable unit tests
  stores/
    __tests__/           # Store tests
  utils/
    __tests__/           # Utility function tests
e2e/
  *.spec.ts               # Playwright E2E tests
```

## Coverage

- Target 80%+ line coverage
- Use `vitest --coverage` with v8 provider
- Focus on business logic and critical paths

## References

See skill: `ui-ux-pro-max` for UI component testing patterns.
See skill: `agent-browser` for E2E testing with Playwright.
