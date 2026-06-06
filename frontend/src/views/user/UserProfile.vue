<template>
  <div class="user-profile">
    <div class="page-header">
      <h1 class="page-title">个人信息</h1>
      <p class="page-sub">查看和编辑您的个人资料</p>
    </div>

    <div class="glass-card">
      <div class="avatar-section">
        <el-upload
          class="avatar-uploader"
          :show-file-list="false"
          :before-upload="beforeUpload"
          :http-request="handleUpload"
        >
          <div v-if="form.avatar" class="avatar-preview" :style="{ backgroundImage: `url(${form.avatar})` }"></div>
          <div v-else class="avatar-placeholder" :style="{ backgroundColor: avatarColor }">
            {{ initial }}
          </div>
          <div class="avatar-overlay">
            <el-icon><Camera /></el-icon>
            <span>更换头像</span>
          </div>
        </el-upload>
        <div class="avatar-hint">支持 jpg/png/gif/webp，大小不超过 2MB</div>
      </div>

      <el-form :model="form" label-width="100px" class="profile-form">
        <el-form-item label="用户名">
          <el-input :model-value="userStore.username" disabled />
        </el-form-item>

        <el-form-item label="昵称">
          <el-input v-model="form.nickname" placeholder="请输入昵称" maxlength="50" />
        </el-form-item>

        <el-form-item label="邮箱">
          <el-input :model-value="form.email || '未设置'" disabled />
        </el-form-item>

        <el-form-item label="角色">
          <el-tag>{{ userStore.role }}</el-tag>
        </el-form-item>

        <el-form-item label="所属部门">
          <el-input :model-value="form.deptName || '未分配'" disabled />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :loading="saving" @click="handleSave"> 保存修改 </el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Camera } from '@element-plus/icons-vue'
import { useUserStore } from '../../stores/user'
import { userApi } from '../../api/user'
import axios from 'axios'
import type { UploadProps, UploadRequestOptions } from 'element-plus'

const userStore = useUserStore()

const PALETTE = ['#ff00ff', '#00ffff', '#39ff14', '#ff6600', '#ffff00', '#ff1493']

const initial = computed(() => {
  const name = userStore.username || 'U'
  return name.charAt(0).toUpperCase()
})

const avatarColor = computed(() => {
  const name = userStore.username || 'unknown'
  let hash = 0
  for (let i = 0; i < name.length; i++) {
    hash = name.charCodeAt(i) + ((hash << 5) - hash)
  }
  return PALETTE[Math.abs(hash) % PALETTE.length]
})

const form = reactive({
  nickname: '',
  email: '',
  avatar: '',
  deptName: '',
})

const saving = ref(false)

onMounted(async () => {
  try {
    const res = await userApi.getProfile()
    const user = res as { nickname?: string; email?: string; avatar?: string; deptName?: string }
    form.nickname = user.nickname || ''
    form.email = user.email || ''
    form.avatar = user.avatar || ''
    form.deptName = user.deptName || ''
    if (!userStore.nickname) {
      userStore.setUser({ nickname: user.nickname || '' })
    }
  } catch {
    ElMessage.error('加载用户信息失败')
  }
})

const beforeUpload: UploadProps['beforeUpload'] = (file) => {
  const validTypes = ['image/jpeg', 'image/png', 'image/gif', 'image/webp']
  if (!validTypes.includes(file.type)) {
    ElMessage.error('仅支持 jpg/png/gif/webp 格式')
    return false
  }
  if (file.size > 2 * 1024 * 1024) {
    ElMessage.error('文件大小不能超过 2MB')
    return false
  }
  return true
}

const handleUpload = async (options: UploadRequestOptions) => {
  const formData = new FormData()
  formData.append('file', options.file)
  try {
    const res = await axios.post('/api/files/upload', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
        Authorization: `Bearer ${userStore.token}`,
      },
    })
    // 响应格式: { code: 200, data: { filePath: "2026\\05\\22\\xxx.jpeg", ... } }
    const filePath = res.data?.data?.filePath as string
    if (!filePath) {
      ElMessage.error('头像上传失败')
      return
    }
    const url = '/uploads/' + filePath.replace(/\\/g, '/')
    form.avatar = url
    await userApi.updateProfile({ avatar: url })
    userStore.setUser({ avatar: url })
    ElMessage.success('头像更新成功')
  } catch {
    ElMessage.error('头像上传失败')
  }
}

async function handleSave() {
  saving.value = true
  try {
    await userApi.updateProfile({ nickname: form.nickname })
    userStore.setUser({ nickname: form.nickname })
    ElMessage.success('个人信息更新成功')
  } catch {
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.user-profile {
  max-width: 600px;
}

.page-header {
  margin-bottom: 24px;
}

.page-title {
  font-size: 20px;
  font-weight: 600;
  color: var(--text-primary, #fff);
  margin: 0 0 4px;
}

.page-sub {
  color: var(--text-secondary, rgba(255, 255, 255, 0.6));
  font-size: 14px;
  margin: 0;
}

.glass-card {
  background: var(--glass-bg, rgba(0, 0, 0, 0.4));
  backdrop-filter: blur(20px);
  border: 1px solid var(--border-subtle, rgba(255, 255, 255, 0.1));
  border-radius: 12px;
  padding: 32px;
}

.avatar-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 32px;
}

.avatar-uploader {
  position: relative;
  cursor: pointer;
}

.avatar-preview,
.avatar-placeholder {
  width: 80px;
  height: 80px;
  border-radius: 50%;
}

.avatar-preview {
  background-size: cover;
  background-position: center;
  border: 2px solid var(--border-glow-magenta, rgba(255, 0, 255, 0.3));
}

.avatar-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  font-weight: 700;
  color: #fff;
}

.avatar-overlay {
  position: absolute;
  inset: 0;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 12px;
  gap: 4px;
  opacity: 0;
  transition: opacity 150ms ease;
}

.avatar-uploader:hover .avatar-overlay {
  opacity: 1;
}

.avatar-hint {
  color: var(--text-secondary, rgba(255, 255, 255, 0.4));
  font-size: 12px;
  margin-top: 8px;
}

.profile-form {
  max-width: 400px;
  margin: 0 auto;
}
</style>
