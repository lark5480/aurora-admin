<template>
  <div class="category-management">
    <div class="page-header">
      <h1 class="page-title">商品分类</h1>
      <p class="page-sub">管理系统商品分类结构</p>
    </div>

    <div class="glass-card">
      <div class="card-header">
        <span class="card-title">分类列表</span>
        <el-button type="primary" @click="handleAddRoot">
          <el-icon><Plus /></el-icon>
          新增根分类
        </el-button>
      </div>

      <el-tree
        ref="treeRef"
        :data="store.categoryTree"
        :props="{ label: 'name', children: 'children' }"
        node-key="id"
        :default-expand-all="true"
        :expand-on-click-node="false"
        class="category-tree"
      >
        <template #default="{ node, data }">
          <div class="tree-node">
            <span class="tree-node-label">
              <el-icon><FolderOpened /></el-icon>
              <span>{{ node.label }}</span>
            </span>
            <span class="tree-node-actions">
              <el-button link type="primary" size="small" @click.stop="handleAddChild(data)">
                <el-icon><Plus /></el-icon>
                添加子分类
              </el-button>
              <el-button link type="warning" size="small" @click.stop="handleEdit(data)">
                <el-icon><Edit /></el-icon>
                编辑
              </el-button>
              <el-button link type="danger" size="small" @click.stop="handleDelete(data)">
                <el-icon><Delete /></el-icon>
                删除
              </el-button>
            </span>
          </div>
        </template>
      </el-tree>
    </div>

    <!-- 新增/编辑分类对话框 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="480px" :close-on-click-modal="false">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="分类名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入分类名称" maxlength="50" show-word-limit />
        </el-form-item>
        <el-form-item label="排序号" prop="sortOrder">
          <el-input-number
            v-model="form.sortOrder"
            :min="0"
            :max="9999"
            controls-position="right"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item v-if="form.parentName" label="父级分类">
          <el-input :model-value="form.parentName" disabled />
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
import { Plus, Edit, Delete, FolderOpened } from '@element-plus/icons-vue'
import { useProductStore } from '../../stores/productStore'
import { categoryApi } from '../../api/product'

const store = useProductStore()
const treeRef = ref(null)

const dialogVisible = ref(false)
const dialogTitle = ref('')
const isEdit = ref(false)
const submitting = ref(false)
const editingId = ref(null)
const formRef = ref(null)

const form = reactive({
  name: '',
  parentId: null,
  parentName: '',
  sortOrder: 0,
})

const rules = {
  name: [{ required: true, message: '请输入分类名称', trigger: 'blur' }],
}

// 新增根分类
const handleAddRoot = () => {
  isEdit.value = false
  editingId.value = null
  dialogTitle.value = '新增根分类'
  form.name = ''
  form.parentId = null
  form.parentName = ''
  form.sortOrder = 0
  dialogVisible.value = true
}

// 添加子分类
const handleAddChild = (data) => {
  isEdit.value = false
  editingId.value = null
  dialogTitle.value = `添加子分类 - ${data.name}`
  form.name = ''
  form.parentId = data.id
  form.parentName = data.name
  form.sortOrder = 0
  dialogVisible.value = true
}

// 编辑分类
const handleEdit = (data) => {
  isEdit.value = true
  editingId.value = data.id
  dialogTitle.value = '编辑分类'
  form.name = data.name
  form.parentId = data.parentId || null
  form.parentName = ''
  form.sortOrder = data.sortOrder ?? 0
  dialogVisible.value = true
}

// 删除分类
const handleDelete = async (data) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除分类「${data.name}」吗？${data.children?.length ? '其子分类将一并删除，' : ''}此操作不可恢复。`,
      '警告',
      { type: 'warning' },
    )
    await categoryApi.delete(data.id)
    ElMessage.success('删除成功')
    store.fetchCategoryTree()
  } catch (e) {
    if (e !== 'cancel' && e !== 'close') {
      ElMessage.error(e?.message || '删除失败')
    }
  }
}

// 提交
const handleSubmit = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    if (isEdit.value) {
      await categoryApi.update(editingId.value, {
        name: form.name,
        parentId: form.parentId,
        sortOrder: form.sortOrder,
      })
      ElMessage.success('更新成功')
    } else {
      await categoryApi.create({
        name: form.name,
        parentId: form.parentId,
        sortOrder: form.sortOrder,
      })
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    store.fetchCategoryTree()
  } catch (e) {
    ElMessage.error(e?.message || (isEdit.value ? '更新失败' : '创建失败'))
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  store.fetchCategoryTree()
})
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Outfit:wght@300;400;500;600;700&family=Sora:wght@300;400;500;600&display=swap');

.category-management {
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

.card-title {
  font-family: 'Sora', sans-serif;
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
}

/* Tree node custom style */
.tree-node {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex: 1;
  padding-right: 8px;
}

.tree-node-label {
  display: flex;
  align-items: center;
  gap: 6px;
  color: var(--text-primary);
  font-size: 14px;
}

.tree-node-label .el-icon {
  color: var(--neon-cyan);
  font-size: 18px;
}

.tree-node-actions {
  display: none;
  gap: 4px;
}

.tree-node:hover .tree-node-actions {
  display: inline-flex;
}

/* Cyberpunk tree overrides */
:deep(.el-tree) {
  background: transparent;
  color: var(--text-secondary);
}

:deep(.el-tree-node__content) {
  height: 44px;
  border-radius: 8px;
  padding: 0 8px;
  transition: background var(--transition-fast);
}

:deep(.el-tree-node__content:hover) {
  background: rgba(255, 0, 255, 0.08);
}

:deep(.el-tree-node.is-current > .el-tree-node__content) {
  background: rgba(0, 255, 255, 0.08);
}

:deep(.el-input__wrapper) {
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

:deep(.el-input-number .el-input__wrapper) {
  background: var(--glass-bg);
  border: 1px solid var(--border-glow-magenta);
  box-shadow: none;
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
