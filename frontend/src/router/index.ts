import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { useUserStore } from '../stores/user'

interface MenuItem {
  id: number
  name: string
  path: string
  component?: string
  menuType?: number
  children?: MenuItem[]
  [key: string]: unknown
}

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    redirect: '/login',
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue'),
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('../views/Register.vue'),
  },
  {
    path: '/home',
    name: 'home',
    component: () => import('../components/layout/AppLayout.vue'),
    meta: { requiresAuth: true },
    children: [
      { path: '', name: 'Home', component: () => import('../views/Home.vue') },
      {
        path: 'profile',
        name: 'UserProfile',
        component: () => import('../views/user/UserProfile.vue'),
        meta: { requiresAuth: true },
      },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

// Component path mapping for dynamic imports
const componentMap: Record<string, () => Promise<unknown>> = {
  'system/DeptManagement': () => import('../views/system/DeptManagement.vue'),
  'system/RoleManagement': () => import('../views/system/RoleManagement.vue'),
  'system/MenuManagement': () => import('../views/system/MenuManagement.vue'),
  'system/SystemSettings': () => import('../views/system/SystemSettings.vue'),
  'user/UserManagement': () => import('../views/user/UserManagement.vue'),
  'statistics/Statistics': () => import('../views/statistics/Statistics.vue'),
  'file/FileManagement': () => import('../views/file/FileManagement.vue'),
  'message/MessageCenter': () => import('../views/message/MessageCenter.vue'),
  'log/OperationLogs': () => import('../views/log/OperationLogs.vue'),
  'notice/NoticeManagement': () => import('../views/notice/NoticeManagement.vue'),
  'user/UserProfile': () => import('../views/user/UserProfile.vue'),
  'product/CategoryManagement': () => import('../views/product/CategoryManagement.vue'),
  'product/ProductManagement': () => import('../views/product/ProductManagement.vue'),
  'order/ShoppingCart': () => import('../views/order/ShoppingCart.vue'),
  'order/OrderManagement': () => import('../views/order/OrderManagement.vue'),
  'order/AddressManagement': () => import('../views/order/AddressManagement.vue'),
  'order/AfterSaleManagement': () => import('../views/order/AfterSaleManagement.vue'),
}

// 动态路由是否已加载
let dynamicRoutesReady = false

router.beforeEach(async (to, _from, next) => {
  const userStore = useUserStore()

  if (userStore.token && to.meta.requiresAuth) {
    // 仅访问需认证页面时才加载菜单，公开页面（login/register）不触发菜单 API
    if (userStore.menuTree.length === 0) {
      try {
        const { menuApi } = await import('../api/menu')
        const menuTree = await menuApi.myMenus()
        if (menuTree && menuTree.length > 0) {
          userStore.setUser({ menuTree: menuTree as any })
          setupRouter(menuTree as any)
          dynamicRoutesReady = true
          // 路由刚注册，需要重新导航让 router 解析当前路径
          if (to.path.startsWith('/home/')) {
            next({ path: to.path, replace: true })
            return
          }
        }
      } catch {
        // 菜单加载失败，token 可能已过期，清除登录状态避免重复 403 报错
        userStore.logout()
      }
    }

    // 动态路由还未加载但有 menuTree，先加载
    if (userStore.menuTree.length > 0 && !dynamicRoutesReady) {
      if (to.path.startsWith('/home/')) {
        setupRouter(userStore.menuTree)
        dynamicRoutesReady = true
        next({ path: to.path, replace: true })
        return
      }
    }
  }

  if (to.meta.requiresAuth && !userStore.token) {
    next('/login')
  } else {
    next()
  }
})

export function setupRouter(menuTree: MenuItem[]): void {
  try {
    const userStore = useUserStore()

    if (!menuTree || menuTree.length === 0) {
      return
    }

    const homeRoute = router.getRoutes().find((r) => r.path === '/home')
    if (!homeRoute) {
      return
    }

    const dynamicRoutes: RouteRecordRaw[] = []
    menuTree.forEach((menu) => {
      if (menu.menuType === 2 && menu.path && menu.component) {
        dynamicRoutes.push({
          path: menu.path,
          name: menu.name,
          component: componentMap[menu.component],
        })
      }
      if (menu.children && menu.children.length > 0) {
        menu.children.forEach((child) => {
          if (child.menuType === 2 && child.path && child.component) {
            dynamicRoutes.push({
              path: child.path,
              name: child.name,
              component: componentMap[child.component],
            })
          }
        })
      }
    })

    dynamicRoutes.forEach((route) => {
      router.addRoute('home', route)
    })

    userStore.menuTree = menuTree
    dynamicRoutesReady = true
  } catch {
    // 路由设置失败，静默处理
  }
}

export function resetRouterState(): void {
  dynamicRoutesReady = false
}

export default router
