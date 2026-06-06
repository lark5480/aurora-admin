<template>
  <AuroraBackground>
    <div class="app-layout">
      <!-- 左侧菜单 -->
      <aside class="sidebar" role="navigation" aria-label="主导航">
        <div class="logo-area" role="link" tabindex="0" @click="goHome" @keydown.enter="goHome">
          <div class="logo-icon">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" aria-hidden="true">
              <path d="M12 2L2 7l10 5 10-5-10-5zM2 17l10 5 10-5M2 12l10 5 10-5" />
            </svg>
          </div>
          <span class="logo-title">{{ configStore.get('site.name', 'Admin System') }}</span>
        </div>

        <el-menu
          :default-active="activeMenu"
          class="sidebar-menu"
          background-color="transparent"
          text-color="rgba(255,255,255,0.7)"
          active-text-color="#fff"
          :unique-opened="true"
          @select="handleMenuSelect"
        >
          <template v-for="item in userStore.menuTree" :key="item.id">
            <el-sub-menu v-if="item.children && item.children.length > 0" :index="String(item.id)">
              <template #title>
                <el-icon><component :is="getIcon(item.icon)" /></el-icon>
                <span>{{ item.name }}</span>
              </template>
              <el-menu-item
                v-for="child in item.children"
                :key="child.id"
                :index="String(child.id)"
                :route="'/home/' + child.path"
              >
                <el-icon><component :is="getIcon(child.icon)" /></el-icon>
                <span>{{ child.name }}</span>
              </el-menu-item>
            </el-sub-menu>
            <el-menu-item v-else :index="String(item.id)" :route="'/home/' + item.path">
              <el-icon><component :is="getIcon(item.icon)" /></el-icon>
              <span>{{ item.name }}</span>
            </el-menu-item>
          </template>
        </el-menu>
      </aside>

      <!-- 主内容区 -->
      <div class="main-area">
        <header class="topbar" role="banner">
          <div class="topbar-left">
            <h1 class="page-title">{{ currentMenuName }}</h1>
          </div>
          <nav class="topbar-right" role="navigation" aria-label="用户操作">
            <el-badge :value="unreadCount" :hidden="unreadCount === 0" :max="99">
              <div
                class="bell-btn"
                role="button"
                aria-label="消息中心"
                tabindex="0"
                @click="goMessages"
                @keydown.enter="goMessages"
              >
                <el-icon :size="20"><Bell /></el-icon>
              </div>
            </el-badge>
            <UserAvatar @command="handleAvatarCommand" />
            <el-button type="danger" plain class="logout-btn" @click="handleLogout">退出登录</el-button>
          </nav>
        </header>
        <el-dialog
          v-model="passwordDialogVisible"
          title="修改密码"
          width="400px"
          :close-on-click-modal="false"
          align-center
        >
          <el-form ref="passwordFormRef" :model="passwordForm" :rules="passwordRules" label-width="80px">
            <el-form-item label="原密码" prop="oldPassword">
              <el-input v-model="passwordForm.oldPassword" type="password" show-password />
            </el-form-item>
            <el-form-item label="新密码" prop="newPassword">
              <el-input v-model="passwordForm.newPassword" type="password" show-password />
            </el-form-item>
            <el-form-item label="确认密码" prop="confirmPassword">
              <el-input v-model="passwordForm.confirmPassword" type="password" show-password />
            </el-form-item>
          </el-form>
          <template #footer>
            <el-button @click="passwordDialogVisible = false">取消</el-button>
            <el-button type="primary" :loading="passwordSaving" @click="handleChangePassword">确认</el-button>
          </template>
        </el-dialog>
        <main class="content-area" role="main">
          <router-view />
          <footer v-if="configStore.get('site.footer')" class="app-footer">
            {{ configStore.get('site.footer') }}
          </footer>
        </main>
      </div>
    </div>
  </AuroraBackground>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '../../stores/user'
import { useMessageStore } from '../../stores/message'
import { useConfigStore } from '../../stores/config'
import AuroraBackground from './AuroraBackground.vue'
import UserAvatar from './UserAvatar.vue'
import { userApi } from '../../api/user'
import {
  ChatDotRound,
  DataAnalysis,
  DataLine,
  DocumentChecked,
  MapLocation,
  Message,
  Tools,
  User,
  Setting,
  Folder,
  Bell,
  Document,
  HomeFilled,
  OfficeBuilding,
  Key,
  Menu,
  Grid,
  List,
  PieChart,
  ShoppingBag,
  ShoppingCart,
  Tickets,
  Clock,
  InfoFilled,
  WarningFilled,
  SuccessFilled,
  CircleCheck,
} from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const messageStore = useMessageStore()
const configStore = useConfigStore()

const unreadCount = computed(() => messageStore.unreadCount)

