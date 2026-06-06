import { defineStore } from 'pinia'
import { ref } from 'vue'
import { resetRouterState } from '../router'

export interface MenuTree {
  id: number
  name: string
  path: string
  icon?: string
  children?: MenuTree[]
}

export const useUserStore = defineStore('user', () => {
  // State
  const token = ref(sessionStorage.getItem('token') || localStorage.getItem('token') || '')
  const username = ref(localStorage.getItem('username') || '')
  const nickname = ref(localStorage.getItem('nickname') || '')
  const email = ref(localStorage.getItem('email') || '')
  const avatar = ref(localStorage.getItem('avatar') || '')
  const role = ref(localStorage.getItem('role') || '')
  const userId = ref<number | null>(null)
  const roles = ref<string[]>([])
  const permissions = ref<string[]>(JSON.parse(sessionStorage.getItem('permissions') || '[]'))
  const menuTree = ref<MenuTree[]>(JSON.parse(sessionStorage.getItem('menuTree') || '[]'))

  // 从现有 token 恢复 roles（页面刷新后）
  function restoreFromToken() {
    if (!token.value) return
    const payload = decodeToken(token.value)
    if (payload && Array.isArray(payload.roles)) {
      roles.value = payload.roles as string[]
    }
  }
  restoreFromToken()

  // 解码 JWT token 获取 payload
  function decodeToken(tokenStr: string): Record<string, unknown> | null {
    try {
      const parts = tokenStr.split('.')
      if (parts.length !== 3) return null
      const payload = parts[1]
      // URL-safe base64 解码
      const base64 = payload.replace(/-/g, '+').replace(/_/g, '/')
      const json = decodeURIComponent(
        atob(base64)
          .split('')
          .map((c) => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
          .join(''),
      )
      return JSON.parse(json)
    } catch {
      // JWT 解码失败时返回 null，不影响登录流程
      return null
    }
  }

  // 权限判断方法
  function hasRole(roleName: string): boolean {
    return roles.value.includes(roleName)
  }

  function hasPermission(permission: string): boolean {
    return permissions.value.includes(permission)
  }

  // 设置用户信息
  function setUser(data: {
    id?: number
    token?: string
    username?: string
    email?: string
    nickname?: string
    avatar?: string
    role?: string
    roles?: string[]
    permissions?: string[]
    menuTree?: MenuTree[]
  }) {
    // 从 token 解码 roles（优先级最高）
    if (data.token) {
      const payload = decodeToken(data.token)
      if (payload && Array.isArray(payload.roles)) {
        roles.value = payload.roles as string[]
      }
    }

    // 处理传入的 roles（覆盖 token 解码的）
    if (data.roles && data.roles.length > 0) {
      roles.value = data.roles
    }

    if (data.id !== undefined) {
      userId.value = data.id
    }

    if (data.token && token.value !== data.token) {
      token.value = data.token
      sessionStorage.setItem('token', data.token)
    }

    if (data.username && username.value !== data.username) {
      username.value = data.username
      localStorage.setItem('username', data.username)
    }

    if (data.nickname !== undefined) {
      nickname.value = data.nickname || ''
      if (data.nickname) {
        localStorage.setItem('nickname', data.nickname)
      } else {
        localStorage.removeItem('nickname')
      }
    }

    const newEmail = data.email || ''
    if (email.value !== newEmail) {
      email.value = newEmail
      localStorage.setItem('email', newEmail)
    }

    if (data.avatar !== undefined && avatar.value !== data.avatar) {
      avatar.value = data.avatar
      if (data.avatar) {
        localStorage.setItem('avatar', data.avatar)
      } else {
        localStorage.removeItem('avatar')
      }
    }

    const newRole = data.role || roles.value[0] || ''
    if (role.value !== newRole) {
      role.value = newRole
      localStorage.setItem('role', newRole)
    }

    if (data.permissions && data.permissions.length > 0) {
      permissions.value = data.permissions
      sessionStorage.setItem('permissions', JSON.stringify(data.permissions))
    }

    if (data.menuTree && data.menuTree.length > 0) {
      menuTree.value = data.menuTree
      sessionStorage.setItem('menuTree', JSON.stringify(data.menuTree))
    }
  }

  // 登出
  function logout() {
    token.value = ''
    username.value = ''
    nickname.value = ''
    email.value = ''
    role.value = ''
    userId.value = null
    roles.value = []
    permissions.value = []
    menuTree.value = []
    sessionStorage.removeItem('token')
    sessionStorage.removeItem('menuTree')
    sessionStorage.removeItem('permissions')
    localStorage.removeItem('token')
    localStorage.removeItem('username')
    localStorage.removeItem('nickname')
    localStorage.removeItem('email')
    localStorage.removeItem('avatar')
    localStorage.removeItem('role')
    resetRouterState()
  }

  return {
    token,
    username,
    nickname,
    email,
    avatar,
    role,
    userId,
    roles,
    permissions,
    menuTree,
    hasRole,
    hasPermission,
    setUser,
    logout,
  }
})
