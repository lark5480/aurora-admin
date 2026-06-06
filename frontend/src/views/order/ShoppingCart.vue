<template>
  <div class="shopping-cart">
    <div class="page-header">
      <h1 class="page-title">购物车</h1>
      <p class="page-sub">管理你的待购商品</p>
    </div>

    <!-- Empty state -->
    <div v-if="!cartLoading && cartItems.length === 0" class="glass-card empty-cart">
      <div class="empty-content">
        <el-icon :size="64" class="empty-icon"><ShoppingCart /></el-icon>
        <h3>购物车是空的</h3>
        <p>快去逛逛，把心仪的商品加入购物车吧</p>
        <el-button type="primary" @click="goShopping">去逛逛</el-button>
      </div>
    </div>

    <!-- Cart list -->
    <div v-else class="glass-card">
      <div v-loading="cartLoading" class="cart-body">
        <div class="cart-table-header">
          <el-checkbox v-model="allSelected" :indeterminate="isIndeterminate" @change="handleSelectAll">
            全选
          </el-checkbox>
          <span class="col-product">商品</span>
          <span class="col-price">单价</span>
          <span class="col-qty">数量</span>
          <span class="col-subtotal">小计</span>
          <span class="col-action">操作</span>
        </div>

        <div class="cart-items">
          <div v-for="item in cartItems" :key="item.id" class="cart-item-row">
            <el-checkbox
              :model-value="selectedIds.has(item.id)"
              :disabled="item.status !== 'ON_SALE' || item.stock <= 0"
              @change="toggleSelect(item.id)"
            />
            <div class="col-product">
              <el-image :src="resolveImageUrl(item.coverImage || '')" class="item-cover" fit="cover">
                <template #error>
                  <div class="image-placeholder">
                    <el-icon :size="18"><Picture /></el-icon>
                  </div>
                </template>
              </el-image>
              <div class="item-info">
                <span class="item-name">{{ item.productName }}</span>
                <div v-if="item.availableSkus && item.availableSkus.length > 0" class="item-sku-row">
                  <el-tag v-if="item.specName" size="small" type="warning" effect="dark" class="item-spec-tag">{{
                    item.specName
                  }}</el-tag>
                  <span v-else class="no-sku-hint">未选规格</span>
                  <el-select
                    :model-value="item.skuId"
                    size="small"
                    class="sku-switch-select"
                    popper-class="sku-popper"
                    placeholder="选规格"
                    @change="(val) => handleSwitchSku(item, val)"
                  >
                    <el-option
                      v-for="sku in item.availableSkus"
                      :key="sku.id"
                      :label="sku.specName + ' ¥' + sku.price"
                      :value="sku.id"
                    >
                      <div class="sku-option-row">
                        <span>{{ sku.specName }}</span>
                        <span class="sku-option-price">¥{{ sku.price }}</span>
                        <el-tag v-if="sku.id === item.skuId" size="small" type="warning" effect="dark">当前</el-tag>
                      </div>
                    </el-option>
                  </el-select>
                </div>
                <el-tag v-if="item.status !== 'ON_SALE'" size="small" type="info" class="item-tag">已下架</el-tag>
                <el-tag v-else-if="item.stock <= 0" size="small" type="danger" class="item-tag">缺货</el-tag>
              </div>
            </div>
            <div class="col-price">
              <span class="price-text">¥{{ item.price }}</span>
            </div>
            <div class="col-qty">
              <el-input-number
                :model-value="item.quantity"
                :min="1"
                :max="999"
                size="small"
                controls-position="right"
                style="width: 120px"
                @change="(val) => handleQtyChange(item, val)"
              />
            </div>
            <div class="col-subtotal">
              <span class="subtotal-text">¥{{ (item.price * item.quantity).toFixed(2) }}</span>
            </div>
            <div class="col-action">
              <el-button link type="danger" size="small" @click="handleRemove(item)"> 删除 </el-button>
            </div>
          </div>
        </div>
      </div>

      <!-- Bottom bar -->
      <div class="cart-footer">
        <div class="footer-left">
          <el-checkbox v-model="allSelected" :indeterminate="isIndeterminate" @change="handleSelectAll">
            全选
          </el-checkbox>
          <el-button link type="danger" :disabled="selectedIds.size === 0" @click="handleBatchDelete">
            删除选中
          </el-button>
          <el-button link type="warning" :disabled="cartItems.length === 0" @click="handleClear">
            清空购物车
          </el-button>
        </div>
        <div class="footer-right">
          <span class="total-label">
            已选 <strong>{{ selectedIds.size }}</strong> 件商品
          </span>
          <span class="total-amount">
            合计：<strong>¥{{ totalAmount.toFixed(2) }}</strong>
          </span>
          <el-button type="primary" size="large" :disabled="selectedIds.size === 0" @click="handleCheckout">
            结算
          </el-button>
        </div>
      </div>
    </div>

    <!-- Checkout dialog -->
    <el-dialog v-model="checkoutVisible" title="确认订单" width="560px" :close-on-click-modal="false">
      <!-- 收货地址区域 -->
      <div class="checkout-section">
        <div class="section-title">收货地址</div>
        <div class="address-selector-row">
          <el-select
            v-model="selectedAddressId"
            placeholder="选择已有收货地址"
            clearable
            :loading="addressStore.loading"
            class="address-select"
            popper-class="address-popper"
            @change="handleAddressChange"
          >
            <el-option
              v-for="addr in addressStore.addresses"
              :key="addr.id"
              :label="`${addr.receiverName}  ${addr.receiverPhone}  ${addr.province}${addr.city}${addr.district}${addr.detail}`"
              :value="addr.id"
            >
              <div class="address-option">
                <div class="option-name">{{ addr.receiverName }} {{ addr.receiverPhone }}</div>
                <div class="option-addr">{{ addr.province }}{{ addr.city }}{{ addr.district }}{{ addr.detail }}</div>
                <el-tag v-if="addr.isDefault" size="small" type="success" effect="dark">默认</el-tag>
              </div>
            </el-option>
          </el-select>
          <el-button link type="primary" @click="goToAddressMgt">
            <el-icon><Plus /></el-icon>
            管理地址
          </el-button>
        </div>
        <div v-if="!addressStore.loading && addressStore.addresses.length === 0" class="no-address-hint">
          <el-icon><WarningFilled /></el-icon>
          <span>暂无收货地址，请在下方直接填写或去地址管理页添加</span>
        </div>
      </div>

      <el-divider />

      <el-form ref="checkoutFormRef" :model="checkoutForm" :rules="checkoutRules" label-width="90px">
        <el-form-item label="收货人" prop="receiverName">
          <el-input v-model="checkoutForm.receiverName" placeholder="请输入收货人姓名" maxlength="50" />
        </el-form-item>
        <el-form-item label="手机号" prop="receiverPhone">
          <el-input v-model="checkoutForm.receiverPhone" placeholder="请输入11位手机号" maxlength="11" />
        </el-form-item>
        <el-form-item label="详细地址" prop="receiverAddress">
          <el-input
            v-model="checkoutForm.receiverAddress"
            type="textarea"
            placeholder="省/市/区 + 详细地址"
            :rows="2"
            maxlength="200"
            show-word-limit
          />
        </el-form-item>
      </el-form>

      <el-divider />

      <!-- 订单备注区域 -->
      <div class="checkout-section">
        <div class="section-title">订单备注</div>
        <el-input
          v-model="checkoutForm.remark"
          placeholder="选填，如配送时间要求、发票信息等"
          maxlength="200"
          show-word-limit
        />
      </div>

      <template #footer>
        <el-button @click="checkoutVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmitOrder">提交订单</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ShoppingCart, Picture, Plus, WarningFilled } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'
