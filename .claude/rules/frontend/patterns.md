---
paths:
  - "**/*.ts"
  - "**/*.tsx"
  - "**/*.js"
  - "**/*.jsx"
---
# TypeScript/JavaScript Patterns

> This file extends [common/patterns.md](../common/patterns.md) with TypeScript/JavaScript specific content.

## Pinia Setup Store Pattern

Use Setup Store for all Pinia stores:

```typescript
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useMessageStore = defineStore('message', () => {
  // State
  const unreadCount = ref(0)
  const messages = ref<Message[]>([])

  // Getters
  const hasUnread = computed(() => unreadCount.value > 0)

  // Actions
  function addMessage(msg: Message) {
    messages.value.unshift(msg)
    unreadCount.value++
  }

  async function markAllRead() {
    await messageApi.markAllRead()
    unreadCount.value = 0
  }

  return { unreadCount, messages, hasUnread, addMessage, markAllRead }
})
```

## Vue 3 Composables Pattern

Extract reusable logic into composables:

```typescript
// composables/useDebounce.ts
import { ref, onUnmounted } from 'vue'

export function useDebounce<T>(value: T, delay: number): T {
  const debouncedValue = ref<T>(value) as { value: T }
  let timeout: ReturnType<typeof setTimeout>

  onUnmounted(() => {
    clearTimeout(timeout)
  })

  const handler = setTimeout(() => {
    debouncedValue.value = value
  }, delay)

  return debouncedValue.value
}

// composables/useWebSocket.ts
import { ref, onMounted, onUnmounted } from 'vue'

export function useWebSocket(url: string) {
  const isConnected = ref(false)
  const lastMessage = ref<unknown>(null)
  let stompClient: any = null

  function connect() {
    // WebSocket connection logic
  }

  function disconnect() {
    if (stompClient) {
      stompClient.disconnect()
      stompClient = null
      isConnected.value = false
    }
  }

  onMounted(connect)
  onUnmounted(disconnect)

  return { isConnected, lastMessage, connect, disconnect }
}
```

## API Module Pattern

Organize API calls by domain:

```typescript
// api/auth.ts
import api from '@/utils/request'

export const authApi = {
  login(data: { username: string; password: string }) {
    return api.post('/auth/login', data)
  },

  register(data: { username: string; password: string; email: string }) {
    return api.post('/auth/register', data)
  },

  getUserInfo() {
    return api.get('/user/info')
  }
}

// api/message.ts
import api from '@/utils/request'

export const messageApi = {
  list(params: { page: number; size: number }) {
    return api.get('/messages', { params })
  },

  markAllRead() {
    return api.post('/messages/mark-all-read')
  }
}
```

## Request Utility Pattern

Centralize Axios configuration:

```typescript
// utils/request.ts
import axios from 'axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'

const api = axios.create({
  baseURL: '/api',
  timeout: 15000
})

// Request interceptor — inject token
api.interceptors.request.use(config => {
  const userStore = useUserStore()
  if (userStore.token) {
    config.headers.Authorization = `Bearer ${userStore.token}`
  }
  return config
})

// Response interceptor — unwrap and handle errors
api.interceptors.response.use(
  response => {
    const res = response.data
    if (res.code !== undefined) {
      if (res.code === 200 || res.code === 0) {
        return res.data !== undefined ? res.data : res
      }
      if (res.code === 401) {
        useUserStore().logout()
        window.location.href = '/login'
      }
    }
    return res
  },
  error => {
    ElMessage.error(error.response?.data?.message || '请求失败')
    return Promise.reject(error)
  }
)

export default api
```

## Element Plus Component Usage

Use Element Plus components with proper typing:

```vue
<template>
  <el-form :model="form" :rules="rules" ref="formRef">
    <el-form-item prop="username" label="用户名">
      <el-input v-model="form.username" placeholder="请输入用户名" />
    </el-form-item>
    <el-form-item prop="password" label="密码">
      <el-input v-model="form.password" type="password" show-password />
    </el-form-item>
    <el-form-item>
      <el-button type="primary" @click="submit" :loading="loading">登录</el-button>
    </el-form-item>
  </el-form>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import type { FormInstance } from 'element-plus'

const formRef = ref<FormInstance>()
const loading = ref(false)

const form = reactive({
  username: '',
  password: ''
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function submit() {
  const valid = await formRef.value?.validate()
  if (!valid) return

  loading.value = true
  try {
    await authApi.login(form)
  } finally {
    loading.value = false
  }
}
</script>
```

## API Response Format

Match the backend `ApiResponse<T>` structure:

```typescript
interface ApiResponse<T> {
  code: number
  message: string
  data: T
}

interface PaginatedResponse<T> {
  code: number
  message: string
  data: T[]
  total: number
  page: number
  size: number
}
```
