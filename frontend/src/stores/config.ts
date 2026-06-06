import { defineStore } from 'pinia'
import { ref } from 'vue'
import api from '../utils/request'

export const useConfigStore = defineStore('config', () => {
  const configs = ref<Record<string, string>>({})
  const loaded = ref(false)

  async function fetchConfigs(): Promise<void> {
    if (loaded.value) return
    try {
      const data: any = await api.get('/config/public')
      configs.value = (data || {}) as Record<string, string>
      loaded.value = true
    } catch (e) {
      console.error('[ConfigStore] 加载系统配置失败:', e)
    }
  }

  function get(key: string, defaultValue: string = ''): string {
    const val = configs.value[key]
    return val !== undefined && val !== null ? val : defaultValue
  }

  function getBool(key: string, defaultValue: boolean = true): boolean {
    const val = get(key, String(defaultValue))
    return val === 'true' || val === '1'
  }

  return { configs, loaded, fetchConfigs, get, getBool }
})
