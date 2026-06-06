<template>
  <div class="home-content">
    <!-- Welcome section -->
    <div class="welcome-section">
      <h1 class="welcome-text">{{ greeting }}，{{ userStore.username }}，欢迎回来</h1>
    </div>

    <!-- Notice banner -->
    <div v-if="configStore.get('notice.banner')" class="notice-banner">
      <el-alert :title="configStore.get('notice.banner')" type="info" show-icon :closable="false" />
    </div>

    <!-- Main content area with two-column layout -->
    <div class="main-content">
      <!-- Left: Product browsing area -->
      <div class="product-area">
        <div class="section-header">
          <h2 class="section-title">热门商品</h2>
          <el-button text type="primary" class="browse-all-btn" @click="goProducts">
            浏览全部
            <el-icon><ArrowRight /></el-icon>
          </el-button>
        </div>

        <div v-if="loading" class="product-grid">
          <div v-for="i in 4" :key="i" class="product-card product-card--skeleton">
            <div class="skeleton-image"></div>
            <div class="skeleton-line skeleton-line--name"></div>
            <div class="skeleton-line skeleton-line--price"></div>
          </div>
        </div>

        <div v-else-if="products.length === 0" class="empty-state">
          <el-empty description="暂无商品" :image-size="80" />
        </div>

        <div v-else class="product-grid">
          <div v-for="product in products" :key="product.id" class="product-card" @click="goProductDetail(product.id)">
            <div class="product-image-wrapper">
              <img
                v-if="product.coverImage"
                :src="resolveImageUrl(product.coverImage)"
                :alt="product.name"
                class="product-image"
              />
              <div v-else class="product-image-placeholder">
                <el-icon :size="36"><ShoppingBag /></el-icon>
              </div>
              <div class="product-price-tag">¥{{ product.price }}</div>
            </div>
            <div class="product-info">
              <div class="product-name" :title="product.name">{{ product.name }}</div>
              <div class="product-meta">
                <span class="product-category">{{ product.categoryName || '未分类' }}</span>
                <span class="product-stock">库存 {{ product.stock || 0 }}</span>
              </div>
            </div>
            <el-button
              class="add-cart-btn"
              type="primary"
              plain
              size="small"
              :disabled="!product.stock"
              @click.stop="handleAddToCart(product)"
            >
              <el-icon><ShoppingCart /></el-icon>
              {{ product.stock ? '加入购物车' : '已售罄' }}
            </el-button>
          </div>
        </div>
      </div>

      <!-- Right: Notice list (300px fixed) -->
      <div class="notice-area">
        <NoticeList />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ShoppingBag, ShoppingCart, ArrowRight } from '@element-plus/icons-vue'
import { useUserStore } from '../stores/user'
import { useConfigStore } from '../stores/config'
import { productApi } from '../api/product'
import { cartApi } from '../api/order'
import { resolveImageUrl } from '../utils/image'
import NoticeList from '@/components/notice/NoticeList.vue'

interface ProductItem {
  id: number
  name: string
  coverImage?: string
  price: number
  stock: number
  categoryName?: string
}

const router = useRouter()
const userStore = useUserStore()
const configStore = useConfigStore()

const products = ref<ProductItem[]>([])
const loading = ref(false)

const greeting = computed(() => {
  const hour = new Date().getHours()
  if (hour < 12) return '早上好'
  if (hour < 18) return '下午好'
  return '晚上好'
})

async function fetchProducts() {
  loading.value = true
  try {
    const res = await productApi.list({ status: 'ON_SALE', page: 1, size: 8 })
    if (Array.isArray(res)) {
      products.value = res as ProductItem[]
    } else if (res && Array.isArray(res.content)) {
      products.value = res.content as ProductItem[]
    } else if (res && Array.isArray(res.list)) {
      products.value = res.list as ProductItem[]
    } else {
      products.value = []
    }
  } catch {
    products.value = []
  } finally {
    loading.value = false
  }
}

function goProducts() {
  router.push('/home/product')
}

function goProductDetail(id: number) {
  router.push('/home/product')
  // 产品详情目前通过 ProductManagement 的弹窗展示，直接跳转到列表页即可
  void id
}

async function handleAddToCart(product: ProductItem) {
  try {
    await cartApi.add({ productId: product.id, quantity: 1 })
    ElMessage.success(`已添加「${product.name}」到购物车`)
  } catch (e: any) {
    ElMessage.error(e?.message || '添加购物车失败')
  }
}