import { useOrderStore } from '../../stores/orderStore'
import { useAddressStore } from '../../stores/addressStore'
import { cartApi } from '../../api/order'
import { resolveImageUrl } from '../../utils/image'

const router = useRouter()
const orderStore = useOrderStore()
const addressStore = useAddressStore()

const cartItems = computed(() => orderStore.cartItems)
const cartLoading = computed(() => orderStore.cartLoading)

const selectedIds = ref(new Set())
const checkoutVisible = ref(false)
const submitting = ref(false)
const checkoutFormRef = ref(null)
const selectedAddressId = ref(null)

const checkoutForm = ref({
  receiverName: '',
  receiverPhone: '',
  receiverAddress: '',
  remark: '',
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

const checkoutRules = {
  receiverName: [{ required: true, message: '请输入收货人姓名', trigger: 'blur' }],
  receiverPhone: [
    { required: true, message: '请输入联系电话', trigger: 'blur' },
    { validator: phoneValidator, trigger: 'blur' },
  ],
  receiverAddress: [{ required: true, message: '请输入收货地址', trigger: 'blur' }],
}

// Computed: all selected
const allSelected = computed({
  get() {
    const selectable = cartItems.value.filter((i) => i.status === 'ON_SALE' && i.stock > 0)
    if (selectable.length === 0) return false
    return selectable.every((i) => selectedIds.value.has(i.id))
  },
  set(val) {
    handleSelectAll(val)
  },
})

// Computed: indeterminate state
const isIndeterminate = computed(() => {
  const selectable = cartItems.value.filter((i) => i.status === 'ON_SALE' && i.stock > 0)
  const selectedCount = selectable.filter((i) => selectedIds.value.has(i.id)).length
  return selectedCount > 0 && selectedCount < selectable.length
})

// Computed: total amount of selected items
const totalAmount = computed(() => {
  let total = 0
  for (const item of cartItems.value) {
    if (selectedIds.value.has(item.id)) {
      total += item.price * item.quantity
    }
  }
  return total
})

function toggleSelect(id) {
  const newSet = new Set(selectedIds.value)
  if (newSet.has(id)) {
    newSet.delete(id)
  } else {
    newSet.add(id)
  }
  selectedIds.value = newSet
}

function handleSelectAll(val) {
  if (val) {
    const selectableIds = cartItems.value.filter((i) => i.status === 'ON_SALE' && i.stock > 0).map((i) => i.id)
    selectedIds.value = new Set(selectableIds)
  } else {
    selectedIds.value = new Set()
  }
}

async function handleQtyChange(item, newQty) {
  if (newQty < 1) return
  await orderStore.updateCartQty(item.id, newQty)
}

async function handleRemove(item) {
  try {
    await ElMessageBox.confirm(`确定要从购物车移除「${item.productName}」吗？`, '提示')
    await orderStore.removeFromCart(item.id)
    selectedIds.value.delete(item.id)
    selectedIds.value = new Set(selectedIds.value)
  } catch {
    // cancelled
  }
}

async function handleBatchDelete() {
  if (selectedIds.value.size === 0) return
  try {
    await ElMessageBox.confirm(`确定要删除选中的 ${selectedIds.value.size} 件商品吗？`, '提示')
    const ids = [...selectedIds.value]
    for (const id of ids) {
      await orderStore.removeFromCart(id)
    }
    selectedIds.value = new Set()
    ElMessage.success('删除成功')
  } catch {
    // cancelled
  }
}

async function handleClear() {
  try {
    await ElMessageBox.confirm('确定要清空购物车吗？', '提示')
    await orderStore.clearCart()
    selectedIds.value = new Set()
  } catch {
    // cancelled
  }
}

async function handleSwitchSku(item, newSkuId) {
  try {
    await cartApi.switchSku(item.id, newSkuId || null)
    ElMessage.success('规格已切换')
    await orderStore.fetchCart()
  } catch (e) {
    ElMessage.error(e?.message || '切换失败')
  }
}

async function handleCheckout() {
  if (selectedIds.value.size === 0) {
    ElMessage.warning('请至少选择一件商品')
    return
  }

  // 二次检查：确保选中的商品都是上架且有库存的
  for (const id of selectedIds.value) {
    const item = cartItems.value.find((i) => i.id === id)
    if (!item || item.status !== 'ON_SALE' || item.stock <= 0) {
      ElMessage.warning('部分商品已下架或库存不足，请重新选择')
      return
    }
  }

  // 每次打开结算弹窗，重新拉取最新地址列表并等待完成
  await addressStore.fetchAddresses()
  // 自动选中默认地址，没有默认则选第一个
  const addrs = addressStore.addresses
  const defaultAddr = addrs.find((a) => a.isDefault) || addrs[0]
  if (defaultAddr) {
    selectedAddressId.value = defaultAddr.id
    // 同步填充表单
    checkoutForm.value = {
      receiverName: defaultAddr.receiverName,
      receiverPhone: defaultAddr.receiverPhone,
      receiverAddress: `${defaultAddr.province}${defaultAddr.city}${defaultAddr.district} ${defaultAddr.detail}`,
      remark: '',
    }
  } else {
    checkoutForm.value = {
      receiverName: '',
      receiverPhone: '',
      receiverAddress: '',
      remark: '',
    }
    selectedAddressId.value = null
  }
  checkoutVisible.value = true
}

function handleAddressChange(addrId) {
  if (!addrId) return
  const addr = addressStore.addresses.find((a) => a.id === addrId)
  if (!addr) return
  checkoutForm.value = {
    ...checkoutForm.value,
    receiverName: addr.receiverName,
    receiverPhone: addr.receiverPhone,
    receiverAddress: `${addr.province}${addr.city}${addr.district} ${addr.detail}`,
  }
}

async function handleSubmitOrder() {
  const valid = await checkoutFormRef.value?.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    const success = await orderStore.createOrder({
      cartItemIds: [...selectedIds.value],
      ...checkoutForm.value,
    })
    if (success) {
      checkoutVisible.value = false
      selectedIds.value = new Set()
    }
  } finally {
    submitting.value = false
  }
}

function goShopping() {
  router.push('/home/product')
}

function goToAddressMgt() {
  router.push('/home/address')
}

onMounted(() => {
  orderStore.fetchCart()
  addressStore.fetchAddresses()
})
</script>

<style scoped>
.shopping-cart {
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

/* Empty state */
.empty-cart {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 360px;
}

.empty-content {
  text-align: center;
}

.empty-icon {
  color: var(--text-muted);
  margin-bottom: 16px;
}

.empty-content h3 {
  font-family: 'Sora', sans-serif;
  font-size: 20px;
  color: var(--text-primary);
  margin-bottom: 8px;
}

.empty-content p {
  font-size: 14px;
  color: var(--text-muted);
  margin-bottom: 20px;
}

/* Cart table header */
.cart-table-header {
  display: flex;
  align-items: center;
  padding: 0 0 12px 0;
  border-bottom: 1px solid var(--border-subtle);
  font-size: 13px;
  color: var(--text-muted);
  gap: 12px;
}

.cart-table-header .el-checkbox {
  width: 60px;
}

.col-product {
  flex: 1;
  min-width: 0;
}

.col-price {
  width: 120px;
  text-align: center;
}

.col-qty {
  width: 140px;
  text-align: center;
}

.col-subtotal {
  width: 120px;
  text-align: center;
}

.col-action {
  width: 80px;
  text-align: center;
}

/* Cart items */
.cart-items {
  min-height: 100px;
}

.cart-item-row {
  display: flex;
  align-items: center;
  padding: 16px 0;
  border-bottom: 1px solid var(--border-subtle);
  gap: 12px;
  transition: background var(--transition-normal);
}

.cart-item-row:hover {
  background: rgba(255, 0, 255, 0.04);
}

.cart-item-row.is-disabled {
  opacity: 0.55;
  background: rgba(255, 255, 255, 0.02);
}

.cart-item-row.is-disabled:hover {
  background: rgba(255, 255, 255, 0.02);
}

.cart-item-row .el-checkbox {
  width: 60px;
}

.cart-item-row .col-product {
  display: flex;
  align-items: center;
  gap: 12px;
}

.item-cover {
  width: 64px;
  height: 64px;
  border-radius: 8px;
  border: 1px solid var(--border-subtle);
  flex-shrink: 0;
}

.image-placeholder {
  width: 64px;
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--glass-bg);
  border-radius: 8px;
  color: var(--text-muted);
}

.item-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.item-name {
  font-size: 14px;
  color: var(--text-primary);
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.item-spec {
  font-size: 12px;
  color: var(--text-muted);
}

/* SKU row: tag + switch selector */
.item-sku-row {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}

.item-spec-tag {
  flex-shrink: 0;
}

.no-sku-hint {
  font-size: 11px;
  color: var(--text-muted);
  font-style: italic;
}

.sku-switch-select {
  width: 140px;
}

:deep(.sku-switch-select .el-select__wrapper) {
  background: var(--glass-bg);
  border: 1px solid var(--border-glow-magenta);
  box-shadow: none;
  min-height: 28px;
  padding: 0 8px;
}

:deep(.sku-switch-select .el-select__wrapper:hover) {
  border-color: var(--neon-cyan);
}

:deep(.sku-switch-select .el-select__placeholder) {
  font-size: 11px;
  color: var(--text-muted);
}

.sku-option-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.sku-option-price {
  color: var(--neon-green);
  font-weight: 500;
  font-size: 13px;
}

/* Prices */
.price-text {
  color: var(--neon-green);
  font-weight: 600;
  font-family: 'Sora', sans-serif;
}

.subtotal-text {
  color: var(--neon-cyan);
  font-weight: 600;
  font-family: 'Sora', sans-serif;
}

/* Cart footer */
.cart-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 16px;
  margin-top: 4px;
  border-top: 1px solid var(--border-subtle);
}

.footer-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.footer-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.total-label {
  font-size: 13px;
  color: var(--text-muted);
}

.total-label strong {
  color: var(--text-primary);
}

.total-amount {
  font-size: 14px;
  color: var(--text-secondary);
}

.total-amount strong {
  font-size: 22px;
  color: var(--neon-magenta);
  font-family: 'Sora', sans-serif;
}

/* Input overrides */
:deep(.el-input__wrapper),
:deep(.el-input-number .el-input__wrapper) {
  background: var(--glass-bg);
  border: 1px solid var(--border-glow-magenta);
  box-shadow: none;
}

:deep(.el-input__inner) {
  color: var(--text-secondary);
}

:deep(.el-input-number.is-controls-right .el-input-number__decrease),
:deep(.el-input-number.is-controls-right .el-input-number__increase) {
  background: var(--glass-bg);
  color: var(--text-muted);
}

:deep(.el-input-number.is-controls-right .el-input-number__decrease:hover),
:deep(.el-input-number.is-controls-right .el-input-number__increase:hover) {
  color: var(--neon-cyan);
}

/* Dialog overrides */
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

:deep(.el-textarea__inner) {
  background: var(--glass-bg);
  border: 1px solid var(--border-glow-magenta);
  box-shadow: none;
  color: var(--text-secondary);
}

/* Checkout dialog sections */
.checkout-section {
  margin-bottom: 4px;
}

.checkout-section + .checkout-section {
  margin-top: 12px;
}

.section-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 12px;
  padding-left: 4px;
  border-left: 3px solid var(--neon-cyan);
}

