<template>
  <div class="statistics">
    <div class="page-header">
      <h1 class="page-title">数据统计</h1>
      <p class="page-sub">系统数据概览</p>
    </div>

    <div class="stats-grid">
      <div
        v-for="(stat, index) in stats"
        :key="stat.label"
        class="stat-card"
        :style="{ animationDelay: `${index * 0.1}s` }"
      >
        <div class="stat-icon" :style="{ background: stat.gradient }">
          <component :is="stat.icon" />
        </div>
        <div class="stat-content">
          <div class="stat-value">{{ stat.value }}</div>
          <div class="stat-label">{{ stat.label }}</div>
        </div>
      </div>
    </div>

    <div class="charts-grid">
      <div class="glass-card chart-card">
        <h3 class="card-title">数据趋势</h3>
        <div class="chart-container">
          <v-chart :option="trendOption" autoresize />
        </div>
      </div>

      <div class="glass-card chart-card">
        <h3 class="card-title">数据分布</h3>
        <div class="chart-container">
          <v-chart :option="pieOption" autoresize />
        </div>
      </div>
    </div>

    <div class="charts-grid" style="margin-top: 24px">
      <div class="glass-card chart-card">
        <h3 class="card-title">订单状态分布</h3>
        <div class="chart-container">
          <v-chart :option="orderStatusOption" autoresize />
        </div>
      </div>

      <div class="glass-card chart-card order-summary-card">
        <h3 class="card-title">订单概览</h3>
        <div class="order-summary">
          <div class="summary-item">
            <div class="summary-label">总订单数</div>
            <div class="summary-value">{{ stats[4].value }}</div>
          </div>
          <div class="summary-item">
            <div class="summary-label">今日订单</div>
            <div class="summary-value">{{ stats[5].value }}</div>
          </div>
          <div class="summary-item">
            <div class="summary-label">累计营收</div>
            <div class="summary-value revenue">¥{{ totalRevenue }}</div>
          </div>
          <div class="summary-item">
            <div class="summary-label">今日营收</div>
            <div class="summary-value revenue">¥{{ revenueToday }}</div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { User, Folder, Message, Clock, ShoppingCart, Money } from '@element-plus/icons-vue'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart, PieChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent } from 'echarts/components'
import VChart from 'vue-echarts'
import { useUserStore } from '../../stores/user'

use([CanvasRenderer, LineChart, PieChart, GridComponent, TooltipComponent, LegendComponent])

const userStore = useUserStore()

const stats = ref([
  { label: '总用户数', value: '-', icon: User, gradient: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)' },
  { label: '总文件数', value: '-', icon: Folder, gradient: 'linear-gradient(135deg, #f093fb 0%, #f5576c 100%)' },
  { label: '总消息数', value: '-', icon: Message, gradient: 'linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)' },
  { label: '今日活跃', value: '-', icon: Clock, gradient: 'linear-gradient(135deg, #43e97b 0%, #38f9d7 100%)' },
  { label: '总订单数', value: '-', icon: ShoppingCart, gradient: 'linear-gradient(135deg, #fa709a 0%, #fee140 100%)' },
  { label: '今日订单', value: '-', icon: ShoppingCart, gradient: 'linear-gradient(135deg, #a18cd1 0%, #fbc2eb 100%)' },
])

const totalRevenue = ref('-')
const revenueToday = ref('-')

const trendData = ref({ dates: [], userData: [], fileData: [], messageData: [], orderData: [] })
const distributionData = ref({ users: 0, files: 0, messages: 0, orders: 0 })
const orderStatusData = ref([])

const statusLabelMap = {
  PENDING: '待支付',
  PAID: '已支付',
  SHIPPED: '已发货',
  COMPLETED: '已完成',
  CANCELLED: '已取消',
  REFUNDING: '退款中',
  REFUNDED: '已退款',
}

const statusColorMap = {
  PENDING: '#e6a23c',
  PAID: '#409eff',
  SHIPPED: '#00f2fe',
  COMPLETED: '#67c23a',
  CANCELLED: '#909399',
  REFUNDING: '#f56c6c',
  REFUNDED: '#f5576c',
}

