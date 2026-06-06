<template>
  <div class="dept-management">
    <div class="page-header">
      <h1 class="page-title">部门管理</h1>
      <p class="page-sub">管理系统部门组织架构</p>
    </div>

    <div class="glass-card">
      <div class="card-header">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索部门名称"
          :prefix-icon="Search"
          clearable
          class="search-input"
          @input="handleSearch"
        />
        <el-button v-permission="'system:dept:list'" type="primary" @click="handleAdd">
          <el-icon><Plus /></el-icon>
          新增部门
        </el-button>
      </div>

      <el-table v-loading="loading" :data="filteredDeptList" stripe class="dept-table" row-key="id" default-expand-all>
        <el-table-column prop="name" label="部门名称" min-width="150" />
        <el-table-column prop="leaderName" label="负责人" width="120" />
        <el-table-column prop="phone" label="联系电话" width="150" />
        <el-table-column prop="email" label="邮箱" min-width="180" />
        <el-table-column prop="sortOrder" label="排序" width="80" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button v-permission="'system:dept:list'" link type="primary" @click="handleEdit(row)">编辑</el-button>
            <el-button v-permission="'system:dept:list'" link type="success" @click="handleAddChild(row)"
              >添加下级</el-button
            >
            <el-button
              v-permission="'system:dept:list'"
              link
              :type="row.status === 1 ? 'danger' : 'success'"
              @click="handleToggleStatus(row)"
            >
              {{ row.status === 1 ? '禁用' : '启用' }}
            </el-button>
            <el-button v-permission="'system:dept:list'" link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="500px" :close-on-click-modal="false">
      <el-form ref="formRef" :model="deptForm" :rules="rules" label-width="80px">
        <el-form-item v-if="isChild" label="上级部门" prop="parentId">
          <el-tree-select
            v-model="deptForm.parentId"
            :data="treeData"
            :props="{ label: 'name', value: 'id', children: 'children' }"
            placeholder="请选择上级部门"
            clearable
            check-strictly
            :render-after-expand="false"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="部门名称" prop="name">
          <el-input v-model="deptForm.name" placeholder="请输入部门名称" />
        </el-form-item>
        <el-form-item label="部门编码" prop="code">
          <el-input v-model="deptForm.code" placeholder="不填则自动生成" />
        </el-form-item>
        <el-form-item label="负责人" prop="leader">
          <el-input v-model="deptForm.leader" placeholder="请输入负责人" />
        </el-form-item>
        <el-form-item label="联系电话" prop="phone">
          <el-input v-model="deptForm.phone" placeholder="请输入联系电话" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="deptForm.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="排序" prop="sort">
          <el-input-number v-model="deptForm.sort" :min="0" :max="9999" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="deptForm.status">
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
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Plus } from '@element-plus/icons-vue'
import { useUserStore } from '../../stores/user'
import { deptApi } from '../../api/dept'

const userStore = useUserStore()

const loading = ref(false)
const deptList = ref([])
const currentPage = ref(1)
const pageSize = ref(100)
const total = ref(0)
const searchKeyword = ref('')

const dialogVisible = ref(false)
const dialogTitle = ref('')
const isEdit = ref(false)
const isChild = ref(false)
const submitting = ref(false)
const formRef = ref(null)

const deptForm = reactive({
  id: null,
  parentId: null,
  name: '',
  code: '',
  leader: '',
  phone: '',
  email: '',
  sort: 0,
  status: 1,
})

const rules = {
  name: [{ required: true, message: '请输入部门名称', trigger: 'blur' }],
}

const treeData = computed(() => {
  const buildTree = (list, parentId = null) => {
    return list
      .filter((item) => item.parentId === parentId)
      .map((item) => ({
        id: item.id,
        name: item.name,
        children: buildTree(list, item.id),
      }))
  }
  return buildTree(deptList.value)
})

const filterTree = (nodes, keyword) => {
  if (!keyword) return nodes
  const kw = keyword.toLowerCase()
  return nodes.reduce((acc, node) => {
    const nameMatch = node.name?.toLowerCase().includes(kw)
    const filteredChildren = node.children ? filterTree(node.children, kw) : []
    if (nameMatch || filteredChildren.length > 0) {
      acc.push({ ...node, children: filteredChildren.length > 0 ? filteredChildren : node.children })
    }
    return acc
  }, [])
}

const filteredDeptList = computed(() => {
  return filterTree(deptList.value, searchKeyword.value)
})

const fetchDepts = async () => {
  loading.value = true
  try {
    const res = await deptApi.tree()
    deptList.value = res || []
    total.value = deptList.value.length
  } catch (e) {
    ElMessage.error('获取部门列表失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  // 搜索由 filteredDeptList computed 实时处理，无需重新请求
}

function resetForm(overrides = {}) {
  Object.keys(deptForm).forEach((key) => delete deptForm[key])
  Object.assign(
    deptForm,
    {
      id: null,
      parentId: null,
      name: '',
      code: '',
      leader: '',
      phone: '',
      email: '',
      sort: 0,
      status: 1,
    },
    overrides,
  )
}

const handleAdd = () => {
  isEdit.value = false
  isChild.value = false
  dialogTitle.value = '新增部门'
  resetForm()
  dialogVisible.value = true
}

const handleAddChild = (row) => {
  isEdit.value = false
  isChild.value = true
  dialogTitle.value = '添加下级部门'
  resetForm({ parentId: row.id })
  dialogVisible.value = true
}

const handleEdit = (row) => {
  isEdit.value = true
  isChild.value = false
  dialogTitle.value = '编辑部门'
  resetForm({
    id: row.id,
    parentId: row.parentId === 0 ? null : row.parentId,
    name: row.name,
    code: row.code,
    leader: row.leaderName,
    phone: row.phone,
    email: row.email,
    sort: row.sortOrder ?? 0,
    status: row.status,
  })
  dialogVisible.value = true
}

const handleSubmit = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    const data = {
      parentId: deptForm.parentId || 0,
      name: deptForm.name,
      code: deptForm.code,
      leaderName: deptForm.leader,
      phone: deptForm.phone,
      email: deptForm.email,
      sortOrder: deptForm.sort,
      status: deptForm.status,
    }
    if (isEdit.value) {
      await deptApi.update(deptForm.id, data)
    } else {
      await deptApi.create(data)
    }
    ElMessage.success(isEdit.value ? '更新成功' : '添加成功')
    dialogVisible.value = false
    fetchDepts()
  } catch (e) {
    ElMessage.error(e?.message || (isEdit.value ? '更新失败' : '添加失败'))
  } finally {
    submitting.value = false
  }
}

const handleToggleStatus = async (row) => {
  try {
    await ElMessageBox.confirm(`确定要${row.status === 1 ? '禁用' : '启用'}部门 ${row.name} 吗？`, '提示')
    const newStatus = row.status === 1 ? 0 : 1
    await deptApi.update(row.id, { ...row, status: newStatus })
    ElMessage.success('操作成功')
    fetchDepts()
  } catch {
    // cancelled
  }
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(`确定要删除部门 ${row.name} 吗？此操作不可恢复。`, '警告', { type: 'warning' })
    await deptApi.delete(row.id)
    ElMessage.success('删除成功')
    fetchDepts()
  } catch {
    // cancelled
  }
}

onMounted(() => {
  fetchDepts()
})
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Outfit:wght@300;400;500;600;700&family=Sora:wght@300;400;500;600&display=swap');

.dept-management {
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

.dept-name {
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