/* Address selector row */
.address-selector-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.address-select {
  flex: 1;
  min-width: 0;
}

:deep(.address-select .el-select__wrapper) {
  background: var(--glass-bg);
  border: 1px solid var(--border-glow-magenta);
  box-shadow: none;
}

:deep(.address-select .el-select__placeholder) {
  color: var(--text-muted);
}

/* Address dropdown panel */
:deep(.el-select-dropdown) {
  background: var(--bg-darker);
  border: 1px solid var(--border-glow-magenta);
}

:deep(.el-select-dropdown__item) {
  color: var(--text-secondary);
  height: auto;
  line-height: 1.4;
  padding: 8px 12px;
}

:deep(.el-select-dropdown__item.hover) {
  background: rgba(255, 0, 255, 0.08);
  color: var(--text-primary);
}

:deep(.el-select-dropdown__item.selected) {
  color: var(--neon-cyan);
  font-weight: 600;
}

/* Address option in dropdown */
.address-option {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.option-name {
  font-size: 14px;
  color: var(--text-primary);
  white-space: nowrap;
}

.option-addr {
  font-size: 12px;
  color: var(--text-muted);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
  min-width: 0;
}

.no-address-hint {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 10px;
  font-size: 13px;
  color: var(--neon-green);
}

:deep(.el-divider) {
  border-color: var(--border-subtle);
  margin: 16px 0;
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
/* address dropdown popper (teleported to body, must be global) */
.address-popper {
  background: #1a1a2e !important;
  border: 1px solid rgba(255, 0, 255, 0.3) !important;
}
.address-popper .el-select-dropdown__item {
  color: rgba(255, 255, 255, 0.85) !important;
}
.address-popper .el-select-dropdown__item.hover,
.address-popper .el-select-dropdown__item:hover {
  background: rgba(255, 0, 255, 0.12) !important;
  color: #fff !important;
}
.address-popper .el-select-dropdown__item.selected {
  color: #00ffff !important;
  font-weight: 600;
}
.address-popper .el-popper__arrow::before {
  background: #1a1a2e !important;
  border: 1px solid rgba(255, 0, 255, 0.3) !important;
}

/* sku switch dropdown */
.sku-popper {
  background: #1a1a2e !important;
  border: 1px solid rgba(255, 0, 255, 0.3) !important;
}
.sku-popper .el-select-dropdown__item {
  color: rgba(255, 255, 255, 0.85) !important;
}
.sku-popper .el-select-dropdown__item.hover,
.sku-popper .el-select-dropdown__item:hover {
  background: rgba(255, 0, 255, 0.12) !important;
  color: #fff !important;
}
</style>
