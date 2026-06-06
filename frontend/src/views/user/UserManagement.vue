<template>
  <div class="user-management">
    <div class="page-header">
      <h1 class="page-title">用户管理</h1>
      <p class="page-sub">管理系统用户账号</p>
    </div>

    <div class="glass-card">
      <div class="card-header">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索用户名或邮箱"
          :prefix-icon="Search"
          clearable
          class="search-input"
          @input="handleSearch"
        />
        <el-button v-permission="'system:user:list'" type="primary" @click="handleAdd">
          <el-icon><Plus /></el-icon>
          新增用户
        </el-button>
      </div>

      <el-table v-loading="loading" :data="users" stripe class="user-table">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="username" label="用户名" min-width="120" />
        <el-table-column prop="email" label="邮箱" min-width="180" />
        <el-table-column prop="nickname" label="昵称" min-width="120" />
        <el-table-column prop="deptName" label="所属部门" min-width="120" />
        <el-table-column prop="role" label="角色" width="100">
          <template #default="{ row }">
            <el-tag :type="row.role === 'admin' ? 'danger' : 'info'">
              {{ row.role === 'admin' ? '管理员' : '普通用户' }}
            </el-tag>
          </template>
        </el-table-column>
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
            <el-button v-permission="'system:user:list'" link type="primary" @click="handleEdit(row)">编辑</el-button>
            <el-button v-permission="'system:user:list'" link type="success" @click="handleAssignRoles(row)"
              >分配角色</el-button
            >
            <el-button
              v-permission="'system:user:list'"
              link
              :type="row.status === 1 ? 'danger' : 'success'"
              @click="handleToggleStatus(row)"
            >
              {{ row.status === 1 ? '禁用' : '启用' }}
            </el-button>
            <el-button v-permission="'system:user:list'" link type="danger" @click="handleDelete(row)">删除</el-button>
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

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="500px" :close-on-click-modal="false">
      <el-form ref="formRef" :model="userForm" :rules="rules" label-width="80px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="userForm.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="userForm.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="userForm.nickname" placeholder="请输入昵称" />
        </el-form-item>
        <el-form-item label="角色" prop="role">
          <el-select v-model="userForm.role" placeholder="请选择角色" style="width: 100%">
            <el-option label="普通用户" value="user" />
            <el-option label="管理员" value="admin" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="isEdit" label="所属部门" prop="deptId">
          <el-tree-select
            v-model="userForm.deptId"
            :data="deptTreeData"
            :props="{ label: 'name', value: 'id', children: 'children' }"
            placeholder="请选择所属部门"
            clearable
            check-strictly
            :render-after-expand="false"
            style="width: 100%"
            @change="handleDeptChange"
          />
        </el-form-item>
        <el-form-item v-if="!isEdit" label="密码" prop="password">
          <el-input v-model="userForm.password" type="password" placeholder="请输入密码" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 分配角色对话框 -->
    <el-dialog v-model="roleDialogVisible" title="分配角色" width="500px" :close-on-click-modal="false">
      <el-checkbox-group v-model="selectedRoleIds">
        <el-checkbox
          v-for="role in allRoles"
          :key="role.id"
          :value="role.id"
          style="display: block; margin-bottom: 10px"
        >
          {{ role.name }} ({{ role.code }})
        </el-checkbox>
      </el-checkbox-group>
      <template #footer>
        <el-button @click="roleDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="roleSubmitting" @click="handleRoleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Plus } from '@element-plus/icons-vue'
import { useUserStore } from '../../stores/user'
import { userApi } from '../../api/user'
import { roleApi } from '../../api/role'
import { deptApi } from '../../api/dept'

const userStore = useUserStore()

const loading = ref(false)
const users = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const searchKeyword = ref('')

const dialogVisible = ref(false)
const dialogTitle = ref('')
const isEdit = ref(false)
const submitting = ref(false)
const formRef = ref(null)

const roleDialogVisible = ref(false)
const roleSubmitting = ref(false)
const allRoles = ref([])
const selectedRoleIds = ref([])
const currentUserId = ref(null)
const deptTreeData = ref([])

const userForm = reactive({
  id: null,
  username: '',
  email: '',
  nickname: '',
  role: 'user',
  password: '',
  deptId: null,
  deptName: '',
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少6位', trigger: 'blur' },
  ],
}

