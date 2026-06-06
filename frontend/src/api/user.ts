import api from '../utils/request'

interface UserQuery {
  page?: number
  size?: number
  keyword?: string
  status?: string
  deptId?: number
}

interface UserForm {
  username: string
  password?: string
  email: string
  nickname?: string
  role?: string
  status?: string
  deptId?: number
}

export const userApi = {
  list(params: UserQuery) {
    return api.get('/users', { params })
  },
  getById(id: number) {
    return api.get(`/users/${id}`)
  },
  create(data: UserForm) {
    return api.post('/users', data)
  },
  update(id: number, data: Partial<UserForm>) {
    return api.put(`/users/${id}`, data)
  },
  delete(id: number) {
    return api.delete(`/users/${id}`)
  },
  updateStatus(id: number, status: string) {
    return api.put(`/users/${id}/status?status=${status}`)
  },
  getRoles(id: number) {
    return api.get(`/users/${id}/roles`)
  },
  assignRoles(id: number, roleIds: number[]) {
    return api.put(`/users/${id}/roles`, roleIds)
  },
  getProfile() {
    return api.get('/users/profile')
  },
  updateProfile(data: { nickname?: string; email?: string; avatar?: string }) {
    return api.put('/users/profile', data)
  },
  changePassword(data: { oldPassword: string; newPassword: string }) {
    return api.put('/users/password', data)
  },
}
