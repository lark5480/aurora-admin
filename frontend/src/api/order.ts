import api from '../utils/request'

export const cartApi = {
  list() {
    return api.get('/cart')
  },
  add(data: { productId: number; skuId?: number; quantity: number }) {
    return api.post('/cart', data)
  },
  update(id: number, data: { quantity: number }) {
    return api.put(`/cart/${id}`, data)
  },
  switchSku(id: number, skuId: number | null) {
    return api.patch(`/cart/${id}/sku`, { skuId })
  },
  remove(id: number) {
    return api.delete(`/cart/${id}`)
  },
  clear() {
    return api.delete('/cart')
  },
}

export const orderApi = {
  create(data: {
    cartItemIds: number[]
    receiverName: string
    receiverPhone: string
    receiverAddress: string
    remark?: string
  }, idempotentKey: string) {
    return api.post('/orders', data, { headers: { 'Idempotent-Key': idempotentKey } })
  },
  list(params: { page: number; size: number; status?: string; orderNo?: string; username?: string }) {
    return api.get('/orders', { params })
  },
  exportExcel(params: { status?: string; orderNo?: string; username?: string }) {
    return api.get('/orders/export', { params, responseType: 'blob' })
  },
  getById(id: number) {
    return api.get(`/orders/${id}`)
  },
  cancel(id: number) {
    return api.patch(`/orders/${id}/cancel`)
  },
  pay(orderNo: string, payMethod: string = 'BALANCE') {
    return api.post('/payments', { orderNo, payMethod })
  },
  ship(id: number) {
    return api.patch(`/orders/${id}/ship`)
  },
  confirm(id: number) {
    return api.patch(`/orders/${id}/confirm`)
  },
  batchDelete(ids: number[]) {
    return api.delete('/orders/batch', { data: { ids } })
  },
}