onMounted(async () => {
  await configStore.fetchConfigs()
  fetchProducts()
})
</script>

<style scoped>
.home-content {
  padding: 24px 32px;
  max-width: 1400px;
  margin: 0 auto;
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

/* Welcome section */
.welcome-section {
  animation: fadeInUp 0.6s ease-out forwards;
  margin-bottom: 24px;
}

.welcome-text {
  font-family: 'Sora', sans-serif;
  font-size: 36px;
  font-weight: 600;
  color: #fff;
  letter-spacing: -0.5px;
}

.notice-banner {
  margin-bottom: 20px;
  animation: fadeInUp 0.6s ease-out forwards;
}

/* Main content two-column layout */
.main-content {
  display: flex;
  gap: 24px;
  animation: fadeInUp 0.6s ease-out forwards;
  animation-delay: 0.2s;
  opacity: 0;
}

.product-area {
  flex: 1;
  min-width: 0;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.section-title {
  font-family: 'Sora', sans-serif;
  font-size: 20px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0;
}

.browse-all-btn {
  font-size: 14px;
  color: var(--neon-cyan);
}

/* Product grid */
.product-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 20px;
}

.product-card {
  background: var(--glass-bg);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-radius: 16px;
  border: 1px solid var(--border-subtle);
  overflow: hidden;
  cursor: pointer;
  transition:
    transform 0.3s ease,
    box-shadow 0.3s ease,
    border-color 0.3s ease;
  position: relative;
}

.product-card:hover {
  transform: translateY(-4px);
  border-color: var(--neon-cyan);
  box-shadow: 0 8px 32px rgba(0, 255, 255, 0.15);
}

.product-card--skeleton {
  cursor: default;
  pointer-events: none;
}

.product-image-wrapper {
  position: relative;
  width: 100%;
  height: 180px;
  overflow: hidden;
  background: rgba(255, 255, 255, 0.03);
}

.product-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s ease;
}

.product-card:hover .product-image {
  transform: scale(1.05);
}

.product-image-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-muted);
}

.product-price-tag {
  position: absolute;
  top: 12px;
  right: 12px;
  background: rgba(0, 0, 0, 0.75);
  backdrop-filter: blur(8px);
  color: var(--neon-green);
  font-family: 'Sora', sans-serif;
  font-size: 16px;
  font-weight: 600;
  padding: 4px 12px;
  border-radius: 20px;
  border: 1px solid rgba(57, 255, 20, 0.3);
}

.product-info {
  padding: 14px 16px 8px;
}

.product-name {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  margin-bottom: 6px;
}

.product-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
  color: var(--text-muted);
}

.add-cart-btn {
  margin: 0 16px 16px;
  width: calc(100% - 32px);
  border-radius: 8px;
  font-size: 13px;
}

/* Skeleton loading */
.skeleton-image {
  width: 100%;
  height: 180px;
  background: linear-gradient(
    90deg,
    rgba(255, 255, 255, 0.04) 25%,
    rgba(255, 255, 255, 0.08) 50%,
    rgba(255, 255, 255, 0.04) 75%
  );
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
}

.skeleton-line {
  margin: 14px 16px;
  height: 14px;
  border-radius: 4px;
  background: linear-gradient(
    90deg,
    rgba(255, 255, 255, 0.04) 25%,
    rgba(255, 255, 255, 0.08) 50%,
    rgba(255, 255, 255, 0.04) 75%
  );
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
}

.skeleton-line--name {
  width: 70%;
  height: 16px;
  margin-bottom: 8px;
}

.skeleton-line--price {
  width: 40%;
  height: 14px;
  margin-bottom: 16px;
}

@keyframes shimmer {
  0% {
    background-position: 200% 0;
  }
  100% {
    background-position: -200% 0;
  }
}

/* Empty state */
.empty-state {
  display: flex;
  justify-content: center;
  padding: 48px 0;
}

.notice-area {
  width: 380px;
  flex-shrink: 0;
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* Responsive */
@media (max-width: 1024px) {
  .main-content {
    flex-direction: column;
  }
  .notice-area {
    width: 100%;
  }
}

@media (max-width: 768px) {
  .home-content {
    padding: 16px;
  }
  .welcome-text {
    font-size: 24px;
  }
}
</style>
