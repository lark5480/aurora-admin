<template>
  <el-dialog
    v-model="dialogVisible"
    :title="notice?.title"
    width="85%"
    top="5vh"
    :show-close="true"
    :close-on-click-modal="true"
    class="notice-detail-dialog"
    :style="{ maxWidth: '1200px' }"
  >
    <div class="dialog-body">
      <div class="dialog-meta">
        <svg class="meta-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <circle cx="12" cy="12" r="10" />
          <path d="M12 6v6l4 2" />
        </svg>
        <span class="meta-time">{{ formatDateTime(notice?.createTime) }}</span>
      </div>

      <!-- eslint-disable-next-line vue/no-v-html -- content sanitized by DOMPurify -->
      <div class="dialog-content" v-html="renderedContent"></div>
    </div>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, watch } from 'vue'
import { marked } from 'marked'
import DOMPurify from 'dompurify'

const props = defineProps({
  visible: {
    type: Boolean,
    default: false,
  },
  notice: {
    type: Object,
    default: null,
  },
})

const emit = defineEmits(['close'])

const dialogVisible = computed({
  get: () => props.visible,
  set: (val) => {
    if (!val) emit('close')
  },
})

watch(
  () => props.visible,
  (newVal) => {
    if (newVal && props.notice) {
      // dialog opened
    }
  },
)

const formatDateTime = (dateStr) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hour = String(date.getHours()).padStart(2, '0')
  const minute = String(date.getMinutes()).padStart(2, '0')
  return `${year}-${month}-${day} ${hour}:${minute}`
}

const renderedContent = computed(() => {
  if (!props.notice?.content) return ''
  const rawHtml = marked.parse(props.notice.content) as string
  return DOMPurify.sanitize(rawHtml)
})
</script>

<style>
/* el-dialog teleported — must be unscoped to apply */
.notice-detail-dialog {
  --el-dialog-bg-color: var(--bg-darker);
  --el-dialog-border-radius: 16px;
}

.notice-detail-dialog .el-dialog {
  background: var(--bg-darker);
  backdrop-filter: blur(24px);
  -webkit-backdrop-filter: blur(24px);
  border: 1px solid var(--border-glow-magenta);
  border-radius: 16px;
  box-shadow:
    0 0 40px rgba(255, 0, 255, 0.12),
    0 0 80px rgba(255, 0, 255, 0.04);
  overflow: hidden;
  max-height: 92vh;
  display: flex;
  flex-direction: column;
  max-width: 1200px;
}

.notice-detail-dialog .el-dialog__header {
  padding: 20px 24px 16px;
  margin-right: 0;
  border-bottom: 1px solid rgba(0, 255, 255, 0.1);
  background: rgba(0, 255, 255, 0.03);
  flex-shrink: 0;
}

.notice-detail-dialog .el-dialog__title {
  color: var(--neon-cyan);
  font-family: 'Sora', sans-serif;
  font-size: 17px;
  font-weight: 600;
  text-shadow: 0 0 10px rgba(0, 255, 255, 0.3);
}

.notice-detail-dialog .el-dialog__headerbtn {
  width: 32px;
  height: 32px;
  background: rgba(0, 255, 255, 0.06);
  border: 1px solid rgba(0, 255, 255, 0.12);
  border-radius: 8px;
  transition: all 0.2s ease;
  top: 16px;
  right: 16px;
}

.notice-detail-dialog .el-dialog__headerbtn:hover {
  background: rgba(255, 0, 255, 0.12);
  border-color: rgba(255, 0, 255, 0.25);
  box-shadow: 0 0 12px rgba(255, 0, 255, 0.15);
}

.notice-detail-dialog .el-dialog__close {
  color: rgba(255, 255, 255, 0.5);
  font-size: 18px;
  transition: color 0.2s ease;
}

.notice-detail-dialog .el-dialog__headerbtn:hover .el-dialog__close {
  color: var(--neon-magenta);
}

.notice-detail-dialog .el-dialog__body {
  padding: 0;
  flex: 1;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}
</style>

<style scoped>
.dialog-body {
  display: flex;
  flex-direction: column;
  flex: 1;
  overflow: hidden;
}