// Icon mapping
const iconMap = {
  ChatDotRound: ChatDotRound,
  DataAnalysis: DataAnalysis,
  DataLine: DataLine,
  DocumentChecked: DocumentChecked,
  MapLocation: MapLocation,
  Message: Message,
  Tools: Tools,
  User: User,
  Setting: Setting,
  Folder: Folder,
  Bell: Bell,
  Document: Document,
  HomeFilled: HomeFilled,
  OfficeBuilding: OfficeBuilding,
  Key: Key,
  Menu: Menu,
  Grid: Grid,
  List: List,
  PieChart: PieChart,
  ShoppingBag: ShoppingBag,
  ShoppingCart: ShoppingCart,
  Tickets: Tickets,
  Clock: Clock,
  InfoFilled: InfoFilled,
  WarningFilled: WarningFilled,
  SuccessFilled: SuccessFilled,
  CircleCheck: CircleCheck,
}

const getIcon = (iconName) => {
  return iconMap[iconName] || Menu
}

// Active menu - find current menu item by route path
const activeMenu = computed(() => {
  const path = route.path
  for (const item of userStore.menuTree) {
    if (item.children && item.children.length > 0) {
      for (const child of item.children) {
        if (`/home/${child.path}` === path) {
          return String(child.id)
        }
      }
    } else {
      if (`/home/${item.path}` === path) {
        return String(item.id)
      }
    }
  }
  return ''
})

// Current menu name for topbar
const currentMenuName = computed(() => {
  const path = route.path
  for (const item of userStore.menuTree) {
    if (item.children && item.children.length > 0) {
      for (const child of item.children) {
        if (`/home/${child.path}` === path) {
          return child.name
        }
      }
    } else {
      if (`/home/${item.path}` === path) {
        return item.name
      }
    }
  }
  return '首页'
})

const handleMenuSelect = (index) => {
  for (const item of userStore.menuTree) {
    if (String(item.id) === index) {
      if (!item.children || item.children.length === 0) {
        router.push('/home/' + item.path)
      }
      return
    }
    if (item.children && item.children.length > 0) {
      for (const child of item.children) {
        if (String(child.id) === index) {
          router.push('/home/' + child.path)
          return
        }
      }
    }
  }
}

const goHome = () => {
  router.push('/home')
}

const goMessages = () => {
  router.push('/home/messages')
}

const handleLogout = () => {
  userStore.logout()
  ElMessage.success('已退出登录')
  router.push('/login')
}

// --- 头像下拉 ---
const passwordDialogVisible = ref(false)
const passwordFormRef = ref(null)
const passwordSaving = ref(false)

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
})

const validateConfirm = (_rule, value, callback) => {
  if (value !== passwordForm.newPassword) {
    callback(new Error('两次密码不一致'))
  } else {
    callback()
  }
}

const passwordRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于6位', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    { validator: validateConfirm, trigger: 'blur' },
  ],
}

function handleAvatarCommand(command) {
  switch (command) {
    case 'profile':
      router.push('/home/profile')
      break
    case 'password':
      passwordForm.oldPassword = ''
      passwordForm.newPassword = ''
      passwordForm.confirmPassword = ''
      passwordDialogVisible.value = true
      break
  }
}

async function handleChangePassword() {
  const valid = await passwordFormRef.value?.validate().catch(() => false)
  if (!valid) return

  passwordSaving.value = true
  try {
    await userApi.changePassword({
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword,
    })
    ElMessage.success('密码修改成功，请重新登录')
    passwordDialogVisible.value = false
    setTimeout(() => {
      userStore.logout()
      router.push('/login')
    }, 1500)
  } catch (err) {
    const msg = err?.response?.data?.message || '修改失败'
    ElMessage.error(msg)
  } finally {
    passwordSaving.value = false
  }
}

onMounted(async () => {
  await configStore.fetchConfigs()
  messageStore.fetchUnreadCount()
  messageStore.connect()
  try {
    const profile = await userApi.getProfile()
    userStore.setUser({
      nickname: profile.nickname || '',
      avatar: profile.avatar || '',
    })
  } catch (e) {
    console.warn('加载用户信息失败', e)
  }
})
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Outfit:wght@300;400;500;600;700&family=Sora:wght@300;400;500;600&display=swap');

.app-layout {
  display: flex;
  min-height: 100vh;
}

.sidebar {
  width: var(--sidebar-width, 240px);
  background: var(--glass-bg, rgba(0, 0, 0, 0.6));
  backdrop-filter: blur(20px);
  border-right: 1px solid var(--border-glow-magenta, rgba(255, 0, 255, 0.3));
  padding: 16px 0;
  overflow-y: auto;
  position: fixed;
  top: 0;
  left: 0;
  bottom: 0;
  z-index: var(--z-sidebar, 100);
}

.logo-area {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 0 20px 20px;
  border-bottom: 1px solid var(--border-subtle, rgba(255, 255, 255, 0.1));
  margin-bottom: 16px;
  cursor: pointer;
  transition: opacity var(--transition-fast, 150ms ease);
}

.logo-area:hover {
  opacity: 0.8;
}

.logo-area:focus-visible {
  outline: 2px solid var(--neon-cyan);
  outline-offset: 4px;
  border-radius: 4px;
}

