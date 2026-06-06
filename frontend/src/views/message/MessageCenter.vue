<template>
  <div class="message-center">
    <div class="page-header">
      <div class="header-left">
        <h1 class="page-title">消息通知</h1>
        <p class="page-sub">查看和管理您的消息</p>
      </div>
      <el-button type="primary" @click="dialogVisible = true">
        <el-icon><Plus /></el-icon>
        发送消息
      </el-button>
      <el-button type="primary" :disabled="messageStore.unreadCount === 0" @click="handleMarkAllRead">
        <el-icon><Check /></el-icon>
        全部已读
      </el-button>
    </div>

    <div class="glass-card">
      <el-tabs v-model="activeTab" class="message-tabs" @tab-change="handleTabChange">
        <el-tab-pane label="全部消息" name="all">
          <template #label>
            <span class="tab-label">全部消息</span>
          </template>
        </el-tab-pane>
        <el-tab-pane label="未读" name="unread">
          <template #label>
            <span class="tab-label">
              未读
              <el-badge :value="messageStore.unreadCount" :hidden="messageStore.unreadCount === 0" :max="99" />
            </span>
          </template>
        </el-tab-pane>
        <el-tab-pane label="已读" name="read">
          <template #label>
            <span class="tab-label">已读</span>
          </template>
        </el-tab-pane>
      </el-tabs>

      <div v-loading="loading" class="message-list">
        <div
          v-for="msg in messages"
          :key="msg.id"
          class="message-item"
          :class="{ unread: !msg.isRead }"
          @click="handleReadMessage(msg)"
        >
          <div class="message-icon" :class="`type-${msg.type}`">
            <el-icon v-if="msg.type === 'system'"><Setting /></el-icon>
            <el-icon v-else-if="msg.type === 'user'"><User /></el-icon>
            <el-icon v-else><Bell /></el-icon>
          </div>
          <div class="message-content">
            <div class="message-header">
              <span class="message-title">{{ msg.title }}</span>
              <el-tag v-if="msg.priority === 'high'" type="danger" size="small">重要</el-tag>
              <el-tag v-else-if="msg.priority === 'low'" type="info" size="small">低</el-tag>
            </div>
            <p class="message-text">{{ msg.content }}</p>
            <div class="message-footer">
              <span v-if="msg.senderName" class="message-sender">{{ msg.senderName }}</span>
              <span class="message-time">{{ formatTime(msg.createTime) }}</span>
            </div>
          </div>
          <div class="message-actions">
            <el-button v-if="!msg.isRead" link type="primary" size="small" @click.stop="handleMarkRead(msg)">
              标记已读
            </el-button>
            <el-button link type="danger" size="small" @click.stop="handleDeleteMessage(msg)"> 删除 </el-button>
          </div>
        </div>

        <el-empty v-if="!loading && messages.length === 0" description="暂无消息" />
      </div>

      <div v-if="total > 0" class="pagination-wrapper">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, prev, pager, next"
          @current-change="handleCurrentChange"
        />
      </div>
    </div>

    <!-- 发送消息对话框 -->
    <el-dialog v-model="dialogVisible" title="发送消息" width="500px" :close-on-click-modal="false" @open="fetchUsers">
      <el-form ref="sendFormRef" :model="sendForm" :rules="sendRules" label-width="80px">
        <el-form-item label="接收者" prop="recipientId">
          <el-select
            v-model="sendForm.recipientId"
            filterable
            placeholder="搜索用户"
            :loading="userLoading"
            style="width: 100%"
          >
            <el-option v-for="user in userList" :key="user.id" :label="user.username" :value="user.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="消息类型" prop="type">
          <el-select v-model="sendForm.type" style="width: 100%">
            <el-option label="用户消息" value="user" />
            <el-option label="系统通知" value="system" />
            <el-option label="提醒" value="notification" />
          </el-select>
        </el-form-item>
        <el-form-item label="标题" prop="title">
          <el-input v-model="sendForm.title" placeholder="请输入消息标题" maxlength="50" show-word-limit />
        </el-form-item>
        <el-form-item label="内容" prop="content">
          <el-input
            v-model="sendForm.content"
            type="textarea"
            :rows="4"
            placeholder="请输入消息内容"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="优先级" prop="priority">
          <el-radio-group v-model="sendForm.priority">
            <el-radio value="low">低</el-radio>
            <el-radio value="normal">普通</el-radio>
            <el-radio value="high">重要</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="sending" @click="handleSendMessage">发送</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Bell, Setting, User, Check, Plus } from '@element-plus/icons-vue'
