import api from '../utils/request'

export const categoryApi = {
  tree() {
    return api.get('/categories/tree')
  },
  create(data: { name: string; parentId?: number | null; sortOrder?: number }) {
    return api.post('/categories', data)
  },
  update(id: number, data: { name?: string; sortOrder?: number }) {
    return api.put(`/categories/${id}`, data)
  },
  delete(id: number) {
    return api.delete(`/categories/${id}`)
  },
}

export const productApi = {
  list(params: { keyword?: string; categoryId?: number; status?: string; page: number; size: number }) {
    return api.get('/products', { params })
  },
  search(params: { keyword?: string; categoryId?: number; status?: string; page: number; size: number }) {
    return api.get('/products/search', { params })
  },
  getById(id: number) {
    return api.get(`/products/${id}`)
  },
  create(data: any) {
    return api.post('/products', data)
  },
  update(id: number, data: any) {
    return api.put(`/products/${id}`, data)
  },
  updateStatus(id: number, status: string) {
    return api.patch(`/products/${id}/status`, { status })
  },
  batchUpdateStatus(ids: number[], status: string) {
    return api.patch('/products/batch-status', { ids, status })
  },
  delete(id: number) {
    return api.delete(`/products/${id}`)
  },
}
