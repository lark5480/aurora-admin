import api from '../utils/request'

interface DeptTreeNode {
  id: number
  name: string
  parentId: number
  children?: DeptTreeNode[]
  [key: string]: unknown
}

export const deptApi = {
  tree(): Promise<DeptTreeNode[]> {
    return api.get('/depts/tree')
  },
  list(): Promise<DeptTreeNode[]> {
    return api.get('/depts')
  },
  getById(id: number): Promise<DeptTreeNode> {
    return api.get(`/depts/${id}`)
  },
  create(data: Partial<DeptTreeNode>): Promise<unknown> {
    return api.post('/depts', data)
  },
  update(id: number, data: Partial<DeptTreeNode>): Promise<unknown> {
    return api.put(`/depts/${id}`, data)
  },
  delete(id: number): Promise<unknown> {
    return api.delete(`/depts/${id}`)
  },
}
