import { defineStore } from 'pinia'
import { ref } from 'vue'
import {
  addHandler,
  removeHandler,
  isConnected as wsIsConnected,
  connect as wsConnect,
  disconnect as wsDisconnect,
} from '../services/websocket'
import api from '../utils/request'
import { useConfigStore } from './config'

interface Message {
  id: number
  title: string
  content: string
  senderId: number
  senderName: string
  isRead: boolean
  createTime: string
  [key: string]: unknown
}

export const useMessageStore = defineStore('message', () => {
  const unreadCount = ref(0)
  const messages = ref<Message[]>([])
  const isConnected = ref(false)
  let handlerRegistered = false

  function setUnreadCount(count: number): void {
    unreadCount.value = count
  }

  function addMessage(msg: Message): void {
    messages.value.unshift(msg)
    unreadCount.value++
  }

  function connect(): void {
    const configStore = useConfigStore()
    if (!configStore.getBool('ws.enabled', true)) {
      return
    }

    // 注册内部 handler（仅一次，避免重复注册）
    if (!handlerRegistered) {
      addHandler((data: any) => {
        const payload = data?.data || data
        if (payload?.id) {
          addMessage(payload)
        } else if (payload?.unreadCount !== undefined) {
          setUnreadCount(payload.unreadCount)
        }
      })
      handlerRegistered = true
    }

    // 仅在 WebSocket 未连接时才发起新连接
    if (!wsIsConnected()) {
      wsConnect()
    }
    isConnected.value = true
  }

  function onNewMessage(handler: ((data: any) => void) | null): void {
    if (handler) {
      addHandler(handler)
    }
  }

  function offNewMessage(handler: ((data: any) => void) | null): void {
    if (handler) {
      removeHandler(handler)
    }
  }

  async function markAllRead(): Promise<boolean> {
    try {
      await api.put('/messages/read-all')
      setUnreadCount(0)
      return true
    } catch (e) {
      console.error('标记已读失败:', e)
      return false
    }
  }

  async function fetchUnreadCount(): Promise<void> {
    try {
      const res = (await api.get('/messages/unread-count')) as unknown as { count: number }
      unreadCount.value = res?.count ?? 0
    } catch (e) {
      console.error('获取未读消息数失败:', e)
    }
  }

  function disconnect(): void {
    wsDisconnect()
    isConnected.value = false
    handlerRegistered = false
  }

  return {
    unreadCount,
    messages,
    isConnected,
    setUnreadCount,
    addMessage,
    connect,
    onNewMessage,
    offNewMessage,
    markAllRead,
    fetchUnreadCount,
    disconnect,
  }
})
