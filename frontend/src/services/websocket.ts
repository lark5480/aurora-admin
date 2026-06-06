import Client from 'stompjs'
import SockJS from 'sockjs-client'

/* eslint-disable @typescript-eslint/no-explicit-any -- stompjs/sockjs-client lack proper TS types */
let stompClient: any = null
let onMessageCallback: ((data: any) => void) | null = null
let reconnectTimer: ReturnType<typeof setTimeout> | null = null
let reconnectAttempts = 0
const MAX_RECONNECT_ATTEMPTS = 10
const RECONNECT_DELAY = 3000

function getToken(): string {
  return sessionStorage.getItem('token') || localStorage.getItem('token') || ''
}

function parseUserIdFromToken(): string | null {
  const token = getToken()
  if (!token) return null
  try {
    const parts = token.split('.')
    if (parts.length !== 3) return null
    const payload = JSON.parse(atob(parts[1].replace(/-/g, '+').replace(/_/g, '/')))
    return payload.userId || payload.user_id || payload.sub || null
  } catch {
    return null
  }
}

function scheduleReconnect(): void {
  if (reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
    console.error('[WebSocket] Max reconnect attempts reached')
    return
  }
  reconnectAttempts++
  const delay = RECONNECT_DELAY * Math.min(reconnectAttempts, 5)
  reconnectTimer = setTimeout(() => {
    connect(onMessageCallback)
  }, delay)
}

export function connect(onMessage: ((data: any) => void) | null = null): void {
  if (stompClient && stompClient.connected) {
    return
  }

  const userId = parseUserIdFromToken()
  if (!userId) {
    console.error('[WebSocket] No userId found in token')
    return
  }

  onMessageCallback = onMessage

  const socket = new SockJS('/ws/message')
  stompClient = Client.over(socket)

  stompClient.connect(
    { Authorization: `Bearer ${getToken()}` },
    () => {
      reconnectAttempts = 0
      const subscription = stompClient.subscribe(`/user/${userId}/queue/messages`, (message: any) => {
        if (message.body) {
          try {
            const data = JSON.parse(message.body)
            if (onMessageCallback) {
              onMessageCallback(data)
            }
          } catch (e) {
            console.error('[WebSocket] Parse error:', e)
          }
        }
      })
      stompClient._subscription = subscription
    },
    (error: any) => {
      console.error('[WebSocket] Error:', error)
      if (reconnectTimer) {
        clearTimeout(reconnectTimer)
        reconnectTimer = null
      }
      scheduleReconnect()
    },
  )

  stompClient.onDisconnect = () => {
    scheduleReconnect()
  }
}

export function disconnect(): void {
  if (reconnectTimer) {
    clearTimeout(reconnectTimer)
    reconnectTimer = null
  }
  reconnectAttempts = MAX_RECONNECT_ATTEMPTS
  if (stompClient) {
    if (stompClient._subscription) {
      stompClient._subscription.unsubscribe()
      stompClient._subscription = null
    }
    stompClient.disconnect()
    stompClient = null
    onMessageCallback = null
  }
}

export function isConnected(): boolean {
  return stompClient && stompClient.connected
}
/* eslint-enable @typescript-eslint/no-explicit-any */