.logo-icon {
  width: 36px;
  height: 36px;
  background: linear-gradient(135deg, var(--neon-magenta) 0%, var(--neon-cyan) 100%);
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.logo-icon svg {
  width: 20px;
  height: 20px;
  color: white;
}

.logo-title {
  font-family: 'Sora', sans-serif;
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary, #fff);
}

.sidebar-menu {
  border: none;
  background: transparent !important;
}

.sidebar-menu :deep(.el-menu-item),
.sidebar-menu :deep(.el-sub-menu__title) {
  height: 48px;
  line-height: 48px;
  margin: 4px 12px;
  border-radius: 8px;
  transition:
    background-color var(--transition-fast),
    box-shadow var(--transition-fast);
}

.sidebar-menu :deep(.el-menu-item:hover),
.sidebar-menu :deep(.el-sub-menu__title:hover) {
  background: rgba(255, 255, 255, 0.08) !important;
}

.sidebar-menu :deep(.el-sub-menu .el-menu-item) {
  height: 40px;
  line-height: 40px;
  padding-left: 52px !important;
}

.sidebar-menu :deep(.el-sub-menu__title) {
  border-radius: 8px;
}

.sidebar-menu :deep(.el-menu-item.is-active) {
  background: linear-gradient(135deg, rgba(255, 0, 255, 0.2) 0%, rgba(0, 255, 255, 0.2) 100%) !important;
  border: 1px solid var(--border-glow-magenta);
  box-shadow: var(--glow-magenta);
}

.main-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  margin-left: var(--sidebar-width, 240px);
}

.topbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 32px;
  background: rgba(0, 0, 0, 0.4);
  backdrop-filter: blur(20px);
  border-bottom: 1px solid var(--border-glow-magenta);
  position: sticky;
  top: 0;
  z-index: var(--z-topbar, 50);
}

.topbar-left {
  display: flex;
  align-items: center;
}

.page-title {
  font-size: 18px;
  font-weight: 500;
  color: var(--text-primary);
  margin: 0;
}

.topbar-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.icon-btn {
  color: var(--text-secondary);
  cursor: pointer;
  transition: color var(--transition-fast);
  padding: 8px;
  border-radius: 8px;
}

.icon-btn:hover {
  color: var(--text-primary);
  background: rgba(255, 255, 255, 0.1);
}

.bell-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.12);
  color: #fff;
  cursor: pointer;
  transition: all var(--transition-fast, 150ms ease);
  border: 1px solid rgba(255, 255, 255, 0.15);
}

.bell-btn:hover {
  background: rgba(255, 255, 255, 0.2);
  box-shadow: 0 0 12px rgba(255, 255, 255, 0.15);
  transform: scale(1.05);
}

.bell-btn:focus-visible {
  outline: 2px solid var(--neon-cyan);
  outline-offset: 2px;
}

.icon-btn:focus-visible {
  outline: 2px solid var(--neon-cyan);
  outline-offset: 2px;
}

.logout-btn {
  font-family: 'Outfit', sans-serif;
  cursor: pointer;
}

.content-area {
  flex: 1;
  padding: 24px 32px;
  overflow-y: auto;
}

.app-footer {
  text-align: center;
  padding: 16px 32px;
  color: var(--text-muted);
  font-size: 12px;
  border-top: 1px solid var(--border-subtle, rgba(255, 255, 255, 0.1));
  margin-top: 32px;
}

@media (max-width: 1024px) {
  .sidebar {
    width: var(--sidebar-width-md, 200px);
  }
  .main-area {
    margin-left: var(--sidebar-width-md, 200px);
  }
}

@media (max-width: 768px) {
  .sidebar {
    transform: translateX(-100%);
    transition: transform var(--transition-normal);
  }
  .sidebar.open {
    transform: translateX(0);
  }
  .main-area {
    margin-left: 0;
  }
}

/* Reduced Motion */
@media (prefers-reduced-motion: reduce) {
  .logo-area,
  .icon-btn,
  .sidebar-menu :deep(.el-menu-item),
  .sidebar-menu :deep(.el-sub-menu__title) {
    transition: none;
  }
}
</style>

<style>
/* 全局 el-dialog 暗色主题 — 修改密码等弹窗 */
.el-dialog {
  background: var(--bg-darker, #1a1a2e) !important;
  backdrop-filter: blur(20px);
  border: 1px solid var(--border-glow-magenta, rgba(255, 0, 255, 0.3)) !important;
  border-radius: 16px !important;
}
.el-dialog__title {
  color: var(--text-primary, #e0e0e0) !important;
  font-family: 'Sora', sans-serif;
}
.el-dialog__body {
  color: var(--text-secondary, #b0b0b0) !important;
}
.el-dialog__headerbtn .el-dialog__close {
  color: var(--text-muted, #888) !important;
}
.el-dialog__headerbtn .el-dialog__close:hover {
  color: var(--neon-cyan, #00ffff) !important;
}
</style>
