import api from '../utils/request'

export interface AddressData {
  receiverName: string
  receiverPhone: string
  province: string
  city: string
  district: string
  detail: string
  isDefault?: boolean
}

export const addressApi = {
  list() {
    return api.get('/addresses')
  },
  create(data: AddressData) {
    return api.post('/addresses', data)
  },
  update(id: number, data: Partial<AddressData>) {
    return api.put(`/addresses/${id}`, data)
  },
  delete(id: number) {
    return api.delete(`/addresses/${id}`)
  },
  setDefault(id: number) {
    return api.patch(`/addresses/${id}/default`)
  },
}