const trendOption = computed(() => ({
  backgroundColor: 'transparent',
  tooltip: {
    trigger: 'axis',
    backgroundColor: 'rgba(0, 0, 0, 0.8)',
    borderColor: 'rgba(255, 255, 255, 0.1)',
    textStyle: { color: '#fff' },
  },
  legend: {
    data: ['用户', '文件', '消息', '订单'],
    textStyle: { color: 'rgba(255, 255, 255, 0.7)' },
    top: 0,
  },
  grid: { left: '3%', right: '4%', bottom: '3%', top: '40px', containLabel: true },
  xAxis: {
    type: 'category',
    boundaryGap: false,
    data: trendData.value.dates,
    axisLine: { lineStyle: { color: 'rgba(255, 255, 255, 0.2)' } },
    axisLabel: { color: 'rgba(255, 255, 255, 0.5)' },
  },
  yAxis: {
    type: 'value',
    axisLine: { lineStyle: { color: 'rgba(255, 255, 255, 0.2)' } },
    axisLabel: { color: 'rgba(255, 255, 255, 0.5)' },
    splitLine: { lineStyle: { color: 'rgba(255, 255, 255, 0.1)' } },
  },
  series: [
    {
      name: '用户',
      type: 'line',
      smooth: true,
      data: trendData.value.userData,
      lineStyle: { color: '#667eea', width: 2 },
      itemStyle: { color: '#667eea' },
      areaStyle: {
        color: {
          type: 'linear',
          x: 0,
          y: 0,
          x2: 0,
          y2: 1,
          colorStops: [
            { offset: 0, color: 'rgba(102, 126, 234, 0.3)' },
            { offset: 1, color: 'rgba(102, 126, 234, 0)' },
          ],
        },
      },
    },
    {
      name: '文件',
      type: 'line',
      smooth: true,
      data: trendData.value.fileData,
      lineStyle: { color: '#f5576c', width: 2 },
      itemStyle: { color: '#f5576c' },
      areaStyle: {
        color: {
          type: 'linear',
          x: 0,
          y: 0,
          x2: 0,
          y2: 1,
          colorStops: [
            { offset: 0, color: 'rgba(245, 87, 108, 0.3)' },
            { offset: 1, color: 'rgba(245, 87, 108, 0)' },
          ],
        },
      },
    },
    {
      name: '消息',
      type: 'line',
      smooth: true,
      data: trendData.value.messageData,
      lineStyle: { color: '#00f2fe', width: 2 },
      itemStyle: { color: '#00f2fe' },
      areaStyle: {
        color: {
          type: 'linear',
          x: 0,
          y: 0,
          x2: 0,
          y2: 1,
          colorStops: [
            { offset: 0, color: 'rgba(0, 242, 254, 0.3)' },
            { offset: 1, color: 'rgba(0, 242, 254, 0)' },
          ],
        },
      },
    },
    {
      name: '订单',
      type: 'line',
      smooth: true,
      data: trendData.value.orderData,
      lineStyle: { color: '#fee140', width: 2 },
      itemStyle: { color: '#fee140' },
      areaStyle: {
        color: {
          type: 'linear',
          x: 0,
          y: 0,
          x2: 0,
          y2: 1,
          colorStops: [
            { offset: 0, color: 'rgba(254, 225, 64, 0.3)' },
            { offset: 1, color: 'rgba(254, 225, 64, 0)' },
          ],
        },
      },
    },
  ],
}))

const pieOption = computed(() => ({
  backgroundColor: 'transparent',
  tooltip: {
    trigger: 'item',
    backgroundColor: 'rgba(0, 0, 0, 0.8)',
    borderColor: 'rgba(255, 255, 255, 0.1)',
    textStyle: { color: '#fff' },
  },
  legend: {
    orient: 'vertical',
    right: '5%',
    top: 'center',
    textStyle: { color: 'rgba(255, 255, 255, 0.7)' },
  },
  series: [
    {
      type: 'pie',
      radius: ['40%', '70%'],
      center: ['35%', '50%'],
      avoidLabelOverlap: false,
      itemStyle: {
        borderRadius: 10,
        borderColor: 'rgba(0, 0, 0, 0.5)',
        borderWidth: 2,
      },
      label: { show: false },
      emphasis: {
        label: { show: true, fontSize: 14, fontWeight: 'bold', color: '#fff' },
      },
      data: [
        { value: distributionData.value.users, name: '用户', itemStyle: { color: '#667eea' } },
        { value: distributionData.value.files, name: '文件', itemStyle: { color: '#f5576c' } },
        { value: distributionData.value.messages, name: '消息', itemStyle: { color: '#00f2fe' } },
        { value: distributionData.value.orders, name: '订单', itemStyle: { color: '#fee140' } },
      ],
    },
  ],
}))

const orderStatusOption = computed(() => ({
  backgroundColor: 'transparent',
  tooltip: {
    trigger: 'item',
    backgroundColor: 'rgba(0, 0, 0, 0.8)',
    borderColor: 'rgba(255, 255, 255, 0.1)',
    textStyle: { color: '#fff' },
    formatter: '{b}: {c} ({d}%)',
  },
  legend: {
    orient: 'vertical',
    right: '5%',
    top: 'center',
    textStyle: { color: 'rgba(255, 255, 255, 0.7)' },
  },
  series: [
    {
      type: 'pie',
      radius: ['40%', '70%'],
      center: ['35%', '50%'],
      avoidLabelOverlap: false,
      itemStyle: {
        borderRadius: 10,
        borderColor: 'rgba(0, 0, 0, 0.5)',
        borderWidth: 2,
      },
      label: { show: false },
      emphasis: {
        label: { show: true, fontSize: 14, fontWeight: 'bold', color: '#fff' },
      },
      data: orderStatusData.value.map((item) => ({
        value: item.count,
        name: statusLabelMap[item.status] || item.status,
        itemStyle: { color: statusColorMap[item.status] || '#909399' },
      })),
    },
  ],
}))

