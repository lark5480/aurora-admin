import api from '../utils/request'

export const afterSaleApi = {
  create(data: { orderItemId: number; type: string; reason?: string }) {
    return api.post('/after-sales', data)
  },

  createBatch(data: { orderId: number; type: string; reason?: string }) {
    return api.post('/after-sales/batch', data)
  },

  list(params: {
    page: number
    size: number
    orderId?: number
    status?: string
    afterSaleNo?: string
    orderNo?: string
  }) {
    return api.get('/after-sales', { params })
  },

  getById(id: number) {
    return api.get(`/after-sales/${id}`)
  },

  approve(id: number, remark?: string) {
    return api.patch(`/after-sales/${id}/approve`, { remark: remark || '' })
  },

  reject(id: number, remark?: string) {
    return api.patch(`/after-sales/${id}/reject`, { remark: remark || '' })
  },
}
