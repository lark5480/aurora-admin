<template>
  <div class="notice-list">
    <div class="notice-header">
      <svg class="notice-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9" />
        <path d="M13.73 21a2 2 0 0 1-3.46 0" />
      </svg>
      <span class="notice-title">系统公告</span>
    </div>

    <div v-if="loading" class="notice-loading">
      <div class="loading-pulse"></div>
    </div>

    <div v-else-if="notices.length === 0" class="notice-empty">暂无公告</div>

    <ul v-else class="notice-items">
      <li v-for="notice in notices" :key="notice.id" class="notice-item" @click="showDetail(notice)">
        <span class="notice-item-title">{{ notice.title }}</span>
        <span class="notice-item-time">{{ formatTime(notice.createTime) }}</span>
      </li>
    </ul>

    <NoticeDetail :visible="detailVisible" :notice="currentNotice" @close="detailVisible = false" />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { noticeApi } from '../../api/notice'
import NoticeDetail from './NoticeDetail.vue'

const notices = ref([])
const loading = ref(false)
const detailVisible = ref(false)
const currentNotice = ref(null)

const showDetail = (notice) => {
  currentNotice.value = notice
  detailVisible.value = true
}

const formatTime = (dateStr) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${month}-${day}`
}

const fetchNotices = async () => {
  loading.value = true
  try {
    // 返回数据可能是数组或 { list: [] } 格式
    const res = await noticeApi.getVisibleNotices()
    if (Array.isArray(res)) {
      notices.value = res.slice(0, 10)
    } else if (res && Array.isArray(res.list)) {
      notices.value = res.list.slice(0, 10)
    } else {
      notices.value = []
    }
  } catch (error) {
    notices.value = []
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchNotices()
})
</script>

<style scoped>
.notice-list {
  background: var(--glass-bg);
  border: 1px solid var(--neon-cyan);
  border-radius: 12px;
  padding: 20px;
  box-shadow: var(--glow-magenta);
}

.notice-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--border-glow-magenta);
}

.notice-icon {
  width: 20px;
  height: 20px;
  color: var(--neon-cyan);
  filter: drop-shadow(0 0 4px var(--neon-cyan));
}

.notice-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--neon-cyan);
  text-shadow: 0 0 8px rgba(0, 255, 255, 0.5);
}

.notice-loading,
.notice-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100px;
  color: var(--text-muted);
  font-size: 14px;
}

.loading-pulse {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: rgba(0, 255, 255, 0.2);
  animation: pulse 1.5s ease-in-out infinite;
}

@keyframes pulse {
  0%,
  100% {
    transform: scale(0.8);
    opacity: 0.5;
  }
  50% {
    transform: scale(1);
    opacity: 1;
  }
}

.notice-items {
  list-style: none;
  margin: 0;
  padding: 0;
}

.notice-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 8px;
  margin-bottom: 4px;
  border-radius: 6px;
  cursor: pointer;
  transition: all var(--transition-fast);
}

.notice-item:hover {
  background: rgba(0, 255, 255, 0.1);
}

.notice-item:hover .notice-item-title {
  color: var(--neon-cyan);
  text-shadow: 0 0 8px rgba(0, 255, 255, 0.5);
}

.notice-item-title {
  flex: 1;
  font-size: 14px;
  color: rgba(255, 255, 255, 0.85);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  transition: color var(--transition-fast);
}

.notice-item-time {
  font-size: 12px;
  color: var(--text-muted);
  margin-left: 12px;
  flex-shrink: 0;
}
</style>
