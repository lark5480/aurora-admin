import { defineStore } from 'pinia'
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { afterSaleApi } from '../api/after-sale'

export interface AfterSaleRecord {
  id: number
  afterSaleNo: string
  orderId: number
  orderNo: string
  orderItemId: number
  type: string
  reason?: string
  refundAmount: number
  status: string
  productName: string
  specName: string
  reviewRemark?: string
  reviewTime?: string
  createTime: string
}

export const useAfterSaleStore = defineStore('afterSale', () => {
  const afterSaleList = ref<AfterSaleRecord[]>([])
  const afterSaleTotal = ref(0)
  const afterSaleLoading = ref(false)

  async function fetchAfterSales(params: {
    page: number
    size: number
    orderId?: number
    status?: string
    afterSaleNo?: string
    orderNo?: string
  }) {
    afterSaleLoading.value = true
    try {
      const res = (await afterSaleApi.list(params)) as any
      const data = res?.list || res?.records || []
      afterSaleList.value = data as AfterSaleRecord[]
      afterSaleTotal.value = res?.total || data.length
    } catch {
      ElMessage.error('获取售后列表失败')
    } finally {
      afterSaleLoading.value = false
    }
  }

  async function createAfterSale(data: { orderItemId: number; type: string; reason?: string }) {
    try {
      await afterSaleApi.create(data)
      ElMessage.success('售后申请已提交，等待管理员审核')
      return true
    } catch (e: unknown) {
      const msg = (e as any)?.response?.data?.message || (e as Error)?.message || '售后申请失败'
      ElMessage.error(msg)
      return false
    }
  }

  async function createAfterSaleBatch(data: { orderId: number; type: string; reason?: string }) {
    try {
      await afterSaleApi.createBatch(data)
      ElMessage.success('整单售后申请已提交，等待管理员审核')
      return true
    } catch (e: unknown) {
      const msg = (e as any)?.response?.data?.message || (e as Error)?.message || '整单售后申请失败'
      ElMessage.error(msg)
      return false
    }
  }

  async function approveAfterSale(id: number, remark?: string) {
    try {
      await afterSaleApi.approve(id, remark)
      ElMessage.success('审核通过，退款已生效')
      return true
    } catch (e: unknown) {
      const msg = (e as any)?.response?.data?.message || (e as Error)?.message || '审核失败'
      ElMessage.error(msg)
      return false
    }
  }

  async function rejectAfterSale(id: number, remark?: string) {
    try {
      await afterSaleApi.reject(id, remark)
      ElMessage.success('已驳回')
      return true
    } catch (e: unknown) {
      const msg = (e as any)?.response?.data?.message || (e as Error)?.message || '驳回失败'
      ElMessage.error(msg)
      return false
    }
  }

  return {
    afterSaleList,
    afterSaleTotal,
    afterSaleLoading,
    fetchAfterSales,
    createAfterSale,
    createAfterSaleBatch,
    approveAfterSale,
    rejectAfterSale,
  }
})
