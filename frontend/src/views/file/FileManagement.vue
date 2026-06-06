<template>
  <div class="file-management">
    <div class="page-header">
      <h1 class="page-title">文件管理</h1>
      <p class="page-sub">管理上传的文件</p>
    </div>

    <div class="glass-card">
      <div class="card-header">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索文件名"
          :prefix-icon="Search"
          clearable
          class="search-input"
          @input="handleSearch"
        />
        <el-button type="primary" @click="showUploadDialog = true">
          <el-icon><Upload /></el-icon>
          上传文件
        </el-button>
      </div>

      <el-table v-loading="loading" :data="files" stripe class="file-table">
        <el-table-column prop="fileName" label="文件名" min-width="200" />
        <el-table-column prop="fileType" label="类型" width="100" />
        <el-table-column prop="fileExt" label="扩展名" width="100" />
        <el-table-column prop="fileSize" label="大小" width="120">
          <template #default="{ row }">
            {{ formatSize(row.fileSize) }}
          </template>
        </el-table-column>
        <el-table-column prop="downloadCount" label="下载次数" width="100" />
        <el-table-column prop="createTime" label="上传时间" width="180" />
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleDownload(row)">下载</el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </div>

    <el-dialog v-model="showUploadDialog" title="上传文件" width="400px">
      <el-upload
        ref="uploadRef"
        drag
        :auto-upload="false"
        :limit="1"
        :on-change="handleFileChange"
        class="file-uploader"
      >
        <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
        <div class="el-upload__text">拖拽文件到此处 或 <em>点击上传</em></div>
      </el-upload>
      <template #footer>
        <el-button @click="showUploadDialog = false">取消</el-button>
        <el-button type="primary" :loading="uploading" @click="handleUpload">上传</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Upload, UploadFilled } from '@element-plus/icons-vue'
import { useUserStore } from '../../stores/user'

const userStore = useUserStore()

const loading = ref(false)
const files = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const searchKeyword = ref('')
const showUploadDialog = ref(false)
const uploading = ref(false)
const uploadRef = ref(null)
const selectedFile = ref(null)

// 监听弹窗打开，清空上传组件状态
watch(showUploadDialog, (val) => {
  if (val && uploadRef.value) {
    uploadRef.value.clearFiles()
    selectedFile.value = null
  }
})

const fetchFiles = async () => {
  loading.value = true
  try {
    const res = await fetch(
      `/api/files?page=${currentPage.value}&size=${pageSize.value}&keyword=${searchKeyword.value}`,
      {
        headers: { Authorization: `Bearer ${userStore.token}` },
      },
    )
    if (res.ok) {
      const response = await res.json()
      const data = response.data || response
      files.value = data.list || data.records || []
      total.value = data.total || 0
    } else {
      ElMessage.error('获取文件列表失败')
    }
  } catch (e) {
    ElMessage.error('获取文件列表失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  currentPage.value = 1
  fetchFiles()
}

const handleSizeChange = () => {
  currentPage.value = 1
  fetchFiles()
}

const handleCurrentChange = () => {
  fetchFiles()
}

const handleFileChange = (file) => {
  selectedFile.value = file.raw
}

const handleUpload = async () => {
  if (!selectedFile.value) {
    ElMessage.warning('请选择文件')
    return
  }
  uploading.value = true
  try {
    const formData = new FormData()
    formData.append('file', selectedFile.value)
    const res = await fetch('/api/files/upload', {
      method: 'POST',
      headers: { Authorization: `Bearer ${userStore.token}` },
      body: formData,
    })
    if (res.ok) {
      ElMessage.success('上传成功')
      showUploadDialog.value = false
      selectedFile.value = null
      fetchFiles()
    } else {
      ElMessage.error('上传失败')
    }
  } catch (e) {
    ElMessage.error('上传失败')
  } finally {
    uploading.value = false
  }
}

const handleDownload = async (row) => {
  try {
    const token = userStore.token
    const url = `/api/files/${row.id}/download`
    const res = await fetch(url, {
      headers: { Authorization: `Bearer ${token}` },
    })
    if (!res.ok) {
      ElMessage.error('下载失败')
      return
    }
    const blob = await res.blob()
    const objectUrl = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = objectUrl
    a.download = row.fileName
    a.click()
    URL.revokeObjectURL(objectUrl)
  } catch (e) {
    ElMessage.error('下载失败')
  }
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(`确定要删除文件 ${row.fileName} 吗？`, '提示', { type: 'warning' })
    const res = await fetch(`/api/files/${row.id}`, {
      method: 'DELETE',
      headers: { Authorization: `Bearer ${userStore.token}` },
    })
    if (res.ok) {
      ElMessage.success('删除成功')
      fetchFiles()
    } else {
      ElMessage.error('删除失败')
    }
  } catch {
    // cancelled
  }
}

const formatSize = (bytes) => {
  if (!bytes) return '-'
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  if (bytes < 1024 * 1024 * 1024) return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
  return (bytes / (1024 * 1024 * 1024)).toFixed(1) + ' GB'
}

onMounted(() => {
  fetchFiles()
})
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Outfit:wght@300;400;500;600;700&family=Sora:wght@300;400;500;600&display=swap');

.file-management {
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

.search-input {
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

.file-uploader {
  width: 100%;
}

:deep(.el-upload-dragger) {
  background: var(--glass-bg);
  border: 1px dashed rgba(255, 255, 255, 0.2);
  border-radius: 12px;
}

:deep(.el-upload-dragger:hover) {
  border-color: var(--neon-cyan);
  cursor: pointer;
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

:deep(.el-dialog__body) {
  overflow-y: visible;
  max-height: none;
}
</style>
