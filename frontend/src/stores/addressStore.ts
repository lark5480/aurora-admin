import { defineStore } from 'pinia'
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { addressApi, type AddressData } from '../api/address'

export interface Address {
  id: number
  receiverName: string
  receiverPhone: string
  province: string
  city: string
  district: string
  detail: string
  isDefault: boolean
  createTime?: string
}

export const useAddressStore = defineStore('address', () => {
  const addresses = ref<Address[]>([])
  const loading = ref(false)

  async function fetchAddresses() {
    loading.value = true
    try {
      const res = (await addressApi.list()) as unknown
      // addressApi.list() 经拦截器处理后已直接返回 data（数组）
      // 但为安全起见，判断一下是否是数组
      if (Array.isArray(res)) {
        addresses.value = res as Address[]
      } else if (res && typeof res === 'object' && 'data' in (res as object)) {
        // 拦截器未处理，整个响应对象回来
        addresses.value = (res as { data: Address[] }).data || []
      } else {
        addresses.value = []
      }
    } catch {
      ElMessage.error('获取地址列表失败')
    } finally {
      loading.value = false
    }
  }

  async function createAddress(data: AddressData): Promise<boolean> {
    try {
      await addressApi.create(data)
      ElMessage.success('地址添加成功')
      await fetchAddresses()
      return true
    } catch (e: unknown) {
      ElMessage.error((e as Error)?.message || '添加地址失败')
      return false
    }
  }

  async function updateAddress(id: number, data: Partial<AddressData>): Promise<boolean> {
    try {
      await addressApi.update(id, data)
      ElMessage.success('地址更新成功')
      await fetchAddresses()
      return true
    } catch (e: unknown) {
      ElMessage.error((e as Error)?.message || '更新地址失败')
      return false
    }
  }

  async function deleteAddress(id: number): Promise<boolean> {
    try {
      await addressApi.delete(id)
      addresses.value = addresses.value.filter((a) => a.id !== id)
      ElMessage.success('地址已删除')
      return true
    } catch (e: unknown) {
      ElMessage.error((e as Error)?.message || '删除地址失败')
      return false
    }
  }

  async function setDefault(id: number): Promise<boolean> {
    try {
      await addressApi.setDefault(id)
      addresses.value = addresses.value.map((a) => ({
        ...a,
        isDefault: a.id === id,
      }))
      ElMessage.success('已设为默认地址')
      return true
    } catch (e: unknown) {
      ElMessage.error((e as Error)?.message || '设置默认地址失败')
      return false
    }
  }

  return {
    addresses,
    loading,
    fetchAddresses,
    createAddress,
    updateAddress,
    deleteAddress,
    setDefault,
  }
})
