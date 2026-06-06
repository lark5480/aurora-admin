<template>
  <div class="address-management">
    <div class="page-header">
      <div class="header-left">
        <h1 class="page-title">收货地址</h1>
        <p class="page-sub">管理你的收货地址</p>
      </div>
      <el-button type="primary" @click="handleAdd">
        <el-icon><Plus /></el-icon>
        添加地址
      </el-button>
    </div>

    <div v-loading="addressStore.loading" class="address-grid">
      <!-- Empty state -->
      <div v-if="!addressStore.loading && addressStore.addresses.length === 0" class="glass-card empty-state">
        <el-icon :size="56" class="empty-icon"><Location /></el-icon>
        <h3>暂无收货地址</h3>
        <p>添加一个地址，方便下单时快速选择</p>
        <el-button type="primary" @click="handleAdd">添加地址</el-button>
      </div>

      <!-- Address cards -->
      <div
        v-for="addr in addressStore.addresses"
        :key="addr.id"
        class="address-card glass-card"
        :class="{ 'is-default': addr.isDefault }"
      >
        <div class="card-top">
          <div class="card-info">
            <div class="name-row">
              <span class="receiver-name">{{ addr.receiverName }}</span>
              <span class="receiver-phone">{{ addr.receiverPhone }}</span>
              <el-tag v-if="addr.isDefault" size="small" class="default-tag" effect="dark">默认</el-tag>
            </div>
            <div class="address-text">{{ addr.province }}{{ addr.city }}{{ addr.district }}{{ addr.detail }}</div>
          </div>
        </div>
        <div class="card-actions">
          <el-button v-if="!addr.isDefault" link size="small" type="primary" @click="handleSetDefault(addr)">
            设为默认
          </el-button>
          <el-button link size="small" @click="handleEdit(addr)">编辑</el-button>
          <el-button link size="small" type="danger" @click="handleDelete(addr)">删除</el-button>
        </div>
      </div>
    </div>

    <!-- Add / Edit dialog -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEditing ? '编辑地址' : '添加地址'"
      width="560px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="收货人" prop="receiverName">
          <el-input v-model="form.receiverName" placeholder="请输入收货人姓名" maxlength="50" />
        </el-form-item>
        <el-form-item label="联系电话" prop="receiverPhone">
          <el-input v-model="form.receiverPhone" placeholder="请输入11位手机号" maxlength="11" />
        </el-form-item>
        <el-form-item label="所在地区" prop="province">
          <div class="region-row">
            <el-input v-model="form.province" placeholder="省" maxlength="20" class="region-input" />
            <el-input v-model="form.city" placeholder="市" maxlength="20" class="region-input" />
            <el-input v-model="form.district" placeholder="区" maxlength="20" class="region-input" />
          </div>
        </el-form-item>
        <el-form-item label="详细地址" prop="detail">
          <el-input
            v-model="form.detail"
            type="textarea"
            placeholder="街道、门牌号等"
            :rows="3"
            maxlength="200"
            show-word-limit
          />
        </el-form-item>
        <el-form-item>
          <el-checkbox v-model="form.isDefault">设为默认地址</el-checkbox>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Location } from '@element-plus/icons-vue'
import type { FormInstance } from 'element-plus'
import { useAddressStore, type Address } from '../../stores/addressStore'
import type { AddressData } from '../../api/address'

const addressStore = useAddressStore()

const dialogVisible = ref(false)
const submitting = ref(false)
const isEditing = ref(false)
const editingId = ref<number | null>(null)
const formRef = ref<FormInstance>()

const form = reactive<AddressData & { isDefault: boolean }>({
  receiverName: '',
  receiverPhone: '',
  province: '',
  city: '',
  district: '',
  detail: '',
  isDefault: false,
})

const phoneValidator = (_rule: any, value: string, callback: Function) => {
  if (!value) {
    callback(new Error('请输入联系电话'))
  } else if (!/^1[3-9]\d{9}$/.test(value)) {
    callback(new Error('请输入正确的11位手机号'))
  } else {
    callback()
  }
}

const rules = {
  receiverName: [{ required: true, message: '请输入收货人姓名', trigger: 'blur' }],
  receiverPhone: [
    { required: true, message: '请输入联系电话', trigger: 'blur' },
    { validator: phoneValidator, trigger: 'blur' },
  ],
  province: [{ required: true, message: '请输入省份', trigger: 'blur' }],
  detail: [{ required: true, message: '请输入详细地址', trigger: 'blur' }],
}

