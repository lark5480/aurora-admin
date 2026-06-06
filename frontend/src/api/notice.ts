import api from '../utils/request'

interface NoticeItem {
  id: number
  title: string
  content: string
  noticeType: string
  targetType: string
  status: number
  priority: number
  expireTime: string | null
  createTime: string
  createBy: string
  [key: string]: unknown
}

interface PageResult<T> {
  list: T[]
  total: number
  [key: string]: unknown
}

interface PageParams {
  page?: number
  size?: number
  title?: string
  status?: number
  [key: string]: unknown
}

export const noticeApi = {
  getVisibleNotices(): Promise<NoticeItem[]> {
    return api.get('/notices')
  },
  getManageList(params: PageParams): Promise<PageResult<NoticeItem>> {
    return api.get('/notices/manage', { params })
  },
  getById(id: number): Promise<NoticeItem> {
    return api.get(`/notices/${id}`)
  },
  create(data: Partial<NoticeItem>): Promise<unknown> {
    return api.post('/notices', data)
  },
  update(id: number, data: Partial<NoticeItem>): Promise<unknown> {
    return api.put(`/notices/${id}`, data)
  },
  delete(id: number): Promise<unknown> {
    return api.delete(`/notices/${id}`)
  },
  publish(id: number): Promise<unknown> {
    return api.put(`/notices/${id}/publish`)
  },
  withdraw(id: number): Promise<unknown> {
    return api.put(`/notices/${id}/withdraw`)
  },
}
