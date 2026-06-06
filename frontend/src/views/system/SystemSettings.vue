<template>
  <div class="system-settings">
    <div class="page-header">
      <h1 class="page-title">系统配置</h1>
      <p class="page-sub">管理系统运行参数与配置项</p>
    </div>

    <div class="glass-card">
      <div class="card-header">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索配置键或名称"
          :prefix-icon="Search"
          clearable
          class="search-input"
        />
        <el-button v-permission="'system:config:list'" type="primary" @click="handleAdd">
          <el-icon><Plus /></el-icon>
          新增配置
        </el-button>
      </div>

      <el-table v-loading="loading" :data="filteredList" stripe class="config-table">
        <el-table-column prop="configKey" label="配置键" min-width="160" />
        <el-table-column prop="configValue" label="配置值" min-width="160" show-overflow-tooltip />
        <el-table-column prop="configType" label="类型" width="100" />
        <el-table-column prop="configName" label="配置名称" min-width="140" />
        <el-table-column prop="configGroup" label="分组" width="120" />
        <el-table-column prop="description" label="描述" min-width="180" show-overflow-tooltip />
        <el-table-column prop="isVisible" label="可见" width="80">
          <template #default="{ row }">
            <el-tag :type="row.isVisible === 1 ? 'success' : 'info'" size="small">
              {{ row.isVisible === 1 ? '是' : '否' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="updateTime" label="更新时间" width="170" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button v-permission="'system:config:list'" link type="primary" @click="handleEdit(row)">编辑</el-button>
            <el-button v-permission="'system:config:list'" link type="danger" @click="handleDelete(row)"
              >删除</el-button
            >
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="560px" :close-on-click-modal="false">
      <el-form ref="formRef" :model="configForm" :rules="rules" label-width="90px">
        <el-form-item label="配置键" prop="configKey">
          <el-input v-model="configForm.configKey" placeholder="请输入配置键" :disabled="isEdit" />
        </el-form-item>
        <el-form-item label="配置值" prop="configValue">
          <el-input v-model="configForm.configValue" placeholder="请输入配置值" />
        </el-form-item>
        <el-form-item label="配置类型" prop="configType">
          <el-select v-model="configForm.configType" placeholder="请选择类型" style="width: 100%">
            <el-option label="string" value="string" />
            <el-option label="number" value="number" />
            <el-option label="boolean" value="boolean" />
            <el-option label="json" value="json" />
          </el-select>
        </el-form-item>
        <el-form-item label="配置名称" prop="configName">
          <el-input v-model="configForm.configName" placeholder="请输入配置名称" />
        </el-form-item>
        <el-form-item label="配置分组" prop="configGroup">
          <el-input v-model="configForm.configGroup" placeholder="请输入分组" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="configForm.description" type="textarea" :rows="3" placeholder="请输入描述" />
        </el-form-item>
        <el-form-item label="是否可见" prop="isVisible">
          <el-radio-group v-model="configForm.isVisible">
            <el-radio :label="1">可见</el-radio>
            <el-radio :label="0">隐藏</el-radio>
          </el-radio-group>
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
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Plus } from '@element-plus/icons-vue'
import { systemApi } from '../../api/system'

const loading = ref(false)
const configList = ref([])
const searchKeyword = ref('')

const dialogVisible = ref(false)
const dialogTitle = ref('')
const isEdit = ref(false)
const submitting = ref(false)
const formRef = ref(null)

const configForm = reactive({
  configKey: '',
  configValue: '',
  configType: 'string',
  configName: '',
  configGroup: 'default',
  description: '',
  isVisible: 1,
})

const rules = {
  configKey: [{ required: true, message: '请输入配置键', trigger: 'blur' }],
  configValue: [{ required: true, message: '请输入配置值', trigger: 'blur' }],
  configType: [{ required: true, message: '请选择配置类型', trigger: 'change' }],
  configName: [{ required: true, message: '请输入配置名称', trigger: 'blur' }],
}

const filteredList = computed(() => {
  if (!searchKeyword.value) return configList.value
  const kw = searchKeyword.value.toLowerCase()
  return configList.value.filter(
    (item) => item.configKey?.toLowerCase().includes(kw) || item.configName?.toLowerCase().includes(kw),
  )
})

const fetchConfigs = async () => {
  loading.value = true
  try {
    const res = await systemApi.getAllConfigs()
    configList.value = res || []
  } catch {
    ElMessage.error('获取配置列表失败')
  } finally {
    loading.value = false
  }
}

const handleAdd = () => {
  isEdit.value = false
  dialogTitle.value = '新增配置'
  Object.assign(configForm, {
    configKey: '',
    configValue: '',
    configType: 'string',
    configName: '',
    configGroup: 'default',
    description: '',
    isVisible: 1,
  })
  dialogVisible.value = true
}

const handleEdit = (row) => {
  isEdit.value = true
  dialogTitle.value = '编辑配置'
  Object.assign(configForm, {
    configKey: row.configKey,
    configValue: row.configValue,
    configType: row.configType || 'string',
    configName: row.configName || '',
    configGroup: row.configGroup || 'default',
    description: row.description || '',
    isVisible: row.isVisible ?? 1,
  })
  dialogVisible.value = true
}

const handleSubmit = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    if (isEdit.value) {
      await systemApi.updateConfig(configForm.configKey, {
        configValue: configForm.configValue,
        configType: configForm.configType,
        configName: configForm.configName,
        configGroup: configForm.configGroup,
        description: configForm.description,
        isVisible: configForm.isVisible,
      })
    } else {
      await systemApi.createConfig({ ...configForm })
    }
    ElMessage.success(isEdit.value ? '更新成功' : '创建成功')
    dialogVisible.value = false
    fetchConfigs()
  } catch {
    ElMessage.error(isEdit.value ? '更新失败' : '创建失败')
  } finally {
    submitting.value = false
  }
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(`确定要删除配置 ${row.configKey} 吗？此操作不可恢复。`, '警告', { type: 'warning' })
    await systemApi.deleteConfig(row.configKey)
    ElMessage.success('删除成功')
    fetchConfigs()
  } catch {
    // cancelled
  }
}

onMounted(() => {
  fetchConfigs()
})
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Outfit:wght@300;400;500;600;700&family=Sora:wght@300;400;500;600&display=swap');

.system-settings {
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
  border: 1px solid var(--border-glow-magenta);
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

:deep(.el-input__wrapper),
:deep(.el-select .el-input__wrapper),
:deep(.el-textarea__inner) {
  background: var(--glass-bg);
  border: 1px solid var(--border-glow-magenta);
  box-shadow: none;
}

:deep(.el-input__inner),
:deep(.el-textarea__inner) {
  color: var(--text-secondary);
}

:deep(.el-dialog) {
  background: var(--bg-darker);
  backdrop-filter: blur(20px);
  border: 1px solid var(--border-glow-magenta);
  border-radius: 16px;
}

:deep(.el-dialog__title) {
  color: var(--text-primary);
  font-family: 'Sora', sans-serif;
}

:deep(.el-form-item__label) {
  color: var(--text-secondary);
}

:deep(.el-radio__label) {
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
