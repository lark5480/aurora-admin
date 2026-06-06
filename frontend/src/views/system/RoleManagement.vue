<template>
  <div class="role-management">
    <div class="page-header">
      <h1 class="page-title">角色管理</h1>
      <p class="page-sub">管理系统角色和权限</p>
    </div>

    <div class="glass-card">
      <div class="card-header">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索角色名称或编码"
          :prefix-icon="Search"
          clearable
          class="search-input"
          @input="handleSearch"
        />
        <el-button v-permission="'system:role:list'" type="primary" @click="handleAdd">
          <el-icon><Plus /></el-icon>
          新增角色
        </el-button>
      </div>

      <el-table v-loading="loading" :data="roleList" stripe class="role-table">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="角色名称" min-width="120" />
        <el-table-column prop="code" label="角色编码" min-width="120" />
        <el-table-column prop="description" label="描述" min-width="200" />
        <el-table-column prop="sortOrder" label="排序" width="80" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button v-permission="'system:role:list'" link type="primary" @click="handleEdit(row)">编辑</el-button>
            <el-button v-permission="'system:role:list'" link type="warning" @click="handleAssignMenus(row)"
              >分配菜单</el-button
            >
            <el-button
              v-permission="'system:role:list'"
              link
              :type="row.status === 1 ? 'danger' : 'success'"
              @click="handleToggleStatus(row)"
            >
              {{ row.status === 1 ? '禁用' : '启用' }}
            </el-button>
            <el-button v-permission="'system:role:list'" link type="danger" @click="handleDelete(row)">删除</el-button>
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
      <el-form ref="formRef" :model="roleForm" :rules="rules" label-width="90px">
        <el-form-item label="角色名称" prop="name">
          <el-input v-model="roleForm.name" placeholder="请输入角色名称" />
        </el-form-item>
        <el-form-item label="角色编码" prop="code">
          <el-input v-model="roleForm.code" placeholder="请输入角色编码" :disabled="isEdit" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="roleForm.description" type="textarea" :rows="3" placeholder="请输入描述" />
        </el-form-item>
        <el-form-item label="排序" prop="sortOrder">
          <el-input-number v-model="roleForm.sortOrder" :min="0" :max="9999" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="roleForm.status">
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

    <!-- 分配菜单对话框 -->
    <el-dialog v-model="menuDialogVisible" title="分配菜单" width="600px" :close-on-click-modal="false">
      <el-tree
        ref="menuTreeRef"
        :data="menuTreeData"
        :props="{ label: 'name', children: 'children' }"
        node-key="id"
        show-checkbox
        default-expand-all
        style="max-height: 400px; overflow-y: auto"
      />
      <template #footer>
        <el-button @click="menuDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="menuSubmitting" @click="handleMenuSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Plus } from '@element-plus/icons-vue'
import { useUserStore } from '../../stores/user'
import { roleApi } from '../../api/role'
import { menuApi } from '../../api/menu'

const userStore = useUserStore()

const loading = ref(false)
const roleList = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const searchKeyword = ref('')

const dialogVisible = ref(false)
const dialogTitle = ref('')
const isEdit = ref(false)
const submitting = ref(false)
const formRef = ref(null)

const roleForm = reactive({
  id: null,
  name: '',
  code: '',
  description: '',
  sortOrder: 0,
  status: 1,
})

const rules = {
  name: [{ required: true, message: '请输入角色名称', trigger: 'blur' }],
  code: [{ required: true, message: '请输入角色编码', trigger: 'blur' }],
}

// 菜单分配
const menuDialogVisible = ref(false)
const menuSubmitting = ref(false)
const menuTreeRef = ref(null)
const menuTreeData = ref([])
const currentRoleId = ref(null)

const fetchRoles = async () => {
  loading.value = true
  try {
    const res = await roleApi.list({ keyword: searchKeyword.value, page: currentPage.value, size: pageSize.value })
    roleList.value = Array.isArray(res) ? res : res?.list || res?.records || []
    total.value = res?.total || (Array.isArray(res) ? res.length : 0)
  } catch (e) {
    ElMessage.error('获取角色列表失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  currentPage.value = 1
  fetchRoles()
}

const handleSizeChange = () => {
  currentPage.value = 1
  fetchRoles()
}

const handleCurrentChange = () => {
  fetchRoles()
}

const handleAdd = () => {
  isEdit.value = false
  dialogTitle.value = '新增角色'
  Object.assign(roleForm, { id: null, name: '', code: '', description: '', sortOrder: 0, status: 1 })
  dialogVisible.value = true
}

const handleEdit = (row) => {
  isEdit.value = true
  dialogTitle.value = '编辑角色'
  Object.assign(roleForm, { ...row })
  dialogVisible.value = true
}

const handleSubmit = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    if (isEdit.value) {
      await roleApi.update(roleForm.id, roleForm)
    } else {
      await roleApi.create(roleForm)
    }
    ElMessage.success(isEdit.value ? '更新成功' : '添加成功')
    dialogVisible.value = false
    fetchRoles()
  } catch (e) {
    ElMessage.error(isEdit.value ? '更新失败' : '添加失败')
  } finally {
    submitting.value = false
  }
}

const handleToggleStatus = async (row) => {
  try {
    await ElMessageBox.confirm(`确定要${row.status === 1 ? '禁用' : '启用'}角色 ${row.name} 吗？`, '提示')
    const newStatus = row.status === 1 ? 0 : 1
    await roleApi.update(row.id, { ...row, status: newStatus })
    ElMessage.success('操作成功')
    fetchRoles()
  } catch {
    // cancelled
  }
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(`确定要删除角色 ${row.name} 吗？此操作不可恢复。`, '警告', { type: 'warning' })
    await roleApi.delete(row.id)
    ElMessage.success('删除成功')
    fetchRoles()
  } catch {
    // cancelled
  }
}

// 分配菜单
const handleAssignMenus = async (row) => {
  currentRoleId.value = row.id
  menuDialogVisible.value = true
  try {
    const [menusRes, roleMenusRes] = await Promise.all([menuApi.tree(), roleApi.getMenus(row.id)])
    menuTreeData.value = menusRes || []
    const checkedIds = Array.isArray(roleMenusRes) ? roleMenusRes.filter((id) => typeof id === 'number') : []
    await nextTick()
    menuTreeRef.value?.setCheckedKeys(checkedIds, false)
  } catch (e) {
    ElMessage.error('加载菜单数据失败')
  }
}

const handleMenuSubmit = async () => {
  menuSubmitting.value = true
  try {
    const checkedKeys = menuTreeRef.value?.getCheckedKeys() || []
    const halfCheckedKeys = menuTreeRef.value?.getHalfCheckedKeys() || []
    const allKeys = [...checkedKeys, ...halfCheckedKeys]
    await roleApi.assignMenus(currentRoleId.value, { menuIds: allKeys })
    ElMessage.success('菜单分配成功')
    menuDialogVisible.value = false
  } catch (e) {
    ElMessage.error('菜单分配失败')
  } finally {
    menuSubmitting.value = false
  }
}

onMounted(() => {
  fetchRoles()
})
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Outfit:wght@300;400;500;600;700&family=Sora:wght@300;400;500;600&display=swap');

.role-management {
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

:deep(.el-radio__label) {
  color: var(--text-secondary);
}

:deep(.el-button) {
  cursor: pointer;
}

:deep(.el-button link) {
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
