import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import AppComponent from './App.vue'
import router, { setupRouter } from './router'
import './assets/theme.css'
import './assets/table.css'
import { vPermission } from './directives/permission'
import { useUserStore } from './stores/user'
import { useConfigStore } from './stores/config'

async function init() {
  const app = createApp(AppComponent)

  for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
    app.component(key, component)
  }

  app.use(createPinia())

  // 在 router 初始化之前加载动态路由，避免页面刷新时 "No match found" 警告
  const userStore = useUserStore()
  if (userStore.token) {
    try {
      const { menuApi } = await import('./api/menu')
      const menuTree = await menuApi.myMenus()
      if (menuTree && menuTree.length > 0) {
        userStore.setUser({ menuTree: menuTree as any })
        setupRouter(menuTree as any)
      }
    } catch {
      // 预加载失败，beforeEach 守卫会重试
    }
  }

  app.use(router)
  app.use(ElementPlus)
  app.directive('permission', vPermission)

  app.mount('#app')

  // 动态标题
  const configStore = useConfigStore()
  configStore.fetchConfigs().then(() => {
    document.title = configStore.get('site.name', '登录注册系统')
  })
}

init()
