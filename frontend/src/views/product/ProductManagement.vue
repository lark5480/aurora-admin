<template>
  <div class="product-management">
    <div class="page-header">
      <h1 class="page-title">{{ isAdmin ? '商品管理' : '商品浏览' }}</h1>
      <p class="page-sub">{{ isAdmin ? '管理系统商品信息与库存' : '浏览商品并加入购物车' }}</p>
    </div>

    <div class="glass-card">
      <div class="card-header">
        <div class="filter-row">
          <el-input
            v-model="searchKeyword"
            placeholder="搜索商品名称或描述"
            :prefix-icon="Search"
            clearable
            class="search-input"
            @input="handleSearch"
          />
          <el-tree-select
            v-model="searchCategoryId"
            :data="productStore.categoryTree"
            :props="{ label: 'name', value: 'id', children: 'children' }"
            placeholder="选择分类"
            clearable
            check-strictly
            :render-after-expand="false"
            class="category-select"
            @change="handleSearch"
          />
          <el-select
            v-model="searchStatus"
            placeholder="状态筛选"
            clearable
            class="status-select"
            @change="handleSearch"
          >
            <el-option label="上架" value="ON_SALE" />
            <el-option label="下架" value="OFF_SHELF" />
          </el-select>
        </div>
        <el-button v-if="isAdmin" v-permission="'system:product:list'" type="primary" @click="handleAdd">
          <el-icon><Plus /></el-icon>
          新增商品
        </el-button>
      </div>
      <div v-if="selectedIds.length > 0" class="batch-bar">
        <span class="batch-hint">已选 {{ selectedIds.length }} 项</span>
        <el-button
          size="small"
          type="success"
          :loading="batchSubmitting"
          :disabled="batchSubmitting"
          @click="handleBatchStatus('ON_SALE')"
          >批量上架</el-button
        >
        <el-button
          size="small"
          type="warning"
          :loading="batchSubmitting"
          :disabled="batchSubmitting"
          @click="handleBatchStatus('OFF_SHELF')"
          >批量下架</el-button
        >
      </div>

      <el-table
        v-loading="productStore.loading"
        :data="productStore.productList"
        stripe
        class="product-table"
        @selection-change="handleSelectionChange"
      >
        <el-table-column v-if="isAdmin" type="selection" width="55" />
        <el-table-column label="封面" width="70">
          <template #default="{ row }">
            <el-image
              :src="resolveImageUrl(row.coverImage)"
              style="width: 40px; height: 40px; border-radius: 4px"
              fit="cover"
              class="cover-image"
            >
              <template #error>
                <div class="image-placeholder">
                  <el-icon :size="18"><Picture /></el-icon>
                </div>
              </template>
            </el-image>
          </template>
        </el-table-column>
        <el-table-column prop="name" label="商品名称" min-width="180" show-overflow-tooltip />
        <el-table-column prop="categoryName" label="分类" width="120" />
        <el-table-column prop="price" label="价格" width="100">
          <template #default="{ row }">
            <span class="price-text">¥{{ row.price }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="stock" label="库存" width="80" />
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ON_SALE' ? 'success' : 'info'" size="small">
              {{ row.status === 'ON_SALE' ? '上架' : '下架' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="500" fixed="right">
          <template #default="{ row }">
            <el-button link type="info" size="small" @click="handleDetail(row)">
              <el-icon><View /></el-icon>
              详情
            </el-button>
            <el-button v-if="row.status === 'ON_SALE'" link type="success" @click="handleAddToCart(row)">
              <el-icon><ShoppingCart /></el-icon>
              加购
            </el-button>
            <el-tooltip v-if="isAdmin" :content="row.status === 'ON_SALE' ? '下架后才能编辑' : ''" placement="top">
              <el-button link type="primary" :disabled="row.status === 'ON_SALE'" @click="handleEdit(row)"
                >编辑</el-button
              >
            </el-tooltip>
            <el-button
              v-if="isAdmin"
              link
              :type="row.status === 'ON_SALE' ? 'warning' : 'success'"
              @click="handleToggleStatus(row)"
            >
              {{ row.status === 'ON_SALE' ? '下架' : '上架' }}
            </el-button>
            <el-tooltip v-if="isAdmin" :content="row.status === 'ON_SALE' ? '下架后才能删除' : ''" placement="top">
              <el-button link type="danger" :disabled="row.status === 'ON_SALE'" @click="handleDelete(row)"
                >删除</el-button
              >
            </el-tooltip>
            <el-button v-if="isAdmin" link type="primary" @click="handleCopy(row)">复制</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="productStore.total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </div>

    <!-- 新增/编辑商品对话框-->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="720px" :close-on-click-modal="false">
      <el-scrollbar max-height="58vh">
        <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="商品名称" prop="name">
                <el-input v-model="form.name" placeholder="请输入商品名称" maxlength="200" show-word-limit />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="所属分类" prop="categoryId">
                <el-tree-select
                  v-model="form.categoryId"
                  :data="productStore.categoryTree"
                  :props="{ label: 'name', value: 'id', children: 'children' }"
                  placeholder="请选择分类"
                  clearable
                  check-strictly
                  :render-after-expand="false"
                />
              </el-form-item>
            </el-col>
          </el-row>

          <el-form-item label="商品描述" prop="description">
            <el-input
              v-model="form.description"
              type="textarea"
              placeholder="请输入商品描述"
              :rows="3"
              maxlength="2000"
              show-word-limit
            />
          </el-form-item>

          <el-form-item label="封面图片" prop="coverImage">
            <el-upload
              :show-file-list="false"
              :before-upload="handleBeforeUpload"
              :http-request="handleUploadRequest"
              accept="image/jpeg,image/png,image/webp"
              class="cover-uploader"
            >
              <img v-if="form.coverImage" :src="resolveImageUrl(form.coverImage)" class="cover-preview" />
              <div v-else class="cover-upload-placeholder">
                <el-icon :size="28"><Plus /></el-icon>
                <span>上传封面</span>
              </div>
            </el-upload>
          </el-form-item>

          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="价格" prop="price">
                <el-input-number
                  v-model="form.price"
                  :min="0.01"
                  :max="99999999"
                  :precision="2"
                  controls-position="right"
                  style="width: 100%"
                />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="库存" prop="stock">
                <el-input-number
                  v-model="form.stock"
                  :disabled="form.skus.length > 0"
                  :min="0"
                  :max="999999"
                  controls-position="right"
                  style="width: 100%"
                />
                <span v-if="form.skus.length > 0" class="stock-hint">
                  有 SKU 时库存自动等于各规格库存之和（当前总计：{{ totalSkuStock }}）
                </span>
              </el-form-item>
            </el-col>
          </el-row>

          <!-- SKU 管理 -->
          <el-form-item label="SKU 管理">
            <div class="sku-section">
              <div class="sku-header">
                <span class="sku-tip">添加不同规格（颜色、尺寸等）的商品变体</span>
                <el-button type="primary" size="small" @click="addSkuRow">
                  <el-icon><Plus /></el-icon>
                  添加规格
                </el-button>
              </div>
              <el-table v-if="form.skus.length > 0" :data="form.skus" size="small" class="sku-table" stripe>
                <el-table-column label="规格名称" min-width="160">
                  <template #default="{ row, $index }">
                    <el-input v-model="row.specName" placeholder="如 红色/L" size="small" />
                  </template>
                </el-table-column>
                <el-table-column label="价格" width="160">
                  <template #default="{ row }">
                    <el-input-number
                      v-model="row.price"
                      :min="0.01"
                      :max="99999999"
                      :precision="2"
                      size="small"
                      controls-position="right"
                      style="width: 100%"
                    />
                  </template>
                </el-table-column>
                <el-table-column label="库存" width="140">
                  <template #default="{ row }">
                    <el-input-number
                      v-model="row.stock"
                      :min="0"
                      :max="999999"
                      size="small"
                      controls-position="right"
                      style="width: 100%"
                    />
                  </template>
                </el-table-column>
                <el-table-column label="操作" width="70">
                  <template #default="{ $index }">
                    <el-button link type="danger" size="small" @click="removeSkuRow($index)">删除</el-button>
                  </template>
                </el-table-column>
              </el-table>
              <el-empty v-else description="暂无 SKU，点击上方按钮添加" :image-size="60" />
            </div>
          </el-form-item>
        </el-form>
      </el-scrollbar>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 商品详情对话框 -->
    <el-dialog v-model="detailVisible" title="商品详情" width="680px" :close-on-click-modal="false">
      <div v-if="currentProductDetail" class="product-detail">
        <!-- 商品信息 -->
        <div class="detail-section">
          <h4 class="section-title">基本信息</h4>
          <div class="info-grid">
            <div class="info-item">
              <span class="info-label">商品名称</span>
              <span class="info-value">{{ currentProductDetail.name }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">所属分类</span>
              <span class="info-value">{{ currentProductDetail.categoryName || '-' }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">价格</span>
              <span class="price-text">¥{{ currentProductDetail.price }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">库存</span>
              <span class="info-value">{{ currentProductDetail.stock }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">状态</span>
              <el-tag :type="currentProductDetail.status === 'ON_SALE' ? 'success' : 'info'" size="small">
                {{ currentProductDetail.status === 'ON_SALE' ? '上架' : '下架' }}
              </el-tag>
            </div>
            <div class="info-item">
              <span class="info-label">创建时间</span>
              <span class="info-value">{{ currentProductDetail.createTime }}</span>
            </div>
          </div>
        </div>

        <!-- 商品描述 -->
        <div v-if="currentProductDetail.description" class="detail-section">
          <h4 class="section-title">商品描述</h4>
          <div class="description-content">
            {{ currentProductDetail.description }}
          </div>
        </div>

        <!-- 封面图片 -->
        <div v-if="currentProductDetail.coverImage" class="detail-section">
          <h4 class="section-title">封面图片</h4>
          <div class="cover-container">
            <el-image :src="resolveImageUrl(currentProductDetail.coverImage)" fit="contain" class="cover-preview">
              <template #error>
                <div class="image-placeholder">
                  <el-icon :size="48"><Picture /></el-icon>
                </div>
              </template>
            </el-image>
          </div>
        </div>

        <!-- SKU列表 -->
        <div v-if="currentProductDetail.skus && currentProductDetail.skus.length > 0" class="detail-section">
          <h4 class="section-title">SKU 规格</h4>
          <el-table :data="currentProductDetail.skus" size="small" class="sku-table" stripe>
            <el-table-column prop="specName" label="规格名称" min-width="160" />
            <el-table-column prop="price" label="价格" width="120">
              <template #default="{ row }">
                <span class="price-text">¥{{ row.price }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="stock" label="库存" width="100" />
          </el-table>
        </div>
      </div>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Plus, Picture, ShoppingCart, View } from '@element-plus/icons-vue'
import { useProductStore } from '../../stores/productStore'
import { useUserStore } from '../../stores/user'
import { productApi } from '../../api/product'
import { cartApi } from '../../api/order'
import { resolveImageUrl } from '../../utils/image'

const productStore = useProductStore()
const userStore = useUserStore()

const isAdmin = computed(() => userStore.hasRole('SUPER_ADMIN') || userStore.hasRole('ADMIN'))

const loading = ref(false)
const batchSubmitting = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)

const searchKeyword = ref('')
const searchCategoryId = ref(null)
const searchStatus = ref('')

const dialogVisible = ref(false)
const dialogTitle = ref('')
const isEdit = ref(false)
const submitting = ref(false)
const editingId = ref(null)
const formRef = ref(null)
const selectedIds = ref<number[]>([])
const detailVisible = ref(false)
const currentProductDetail = ref(null)

const totalSkuStock = computed(() => {
  return form.skus.reduce((sum, s) => sum + (s.stock || 0), 0)
})

const form = reactive({
  name: '',
  categoryId: null,
  description: '',
  coverImage: '',
  price: 0,
  stock: 0,
  skus: [],
})

const rules = {
  name: [{ required: true, message: '请输入商品名称', trigger: 'blur' }],
  categoryId: [{ required: true, message: '请选择分类', trigger: 'change' }],
  price: [
    { required: true, message: '请输入价格', trigger: 'blur' },
    { type: 'number', min: 0.01, message: '价格必须大于0', trigger: 'blur' },
  ],
  stock: [
    { required: true, message: '请输入库存', trigger: 'blur' },
    { type: 'number', min: 0, message: '库存不能小于0', trigger: 'blur' },
  ],
}

const fetchProducts = () => {
  productStore.fetchProducts({
    keyword: searchKeyword.value,
    categoryId: searchCategoryId.value || undefined,
    status: searchStatus.value || undefined,
    page: currentPage.value,
    size: pageSize.value,
  })
}

const handleSelectionChange = (selection: any[]) => {
  selectedIds.value = selection.map((item) => item.id)
}

const handleBatchStatus = async (status: string) => {
  if (selectedIds.value.length === 0) {
    ElMessage.warning('请先选择商品')
    return
  }
  if (batchSubmitting.value) return
  const label = status === 'ON_SALE' ? '上架' : '下架'
  batchSubmitting.value = true
  try {
    await ElMessageBox.confirm(`确定要批量「${label}」选中的 ${selectedIds.value.length} 个商品吗？`, '提示')
    await productApi.batchUpdateStatus(selectedIds.value, status)
    ElMessage.success('批量操作成功')
    selectedIds.value = []
    fetchProducts()
  } catch (e) {
    if (e !== 'cancel' && e !== 'close') {
      ElMessage.error(e?.message || '批量操作失败')
    }
  } finally {
    batchSubmitting.value = false
  }
}

const handleSearch = () => {
  currentPage.value = 1
  fetchProducts()
}

const handleSizeChange = (newSize) => {
  pageSize.value = newSize
  currentPage.value = 1
  fetchProducts()
}

const handleCurrentChange = () => {
  fetchProducts()
}

const handleAdd = () => {
  isEdit.value = false
  editingId.value = null
  dialogTitle.value = '新增商品'
  form.name = ''
  form.categoryId = null
  form.description = ''
  form.coverImage = ''
  form.price = 0
  form.stock = 0
  form.skus = []
  dialogVisible.value = true
}

const handleEdit = async (row) => {
  isEdit.value = true
  editingId.value = row.id
  dialogTitle.value = '编辑商品'
  form.name = row.name
  form.categoryId = row.categoryId
  form.description = row.description || ''
  form.coverImage = row.coverImage || ''
  form.price = row.price ?? 0
  form.stock = row.stock ?? 0

  // 尝试加载 SKU
  try {
    const detail = await productApi.getById(row.id)
    form.skus = detail?.skus
      ? detail.skus.map((s) => ({
          id: s.id,
          specName: s.specName || '',
          price: s.price ?? 0,
          stock: s.stock ?? 0,
        }))
      : []
  } catch {
    form.skus = []
  }

  dialogVisible.value = true
}

const handleDetail = async (row) => {
  try {
    const detail = await productApi.getById(row.id)
    currentProductDetail.value = detail
    detailVisible.value = true
  } catch (e) {
    ElMessage.error(e?.message || '获取商品详情失败')
  }
}

const handleSubmit = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    const data = {
      name: form.name,
      categoryId: form.categoryId,
      description: form.description,
      coverImage: form.coverImage,
      price: form.price,
      stock: form.stock,
      skus: form.skus.filter((s) => s.specName.trim()),
    }

    if (isEdit.value) {
      await productApi.update(editingId.value, data)
      ElMessage.success('更新成功')
    } else {
      await productApi.create(data)
      ElMessage.success('添加成功')
    }
    dialogVisible.value = false
    fetchProducts()
  } catch (e) {
    ElMessage.error(e?.message || (isEdit.value ? '更新失败' : '添加失败'))
  } finally {
    submitting.value = false
  }
}

const handleToggleStatus = async (row) => {
  const newStatus = row.status === 'ON_SALE' ? 'OFF_SHELF' : 'ON_SALE'
  const label = newStatus === 'ON_SALE' ? '上架' : '下架'
  try {
    await ElMessageBox.confirm(`确定要「${label}」商品「${row.name}」吗？`, '提示')
    await productApi.updateStatus(row.id, newStatus)
    ElMessage.success(`${label}成功`)
    fetchProducts()
  } catch (e) {
    if (e !== 'cancel' && e !== 'close') {
      ElMessage.error(e?.message || `${label}失败`)
    }
  }
}

const handleAddToCart = async (row) => {
  if (!row.stock || row.stock <= 0) {
    ElMessage.warning(`「${row.name}」库存不足，无法加购`)
    return
  }
  try {
    await cartApi.add({ productId: row.id, quantity: 1 })
    ElMessage.success(`已添加「${row.name}」到购物车`)
  } catch (e) {
    ElMessage.error(e?.message || '添加购物车失败')
  }
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(`确定要删除商品「${row.name}」吗？此操作不可恢复。`, '警告', { type: 'warning' })
    await productApi.delete(row.id)
    ElMessage.success('删除成功')
    fetchProducts()
  } catch (e) {
    if (e !== 'cancel' && e !== 'close') {
      ElMessage.error(e?.message || '删除失败')
    }
  }
}

const handleCopy = async (row) => {
  isEdit.value = false
  editingId.value = null
  dialogTitle.value = '复制商品'
  form.name = row.name + '（复制）'
  form.categoryId = row.categoryId
  form.description = row.description || ''
  form.coverImage = row.coverImage || ''
  form.price = row.price ?? 0
  form.stock = row.stock ?? 0

  // 尝试加载 SKU（不传 id，后端会生成新记录）
  try {
    const detail = await productApi.getById(row.id)
    form.skus = detail?.skus
      ? detail.skus.map((s) => ({
          specName: s.specName || '',
          price: s.price ?? 0,
          stock: s.stock ?? 0,
        }))
      : []
  } catch {
    form.skus = []
  }

  dialogVisible.value = true
}

// SKU 管理
const addSkuRow = () => {
  form.skus.push({
    specName: '',
    price: form.price || 0,
    stock: form.stock || 0,
  })
}

const removeSkuRow = (index) => {
  form.skus.splice(index, 1)
}

// 图片上传
const handleBeforeUpload = (file) => {
  const isImage = file.type.startsWith('image/')
  const isLt5M = file.size / 1024 / 1024 < 5
  if (!isImage) {
    ElMessage.error('只能上传图片文件')
    return false
  }
  if (!isLt5M) {
    ElMessage.error('图片大小不能超过 5MB')
    return false
  }
  return true
}

const handleUploadRequest = async (options) => {
  const formData = new FormData()
  formData.append('file', options.file)
  try {
    const res = await fetch('/api/files/upload', {
      method: 'POST',
      headers: {
        Authorization: `Bearer ${sessionStorage.getItem('token')}`,
      },
      body: formData,
    })
    const json = await res.json()
    if (json.code === 200) {
      form.coverImage = '/uploads/' + json.data.filePath
    } else {
      ElMessage.error(json.message || '上传失败')
    }
  } catch {
    ElMessage.error('上传失败')
  }
}

onMounted(() => {
  productStore.fetchCategoryTree()
  fetchProducts()
})
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Outfit:wght@300;400;500;600;700&family=Sora:wght@300;400;500;600&display=swap');

.product-management {
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
  flex-wrap: wrap;
  gap: 12px;
}

.filter-row {
  display: flex;
  gap: 12px;
  align-items: center;
  flex-wrap: wrap;
}

.search-input {
  width: 220px;
}

.category-select {
  width: 200px;
}

.status-select {
  width: 130px;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
}

/* Price column */
.price-text {
  color: var(--neon-green);
  font-weight: 600;
  font-family: 'Sora', sans-serif;
}

/* Cover image */
.cover-image {
  border: 1px solid var(--border-subtle);
}

.image-placeholder {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--glass-bg);
  border-radius: 4px;
  color: var(--text-muted);
}

/* Upload */
.cover-uploader {
  display: flex;
}

.cover-preview {
  width: 120px;
  height: 120px;
  object-fit: cover;
  border-radius: 8px;
  border: 1px solid var(--border-glow-magenta);
}

.cover-upload-placeholder {
  width: 120px;
  height: 120px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  border: 2px dashed var(--border-glow-magenta);
  border-radius: 8px;
  cursor: pointer;
  color: var(--text-muted);
  font-size: 13px;
  transition: all var(--transition-normal);
  background: var(--glass-bg);
}

.cover-upload-placeholder:hover {
  border-color: var(--neon-cyan);
  color: var(--neon-cyan);
  box-shadow: var(--glow-cyan);
}

/* SKU section */
.sku-section {
  width: 100%;
}

.sku-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.sku-tip {
  font-size: 13px;
  color: var(--text-muted);
}

/* Table cyberpunk overrides */
:deep(.el-table) {
  --el-table-bg-color: transparent;
  --el-table-tr-bg-color: transparent;
  --el-table-header-bg-color: rgba(255, 255, 255, 0.03);
  --el-table-row-hover-bg-color: rgba(255, 0, 255, 0.06);
  --el-table-text-color: var(--text-secondary);
  --el-table-header-text-color: var(--text-secondary);
}

:deep(.el-table th.el-table__cell) {
  background: rgba(255, 255, 255, 0.03);
  font-weight: 600;
}

/* Input/Select dark theme */
:deep(.el-input__wrapper),
:deep(.el-select .el-input__wrapper),
:deep(.el-tree-select .el-input__wrapper) {
  background: var(--glass-bg);
  border: 1px solid var(--border-glow-magenta);
  box-shadow: none;
}

:deep(.el-input__inner) {
  color: var(--text-secondary);
}

:deep(.el-input-number .el-input__wrapper) {
  background: var(--glass-bg);
  border: 1px solid var(--border-glow-magenta);
  box-shadow: none;
}

/* Dialog dark theme */
:deep(.el-dialog) {
  background: var(--bg-darker);
  backdrop-filter: blur(20px);
  border: 1px solid var(--border-glow-magenta);
  border-radius: 16px;
}

:deep(.el-dialog__body) {
  flex: 1;
  overflow-y: auto;
  padding-top: 16px;
  padding-bottom: 16px;
}

:deep(.el-dialog__footer) {
  flex-shrink: 0;
}

:deep(.el-dialog__title) {
  color: var(--text-primary);
  font-family: 'Sora', sans-serif;
}

:deep(.el-form-item__label) {
  color: var(--text-secondary);
}

:deep(.el-textarea__inner) {
  background: var(--glass-bg);
  border: 1px solid var(--border-glow-magenta);
  box-shadow: none;
  color: var(--text-secondary);
}

/* SKU table dark theme */
:deep(.sku-table) {
  border: 1px solid var(--border-subtle);
}

:deep(.sku-table .el-input__wrapper) {
  background: rgba(0, 0, 0, 0.4);
}

/* Empty state */
:deep(.el-empty__description p) {
  color: var(--text-muted);
}

/* Batch action bar */
.batch-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 16px;
  margin-bottom: 12px;
  background: rgba(255, 0, 255, 0.06);
  border: 1px solid var(--border-glow-magenta);
  border-radius: 8px;
  flex-wrap: wrap;
}

.batch-hint {
  font-size: 13px;
  color: var(--neon-cyan);
  font-weight: 500;
}

/* Product detail dialog */
.product-detail {
  padding: 0 8px;
}

.detail-section {
  margin-bottom: 24px;
}

.section-title {
  font-family: 'Sora', sans-serif;
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 12px;
  padding-bottom: 8px;
  border-bottom: 1px solid var(--border-subtle);
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.info-item-full {
  grid-column: 1 / -1;
}

.info-label {
  font-size: 12px;
  color: var(--text-muted);
  font-weight: 500;
}

.info-value {
  font-size: 14px;
  color: var(--text-secondary);
}

.description-content {
  padding: 12px;
  background: var(--glass-bg);
  border-radius: 8px;
  border: 1px solid var(--border-subtle);
  color: var(--text-secondary);
  line-height: 1.6;
  white-space: pre-wrap;
}

.cover-container {
  display: flex;
  justify-content: center;
  padding: 12px;
  background: var(--glass-bg);
  border-radius: 8px;
  border: 1px solid var(--border-subtle);
}

.cover-review {
  max-width: 100%;
  max-height: 300px;
  border-radius: 8px;
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
