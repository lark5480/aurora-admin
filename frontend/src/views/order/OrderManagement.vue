<template>
  <div class="order-management">
    <div class="page-header">
      <h1 class="page-title">订单管理</h1>
      <p class="page-sub">查看和管理系统订单</p>
    </div>

    <div class="glass-card">
      <!-- Search & filter bar -->
      <div class="filter-tabs">
        <div class="filter-row">
          <el-input
            v-model="searchOrderNo"
            placeholder="搜索订单号"
            clearable
            class="search-input"
            @change="handleFilterChange"
          />
          <el-input
            v-model="searchUsername"
            placeholder="搜索用户名"
            clearable
            class="search-input"
            @change="handleFilterChange"
          />
        </div>
        <el-radio-group v-model="statusFilter" @change="handleFilterChange">
          <el-radio-button value="">全部</el-radio-button>
          <el-radio-button value="PENDING">待支付</el-radio-button>
          <el-radio-button value="PAID">已支付</el-radio-button>
          <el-radio-button value="SHIPPED">已发货</el-radio-button>
          <el-radio-button value="COMPLETED">已完成</el-radio-button>
          <el-radio-button value="CANCELLED">已取消</el-radio-button>
          <el-radio-button value="REFUNDING">售后中</el-radio-button>
          <el-radio-button value="REFUNDED">已退款</el-radio-button>
        </el-radio-group>

        <div v-if="selectedOrderIds.length > 0" class="batch-bar">
          <span class="batch-hint">已选 {{ selectedOrderIds.length }} 项</span>
          <el-button
            type="danger"
            size="small"
            :loading="batchSubmitting"
            :disabled="batchSubmitting"
            @click="handleBatchDelete"
          >
            批量删除
          </el-button>
        </div>
        <el-button type="primary" size="small" :loading="exporting" :disabled="exporting" @click="handleExport">
          导出Excel
        </el-button>
      </div>

      <!-- Order table -->
      <el-table
        v-loading="orderStore.orderLoading"
        :data="orderStore.orderList"
        stripe
        class="order-table"
        @selection-change="handleOrderSelectionChange"
      >
        <el-table-column type="selection" width="50" />
        <el-table-column prop="orderNo" label="订单号" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">
            <el-button link type="primary" @click="showDetail(row)">{{ row.orderNo }}</el-button>
          </template>
        </el-table-column>
        <el-table-column prop="username" label="用户名" width="120" />
        <el-table-column label="金额" width="120">
          <template #default="{ row }">
            <span class="amount-text">¥{{ row.totalAmount }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" size="small">
              {{ statusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="下单时间" width="180" />
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="showDetail(row)">详情</el-button>

            <!-- PENDING: 支付 + 取消 -->
            <el-button
              v-if="row.status === 'PENDING'"
              link
              type="success"
              size="small"
              :loading="operatingOrderId === row.id"
              @click="handlePay(row)"
            >
              支付
            </el-button>
            <el-button
              v-if="row.status === 'PENDING'"
              link
              type="danger"
              size="small"
              :loading="operatingOrderId === row.id"
              @click="handleCancel(row)"
            >
              取消
            </el-button>

            <!-- PAID: 管理员发货 -->
            <el-button
              v-if="isAdmin && row.status === 'PAID'"
              link
              type="primary"
              size="small"
              :loading="operatingOrderId === row.id"
              @click="handleShip(row)"
            >
              发货
            </el-button>

            <!-- SHIPPED: 确认收货 -->
            <el-button
              v-if="row.status === 'SHIPPED'"
              link
              type="success"
              size="small"
              :loading="operatingOrderId === row.id"
              @click="handleConfirm(row)"
            >
              确认收货
            </el-button>

            <!-- PAID: 退款 -->
            <el-button v-if="row.status === 'PAID'" link type="warning" size="small" @click="handleRefund(row)">
              退款
            </el-button>

            <!-- SHIPPED / COMPLETED: 退货退款 -->
            <el-button
              v-if="row.status === 'SHIPPED' || row.status === 'COMPLETED'"
              link
              type="warning"
              size="small"
              @click="handleReturn(row)"
            >
              退货退款
            </el-button>

            <!-- CANCELLED: 删除 -->
            <el-button v-if="row.status === 'CANCELLED'" link type="danger" size="small" @click="handleDelete(row)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="orderStore.orderTotal"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </div>

    <!-- Order detail dialog -->
    <el-dialog v-model="detailVisible" title="订单详情" width="680px" :close-on-click-modal="false">
      <div v-if="currentOrder" class="order-detail">
        <!-- Order info -->
        <div class="detail-section">
          <h4 class="section-title">订单信息</h4>
          <div class="info-grid">
            <div class="info-item">
              <span class="info-label">订单编号</span>
              <span class="info-value">{{ currentOrder.orderNo }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">订单状态</span>
              <el-tag :type="statusTagType(currentOrder.status)" size="small">
                {{ statusLabel(currentOrder.status) }}
              </el-tag>
            </div>
            <div class="info-item">
              <span class="info-label">订单金额</span>
              <span class="info-value amount-text">¥{{ currentOrder.totalAmount }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">下单时间</span>
              <span class="info-value">{{ currentOrder.createTime }}</span>
            </div>
            <div v-if="currentOrder.trackingNumber" class="info-item">
              <span class="info-label">快递单号</span>
              <span class="info-value tracking-no">{{ currentOrder.trackingNumber }}</span>
            </div>
            <div class="info-item info-item-full">
              <span class="info-label">订单备注</span>
              <span class="info-value">{{ currentOrder.remark || '-' }}</span>
            </div>
          </div>
        </div>

        <!-- Receiver info -->
        <div class="detail-section">
          <h4 class="section-title">收货信息</h4>
          <div class="info-grid">
            <div class="info-item">
              <span class="info-label">收货人</span>
              <span class="info-value">{{ currentOrder.receiverName }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">联系电话</span>
              <span class="info-value">{{ currentOrder.receiverPhone }}</span>
            </div>
            <div class="info-item info-item-full">
              <span class="info-label">收货地址</span>
              <span class="info-value">{{ currentOrder.receiverAddress }}</span>
            </div>
          </div>
        </div>

        <!-- Order items -->
        <div class="detail-section">
          <h4 class="section-title">商品明细</h4>
          <el-table :data="currentOrder.orderItems || []" size="small" class="items-table">
            <el-table-column label="商品" min-width="240">
              <template #default="{ row }">
                <div class="item-cell">
                  <el-image :src="resolveImageUrl(row.coverImage || '')" class="item-thumb" fit="cover">
                    <template #error>
                      <div class="thumb-placeholder">
                        <el-icon :size="14"><Picture /></el-icon>
                      </div>
                    </template>
                  </el-image>
                  <div class="item-cell-info">
                    <span class="item-cell-name">{{ row.productName }}</span>
                    <el-tag v-if="row.specName" size="small" type="warning" effect="dark" class="item-sku-tag">
                      {{ row.specName }}
                    </el-tag>
                  </div>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="price" label="单价" width="100">
              <template #default="{ row }"> ¥{{ row.price }} </template>
            </el-table-column>
            <el-table-column prop="quantity" label="数量" width="80" />
            <el-table-column label="退款状态" width="100">
              <template #default="{ row: itemRow }">
                <el-tag v-if="itemRow.refundStatus === 'REFUNDED'" type="success" size="small">已退款</el-tag>
                <span v-else class="text-muted">-</span>
              </template>
            </el-table-column>
            <el-table-column prop="subtotal" label="小计" width="100">
              <template #default="{ row }">
                <span class="amount-text">¥{{ row.subtotal }}</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="120">
              <template #default="{ row: itemRow }">
                <el-button
                  v-if="
                    itemRow.refundStatus !== 'REFUNDED' &&
                    (currentOrder.status === 'PAID' ||
                      currentOrder.status === 'SHIPPED' ||
                      currentOrder.status === 'COMPLETED')
                  "
                  link
                  type="warning"
                  size="small"
                  @click="handleItemRefund(itemRow)"
                >
                  {{ currentOrder.status === 'PAID' ? '退款' : '退货退款' }}
                </el-button>
              </template>
            </el-table-column>
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
import { ref, onMounted, h, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Picture } from '@element-plus/icons-vue'
import { useUserStore } from '../../stores/user'
import { useOrderStore } from '../../stores/orderStore'
import { useAfterSaleStore } from '../../stores/afterSaleStore'
import { orderApi } from '../../api/order'
import { resolveImageUrl } from '../../utils/image'

const userStore = useUserStore()
const orderStore = useOrderStore()
const afterSaleStore = useAfterSaleStore()
const isAdmin = computed(() => userStore.hasRole('SUPER_ADMIN') || userStore.hasRole('ADMIN'))

const currentPage = ref(1)
const pageSize = ref(10)
const statusFilter = ref('')
const searchOrderNo = ref('')
const searchUsername = ref('')
const batchSubmitting = ref(false)
const exporting = ref(false)
const operatingOrderId = ref<number | null>(null)

const detailVisible = ref(false)
const currentOrder = ref(null)
const selectedOrderIds = ref<number[]>([])

function statusTagType(status) {
  const map = {
    PENDING: 'warning',
    PAID: 'primary',
    SHIPPED: '',
    COMPLETED: 'success',
    CANCELLED: 'info',
    REFUNDING: 'warning',
    REFUNDED: 'info',
  }
  return map[status] || 'info'
}

function statusLabel(status) {
  const map = {
    PENDING: '待支付',
    PAID: '已支付',
    SHIPPED: '已发货',
    COMPLETED: '已完成',
    CANCELLED: '已取消',
    REFUNDING: '售后中',
    REFUNDED: '已退款',
  }
  return map[status] || status
}

async function fetchOrders() {
  await orderStore.fetchOrders({
    page: currentPage.value,
    size: pageSize.value,
    status: statusFilter.value || undefined,
    orderNo: searchOrderNo.value || undefined,
    username: searchUsername.value || undefined,
  })
}

function handleFilterChange() {
  currentPage.value = 1
  fetchOrders()
}

function handleSizeChange(newSize) {
  pageSize.value = newSize
  currentPage.value = 1
  fetchOrders()
}

function handleCurrentChange() {
  fetchOrders()
}

async function showDetail(row) {
  try {
    const res = await orderApi.getById(row.id)
    currentOrder.value = res
  } catch {
    currentOrder.value = row
  }
  detailVisible.value = true
}

async function handleCancel(row) {
  if (operatingOrderId.value) return
  try {
    await ElMessageBox.confirm(`确定要取消订单「${row.orderNo}」吗？`, '提示')
    operatingOrderId.value = row.id
    const success = await orderStore.cancelOrder(row.id)
    if (success) {
      fetchOrders()
    }
  } catch {
    // cancelled
  } finally {
    operatingOrderId.value = null
  }
}

async function handlePay(row) {
  if (operatingOrderId.value) return
  try {
    await ElMessageBox.confirm(`确定支付订单「${row.orderNo}」吗？`, '提示')
    operatingOrderId.value = row.id
    await orderApi.pay(row.orderNo)
    ElMessage.success('支付成功')
    fetchOrders()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error(e.response?.data?.message || '支付失败')
    }
  } finally {
    operatingOrderId.value = null
  }
}

async function handleShip(row) {
  if (operatingOrderId.value) return
  try {
    await ElMessageBox.confirm(`确定订单「${row.orderNo}」已发货吗？`, '提示')
    operatingOrderId.value = row.id
    await orderApi.ship(row.id)
    ElMessage.success('发货成功')
    fetchOrders()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error(e.response?.data?.message || '发货失败')
    }
  } finally {
    operatingOrderId.value = null
  }
}

async function handleConfirm(row) {
  if (operatingOrderId.value) return
  try {
    await ElMessageBox.confirm(`确定已收到订单「${row.orderNo}」吗？`, '提示')
    operatingOrderId.value = row.id
    await orderApi.confirm(row.id)
    ElMessage.success('确认收货成功')
    fetchOrders()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error(e.response?.data?.message || '确认收货失败')
    }
  } finally {
    operatingOrderId.value = null
  }
}

// 通用售后申请：自动处理单/多商品选择
async function applyAfterSale(row: any, type: 'REFUND' | 'RETURN') {
  const label = type === 'REFUND' ? '退款' : '退货退款'
  const items = (row.orderItems || []).filter((it: any) => it.refundStatus !== 'REFUNDED')
  if (items.length === 0) {
    ElMessage.warning('没有可申请售后的商品')
    return
  }

  // 单商品直接确认，多商品弹窗选择
  let selectedItem: any
  if (items.length === 1) {
    selectedItem = items[0]
  } else {
    // 加载完整订单数据以获取 orderItems
    let fullOrder = row
    if (!row.orderItems || row.orderItems.length === 0) {
      try {
        fullOrder = await orderApi.getById(row.id)
      } catch {
        fullOrder = row
      }
    }
    const fullItems = (fullOrder.orderItems || []).filter((it: any) => it.refundStatus !== 'REFUNDED')

    const radioRef = { value: 'ALL' }
    // 整单售后选项
    const allOption = h(
      'div',
      {
        class: 'select-item-row select-item-all',
        style:
          'display:flex;align-items:center;gap:8px;padding:8px 6px;cursor:pointer;border-bottom:1px solid rgba(255,0,255,0.15);margin-bottom:4px',
        onClick: () => {
          radioRef.value = 'ALL'
        },
      },
      [
        h('input', {
          type: 'radio',
          name: 'afterSaleItem',
          value: 'ALL',
          checked: radioRef.value === 'ALL',
          style: 'accent-color:#ff00ff;cursor:pointer',
          onClick: (e: Event) => e.stopPropagation(),
          onChange: (e: Event) => {
            radioRef.value = (e.target as HTMLInputElement).value as any
          },
        }),
        h('span', { style: 'color:#ff00ff;font-size:13px;font-weight:600' }, '全部商品（整单售后）'),
        h('span', { style: 'color:#39ff14;font-size:12px;margin-left:auto' }, `${fullItems.length} 件商品`),
      ],
    )
    const itemList = [
      allOption,
      ...fullItems.map((item: any) =>
        h(
          'div',
          {
            class: 'select-item-row',
            style: 'display:flex;align-items:center;gap:8px;padding:6px 0;cursor:pointer',
            onClick: () => {
              radioRef.value = item.id
            },
          },
          [
            h('input', {
              type: 'radio',
              name: 'afterSaleItem',
              value: item.id,
              checked: radioRef.value === item.id,
              style: 'accent-color:#ff00ff;cursor:pointer',
              onClick: (e: Event) => e.stopPropagation(),
              onChange: (e: Event) => {
                radioRef.value = (e.target as HTMLInputElement).value as any
              },
            }),
            h('span', { style: 'color:#e0e0e0;font-size:13px' }, item.productName),
            item.specName ? h('span', { style: 'color:#888;font-size:12px' }, `(${item.specName})`) : null,
            h('span', { style: 'color:#39ff14;font-size:12px;margin-left:auto' }, `¥${item.price} × ${item.quantity}`),
          ],
        ),
      ),
    ]

    try {
      await ElMessageBox({
        title: `选择要${label}的商品`,
        message: h('div', { style: 'max-height:280px;overflow-y:auto' }, itemList),
        confirmButtonText: '确认',
        cancelButtonText: '取消',
      })
    } catch {
      return
    }

    if (radioRef.value === 'ALL') {
      selectedItem = null // 整单
    } else {
      selectedItem = fullItems.find((it: any) => String(it.id) === String(radioRef.value))
      if (!selectedItem) {
        ElMessage.warning('未选择商品')
        return
      }
    }
  }

  // 输入原因
  try {
    const { value: reason } = await ElMessageBox.prompt(`请输入${label}原因`, `申请${label}`, {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      inputPlaceholder: '选填...',
      inputType: 'text',
    }).catch(() => ({ value: undefined }))
    if (reason === undefined) return

    let success: boolean
    if (selectedItem) {
      success = await afterSaleStore.createAfterSale({
        orderItemId: selectedItem.id,
        type,
        reason: reason || undefined,
      })
    } else {
      success = await afterSaleStore.createAfterSaleBatch({
        orderId: row.id,
        type,
        reason: reason || undefined,
      })
    }
    if (success) fetchOrders()
  } catch {
    // user cancelled
  }
}

async function handleRefund(row: any) {
  await applyAfterSale(row, 'REFUND')
}

async function handleReturn(row: any) {
  await applyAfterSale(row, 'RETURN')
}

async function handleItemRefund(itemRow: any) {
  const orderStatus = currentOrder.value?.status
  const type = orderStatus === 'PAID' ? 'REFUND' : 'RETURN'
  const label = type === 'REFUND' ? '申请退款' : '申请退货退款'

  try {
    const { value: reason } = await ElMessageBox.prompt(`请输入${label}原因`, label, {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      inputPlaceholder: '选填...',
      inputType: 'text',
    }).catch(() => ({ value: undefined }))
    if (reason === undefined) return

    const success = await afterSaleStore.createAfterSale({
      orderItemId: itemRow.id,
      type,
      reason: reason || undefined,
    })
    if (success) {
      showDetail(currentOrder.value)
    }
  } catch {
    // user cancelled
  }
}

async function handleExport() {
  if (exporting.value) return
  exporting.value = true
  try {
    const blob = await orderApi.exportExcel({
      status: statusFilter.value || undefined,
      orderNo: searchOrderNo.value || undefined,
      username: searchUsername.value || undefined,
    })
    const url = window.URL.createObjectURL(blob as Blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `订单导出_${new Date().toISOString().slice(0, 10)}.xlsx`
    a.click()
    window.URL.revokeObjectURL(url)
    ElMessage.success('导出成功')
  } catch {
    ElMessage.error('导出失败')
  } finally {
    exporting.value = false
  }
}

function handleOrderSelectionChange(selection: any[]) {
  selectedOrderIds.value = selection.map((item: any) => item.id)
}

async function handleDelete(row: any) {
  try {
    await ElMessageBox.confirm(`确定要删除订单「${row.orderNo}」吗？删除后不可恢复。`, '提示', { type: 'warning' })
    const res = await orderApi.batchDelete([row.id])
    ElMessage.success(res || '删除成功')
    fetchOrders()
  } catch (e) {
    if (e !== 'cancel' && e !== 'close') {
      ElMessage.error(e.response?.data?.message || e.message || '删除失败')
    }
  }
}

async function handleBatchDelete() {
  if (selectedOrderIds.value.length === 0) {
    ElMessage.warning('请先选择订单')
    return
  }
  if (batchSubmitting.value) return
  batchSubmitting.value = true
  try {
    await ElMessageBox.confirm(
      `已选 ${selectedOrderIds.value.length} 条订单，仅已取消状态的订单会被删除。确定继续？`,
      '提示',
      { type: 'warning' },
    )
    const res = await orderApi.batchDelete(selectedOrderIds.value)
    ElMessage.success(res || '批量删除成功')
    selectedOrderIds.value = []
    fetchOrders()
  } catch (e) {
    if (e !== 'cancel' && e !== 'close') {
      ElMessage.error(e.response?.data?.message || e.message || '批量删除失败')
    }
  } finally {
    batchSubmitting.value = false
  }
}

onMounted(() => {
  fetchOrders()
})
</script>

<style scoped>
.order-management {
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

/* Search & filter bar */
.filter-tabs {
  margin-bottom: 20px;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
}

.filter-row {
  display: flex;
  gap: 12px;
  align-items: center;
}

.search-input {
  width: 200px;
}

:deep(.search-input .el-input__wrapper) {
  background: var(--glass-bg);
  border: 1px solid var(--border-glow-magenta);
  box-shadow: none;
}

:deep(.search-input .el-input__inner) {
  color: var(--text-secondary);
}

:deep(.el-radio-group) {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

:deep(.el-radio-button__inner) {
  background: var(--glass-bg);
  border: 1px solid var(--border-glow-magenta);
  color: var(--text-muted);
  font-size: 13px;
  border-radius: 20px !important;
  padding: 6px 16px;
  transition: all var(--transition-normal);
}

:deep(.el-radio-button__original-radio:checked + .el-radio-button__inner) {
  background: linear-gradient(135deg, rgba(255, 0, 255, 0.2), rgba(0, 255, 255, 0.2));
  border-color: var(--neon-magenta);
  color: var(--neon-cyan);
  box-shadow: var(--glow-magenta);
}

:deep(.el-radio-button:first-child .el-radio-button__inner) {
  border-left: 1px solid var(--border-glow-magenta);
  border-radius: 20px !important;
}

:deep(.el-radio-button:last-child .el-radio-button__inner) {
  border-radius: 20px !important;
}

:deep(.el-radio-button__inner:hover) {
  color: var(--neon-cyan);
}

/* Amount */
.amount-text {
  color: var(--neon-green);
  font-weight: 600;
  font-family: 'Sora', sans-serif;
}

/* Pagination */
.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
}

/* Batch action bar */
.batch-bar {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  margin-left: 20px;
  padding: 4px 14px;
  background: rgba(255, 0, 255, 0.06);
  border: 1px solid var(--border-glow-magenta);
  border-radius: 8px;
  vertical-align: middle;
}

.batch-hint {
  font-size: 13px;
  color: var(--neon-cyan);
  font-weight: 500;
}

/* Table dark theme */
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

/* Order detail */
.order-detail {
  max-height: 560px;
  overflow-y: auto;
}

.detail-section {
  margin-bottom: 24px;
}

.detail-section:last-child {
  margin-bottom: 0;
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
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.info-item-full {
  grid-column: 1 / -1;
}

.tracking-no {
  color: var(--neon-cyan);
  font-weight: 600;
  font-family: 'Sora', monospace;
}

.info-label {
  font-size: 12px;
  color: var(--text-muted);
}

.info-value {
  font-size: 14px;
  color: var(--text-secondary);
}

/* Items table */
.items-table {
  border: 1px solid var(--border-subtle);
}

:deep(.items-table .el-table__cell) {
  padding: 8px 12px;
}

.item-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.item-thumb {
  width: 40px;
  height: 40px;
  border-radius: 4px;
  border: 1px solid var(--border-subtle);
  flex-shrink: 0;
}

.thumb-placeholder {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--glass-bg);
  border-radius: 4px;
  color: var(--text-muted);
}

.item-cell-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.item-cell-name {
  font-size: 13px;
  color: var(--text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.item-sku-tag {
  margin-top: 4px;
  font-size: 11px;
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
/* 售后商品选择弹窗 — 非 scoped 因为 ElMessageBox 渲染在 body 下 */
.select-item-row:hover {
  background: rgba(255, 0, 255, 0.08);
  border-radius: 6px;
}
.select-item-row input[type='radio'] {
  width: 16px;
  height: 16px;
  margin: 0;
}

/* ElMessageBox 暗色主题 — 匹配订单详情弹窗风格 */
.el-message-box {
  background: var(--bg-darker, #1a1a2e) !important;
  backdrop-filter: blur(20px);
  border: 1px solid var(--border-glow-magenta, rgba(255, 0, 255, 0.3)) !important;
  border-radius: 16px !important;
}
.el-message-box__title {
  color: var(--text-primary, #e0e0e0) !important;
  font-family: 'Sora', sans-serif;
  font-size: 18px !important;
}
.el-message-box__message {
  color: var(--text-secondary, #b0b0b0) !important;
}
.el-message-box__headerbtn .el-message-box__close {
  color: var(--text-muted, #888) !important;
}
.el-message-box__headerbtn .el-message-box__close:hover {
  color: var(--neon-cyan, #00ffff) !important;
}
.el-message-box__input .el-input__wrapper {
  background: var(--glass-bg, rgba(255, 255, 255, 0.05)) !important;
  border: 1px solid var(--border-glow-magenta, rgba(255, 0, 255, 0.3)) !important;
  box-shadow: none !important;
}
.el-message-box__input .el-input__inner {
  color: var(--text-secondary, #b0b0b0) !important;
}
.el-message-box .el-button:not(.el-button--primary) {
  background: var(--glass-bg, rgba(255, 255, 255, 0.05)) !important;
  border: 1px solid var(--border-glow-magenta, rgba(255, 0, 255, 0.3)) !important;
  color: var(--text-secondary, #b0b0b0) !important;
}
</style>
