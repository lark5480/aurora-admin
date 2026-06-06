import api from '../utils/request'

interface MenuItem {
  id: number
  name: string
  path: string
  component: string
  menuType: number
  parentId: number
  icon: string
  sort: number
  children?: MenuItem[]
  [key: string]: unknown
}

export const menuApi = {
  tree(): Promise<MenuItem[]> {
    return api.get('/menus/tree')
  },
  myMenus(): Promise<MenuItem[]> {
    return api.get('/menus/my')
  },
  myPermissions(): Promise<string[]> {
    return api.get('/menus/my/permissions')
  },
  list(): Promise<MenuItem[]> {
    return api.get('/menus')
  },
  getById(id: number): Promise<MenuItem> {
    return api.get(`/menus/${id}`)
  },
  create(data: Partial<MenuItem>): Promise<unknown> {
    return api.post('/menus', data)
  },
  update(id: number, data: Partial<MenuItem>): Promise<unknown> {
    return api.put(`/menus/${id}`, data)
  },
  delete(id: number): Promise<unknown> {
    return api.delete(`/menus/${id}`)
  },
}