function resetForm() {
  form.receiverName = ''
  form.receiverPhone = ''
  form.province = ''
  form.city = ''
  form.district = ''
  form.detail = ''
  form.isDefault = false
  editingId.value = null
  isEditing.value = false
}

function handleAdd() {
  resetForm()
  dialogVisible.value = true
}

function handleEdit(addr: Address) {
  form.receiverName = addr.receiverName
  form.receiverPhone = addr.receiverPhone
  form.province = addr.province
  form.city = addr.city
  form.district = addr.district
  form.detail = addr.detail
  form.isDefault = addr.isDefault
  editingId.value = addr.id
  isEditing.value = true
  dialogVisible.value = true
}

async function handleSetDefault(addr: Address) {
  await addressStore.setDefault(addr.id)
}

async function handleDelete(addr: Address) {
  try {
    await ElMessageBox.confirm(`确定要删除「${addr.receiverName}」的收货地址吗？`, '提示')
    await addressStore.deleteAddress(addr.id)
  } catch {
    // cancelled
  }
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    if (isEditing.value && editingId.value) {
      await addressStore.updateAddress(editingId.value, { ...form })
    } else {
      await addressStore.createAddress({ ...form })
    }
    dialogVisible.value = false
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  addressStore.fetchAddresses()
})
</script>

<style scoped>
.address-management {
  animation: fadeInUp 0.5s ease-out forwards;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
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

/* Address grid */
.address-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(380px, 1fr));
  gap: 20px;
}

/* Empty state */
.empty-state {
  grid-column: 1 / -1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 300px;
  text-align: center;
}

.empty-icon {
  color: var(--text-muted);
  margin-bottom: 16px;
}

.empty-state h3 {
  font-family: 'Sora', sans-serif;
  font-size: 20px;
  color: var(--text-primary);
  margin-bottom: 8px;
}

.empty-state p {
  font-size: 14px;
  color: var(--text-muted);
  margin-bottom: 20px;
}

/* Address card */
.address-card {
  position: relative;
  transition: all var(--transition-normal);
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  gap: 16px;
}

.address-card:hover {
  border-color: rgba(255, 0, 255, 0.5);
  box-shadow: var(--glow-magenta);
}

.address-card.is-default {
  border-color: var(--neon-cyan);
  box-shadow: 0 0 15px rgba(0, 255, 255, 0.15);
}

.address-card.is-default::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
  background: linear-gradient(90deg, var(--neon-cyan), var(--neon-magenta));
  border-radius: 20px 20px 0 0;
}

.card-top {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.card-info {
  flex: 1;
  min-width: 0;
}

.name-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 10px;
  flex-wrap: wrap;
}

.receiver-name {
  font-family: 'Sora', sans-serif;
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
}

.receiver-phone {
  font-size: 14px;
  color: var(--text-secondary);
}

.default-tag {
  background: linear-gradient(135deg, var(--neon-cyan), var(--neon-cyan-dim)) !important;
  border: none !important;
  color: #000 !important;
  font-weight: 600;
}

.address-text {
  font-size: 14px;
  color: var(--text-secondary);
  line-height: 1.6;
}

.card-actions {
  display: flex;
  gap: 8px;
  padding-top: 12px;
  border-top: 1px solid var(--border-subtle);
}

/* Dialog form overrides */
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

:deep(.el-input__wrapper),
:deep(.el-textarea__inner) {
  background: var(--glass-bg);
  border: 1px solid var(--border-glow-magenta);
  box-shadow: none;
  color: var(--text-secondary);
}

:deep(.el-input__inner) {
  color: var(--text-secondary);
}

:deep(.el-textarea__inner) {
  background: var(--glass-bg);
  border: 1px solid var(--border-glow-magenta);
  box-shadow: none;
  color: var(--text-secondary);
}

.region-row {
  display: flex;
  gap: 8px;
  width: 100%;
}

.region-input {
  flex: 1;
}

/* Button overrides */
:deep(.el-button--primary) {
  background: linear-gradient(135deg, var(--neon-magenta), var(--neon-magenta-dim));
  border: none;
  color: #fff;
  font-weight: 600;
}

:deep(.el-button--primary:hover) {
  box-shadow: var(--glow-magenta);
  opacity: 0.9;
}

:deep(.el-checkbox__label) {
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
