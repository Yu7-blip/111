<template>
  <div class="page-container">
    <el-row :gutter="16" class="stats-row">
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-info">
              <div class="stat-value">{{ stats.userCount }}</div>
              <div class="stat-label">用户总数</div>
            </div>
            <el-icon class="stat-icon" color="#409EFF" :size="48"><User /></el-icon>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-info">
              <div class="stat-value">{{ stats.merchantCount }}</div>
              <div class="stat-label">商家总数</div>
            </div>
            <el-icon class="stat-icon" color="#67C23A" :size="48"><Shop /></el-icon>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-info">
              <div class="stat-value">¥{{ stats.todayRevenue }}</div>
              <div class="stat-label">今日营收</div>
            </div>
            <el-icon class="stat-icon" color="#E6A23C" :size="48"><Money /></el-icon>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-info">
              <div class="stat-value">{{ stats.orderToday }}</div>
              <div class="stat-label">今日订单</div>
            </div>
            <el-icon class="stat-icon" color="#F56C6C" :size="48"><Document /></el-icon>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-info">
              <div class="stat-value">{{ stats.deliveryOnline }}</div>
              <div class="stat-label">在线骑手</div>
            </div>
            <el-icon class="stat-icon" color="#909399" :size="48"><Van /></el-icon>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="mt-16">
      <el-col :span="14">
        <el-card shadow="hover" class="content-card">
          <template #header>
            <span class="card-title">近6个月订单趋势</span>
          </template>
          <div ref="chartRef" class="chart-container"></div>
        </el-card>
      </el-col>
      <el-col :span="10">
        <el-card shadow="hover" class="content-card">
          <template #header>
            <span class="card-title">最近订单</span>
          </template>
          <el-table :data="recentOrders" size="small" stripe>
            <el-table-column prop="orderNo" label="订单号" width="170" />
            <el-table-column prop="merchantName" label="商家" />
            <el-table-column prop="amount" label="金额" width="80">
              <template #default="{ row }">￥{{ row.amount }}</template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="80">
              <template #default="{ row }">
                <el-tag :type="getStatusType(row.status)" size="small">{{ getStatusText(row.status) }}</el-tag>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-if="recentOrders.length === 0" description="暂无最近订单" :image-size="80" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import { getDashboardStats, getOrderTrend } from '@/api/dashboard'
import { getOrderList, statusMap } from '@/api/order'

const chartRef = ref(null)
const stats = reactive({
  userCount: 0,
  merchantCount: 0,
  orderToday: 0,
  deliveryOnline: 0,
  todayRevenue: '0.00'
})

const recentOrders = ref([])

function getStatusType(status) {
  const map = { 0: 'info', 1: '', 2: 'warning', 3: 'success', 4: 'info', 5: 'warning', 6: 'success' }
  return map[status] || 'info'
}

function getStatusText(status) {
  return statusMap[status] || '未知'
}

let chartInstance = null

onMounted(async () => {
  const [statsRes, trendRes, orderRes] = await Promise.all([
    getDashboardStats(),
    getOrderTrend(),
    getOrderList({ page: 1, pageSize: 5 })
  ])
  if (statsRes.code === 200) {
    stats.userCount = statsRes.data.userCount
    stats.merchantCount = statsRes.data.merchantCount
    stats.orderToday = statsRes.data.todayOrderCount
    stats.deliveryOnline = statsRes.data.onlineDeliveryCount
    stats.todayRevenue = Number(statsRes.data.todayRevenue || 0).toFixed(2)
  }
  recentOrders.value = orderRes.data.records

  await nextTick()
  if (chartRef.value && trendRes.code === 200) {
    const trendData = trendRes.data
    chartInstance = echarts.init(chartRef.value)
    chartInstance.setOption({
      tooltip: { trigger: 'axis' },
      xAxis: { type: 'category', data: trendData.map(d => d.month) },
      yAxis: { type: 'value' },
      grid: { top: 20, right: 20, bottom: 30, left: 40 },
      series: [{
        name: '订单量',
        type: 'bar',
        data: trendData.map(d => d.count),
        itemStyle: { color: '#409EFF', borderRadius: [4, 4, 0, 0] }
      }]
    })
  }

  // 监听窗口/侧边栏变化，图表自适应
  window.addEventListener('resize', handleChartResize)
})

import { onBeforeUnmount } from 'vue'
onBeforeUnmount(() => {
  window.removeEventListener('resize', handleChartResize)
  chartInstance?.dispose()
})

function handleChartResize() {
  chartInstance?.resize()
}
</script>

<style scoped lang="scss">
.stats-row {
  .stat-card {
    :deep(.el-card__body) {
      padding: 20px;
    }
  }

  .stat-content {
    display: flex;
    justify-content: space-between;
    align-items: center;

    .stat-value {
      font-size: 28px;
      font-weight: 700;
      color: #303133;
    }

    .stat-label {
      font-size: 14px;
      color: #909399;
      margin-top: 4px;
    }
  }
}

.card-title {
  font-weight: 600;
  color: #303133;
}

.chart-container {
  width: 100%;
  height: 300px;
}

.content-card {
  :deep(.el-card__body) {
    padding: 0 20px 20px;
  }
}
</style>