import { useUserStore } from '../../stores/user'
import { useMessageStore } from '../../stores/message'
import { formatTime } from '../../utils/time'

const userStore = useUserStore()
const messageStore = useMessageStore()

const loading = ref(false)
const messages = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const activeTab = ref('unread')

const dialogVisible = ref(false)
const sendFormRef = ref(null)
const sending = ref(false)
const userLoading = ref(false)
const userList = ref([])
const sendForm = ref({
  recipientId: null,
  type: 'user',
  title: '',
  content: '',
  priority: 'normal',
})
const sendRules = {
  recipientId: [{ required: true, message: '请选择接收者', trigger: 'change' }],
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  content: [{ required: true, message: '请输入内容', trigger: 'blur' }],
}

const handleNewMessage = (msg) => {
  // 如果在未读 tab，直接刷新列表（用户在看未读消息，小红点不用加）
  if (activeTab.value === 'unread') {
    fetchMessages()
  } else {
    // 不在未读 tab 才增加未读计数
    messageStore.addMessage(msg)
  }
}

const fetchUsers = async () => {
  userLoading.value = true
  try {
    const res = await fetch('/api/users?page=1&size=100', {
      headers: { Authorization: `Bearer ${userStore.token}` },
    })
    if (res.ok) {
      const data = await res.json()
      userList.value = (data.data?.list || []).filter((u) => u.id !== userStore.userId && u.status === 1)
    }
  } catch (e) {
    console.error('Failed to fetch users:', e)
    ElMessage.warning('用户列表加载失败')
  } finally {
    userLoading.value = false
  }
}

