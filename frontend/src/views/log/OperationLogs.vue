<template>
  <div class="operation-logs">
    <div class="page-header">
      <h1 class="page-title">操作日志</h1>
      <p class="page-sub">查看用户操作记录</p>
    </div>

    <div class="glass-card">
      <div class="card-header">
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          class="date-picker"
          @change="handleSearch"
        />
        <el-button type="danger" :loading="cleaning" :disabled="selectedLogs.length === 0" @click="handleBatchClean">
          <el-icon><Delete /></el-icon>
          批量清理 ({{ selectedLogs.length }})
        </el-button>
      </div>

      <el-table v-loading="loading" :data="logs" stripe class="log-table" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="50" />
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="username" label="用户名" width="120" />
        <el-table-column prop="operation" label="操作" min-width="150" />
        <el-table-column prop="method" label="方法" width="80" />
        <el-table-column prop="url" label="URL" min-width="200" show-overflow-tooltip />
        <el-table-column prop="ip" label="IP地址" width="140" />
        <el-table-column prop="durationMs" label="耗时(ms)" width="100" />
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="时间" width="180" />
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete } from '@element-plus/icons-vue'
import { useUserStore } from '../../stores/user'

const userStore = useUserStore()

const loading = ref(false)
const logs = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const dateRange = ref(null)
const cleaning = ref(false)
const selectedLogs = ref([])

const fetchLogs = async () => {
  loading.value = true
  try {
    let url = `/api/logs?page=${currentPage.value}&size=${pageSize.value}`
    if (dateRange.value && dateRange.value.length === 2) {
      const [start, end] = dateRange.value
      url += `&startDate=${start.toISOString().split('T')[0]}&endDate=${end.toISOString().split('T')[0]}`
    }
    const res = await fetch(url, {
      headers: { Authorization: `Bearer ${userStore.token}` },
    })
    if (res.ok) {
      const response = await res.json()
      const data = response.data || response
      logs.value = data.list || data.records || []
      total.value = data.total || 0
    } else {
      ElMessage.error('获取日志列表失败')
    }
  } catch (e) {
    ElMessage.error('获取日志列表失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  currentPage.value = 1
  fetchLogs()
}

const handleSizeChange = () => {
  currentPage.value = 1
  fetchLogs()
}

const handleCurrentChange = () => {
  fetchLogs()
}

const handleSelectionChange = (selection) => {
  selectedLogs.value = selection
}

const handleBatchClean = async () => {
  if (selectedLogs.value.length === 0) return
  try {
    await ElMessageBox.confirm(`确定要清理选中的 ${selectedLogs.value.length} 条日志吗？此操作不可恢复。`, '警告', {
      type: 'warning',
    })
    cleaning.value = true
    const ids = selectedLogs.value.map((log) => log.id)
    const res = await fetch('/api/logs/batch', {
      method: 'DELETE',
      headers: {
        Authorization: `Bearer ${userStore.token}`,
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ ids }),
    })
    if (res.ok) {
      ElMessage.success('清理成功')
      selectedLogs.value = []
      fetchLogs()
    } else {
      ElMessage.error('清理失败')
    }
  } catch {
    // cancelled
  } finally {
    cleaning.value = false
  }
}

onMounted(() => {
  fetchLogs()
})
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Outfit:wght@300;400;500;600;700&family=Sora:wght@300;400;500;600&display=swap');

.operation-logs {
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

.date-picker {
  width: 280px;
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
