import { defineStore } from 'pinia'
import { ref } from 'vue'
import api from '../utils/request'
import { connect as wsConnect, disconnect as wsDisconnect } from '../services/websocket'
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

  function setUnreadCount(count: number): void {
    unreadCount.value = count
  }

  function addMessage(msg: Message): void {
    messages.value.unshift(msg)
    unreadCount.value++
  }

  function connect(onMessage?: (data: Message) => void): void {
    const configStore = useConfigStore()
    if (!configStore.getBool('ws.enabled', true)) {
      return
    }
    if (isConnected.value) return
    wsConnect((data: Message) => {
      addMessage(data)
      if (onMessage) onMessage(data)
    })
    isConnected.value = true
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
  }

  return {
    unreadCount,
    messages,
    isConnected,
    setUnreadCount,
    addMessage,
    connect,
    markAllRead,
    fetchUnreadCount,
    disconnect,
  }
})