const handleSendMessage = async () => {
  const form = sendFormRef.value
  if (!form) return
  await form.validate(async (valid) => {
    if (!valid) return
    sending.value = true
    try {
      const res = await fetch('/api/messages', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${userStore.token}`,
        },
        body: JSON.stringify(sendForm.value),
      })
      if (res.ok) {
        ElMessage.success('发送成功')
        dialogVisible.value = false
        sendForm.value = { recipientId: null, type: 'user', title: '', content: '', priority: 'normal' }
      } else {
        const err = await res.json()
        ElMessage.error(err.message || '发送失败')
      }
    } catch (e) {
      ElMessage.error('发送失败')
    } finally {
      sending.value = false
    }
  })
}

const fetchMessages = async () => {
  loading.value = true
  try {
    const isRead = activeTab.value === 'read' ? 1 : activeTab.value === 'unread' ? 0 : undefined
    let url = `/api/messages?page=${currentPage.value}&size=${pageSize.value}`
    if (isRead !== undefined) {
      url += `&isRead=${isRead}`
    }
    const res = await fetch(url, {
      headers: { Authorization: `Bearer ${userStore.token}` },
    })
    if (res.ok) {
      const response = await res.json()
      // response = {code:200, data:{list:[...], total:N}}
      const data = response.data || response
      messages.value = data.list || []
      total.value = data.total || 0
    } else {
      ElMessage.error('获取消息列表失败')
    }
  } catch (e) {
    ElMessage.error('网络错误，请稍后重试')
  } finally {
    loading.value = false
  }
}

const fetchUnreadCount = async () => {
  try {
    const res = await fetch('/api/messages/unread-count', {
      headers: { Authorization: `Bearer ${userStore.token}` },
    })
    if (res.ok) {
      const data = await res.json()
      messageStore.setUnreadCount(data.count || 0)
    }
  } catch (e) {
    console.error('Failed to fetch unread count:', e)
  }
}

const handleTabChange = () => {
  currentPage.value = 1
  fetchMessages()
}

const handleCurrentChange = () => {
  fetchMessages()
}

const handleReadMessage = async (msg) => {
  if (!msg.isRead) {
    await handleMarkRead(msg)
  }
}

const handleMarkRead = async (msg) => {
  try {
    const res = await fetch(`/api/messages/${msg.id}/read`, {
      method: 'PUT',
      headers: { Authorization: `Bearer ${userStore.token}` },
    })
    if (res.ok) {
      messageStore.setUnreadCount(Math.max(0, messageStore.unreadCount - 1))
      fetchMessages()
    } else {
      ElMessage.error('标记已读失败')
    }
  } catch (e) {
    ElMessage.error('标记已读失败')
  }
}

const handleMarkAllRead = async () => {
  try {
    await ElMessageBox.confirm('确定要将所有消息标记为已读吗？', '提示')
    const success = await messageStore.markAllRead()
    if (success) {
      ElMessage.success('操作成功')
      fetchMessages()
    } else {
      ElMessage.error('操作失败')
    }
  } catch {
    // cancelled
  }
}

const handleDeleteMessage = async (msg) => {
  try {
    await ElMessageBox.confirm('确定要删除这条消息吗？', '提示', { type: 'warning' })
    const res = await fetch(`/api/messages/${msg.id}`, {
      method: 'DELETE',
      headers: { Authorization: `Bearer ${userStore.token}` },
    })
    if (res.ok) {
      ElMessage.success('删除成功')
      if (!msg.isRead) {
        messageStore.setUnreadCount(Math.max(0, messageStore.unreadCount - 1))
      }
      fetchMessages()
    } else {
      ElMessage.error('删除失败')
    }
  } catch {
    // cancelled
  }
}

onMounted(() => {
  messageStore.connect(handleNewMessage)
  fetchMessages()
  fetchUnreadCount()
})

onUnmounted(() => {
  messageStore.disconnect()
})
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Outfit:wght@300;400;500;600;700&family=Sora:wght@300;400;500;600&display=swap');

.message-center {
  animation: fadeInUp 0.5s ease-out forwards;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 24px;
}

.page-title {
  font-family: 'Sora', sans-serif;
  font-size: 28px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 8px;
}

.page-sub {
  font-size: 14px;
  color: var(--text-muted);
}

.glass-card {
  background: var(--glass-bg);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-radius: 20px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  padding: 24px;
}

.message-tabs {
  margin-bottom: 20px;
}

:deep(.el-tabs__item) {
  color: var(--text-muted);
  font-size: 14px;
}

:deep(.el-tabs__item.is-active) {
  color: var(--text-primary);
}

:deep(.el-tabs__active-bar) {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

:deep(.el-tabs__nav-wrap::after) {
  background: rgba(255, 255, 255, 0.08);
}

.tab-label {
  display: flex;
  align-items: center;
  gap: 8px;
}

.message-list {
  min-height: 300px;
}

.message-item {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  padding: 16px;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.3s ease;
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
}

.message-item:last-child {
  border-bottom: none;
}

.message-item:hover {
  background: rgba(255, 255, 255, 0.05);
}

.message-item.unread {
  background: rgba(102, 126, 234, 0.1);
}

.message-item.unread::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 3px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 0 2px 2px 0;
}

.message-icon {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.message-icon.type-system {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.message-icon.type-user {
  background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
}

.message-icon.type-notification {
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
}

.message-icon :deep(.el-icon) {
  width: 22px;
  height: 22px;
  color: white;
}

.message-content {
  flex: 1;
  min-width: 0;
}

.message-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}

.message-title {
  font-size: 15px;
  font-weight: 500;
  color: var(--text-primary);
}

.message-text {
  font-size: 13px;
  color: var(--text-muted);
  margin-bottom: 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.message-footer {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 12px;
  color: var(--text-muted);
}

.message-actions {
  display: flex;
  flex-direction: column;
  gap: 4px;
  opacity: 0;
  transition: opacity 0.3s ease;
}

.message-item:hover .message-actions {
  opacity: 1;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>
