import api from '../utils/request'

interface RoleItem {
  id: number
  name: string
  code: string
  description: string
  status: number
  [key: string]: unknown
}

interface PageResult<T> {
  list: T[]
  total: number
}

interface PageParams {
  page?: number
  size?: number
  [key: string]: unknown
}

export const roleApi = {
  list(params: PageParams): Promise<PageResult<RoleItem>> {
    return api.get('/roles', { params })
  },
  getById(id: number): Promise<RoleItem> {
    return api.get(`/roles/${id}`)
  },
  create(data: Partial<RoleItem>): Promise<unknown> {
    return api.post('/roles', data)
  },
  update(id: number, data: Partial<RoleItem>): Promise<unknown> {
    return api.put(`/roles/${id}`, data)
  },
  delete(id: number): Promise<unknown> {
    return api.delete(`/roles/${id}`)
  },
  getMenus(id: number): Promise<number[]> {
    return api.get(`/roles/${id}/menus`)
  },
  assignMenus(id: number, menuIds: number[]): Promise<unknown> {
    return api.put(`/roles/${id}/menus`, menuIds)
  },
}
