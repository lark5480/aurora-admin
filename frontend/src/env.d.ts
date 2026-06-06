/// <reference types="vite/client" />

declare module '*.vue' {
  import type { DefineComponent } from 'vue'
  const component: DefineComponent<Record<string, unknown>, Record<string, unknown>, unknown>
  export default component
}

declare module 'stompjs' {
  const Client: any
  export default Client
}

declare module 'sockjs-client' {
  const SockJS: any
  export default SockJS
}