const fetchUsers = async () => {
  loading.value = true
  try {
    const res = await userApi.list({ keyword: searchKeyword.value, page: currentPage.value, size: pageSize.value })
    users.value = res?.list || res?.records || []
    total.value = res?.total || 0
  } catch (e) {
    ElMessage.error('获取用户列表失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  currentPage.value = 1
  fetchUsers()
}

const handleSizeChange = () => {
  currentPage.value = 1
  fetchUsers()
}

const handleCurrentChange = () => {
  fetchUsers()
}

const handleAdd = () => {
  isEdit.value = false
  dialogTitle.value = '新增用户'
  Object.keys(userForm).forEach((key) => delete userForm[key])
  Object.assign(userForm, {
    id: null,
    username: '',
    email: '',
    nickname: '',
    role: 'user',
    password: '',
    deptId: null,
    deptName: '',
  })
  dialogVisible.value = true
}

const handleEdit = (row) => {
  isEdit.value = true
  dialogTitle.value = '编辑用户'
  Object.keys(userForm).forEach((key) => delete userForm[key])
  Object.assign(userForm, {
    id: row.id,
    username: row.username,
    email: row.email,
    nickname: row.nickname,
    role: row.role,
    status: row.status,
    password: '',
    deptId: row.deptId,
    deptName: row.deptName,
  })
  dialogVisible.value = true
}

const handleSubmit = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    if (isEdit.value) {
      await userApi.update(userForm.id, userForm)
    } else {
      await userApi.create(userForm)
    }
    ElMessage.success(isEdit.value ? '更新成功' : '添加成功')
    dialogVisible.value = false
    fetchUsers()
  } catch (e) {
    ElMessage.error(e?.message || (isEdit.value ? '更新失败' : '添加失败'))
  } finally {
    submitting.value = false
  }
}

const handleToggleStatus = async (row) => {
  try {
    await ElMessageBox.confirm(`确定要${row.status === 1 ? '禁用' : '启用'}用户 ${row.username} 吗？`, '提示')
    const newStatus = row.status === 1 ? 0 : 1
    await userApi.updateStatus(row.id, newStatus)
    ElMessage.success('操作成功')
    fetchUsers()
  } catch {
    // cancelled
  }
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(`确定要删除用户 ${row.username} 吗？此操作不可恢复。`, '警告', { type: 'warning' })
    await userApi.delete(row.id)
    ElMessage.success('删除成功')
    fetchUsers()
  } catch {
    // cancelled
  }
}

const loadAllRoles = async () => {
  try {
    const res = await roleApi.list({ size: 999 })
    allRoles.value = Array.isArray(res) ? res : res?.list || []
  } catch (e) {
    console.error('加载角色列表失败', e)
    ElMessage.warning('角色列表加载失败，分配角色功能可能不可用')
  }
}

const handleAssignRoles = async (row) => {
  currentUserId.value = row.id
  selectedRoleIds.value = []
  try {
    const res = await userApi.getRoles(row.id)
    selectedRoleIds.value = Array.isArray(res) ? res.map((r) => r.id) : (res?.data || []).map((r) => r.id)
    roleDialogVisible.value = true
  } catch (e) {
    ElMessage.error('加载用户角色失败')
  }
}

const handleRoleSubmit = async () => {
  roleSubmitting.value = true
  try {
    await userApi.assignRoles(currentUserId.value, selectedRoleIds.value)
    ElMessage.success('角色分配成功')
    roleDialogVisible.value = false
  } catch (e) {
    ElMessage.error('角色分配失败')
  } finally {
    roleSubmitting.value = false
  }
}

const findDeptInTree = (nodes, id) => {
  for (const node of nodes) {
    if (node.id === id) return node
    if (node.children) {
      const found = findDeptInTree(node.children, id)
      if (found) return found
    }
  }
  return null
}

const handleDeptChange = (val) => {
  if (val) {
    const dept = findDeptInTree(deptTreeData.value, val)
    userForm.deptName = dept?.name || ''
  } else {
    userForm.deptName = ''
  }
}

const loadDeptTree = async () => {
  try {
    deptTreeData.value = await deptApi.tree()
  } catch (e) {
    console.warn('加载部门树失败', e)
  }
}

onMounted(() => {
  fetchUsers()
  loadAllRoles()
  loadDeptTree()
})
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Outfit:wght@300;400;500;600;700&family=Sora:wght@300;400;500;600&display=swap');

.user-management {
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

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
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
