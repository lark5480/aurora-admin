<template>
  <div class="after-sale-management">
    <div class="page-header">
      <h1 class="page-title">售后管理</h1>
      <p class="page-sub">审核用户的退货退款申请</p>
    </div>

    <div class="glass-card">
      <div class="filter-tabs">
        <div class="filter-row">
          <el-input
            v-model="searchAfterSaleNo"
            placeholder="搜索售后单号"
            clearable
            class="search-input"
            @change="handleFilterChange"
          />
          <el-input
            v-model="searchOrderNo"
            placeholder="搜索订单号"
            clearable
            class="search-input"
            @change="handleFilterChange"
          />
        </div>
        <el-radio-group v-model="statusFilter" @change="handleFilterChange">
          <el-radio-button value="">全部</el-radio-button>
          <el-radio-button value="APPLIED">待审核</el-radio-button>
          <el-radio-button value="COMPLETED">已退款</el-radio-button>
          <el-radio-button value="REJECTED">已驳回</el-radio-button>
        </el-radio-group>
      </div>

      <el-table
        v-loading="afterSaleStore.afterSaleLoading"
        :data="afterSaleStore.afterSaleList"
        stripe
        class="order-table"
      >
        <el-table-column label="售后单号" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">
            <el-button link type="primary" @click="showDetail(row)">{{ row.afterSaleNo }}</el-button>
          </template>
        </el-table-column>
        <el-table-column label="关联订单" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">
            <el-button link type="primary" @click="showOrderDetail(row)">{{ row.orderNo }}</el-button>
          </template>
        </el-table-column>
        <el-table-column prop="productName" label="商品" min-width="160">
          <template #default="{ row }">
            <div class="product-cell">
              <span>{{ row.productName }}</span>
              <el-tag v-if="row.specName" size="small" type="warning" effect="dark" class="spec-tag">
                {{ row.specName }}
              </el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="类型" width="100">
          <template #default="{ row }">
            <el-tag :type="row.type === 'REFUND' ? 'warning' : 'primary'" size="small">
              {{ row.type === 'REFUND' ? '仅退款' : '退货退款' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="退款金额" width="120">
          <template #default="{ row }">
            <span class="amount-text">¥{{ row.refundAmount }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" size="small">
              {{ statusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="reason" label="申请原因" min-width="150" show-overflow-tooltip>
          <template #default="{ row }">
            <span v-if="row.reason">{{ row.reason }}</span>
            <span v-else class="text-muted">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="申请时间" width="180" />
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="showDetail(row)"> 详情 </el-button>
            <el-button
              v-if="isAdmin && row.status === 'APPLIED'"
              link
              type="success"
              size="small"
              @click="handleApprove(row)"
            >
              通过
            </el-button>
            <el-button
              v-if="isAdmin && row.status === 'APPLIED'"
              link
              type="danger"
              size="small"
              @click="handleReject(row)"
            >
              驳回
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="afterSaleStore.afterSaleTotal"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </div>

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailVisible" title="售后详情" width="560px" :close-on-click-modal="false">
      <div v-if="detailRow" class="detail-body">
        <div class="detail-section">
          <h4 class="section-title">售后信息</h4>
          <div class="info-grid">
            <div class="info-item">
              <span class="info-label">售后单号</span>
              <span class="info-value">{{ detailRow.afterSaleNo }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">售后状态</span>
              <el-tag :type="statusTagType(detailRow.status)" size="small">
                {{ statusLabel(detailRow.status) }}
              </el-tag>
            </div>
            <div class="info-item">
              <span class="info-label">售后类型</span>
              <el-tag :type="detailRow.type === 'REFUND' ? 'warning' : 'primary'" size="small">
                {{ detailRow.type === 'REFUND' ? '仅退款' : '退货退款' }}
              </el-tag>
            </div>
            <div class="info-item">
              <span class="info-label">退款金额</span>
              <span class="info-value amount-text">¥{{ detailRow.refundAmount }}</span>
            </div>
            <div class="info-item info-item-full">
              <span class="info-label">申请原因</span>
              <span class="info-value">{{ detailRow.reason || '-' }}</span>
            </div>
          </div>
        </div>

        <div class="detail-section">
          <h4 class="section-title">关联信息</h4>
          <div class="info-grid">
            <div class="info-item">
              <span class="info-label">关联订单</span>
              <span class="info-value">{{ detailRow.orderNo }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">商品名称</span>
              <span class="info-value">{{ detailRow.productName }}</span>
            </div>
            <div v-if="detailRow.specName" class="info-item">
              <span class="info-label">规格</span>
              <span class="info-value">{{ detailRow.specName }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">申请时间</span>
              <span class="info-value">{{ detailRow.createTime }}</span>
            </div>
          </div>
        </div>

        <div v-if="detailRow.status !== 'APPLIED'" class="detail-section">
          <h4 class="section-title">审核信息</h4>
          <div class="info-grid">
            <div class="info-item info-item-full">
              <span class="info-label">审核备注</span>
              <span class="info-value">{{ detailRow.reviewRemark || '-' }}</span>
            </div>
            <div v-if="detailRow.reviewTime" class="info-item">
              <span class="info-label">审核时间</span>
              <span class="info-value">{{ detailRow.reviewTime }}</span>
            </div>
          </div>
        </div>
      </div>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 订单详情弹窗 -->
    <el-dialog v-model="orderDetailVisible" title="订单详情" width="680px" :close-on-click-modal="false">
      <div v-if="currentOrder" class="order-detail">
        <div class="detail-section">
          <h4 class="section-title">订单信息</h4>
          <div class="info-grid">
            <div class="info-item">
              <span class="info-label">订单编号</span>
              <span class="info-value">{{ currentOrder.orderNo }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">订单状态</span>
              <el-tag :type="orderStatusTagType(currentOrder.status)" size="small">
                {{ orderStatusLabel(currentOrder.status) }}
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

        <div class="detail-section">
          <h4 class="section-title">收货信息</h4>
          <div class="info-grid">
            <div class="info-item">
              <span class="info-label">收货人</span>
              <span class="info-value">{{ currentOrder.receiverName || '-' }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">联系电话</span>
              <span class="info-value">{{ currentOrder.receiverPhone || '-' }}</span>
            </div>
            <div class="info-item info-item-full">
              <span class="info-label">收货地址</span>
              <span class="info-value">{{ currentOrder.receiverAddress || '-' }}</span>
            </div>
          </div>
        </div>

        <div v-if="currentOrder.orderItems && currentOrder.orderItems.length > 0" class="detail-section">
          <h4 class="section-title">商品明细</h4>
          <el-table :data="currentOrder.orderItems" size="small" class="items-table">
            <el-table-column label="商品" min-width="240">
              <template #default="{ row: item }">
                <div class="item-cell">
                  <el-image :src="resolveImageUrl(item.coverImage || '')" class="item-thumb" fit="cover">
                    <template #error>
                      <div class="thumb-placeholder">
                        <el-icon :size="14"><Picture /></el-icon>
                      </div>
                    </template>
                  </el-image>
                  <div class="item-cell-info">
                    <span class="item-cell-name">{{ item.productName }}</span>
                    <el-tag v-if="item.specName" size="small" type="warning" effect="dark">{{ item.specName }}</el-tag>
                  </div>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="单价" width="100">
              <template #default="{ row: item }">
                <span class="amount-text">¥{{ item.price }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="quantity" label="数量" width="60" />
            <el-table-column label="小计" width="100">
              <template #default="{ row: item }">
                <span class="amount-text">¥{{ (item.price * item.quantity).toFixed(2) }}</span>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </div>
      <template #footer>
        <el-button @click="orderDetailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { ElMessageBox, ElMessage } from 'element-plus'
import { Picture } from '@element-plus/icons-vue'
import { useAfterSaleStore } from '../../stores/afterSaleStore'
import { useUserStore } from '../../stores/user'
import { orderApi } from '../../api/order'
import { resolveImageUrl } from '../../utils/image'

const userStore = useUserStore()
const afterSaleStore = useAfterSaleStore()
const isAdmin = computed(() => userStore.hasRole('SUPER_ADMIN') || userStore.hasRole('ADMIN'))

const currentPage = ref(1)
const pageSize = ref(10)
const statusFilter = ref('')
const searchAfterSaleNo = ref('')
const searchOrderNo = ref('')
const detailVisible = ref(false)
const detailRow = ref<any>(null)

const orderDetailVisible = ref(false)
const currentOrder = ref<any>(null)

function statusTagType(status: string) {
  const map: Record<string, string> = {
    APPLIED: 'warning',
    COMPLETED: 'success',
    REJECTED: 'danger',
  }
  return map[status] || 'info'
}

function statusLabel(status: string) {
  const map: Record<string, string> = {
    APPLIED: '待审核',
    COMPLETED: '已退款',
    REJECTED: '已驳回',
  }
  return map[status] || status
}

async function fetchData() {
  await afterSaleStore.fetchAfterSales({
    page: currentPage.value,
    size: pageSize.value,
    status: statusFilter.value || undefined,
    afterSaleNo: searchAfterSaleNo.value || undefined,
    orderNo: searchOrderNo.value || undefined,
  })
}

function handleFilterChange() {
  currentPage.value = 1
  fetchData()
}

function handleSizeChange(newSize: number) {
  pageSize.value = newSize
  currentPage.value = 1
  fetchData()
}

function handleCurrentChange() {
  fetchData()
}

function showDetail(row: any) {
  detailRow.value = row
  detailVisible.value = true
}

async function showOrderDetail(row: any) {
  try {
    const res = await orderApi.getById(row.orderId)
    currentOrder.value = res
  } catch {
    currentOrder.value = { orderNo: row.orderNo }
  }
  orderDetailVisible.value = true
}

function orderStatusTagType(status: string) {
  const map: Record<string, string> = {
    PENDING: 'warning',
    PAID: 'primary',
    SHIPPED: 'success',
    COMPLETED: '',
    CANCELLED: 'info',
    REFUNDING: 'danger',
    REFUNDED: 'danger',
  }
  return map[status] || 'info'
}

function orderStatusLabel(status: string) {
  const map: Record<string, string> = {
    PENDING: '待支付',
    PAID: '已支付',
    SHIPPED: '已发货',
    COMPLETED: '已完成',
    CANCELLED: '已取消',
    REFUNDING: '退款中',
    REFUNDED: '已退款',
  }
  return map[status] || status
}

async function handleApprove(row: any) {
  try {
    const { value: remark } = await ElMessageBox.prompt('通过后将立即退款给用户，确定通过？', '审核通过', {
      confirmButtonText: '确认通过',
      cancelButtonText: '取消',
      inputPlaceholder: '审核备注（选填）...',
      inputType: 'text',
    }).catch(() => ({ value: undefined }))
    if (remark === undefined) return

    const success = await afterSaleStore.approveAfterSale(row.id, remark || undefined)
    if (success) fetchData()
  } catch {
    // cancelled
  }
}

async function handleReject(row: any) {
  try {
    const { value: remark } = await ElMessageBox.prompt('驳回后订单将恢复原状态，确定驳回？', '驳回申请', {
      confirmButtonText: '确认驳回',
      cancelButtonText: '取消',
      inputPlaceholder: '驳回原因（必填）...',
      inputType: 'text',
    }).catch(() => ({ value: undefined }))
    if (remark === undefined) return
    if (!remark.trim()) {
      ElMessage.warning('请输入驳回原因')
      return
    }

    const success = await afterSaleStore.rejectAfterSale(row.id, remark)
    if (success) fetchData()
  } catch {
    // cancelled
  }
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.after-sale-management {
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

.amount-text {
  color: var(--neon-green);
  font-weight: 600;
  font-family: 'Sora', sans-serif;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
}

.text-muted {
  color: var(--text-muted);
  font-size: 13px;
}

.product-cell {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.spec-tag {
  width: fit-content;
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

/* Detail dialog */
.detail-body {
  max-height: 480px;
  overflow-y: auto;
}

.detail-section {
  margin-bottom: 20px;
}

.detail-section:last-child {
  margin-bottom: 0;
}

.section-title {
  font-family: 'Sora', sans-serif;
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 10px;
  padding-bottom: 6px;
  border-bottom: 1px solid var(--border-subtle);
}

.info-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.info-item-full {
  grid-column: 1 / -1;
}

.info-label {
  font-size: 12px;
  color: var(--text-muted);
}

.info-value {
  font-size: 14px;
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

/* Order detail dialog */
.order-detail {
  max-height: 500px;
  overflow-y: auto;
}

.tracking-no {
  color: var(--neon-cyan);
  font-family: monospace;
}

.items-table {
  margin-top: 8px;
}

.item-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.item-thumb {
  width: 40px;
  height: 40px;
  border-radius: 6px;
  flex-shrink: 0;
}

.thumb-placeholder {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--glass-bg);
  border-radius: 6px;
  color: var(--text-muted);
}

.item-cell-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.item-cell-name {
  font-size: 13px;
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

<style>
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
