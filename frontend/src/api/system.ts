import api from '../utils/request'

interface ConfigItem {
  key: string
  value: string
  description: string
  [key: string]: unknown
}

export const systemApi = {
  getConfigs(): Promise<ConfigItem[]> {
    return api.get('/config')
  },
  getAllConfigs(): Promise<ConfigItem[]> {
    return api.get('/config/all')
  },
  updateConfig(key: string, data: Partial<ConfigItem>): Promise<unknown> {
    return api.put(`/config/${key}`, data)
  },
  createConfig(data: Partial<ConfigItem>): Promise<unknown> {
    return api.post('/config', data)
  },
  deleteConfig(key: string): Promise<unknown> {
    return api.delete(`/config/${key}`)
  },
}
