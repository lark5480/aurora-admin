<template>
  <el-dropdown trigger="click" @command="handleCommand">
    <span class="user-trigger">
      <div v-if="userStore.avatar" class="avatar-img" :style="{ backgroundImage: `url(${userStore.avatar})` }"></div>
      <div v-else class="avatar-letter" :style="{ backgroundColor: avatarColor }">
        {{ initial }}
      </div>
      <span class="username">{{ displayName }}</span>
      <el-icon class="arrow-icon"><ArrowDown /></el-icon>
    </span>
    <template #dropdown>
      <el-dropdown-menu>
        <el-dropdown-item command="profile">
          <el-icon><User /></el-icon>
          个人信息
        </el-dropdown-item>
        <el-dropdown-item command="password">
          <el-icon><Lock /></el-icon>
          修改密码
        </el-dropdown-item>
      </el-dropdown-menu>
    </template>
  </el-dropdown>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { ArrowDown, User, Lock } from '@element-plus/icons-vue'
import { useUserStore } from '../../stores/user'

const emit = defineEmits<{
  (e: 'command', command: string): void
}>()

const userStore = useUserStore()

const PALETTE = ['#ff00ff', '#00ffff', '#39ff14', '#ff6600', '#ffff00', '#ff1493']

const displayName = computed(() => {
  return userStore.nickname || userStore.username || 'U'
})

const initial = computed(() => {
  return displayName.value.charAt(0).toUpperCase()
})

const avatarColor = computed(() => {
  const name = displayName.value
  let hash = 0
  for (let i = 0; i < name.length; i++) {
    hash = name.charCodeAt(i) + ((hash << 5) - hash)
  }
  return PALETTE[Math.abs(hash) % PALETTE.length]
})

function handleCommand(command: string) {
  emit('command', command)
}
</script>

<style scoped>
.user-trigger {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 8px;
  transition: background-color 150ms ease;
}

.user-trigger:hover {
  background: rgba(255, 255, 255, 0.08);
}

.avatar-img,
.avatar-letter {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  flex-shrink: 0;
}

.avatar-img {
  background-size: cover;
  background-position: center;
  border: 2px solid var(--border-glow-magenta, rgba(255, 0, 255, 0.3));
}

.avatar-letter {
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 600;
  color: #fff;
}

.username {
  color: var(--text-primary, #fff);
  font-size: 14px;
  max-width: 100px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.arrow-icon {
  color: var(--text-secondary, rgba(255, 255, 255, 0.6));
  font-size: 12px;
}
</style>
