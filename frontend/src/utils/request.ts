import axios, { type AxiosResponse, type InternalAxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '../stores/user'

function formatDate(date: Date): string {
  const pad = (n: number) => n.toString().padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}

function isIsoDateString(str: string): boolean {
  return /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}/.test(str)
}

function isoToDateTime(isoStr: string): string {
  const d = new Date(isoStr)
  const pad = (n: number) => n.toString().padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

function processData(obj: unknown): unknown {
  if (obj === null || obj === undefined) return obj
  if (obj instanceof Date) return formatDate(obj)
  if (Array.isArray(obj)) return obj.map(processData)
  if (typeof obj === 'object') {
    const plain = JSON.parse(JSON.stringify(obj))
    const result: Record<string, unknown> = {}
    for (const key in plain) {
      if (Object.prototype.hasOwnProperty.call(plain, key)) {
        const val = (plain as Record<string, unknown>)[key]
        if (typeof val === 'string' && isIsoDateString(val)) {
          result[key] = isoToDateTime(val)
        } else if (typeof val === 'object') {
          result[key] = processData(val)
        } else {
          result[key] = val
        }
      }
    }
    return result
  }
  return obj
}

const api = axios.create({
  baseURL: '/api',
  timeout: 15000,
})

api.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const userStore = useUserStore()
  if (userStore.token) {
    config.headers.Authorization = `Bearer ${userStore.token}`
  }
  if (config.data && typeof config.data === 'object') {
    config.data = processData(config.data)
  }
  return config
})

api.interceptors.response.use(
  (response: AxiosResponse) => {
    const res = response.data
    if (res.code !== undefined) {
      if (res.code === 200 || res.code === 0) {
        return res.data !== undefined ? res.data : res
      }
      if (res.code === 401) {
        const userStore = useUserStore()
        userStore.logout()
        window.location.href = '/login'
        return Promise.reject(new Error(res.message || '登录已过期'))
      }
      return Promise.reject(new Error(res.message || '请求失败'))
    }
    return res
  },
  (error) => {
    if (error.response) {
      switch (error.response.status) {
        case 401:
          useUserStore().logout()
          window.location.href = '/login'
          ElMessage.error('登录已过期，请重新登录')
          break
        case 403:
          ElMessage.error(error.response?.data?.message || '没有权限')
          break
        case 404:
          ElMessage.error(error.response?.data?.message || '请求的资源不存在')
          break
        case 500:
          ElMessage.error('服务器错误')
          break
        default:
          ElMessage.error(error.message || '网络错误')
      }
    } else {
      ElMessage.error('网络连接失败')
    }
    return Promise.reject(error)
  },
)

export default api
