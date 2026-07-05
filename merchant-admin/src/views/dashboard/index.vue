<template>
  <div class="dashboard-container">
    <div class="stat-cards">
      <div class="stat-card" v-for="c in statCards" :key="c.label">
        <div class="stat-icon" :style="{ background: c.bg }">{{ c.icon }}</div>
        <div class="stat-info">
          <span class="stat-label">{{ c.label }}</span>
          <span class="stat-value">{{ c.value }}</span>
        </div>
      </div>
    </div>

    <div class="chart-row">
      <div class="chart-card">
        <h3>近7日销售额趋势</h3>
        <div ref="salesChartRef" class="chart-box"></div>
      </div>
      <div class="chart-card">
        <h3>商品分类占比</h3>
        <div ref="categoryChartRef" class="chart-box"></div>
      </div>
    </div>

    <div class="chart-card">
      <h3>近7日订单统计</h3>
      <div ref="orderChartRef" class="chart-box"></div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import request from '@/api/request'

const salesChartRef = ref(null)
const categoryChartRef = ref(null)
const orderChartRef = ref(null)

const statCards = ref([
  { label: '商品总数', value: 0, icon: '📦', bg: '#ecf5ff' },
  { label: '今日订单', value: 0, icon: '📋', bg: '#f0f9eb' },
  { label: '今日营收', value: '¥0', icon: '💰', bg: '#fdf6ec' },
  { label: '客户总数', value: 0, icon: '👤', bg: '#fef0f0' }
])

let dashboardData = null
let charts = []

onMounted(async () => {
  await loadDashboard()
  await nextTick()
  initCharts(echarts)
})

onUnmounted(() => {
  charts.forEach(c => c.dispose())
})

async function loadDashboard() {
  try {
    const res = await request.get('/merchant/dashboard')
    if (res.code === 200) {
      dashboardData = res.data
      statCards.value = [
        { label: '商品总数', value: dashboardData.goodsCount || 0, icon: '📦', bg: '#ecf5ff' },
        { label: '今日订单', value: dashboardData.todayOrderCount || 0, icon: '📋', bg: '#f0f9eb' },
        { label: '今日营收', value: '¥' + (Number(dashboardData.todayRevenue || 0).toFixed(0)), icon: '💰', bg: '#fdf6ec' },
        { label: '客户总数', value: dashboardData.customerCount || 0, icon: '👤', bg: '#fef0f0' }
      ]
    }
  } catch (e) {
    console.error('Dashboard load failed:', e)
  }
}

function initCharts(echarts) {
  initSalesChart(echarts)
  initCategoryChart(echarts)
  initOrderChart(echarts)
}

function initSalesChart(echarts) {
  if (!salesChartRef.value || !dashboardData) return
  const chart = echarts.init(salesChartRef.value)
  const trend = dashboardData.salesTrend || []
  chart.setOption({
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: trend.map(t => t.date) },
    yAxis: { type: 'value' },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    series: [{
      data: trend.map(t => t.amount),
      type: 'line',
      smooth: true,
      areaStyle: {
        color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1, colorStops: [{ offset: 0, color: 'rgba(64,158,255,0.3)' }, { offset: 1, color: 'rgba(64,158,255,0.05)' }] }
      },
      lineStyle: { color: '#409eff', width: 2 },
      itemStyle: { color: '#409eff' }
    }]
  })
  charts.push(chart)
}

function initCategoryChart(echarts) {
  if (!categoryChartRef.value || !dashboardData) return
  const chart = echarts.init(categoryChartRef.value)
  const pieData = dashboardData.categoryDistribution || []
  chart.setOption({
    tooltip: { trigger: 'item' },
    legend: { bottom: 0 },
    series: [{
      type: 'pie',
      radius: ['40%', '70%'],
      avoidLabelOverlap: false,
      itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
      data: pieData.map(d => ({ value: d.value, name: d.name }))
    }]
  })
  charts.push(chart)
}

function initOrderChart(echarts) {
  if (!orderChartRef.value || !dashboardData) return
  const chart = echarts.init(orderChartRef.value)
  const orderTrend = dashboardData.orderTrend || []
  chart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['订单量', '完成量'] },
    xAxis: { type: 'category', data: orderTrend.map(t => t.date) },
    yAxis: { type: 'value' },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    series: [
      { name: '订单量', type: 'bar', data: orderTrend.map(t => t.total), barWidth: '40%', itemStyle: { color: '#409eff', borderRadius: [4, 4, 0, 0] } },
      { name: '完成量', type: 'bar', data: orderTrend.map(t => t.completed), barWidth: '40%', itemStyle: { color: '#67c23a', borderRadius: [4, 4, 0, 0] } }
    ]
  })
  charts.push(chart)
}
</script>

<style scoped>
.dashboard-container { padding: 0; }
.stat-cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  margin-bottom: 24px;
}
@media (max-width: 1200px) {
  .stat-cards { grid-template-columns: repeat(2, 1fr); }
}
.stat-card {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.06);
}
.stat-icon {
  width: 56px; height: 56px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
}
.stat-label { font-size: 14px; color: #909399; }
.stat-value { font-size: 26px; font-weight: 600; color: #303133; }
.chart-row {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 20px;
  margin-bottom: 24px;
}
@media (max-width: 1200px) {
  .chart-row { grid-template-columns: 1fr; }
}
.chart-card {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.06);
}
.chart-card h3 { font-size: 16px; font-weight: 600; color: #303133; margin-bottom: 16px; }
.chart-box { width: 100%; height: 300px; }
</style>
