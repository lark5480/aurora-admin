import { defineStore } from 'pinia'
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { cartApi, orderApi } from '../api/order'

export interface SkuOption {
  id: number
  specName: string
  price: number
  stock: number
}

export interface CartItem {
  id: number
  productId: number
  productName: string
  skuId?: number
  specName?: string
  coverImage?: string
  price: number
  stock: number
  quantity: number
  status: string
  subtotal?: number
  availableSkus?: SkuOption[]
}

export interface OrderItem {
  id: number
  orderId: number
  productId: number
  productName: string
  specName?: string
  coverImage?: string
  price: number
  quantity: number
  subtotal: number
}

export interface Order {
  id: number
  orderNo: string
  totalAmount: number
  status: string
  receiverName: string
  receiverPhone: string
  receiverAddress: string
  remark?: string
  trackingNumber?: string
  orderItems?: OrderItem[]
  createTime?: string
}

interface OrderPageResult {
  list?: Order[]
  records?: Order[]
  total?: number
}

export const useOrderStore = defineStore('order', () => {
  const cartItems = ref<CartItem[]>([])
  const cartLoading = ref(false)
  const orderList = ref<Order[]>([])
  const orderTotal = ref(0)
  const orderLoading = ref(false)

  async function fetchCart() {
    cartLoading.value = true
    try {
      const res = (await cartApi.list()) as unknown as CartItem[]
      cartItems.value = Array.isArray(res) ? res : []
    } catch {
      ElMessage.error('获取购物车失败')
    } finally {
      cartLoading.value = false
    }
  }

  async function addToCart(data: { productId: number; skuId?: number; quantity: number }) {
    try {
      await cartApi.add(data)
      ElMessage.success('已加入购物车')
      return true
    } catch (e: unknown) {
      ElMessage.error((e as Error)?.message || '添加购物车失败')
      return false
    }
  }

  async function updateCartQty(id: number, quantity: number) {
    try {
      await cartApi.update(id, { quantity })
      // Update local state optimistically
      const item = cartItems.value.find((i) => i.id === id)
      if (item) {
        item.quantity = quantity
      }
      return true
    } catch (e: unknown) {
      ElMessage.error((e as Error)?.message || '更新数量失败')
      return false
    }
  }

  async function removeFromCart(id: number) {
    try {
      await cartApi.remove(id)
      cartItems.value = cartItems.value.filter((i) => i.id !== id)
      ElMessage.success('已移除')
      return true
    } catch (e: unknown) {
      ElMessage.error((e as Error)?.message || '移除失败')
      return false
    }
  }

  async function clearCart() {
    try {
      await cartApi.clear()
      cartItems.value = []
      ElMessage.success('购物车已清空')
      return true
    } catch (e: unknown) {
      ElMessage.error((e as Error)?.message || '清空失败')
      return false
    }
  }

  async function fetchOrders(params: {
    page: number
    size: number
    status?: string
    orderNo?: string
    username?: string
  }) {
    orderLoading.value = true
    try {
      const res = (await orderApi.list(params)) as unknown as OrderPageResult
      const data = res?.list || res?.records || []
      orderList.value = data as Order[]
      orderTotal.value = res?.total || data.length
    } catch {
      ElMessage.error('获取订单列表失败')
    } finally {
      orderLoading.value = false
    }
  }

  async function createOrder(data: {
    cartItemIds: number[]
    receiverName: string
    receiverPhone: string
    receiverAddress: string
    remark?: string
  }) {
    try {
      const idempotentKey = crypto.randomUUID()
      await orderApi.create(data, idempotentKey)
      // 下单成功后重新拉购物车（不再直接清空，因为可能只结算了部分商品）
      await fetchCart()
      ElMessage.success('下单成功')
      return true
    } catch (e: unknown) {
      ElMessage.error((e as Error)?.message || '下单失败')
      return false
    }
  }

  async function cancelOrder(id: number) {
    try {
      await orderApi.cancel(id)
      ElMessage.success('订单已取消')
      return true
    } catch (e: unknown) {
      ElMessage.error((e as Error)?.message || '取消失败')
      return false
    }
  }

  return {
    cartItems,
    cartLoading,
    orderList,
    orderTotal,
    orderLoading,
    fetchCart,
    addToCart,
    updateCartQty,
    removeFromCart,
    clearCart,
    fetchOrders,
    createOrder,
    cancelOrder,
  }
})