const fetchStats = async () => {
  try {
    const res = await fetch('/api/stats/dashboard', {
      headers: { Authorization: `Bearer ${userStore.token}` },
    })
    if (res.ok) {
      const response = await res.json()
      const data = response.data || response
      stats.value[0].value = data.totalUsers ?? '-'
      stats.value[1].value = data.totalFiles ?? '-'
      stats.value[2].value = data.totalMessages ?? '-'
      stats.value[3].value = data.activeUsersToday ?? '-'
      stats.value[4].value = data.totalOrders ?? '-'
      stats.value[5].value = data.ordersToday ?? '-'
      totalRevenue.value = data.totalRevenue != null ? Number(data.totalRevenue).toFixed(2) : '-'
      revenueToday.value = data.revenueToday != null ? Number(data.revenueToday).toFixed(2) : '-'
    } else {
      ElMessage.error('获取统计数据失败')
    }
  } catch (e) {
    ElMessage.error('获取统计数据失败')
  }
}

const fetchTrendData = async () => {
  try {
    const res = await fetch('/api/stats/trend?days=7', {
      headers: { Authorization: `Bearer ${userStore.token}` },
    })
    if (res.ok) {
      const response = await res.json()
      const data = response.data || response
      trendData.value = data
    }
  } catch (e) {
    console.error('获取趋势数据失败', e)
    ElMessage.warning('趋势数据加载失败')
  }
}

const fetchDistribution = async () => {
  try {
    const res = await fetch('/api/stats/type-distribution', {
      headers: { Authorization: `Bearer ${userStore.token}` },
    })
    if (res.ok) {
      const response = await res.json()
      distributionData.value = response.data || response
    }
  } catch (e) {
    console.error('获取分布数据失败', e)
    ElMessage.warning('分布数据加载失败')
  }
}

const fetchOrderStatus = async () => {
  try {
    const res = await fetch('/api/stats/order-status', {
      headers: { Authorization: `Bearer ${userStore.token}` },
    })
    if (res.ok) {
      const response = await res.json()
      orderStatusData.value = response.data || []
    }
  } catch (e) {
    console.error('获取订单状态分布失败', e)
    ElMessage.warning('订单状态数据加载失败')
  }
}

onMounted(() => {
  fetchStats()
  fetchTrendData()
  fetchDistribution()
  fetchOrderStatus()
})
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Outfit:wght@300;400;500;600;700&family=Sora:wght@300;400;500;600&display=swap');

.statistics {
  animation: fadeInUp 0.5s ease-out forwards;
}

.page-header {
  margin-bottom: 24px;
}

.page-title {
  font-family: 'Sora', sans-serif;
  font-size: 28px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 8px;
}

.page-sub {
  font-size: 14px;
  color: var(--text-muted);
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 24px;
  margin-bottom: 24px;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 20px;
  background: var(--glass-bg);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-radius: 20px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  padding: 28px 24px;
  animation: fadeInUp 0.5s ease-out forwards;
  opacity: 0;
  transition:
    transform 0.3s ease,
    box-shadow 0.3s ease;
  cursor: pointer;
}

.stat-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.3);
}

.stat-icon {
  width: 64px;
  height: 64px;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.2);
}

.stat-icon :deep(.el-icon),
.stat-icon :deep(svg) {
  width: 32px;
  height: 32px;
  color: white;
}

.stat-content {
  flex: 1;
}

.stat-value {
  font-family: 'Sora', sans-serif;
  font-size: 36px;
  font-weight: 600;
  color: var(--text-primary);
  line-height: 1.2;
}

.stat-label {
  font-size: 14px;
  color: var(--text-muted);
  margin-top: 4px;
}

.glass-card {
  background: var(--glass-bg);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-radius: 20px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  padding: 24px;
}

.charts-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(400px, 1fr));
  gap: 24px;
}

.chart-card {
  min-height: 350px;
}

.card-title {
  font-family: 'Sora', sans-serif;
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 20px;
}

.chart-container {
  height: 280px;
}

.order-summary-card {
  display: flex;
  flex-direction: column;
}

.order-summary {
  flex: 1;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;
  align-content: center;
  padding: 20px 0;
}

.summary-item {
  text-align: center;
}

.summary-label {
  font-size: 14px;
  color: var(--text-muted);
  margin-bottom: 8px;
}

.summary-value {
  font-family: 'Sora', sans-serif;
  font-size: 32px;
  font-weight: 600;
  color: var(--text-primary);
}

.summary-value.revenue {
  color: #fee140;
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
</style>
