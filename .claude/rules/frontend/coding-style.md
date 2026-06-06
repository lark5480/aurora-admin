---
paths:
  - "**/*.ts"
  - "**/*.tsx"
  - "**/*.js"
  - "**/*.jsx"
---
# TypeScript/JavaScript Coding Style

> This file extends [common/coding-style.md](../common/coding-style.md) with TypeScript/JavaScript specific content.

## Types and Interfaces

Use types to make public APIs, shared models, and component props explicit, readable, and reusable.

### Public APIs

- Add parameter and return types to exported functions, shared utilities, and public class methods
- Let TypeScript infer obvious local variable types
- Extract repeated inline object shapes into named types or interfaces

```typescript
// WRONG: Exported function without explicit types
export function formatUser(user) {
  return `${user.firstName} ${user.lastName}`
}

// CORRECT: Explicit types on public APIs
interface User {
  firstName: string
  lastName: string
}

export function formatUser(user: User): string {
  return `${user.firstName} ${user.lastName}`
}
```

### Interfaces vs. Type Aliases

- Use `interface` for object shapes that may be extended or implemented
- Use `type` for unions, intersections, tuples, mapped types, and utility types
- Prefer string literal unions over `enum` unless an `enum` is required for interoperability

```typescript
interface User {
  id: string
  email: string
}

type UserRole = 'admin' | 'member'
type UserWithRole = User & {
  role: UserRole
}
```

### Avoid `any`

- Avoid `any` in application code
- Use `unknown` for external or untrusted input, then narrow it safely
- Use generics when a value's type depends on the caller

```typescript
// WRONG: any removes type safety
function getErrorMessage(error: any) {
  return error.message
}

// CORRECT: unknown forces safe narrowing
function getErrorMessage(error: unknown): string {
  if (error instanceof Error) {
    return error.message
  }

  return 'Unexpected error'
}
```

## Vue 3 Composition API

Use `<script setup lang="ts">` with TypeScript for all Vue components:

```typescript
<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import type { User } from '@/types'

interface UserCardProps {
  user: User
  onSelect: (id: string) => void
}

const props = defineProps<UserCardProps>()
const emit = defineEmits<{
  (e: 'update', user: User): void
}>()

const isLoading = ref(false)
const userName = computed(() => `${props.user.firstName} ${props.user.lastName}`)

function handleSelect() {
  props.onSelect(props.user.id)
}

onMounted(() => {
  console.log('UserCard mounted')
})
</script>
```

## Pinia Setup Store

Use Setup Store pattern with `defineStore`:

```typescript
// stores/user.ts
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useUserStore = defineStore('user', () => {
  // State
  const token = ref(sessionStorage.getItem('token') || '')
  const username = ref('')
  const role = ref<'admin' | 'member'>('member')

  // Getters
  const isAdmin = computed(() => role.value === 'admin')
  const isLoggedIn = computed(() => !!token.value)

  // Actions
  function setUser(data: { token: string; username: string; role: string }) {
    token.value = data.token
    username.value = data.username
    role.value = data.role as 'admin' | 'member'
    sessionStorage.setItem('token', data.token)
  }

  function logout() {
    token.value = ''
    username.value = ''
    role.value = 'member'
    sessionStorage.removeItem('token')
  }

  return { token, username, role, isAdmin, isLoggedIn, setUser, logout }
})
```

## Immutability

Use spread operator for immutable updates:

```typescript
interface User {
  id: string
  name: string
}

// WRONG: Mutation
function updateUser(user: User, name: string): User {
  user.name = name // MUTATION!
  return user
}

// CORRECT: Immutability
function updateUser(user: Readonly<User>, name: string): User {
  return {
    ...user,
    name
  }
}
```

## Error Handling

Use async/await with try-catch and narrow unknown errors safely:

```typescript
async function loadUser(userId: string): Promise<User> {
  try {
    const result = await userApi.getById(userId)
    return result
  } catch (error: unknown) {
    logger.error('Operation failed', error)
    throw new Error(getErrorMessage(error))
  }
}

function getErrorMessage(error: unknown): string {
  if (error instanceof Error) {
    return error.message
  }
  return 'Unexpected error'
}
```

## Input Validation

Use Zod for schema-based validation and infer types from the schema:

```typescript
import { z } from 'zod'

const userSchema = z.object({
  email: z.string().email(),
  age: z.number().int().min(0).max(150)
})

type UserInput = z.infer<typeof userSchema>

const validated: UserInput = userSchema.parse(input)
```

## Console.log

- No `console.log` statements in production code
- Use proper logging libraries instead
- See hooks for automatic detection