.dialog-meta {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 14px 24px;
  border-bottom: 1px solid rgba(0, 255, 255, 0.06);
  flex-shrink: 0;
}

.meta-icon {
  width: 14px;
  height: 14px;
  color: var(--text-muted);
  flex-shrink: 0;
}

.meta-time {
  font-size: 13px;
  color: var(--text-muted);
}

.dialog-content {
  flex: 1;
  overflow-y: auto;
  padding: 20px 24px 28px;
  font-size: 14px;
  line-height: 1.85;
  color: rgba(255, 255, 255, 0.8);
}

.dialog-content :deep(h1) {
  font-size: 18px;
  font-weight: 600;
  color: var(--neon-cyan);
  margin: 20px 0 10px;
  padding-bottom: 8px;
  border-bottom: 1px solid rgba(0, 255, 255, 0.1);
}

.dialog-content :deep(h1:first-child) {
  margin-top: 4px;
}

.dialog-content :deep(h2) {
  font-size: 15px;
  font-weight: 600;
  color: #00d4ff;
  margin: 16px 0 8px;
}

.dialog-content :deep(h3) {
  font-size: 14px;
  font-weight: 600;
  color: var(--neon-magenta);
  margin: 14px 0 6px;
}

.dialog-content :deep(hr) {
  border: none;
  border-top: 1px solid rgba(0, 255, 255, 0.08);
  margin: 16px 0;
}

.dialog-content :deep(p) {
  margin: 0 0 10px;
}

.dialog-content :deep(p:last-child) {
  margin-bottom: 0;
}

.dialog-content :deep(img) {
  max-width: 100%;
  border-radius: 8px;
  margin: 8px 0;
}

.dialog-content :deep(ul),
.dialog-content :deep(ol) {
  margin: 8px 0;
  padding-left: 20px;
}

.dialog-content :deep(li) {
  margin-bottom: 4px;
}

.dialog-content :deep(code) {
  background: rgba(0, 255, 255, 0.08);
  color: var(--neon-green);
  padding: 2px 6px;
  border-radius: 4px;
  font-family: 'Fira Code', 'Cascadia Code', monospace;
  font-size: 13px;
}

.dialog-content :deep(pre) {
  background: rgba(0, 0, 0, 0.4);
  border: 1px solid rgba(0, 255, 255, 0.08);
  padding: 14px 16px;
  border-radius: 8px;
  overflow-x: auto;
}

.dialog-content :deep(pre code) {
  background: none;
  color: rgba(255, 255, 255, 0.78);
  padding: 0;
}

.dialog-content :deep(a) {
  color: var(--neon-cyan);
  text-decoration: none;
}

.dialog-content :deep(a:hover) {
  text-decoration: underline;
}

.dialog-content :deep(blockquote) {
  border-left: 3px solid var(--neon-cyan);
  margin: 12px 0;
  padding: 4px 14px;
  color: rgba(255, 255, 255, 0.6);
  background: rgba(0, 255, 255, 0.03);
}

.dialog-content :deep(strong) {
  color: rgba(255, 255, 255, 0.9);
  font-weight: 600;
}

.dialog-content :deep(table) {
  width: 100%;
  border-collapse: collapse;
  margin: 12px 0;
  font-size: 13px;
}

.dialog-content :deep(th),
.dialog-content :deep(td) {
  border: 1px solid rgba(0, 255, 255, 0.1);
  padding: 8px 12px;
  text-align: left;
}

.dialog-content :deep(th) {
  background: rgba(0, 255, 255, 0.06);
  color: var(--neon-cyan);
  font-weight: 600;
}

/* scrollbar */
.dialog-content::-webkit-scrollbar {
  width: 5px;
}

.dialog-content::-webkit-scrollbar-track {
  background: transparent;
}

.dialog-content::-webkit-scrollbar-thumb {
  background: rgba(0, 255, 255, 0.12);
  border-radius: 3px;
}

.dialog-content::-webkit-scrollbar-thumb:hover {
  background: rgba(0, 255, 255, 0.25);
}
</style>
