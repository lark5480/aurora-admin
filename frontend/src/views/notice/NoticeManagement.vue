<template>
  <div class="notice-management">
    <div class="page-header">
      <h1 class="page-title">公告管理</h1>
      <p class="page-sub">管理系统公告信息</p>
    </div>

    <div class="glass-card">
      <div class="card-header">
        <div class="filter-row">
          <el-input
            v-model="searchKeyword"
            placeholder="搜索公告标题"
            :prefix-icon="Search"
            clearable
            class="search-input"
            @input="handleSearch"
          />
          <el-select
            v-model="statusFilter"
            placeholder="状态筛选"
            clearable
            class="status-select"
            @change="handleSearch"
          >
            <el-option label="草稿" value="DRAFT" />
            <el-option label="已发布" value="PUBLISHED" />
            <el-option label="已过期" value="EXPIRED" />
            <el-option label="已撤回" value="WITHDRAWN" />
          </el-select>
        </div>
        <el-button v-if="isAdmin" type="primary" @click="handleAdd">
          <el-icon><Plus /></el-icon>
          新增公告
        </el-button>
      </div>

      <el-table v-loading="loading" :data="notices" stripe class="notice-table">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
        <el-table-column prop="targetType" label="目标类型" width="120">
          <template #default="{ row }">
            <el-tag :type="getTargetTypeTagType(row.targetType)">
              {{ getTargetTypeLabel(row.targetType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="publishTime" label="发布时间" width="160">
          <template #default="{ row }">
            {{ formatDateTime(row.publishTime) }}
          </template>
        </el-table-column>
        <el-table-column prop="expireTime" label="过期时间" width="160">
          <template #default="{ row }">
            {{ formatDateTime(row.expireTime) || '永不过期' }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="getStatusTagType(row.status)">
              {{ getStatusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button v-if="isAdmin" link type="primary" @click="handleEdit(row)">编辑</el-button>
            <el-button v-if="isAdmin" link type="danger" @click="handleDelete(row)">删除</el-button>
            <el-button
              v-if="isAdmin && (row.status === 'DRAFT' || row.status === 'WITHDRAWN')"
              link
              type="success"
              @click="handlePublish(row)"
            >
              发布
            </el-button>
            <el-button v-if="isAdmin && row.status === 'PUBLISHED'" link type="warning" @click="handleWithdraw(row)">
              撤回
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </div>

    <!-- 新增/编辑公告对话框 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="600px" :close-on-click-modal="false">
      <el-form ref="formRef" :model="noticeForm" :rules="rules" label-width="100px">
        <el-form-item label="标题" prop="title">
          <el-input v-model="noticeForm.title" placeholder="请输入公告标题" maxlength="200" show-word-limit />
        </el-form-item>
        <el-form-item label="内容" prop="content">
          <el-input
            v-model="noticeForm.content"
            type="textarea"
            placeholder="请输入公告内容"
            :rows="6"
            maxlength="5000"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="目标类型" prop="targetType">
          <el-radio-group v-model="noticeForm.targetType" @change="handleTargetTypeChange">
            <el-radio value="ALL">全部用户</el-radio>
            <el-radio value="DEPT">指定部门</el-radio>
            <el-radio value="USER">指定用户</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="noticeForm.targetType === 'DEPT'" label="目标部门" prop="targetIds">
          <el-tree-select
            v-model="noticeForm.targetIds"
            :data="deptList"
            :props="{ label: 'name', value: 'id', children: 'children' }"
            placeholder="请选择部门"
            multiple
            clearable
            check-strictly
            :render-after-expand="false"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item v-if="noticeForm.targetType === 'USER'" label="目标用户" prop="targetIds">
          <el-select
            v-model="noticeForm.targetIds"
            multiple
            filterable
            remote
            placeholder="请输入用户名搜索"
            :remote-method="searchUsers"
            style="width: 100%"
          >
            <el-option v-for="user in userList" :key="user.id" :label="user.username" :value="user.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="定时发布时间" prop="publishTime">
          <el-date-picker
            v-model="noticeForm.publishTime"
            type="datetime"
            placeholder="留空则立即发布"
            style="width: 100%"
            format="YYYY-MM-DD HH:mm:ss"
            value-format="YYYY-MM-DD HH:mm:ss"
          />
        </el-form-item>
        <el-form-item label="过期时间" prop="expireTime">
          <el-date-picker
            v-model="noticeForm.expireTime"
            type="datetime"
            placeholder="留空则永不过期"
            style="width: 100%"
            format="YYYY-MM-DD HH:mm:ss"
            value-format="YYYY-MM-DD HH:mm:ss"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Plus } from '@element-plus/icons-vue'
import { useUserStore } from '../../stores/user'
import { noticeApi } from '../../api/notice'
import { deptApi } from '../../api/dept'

const userStore = useUserStore()
const isAdmin = userStore.hasRole('ADMIN') || userStore.hasRole('SUPER_ADMIN')
const loading = ref(false)
const notices = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const searchKeyword = ref('')
const statusFilter = ref('')

const dialogVisible = ref(false)
const dialogTitle = ref('')
const isEdit = ref(false)
const submitting = ref(false)
const formRef = ref(null)

// 部门列表
const deptList = ref([])

// 用户列表（搜索用）
const userList = ref([])

const noticeForm = reactive({
  id: null,
  title: '',
  content: '',
  targetType: 'ALL',
  targetIds: [],
  publishTime: null,
  expireTime: null,
})

const rules = {
  title: [{ required: true, message: '请输入公告标题', trigger: 'blur' }],
  content: [{ required: true, message: '请输入公告内容', trigger: 'blur' }],
  targetType: [{ required: true, message: '请选择目标类型', trigger: 'change' }],
}

// 获取公告列表
const fetchNotices = async () => {
  loading.value = true
  try {
    const res = await noticeApi.getManageList({
      page: currentPage.value,
      size: pageSize.value,
      status: statusFilter.value,
      keyword: searchKeyword.value,
    })
    const data = res?.list || res?.records || []
    notices.value = data
    total.value = res?.total || data.length
  } catch (e) {
    ElMessage.error('获取公告列表失败')
  } finally {
    loading.value = false
  }
}

// 搜索
const handleSearch = () => {
  currentPage.value = 1
  fetchNotices()
}

// 分页
const handleSizeChange = (newSize) => {
  pageSize.value = newSize
  currentPage.value = 1
  fetchNotices()
}

const handleCurrentChange = () => {
  fetchNotices()
}

// 新增
const handleAdd = () => {
  isEdit.value = false
  dialogTitle.value = '新增公告'
  Object.assign(noticeForm, {
    id: null,
    title: '',
    content: '',
    targetType: 'ALL',
    targetIds: [],
    publishTime: null,
    expireTime: null,
  })
  dialogVisible.value = true
}

// 编辑
const handleEdit = (row) => {
  isEdit.value = true
  dialogTitle.value = '编辑公告'
  Object.assign(noticeForm, {
    id: row.id,
    title: row.title,
    content: row.content,
    targetType: row.targetType,
    targetIds: row.targetIds || [],
    publishTime: row.publishTime ? formatDateTimeForInput(row.publishTime) : null,
    expireTime: row.expireTime ? formatDateTimeForInput(row.expireTime) : null,
  })
  // 如果是DEPT/USER类型，先加载对应数据
  if (row.targetType === 'USER' && row.targetIds?.length > 0) {
    loadUsersByIds(row.targetIds)
  }
  dialogVisible.value = true
}

// 提交
const handleSubmit = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    const data = {
      title: noticeForm.title,
      content: noticeForm.content,
      targetType: noticeForm.targetType,
      targetIds: noticeForm.targetType === 'ALL' ? [] : noticeForm.targetIds,
      publishTime: noticeForm.publishTime || null,
      expireTime: noticeForm.expireTime || null,
    }

    if (isEdit.value) {
      await noticeApi.update(noticeForm.id, data)
      ElMessage.success('更新成功')
    } else {
      await noticeApi.create(data)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    fetchNotices()
  } catch (e) {
    ElMessage.error(e?.message || (isEdit.value ? '更新失败' : '创建失败'))
  } finally {
    submitting.value = false
  }
}

// 删除
const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(`确定要删除公告「${row.title}」吗？此操作不可恢复。`, '警告', {
      type: 'warning',
    })
    await noticeApi.delete(row.id)
    ElMessage.success('删除成功')
    fetchNotices()
  } catch (e) {
    if (e !== 'cancel' && e !== 'close') {
      ElMessage.error(e?.message || '删除失败')
    }
  }
}

// 发布
const handlePublish = async (row) => {
  try {
    await ElMessageBox.confirm(`确定要发布公告「${row.title}」吗？`, '提示')
    await noticeApi.publish(row.id)
    ElMessage.success('发布成功')
    fetchNotices()
  } catch (e) {
    if (e !== 'cancel' && e !== 'close') {
      ElMessage.error(e?.message || '发布失败')
    }
  }
}

// 撤回
const handleWithdraw = async (row) => {
  try {
    await ElMessageBox.confirm(`确定要撤回公告「${row.title}」吗？`, '提示')
    await noticeApi.withdraw(row.id)
    ElMessage.success('撤回成功')
    fetchNotices()
  } catch (e) {
    if (e !== 'cancel' && e !== 'close') {
      ElMessage.error(e?.message || '撤回失败')
    }
  }
}

// 目标类型变化
const handleTargetTypeChange = () => {
  noticeForm.targetIds = []
  userList.value = []
}

// 搜索用户
const searchUsers = async (query) => {
  if (!query) {
    userList.value = []
    return
  }
  try {
    const res = await fetch(`/api/users?page=1&size=20&keyword=${query}`, {
      headers: { Authorization: `Bearer ${sessionStorage.getItem('token')}` },
    })
    const data = await res.json()
    userList.value = data.data?.list || data.data || []
  } catch (e) {
    console.error('搜索用户失败', e)
    ElMessage.warning('搜索用户失败，请稍后重试')
  }
}

// 根据ID加载用户（编辑时用）
const loadUsersByIds = async (ids) => {
  if (!ids?.length) return
  try {
    const users = []
    for (const id of ids) {
      const res = await fetch(`/api/users/${id}`, {
        headers: { Authorization: `Bearer ${sessionStorage.getItem('token')}` },
      })
      const data = await res.json()
      if (data.data) {
        users.push(data.data)
      }
    }
    userList.value = users
  } catch (e) {
    console.error('加载用户失败', e)
    ElMessage.warning('加载用户信息失败，请刷新重试')
  }
}

// 加载部门列表
const loadDepts = async () => {
  try {
    const res = await deptApi.tree()
    deptList.value = res || []
  } catch (e) {
    console.error('加载部门列表失败', e)
    ElMessage.warning('部门列表加载失败')
  }
}

// 工具函数
const getTargetTypeLabel = (type) => {
  const map = { ALL: '全部', DEPT: '部门', USER: '用户' }
  return map[type] || type
}

const getTargetTypeTagType = (type) => {
  const map = { ALL: '', DEPT: 'success', USER: 'warning' }
  return map[type] || 'info'
}

const getStatusLabel = (status) => {
  const map = { DRAFT: '草稿', PUBLISHED: '已发布', EXPIRED: '已过期', WITHDRAWN: '已撤回' }
  return map[status] || status
}

const getStatusTagType = (status) => {
  const map = { DRAFT: 'info', PUBLISHED: 'success', EXPIRED: 'warning', WITHDRAWN: 'danger' }
  return map[status] || 'info'
}

const formatDateTime = (dt) => {
  if (!dt) return ''
  if (typeof dt === 'string') return dt.replace('T', ' ')
  return dt
}

const formatDateTimeForInput = (dt) => {
  if (!dt) return null
  if (typeof dt === 'string') return dt.replace('T', ' ')
  // LocalDateTime format
  return `${dt.year}-${String(dt.monthValue).padStart(2, '0')}-${String(dt.dayOfMonth).padStart(2, '0')} ${String(dt.hour).padStart(2, '0')}:${String(dt.minute).padStart(2, '0')}:${String(dt.second).padStart(2, '0')}`
}

onMounted(() => {
  fetchNotices()
  loadDepts()
})
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Outfit:wght@300;400;500;600;700&family=Sora:wght@300;400;500;600&display=swap');

.notice-management {
  animation: fadeInUp 0.5s ease-out forwards;
}

.page-header {
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

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.filter-row {
  display: flex;
  gap: 12px;
  align-items: center;
}

.search-input {
  width: 240px;
}

.status-select {
  width: 140px;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
}

:deep(.el-input__wrapper) {
  background: var(--glass-bg);
  border: 1px solid rgba(255, 255, 255, 0.1);
  box-shadow: none;
}

:deep(.el-input__inner) {
  color: var(--text-secondary);
}

:deep(.el-dialog) {
  background: var(--bg-dark);
  backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 16px;
}

:deep(.el-dialog__title) {
  color: var(--text-primary);
  font-family: 'Sora', sans-serif;
}

:deep(.el-form-item__label) {
  color: var(--text-secondary);
}

:deep(.el-input__wrapper),
:deep(.el-select .el-input__wrapper) {
  background: var(--glass-bg);
  border: 1px solid rgba(255, 255, 255, 0.1);
  box-shadow: none;
}

:deep(.el-input__inner) {
  color: var(--text-secondary);
}

:deep(.el-radio__label) {
  color: var(--text-secondary);
}

:deep(.el-radio__input.is-checked + .el-radio__label) {
  color: var(--neon-cyan);
}

:deep(.el-table) {
  --el-table-bg-color: transparent;
  --el-table-tr-bg-color: transparent;
  --el-table-header-bg-color: rgba(255, 255, 255, 0.03);
  --el-table-row-hover-bg-color: rgba(255, 255, 255, 0.05);
  --el-table-text-color: var(--text-secondary);
  --el-table-header-text-color: var(--text-secondary);
}

:deep(.el-table th.el-table__cell) {
  background: rgba(255, 255, 255, 0.03);
  font-weight: 600;
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
