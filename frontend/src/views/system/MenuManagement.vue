<template>
  <div class="menu-management">
    <div class="page-header">
      <h1 class="page-title">菜单管理</h1>
      <p class="page-sub">管理系统菜单和权限</p>
    </div>

    <div class="glass-card">
      <div class="card-header">
        <el-button type="primary" @click="handleAdd(null)">
          <el-icon><Plus /></el-icon>
          新增菜单
        </el-button>
      </div>

      <el-table v-loading="loading" :data="menuList" stripe class="menu-table" row-key="id" default-expand-all>
        <el-table-column prop="name" label="菜单名称" min-width="150" />
        <el-table-column prop="path" label="路由路径" min-width="200" />
        <el-table-column prop="icon" label="图标" width="100">
          <template #default="{ row }">
            <span v-if="row.icon">{{ row.icon }}</span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="menuType" label="类型" width="100">
          <template #default="{ row }">
            <el-tag :type="typeTagType(row.menuType)">
              {{ typeLabel(row.menuType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="permission" label="权限标识" min-width="180">
          <template #default="{ row }">
            {{ row.permission || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="sortOrder" label="排序" width="80" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleAdd(row)">添加下级</el-button>
            <el-button link type="primary" @click="handleEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>

  <el-dialog
    v-model="dialogVisible"
    :title="dialogTitle"
    width="550px"
    :close-on-click-modal="false"
    :teleported="false"
    destroy-on-close
    class="menu-dialog"
  >
    <el-form ref="formRef" :model="menuForm" :rules="rules" label-width="100px">
      <el-form-item label="上级菜单" prop="parentId">
        <el-tree-select
          v-model="menuForm.parentId"
          :data="treeData"
          :props="{ label: 'name', value: 'id', children: 'children' }"
          placeholder="请选择上级菜单"
          clearable
          check-strictly
          :render-after-expand="false"
          style="width: 100%"
        />
      </el-form-item>
      <el-form-item label="菜单类型" prop="menuType">
        <el-radio-group v-model="menuForm.menuType" @change="handleTypeChange">
          <el-radio :label="1">目录</el-radio>
          <el-radio :label="2">菜单</el-radio>
          <el-radio :label="3">按钮</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="菜单名称" prop="name">
        <el-input v-model="menuForm.name" placeholder="请输入菜单名称" />
      </el-form-item>
      <el-form-item v-if="menuForm.menuType !== 3" label="路由路径" prop="path">
        <el-input v-model="menuForm.path" placeholder="请输入路由路径" />
      </el-form-item>
      <el-form-item v-if="menuForm.menuType !== 3" label="图标" prop="icon">
        <el-input v-model="menuForm.icon" placeholder="请输入图标名称" />
      </el-form-item>
      <el-form-item v-if="menuForm.menuType === 2" label="组件路径" prop="component">
        <el-input v-model="menuForm.component" placeholder="如: system/UserManagement" />
      </el-form-item>
      <el-form-item v-if="menuForm.menuType !== 1" label="权限标识" prop="permission">
        <el-input v-model="menuForm.permission" placeholder="如: system:user:list" />
      </el-form-item>
      <el-form-item label="排序" prop="sortOrder">
        <el-input-number v-model="menuForm.sortOrder" :min="0" :max="9999" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-radio-group v-model="menuForm.status">
          <el-radio :label="1">启用</el-radio>
          <el-radio :label="0">禁用</el-radio>
        </el-radio-group>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { useUserStore } from '../../stores/user'
import { menuApi } from '../../api/menu'

const userStore = useUserStore()

const loading = ref(false)
const menuList = ref([])
const dialogVisible = ref(false)
const dialogTitle = ref('')
const isEdit = ref(false)
const submitting = ref(false)
const formRef = ref(null)

const menuForm = reactive({
  id: null as number | null,
  parentId: null as number | null,
  name: '',
  path: '',
  icon: '',
  component: '',
  permission: '',
  menuType: 2,
  sortOrder: 0,
  status: 1,
})

const rules = {
  name: [{ required: true, message: '请输入菜单名称', trigger: 'blur' }],
  menuType: [{ required: true, message: '请选择菜单类型', trigger: 'change' }],
}

const typeLabel = (menuType: number) => {
  const labels: Record<number, string> = { 1: '目录', 2: '菜单', 3: '按钮' }
  return labels[menuType] || '-'
}

const typeTagType = (menuType: number) => {
  const types: Record<number, string> = { 1: 'warning', 2: 'success', 3: 'info' }
  return types[menuType] || 'info'
}

const treeData = computed(() => {
  const buildTree = (list: any[], parentId: number | null = null) => {
    return list
      .filter((item: any) => item.parentId === parentId)
      .map((item: any) => ({
        id: item.id,
        name: item.name,
        children: buildTree(list, item.id),
      }))
  }
  return buildTree(menuList.value)
})

const fetchMenus = async () => {
  loading.value = true
  try {
    const res = await menuApi.tree()
    menuList.value = res || []
  } catch (e) {
    ElMessage.error('获取菜单列表失败')
  } finally {
    loading.value = false
  }
}

const handleTypeChange = () => {
  if (menuForm.menuType === 3) {
    menuForm.path = ''
    menuForm.icon = ''
    menuForm.component = ''
  }
}

const handleAdd = (parent: any) => {
  isEdit.value = false
  dialogTitle.value = parent ? '添加下级菜单' : '新增菜单'
  Object.assign(menuForm, {
    id: null,
    parentId: parent?.id || null,
    name: '',
    path: '',
    icon: '',
    component: '',
    permission: '',
    menuType: 2,
    status: 1,
  })
  dialogVisible.value = true
}

const handleEdit = (row: any) => {
  isEdit.value = true
  dialogTitle.value = '编辑菜单'
  Object.assign(menuForm, { ...row, parentId: row.parentId === 0 ? null : row.parentId })
  dialogVisible.value = true
}

const handleSubmit = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    const data = {
      ...menuForm,
      parentId: menuForm.parentId || 0,
    }
    if (isEdit.value) {
      await menuApi.update(menuForm.id!, data)
    } else {
      await menuApi.create(data)
    }
    ElMessage.success(isEdit.value ? '更新成功' : '添加成功')
    dialogVisible.value = false
    fetchMenus()
  } catch (e) {
    ElMessage.error(isEdit.value ? '更新失败' : '添加失败')
  } finally {
    submitting.value = false
  }
}

const handleDelete = async (row: any) => {
  try {
    await ElMessageBox.confirm(`确定要删除菜单 ${row.name} 吗？此操作不可恢复。`, '警告', { type: 'warning' })
    await menuApi.delete(row.id)
    ElMessage.success('删除成功')
    fetchMenus()
  } catch {
    // cancelled
  }
}

onMounted(() => {
  fetchMenus()
})
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Outfit:wght@300;400;500;600;700&family=Sora:wght@300;400;500;600&display=swap');

.menu-management {
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
  justify-content: flex-start;
  align-items: center;
  margin-bottom: 20px;
}

.menu-name {
  color: var(--text-secondary);
  font-weight: 500;
}

:deep(.el-input__wrapper),
:deep(.el-select .el-input__wrapper) {
  background: var(--glass-bg);
  border: 1px solid var(--border-glow-magenta);
  box-shadow: none;
}

:deep(.el-input__inner) {
  color: var(--text-secondary);
}

:deep(.el-dialog__wrapper) {
  z-index: 3000 !important;
}
:deep(.v-modal) {
  z-index: 2999 !important;
}

:deep(.el-dialog) {
  background: var(--bg-darker, #1a1a2e) !important;
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid var(--border-glow-magenta, rgba(255, 0, 255, 0.3)) !important;
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

:deep(.el-button) {
  cursor: pointer;
}

:deep(.el-button.link) {
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
</style>

<style>
.menu-dialog.el-dialog {
  background: #0d0d1a !important;
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 0, 255, 0.3) !important;
  border-radius: 16px;
}

.menu-dialog .el-dialog__header {
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  padding-bottom: 16px;
}

.menu-dialog .el-dialog__title {
  color: #fff;
  font-family: 'Sora', sans-serif;
}

.menu-dialog .el-dialog__headerbtn .el-dialog__close {
  color: rgba(255, 255, 255, 0.5);
}

.menu-dialog .el-dialog__headerbtn:hover .el-dialog__close {
  color: #fff;
}

.menu-dialog .el-form-item__label {
  color: rgba(255, 255, 255, 0.7);
}

.menu-dialog .el-radio__label {
  color: rgba(255, 255, 255, 0.7);
}

.menu-dialog .el-input__wrapper,
.menu-dialog .el-select .el-input__wrapper,
.menu-dialog .el-tree-select .el-input__wrapper {
  background: rgba(0, 0, 0, 0.6);
  border: 1px solid rgba(255, 0, 255, 0.3);
  box-shadow: none;
}

.menu-dialog .el-input__inner {
  color: rgba(255, 255, 255, 0.7);
}

.menu-dialog .el-input-number .el-input__wrapper {
  background: rgba(0, 0, 0, 0.6);
  border: 1px solid rgba(255, 0, 255, 0.3);
  box-shadow: none;
}

.menu-dialog .el-dialog__footer {
  border-top: 1px solid rgba(255, 255, 255, 0.08);
  padding-top: 16px;
}
</style>
