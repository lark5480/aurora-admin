import { defineStore } from 'pinia'
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { categoryApi, productApi } from '../api/product'

export interface CategoryNode {
  id: number
  name: string
  parentId: number | null
  sortOrder: number
  children?: CategoryNode[]
}

export interface ProductItem {
  id: number
  name: string
  description?: string
  categoryId?: number
  categoryName?: string
  coverImage?: string
  price: number
  stock: number
  status: string
  skus?: SkuItem[]
  createTime?: string
}

export interface SkuItem {
  id?: number
  specName: string
  price: number
  stock: number
}

interface PageResult {
  list?: ProductItem[]
  records?: ProductItem[]
  total?: number
}

interface EsSearchResult {
  content?: ProductItem[]
  totalElements?: number
}

export const useProductStore = defineStore('product', () => {
  const categoryTree = ref<CategoryNode[]>([])
  const productList = ref<ProductItem[]>([])
  const total = ref(0)
  const loading = ref(false)

  async function fetchCategoryTree() {
    try {
      const res = await categoryApi.tree()
      categoryTree.value = (res as unknown as CategoryNode[]) || []
    } catch {
      ElMessage.error('获取分类树失败')
    }
  }

  async function fetchProducts(params: {
    keyword?: string
    categoryId?: number
    status?: string
    page: number
    size: number
  }) {
    loading.value = true
    try {
      // 有关键词走 ES 全文搜索（带筛选），纯筛选走 MySQL
      if (params.keyword && params.keyword.trim()) {
        const res = (await productApi.search({
          keyword: params.keyword.trim(),
          categoryId: params.categoryId || undefined,
          status: params.status || undefined,
          page: params.page,
          size: params.size,
        })) as unknown as EsSearchResult
        productList.value = res?.content || []
        total.value = res?.totalElements || 0
      } else {
        const res = (await productApi.list(params)) as unknown as PageResult
        const data = res?.list || res?.records || []
        productList.value = data as ProductItem[]
        total.value = res?.total || data.length
      }
    } catch {
      ElMessage.error('获取商品列表失败')
    } finally {
      loading.value = false
    }
  }

  async function createProduct(data: any) {
    try {
      await productApi.create(data)
      ElMessage.success('添加成功')
      return true
    } catch (e: any) {
      ElMessage.error(e?.message || '添加失败')
      return false
    }
  }

  async function updateProduct(id: number, data: any) {
    try {
      await productApi.update(id, data)
      ElMessage.success('更新成功')
      return true
    } catch (e: any) {
      ElMessage.error(e?.message || '更新失败')
      return false
    }
  }

  async function updateProductStatus(id: number, status: string) {
    try {
      await productApi.updateStatus(id, status)
      ElMessage.success('状态更新成功')
      return true
    } catch (e: any) {
      ElMessage.error(e?.message || '状态更新失败')
      return false
    }
  }

  async function deleteProduct(id: number) {
    try {
      await productApi.delete(id)
      ElMessage.success('删除成功')
      return true
    } catch (e: any) {
      ElMessage.error(e?.message || '删除失败')
      return false
    }
  }

  return {
    categoryTree,
    productList,
    total,
    loading,
    fetchCategoryTree,
    fetchProducts,
    createProduct,
    updateProduct,
    updateProductStatus,
    deleteProduct,
  }
})
