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
  // 但公开页面（login/register）无需加载菜单，避免因过期 token 触发 403 错误提示
  const publicPaths = ['/', '/login', '/register']
  const userStore = useUserStore()
  if (userStore.token && !publicPaths.includes(window.location.pathname)) {
    try {
      const { menuApi } = await import('./api/menu')
      const menuTree = await menuApi.myMenus()
      if (menuTree && menuTree.length > 0) {
        userStore.setUser({ menuTree: menuTree as any })
        setupRouter(menuTree as any)
      }
    } catch {
      // 预加载失败，token 可能已过期，清除避免后续重复 403 报错
      userStore.logout()
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
