<script setup>
import { ref, onMounted } from 'vue'

const currentTime = ref('')
const username = ref('管理员')

const quickActions = [
  { icon: '📦', title: '商品管理', desc: '管理您的商品信息', color: '#409eff' },
  { icon: '📋', title: '订单管理', desc: '查看和处理订单', color: '#67c23a' },
  { icon: '👤', title: '客户管理', desc: '管理客户资料', color: '#e6a23c' },
  { icon: '📊', title: '数据统计', desc: '查看经营数据', color: '#f56c6c' },
]

onMounted(() => {
  updateTime()
  setInterval(updateTime, 1000)
})

function updateTime() {
  const now = new Date()
  const hours = now.getHours().toString().padStart(2, '0')
  const minutes = now.getMinutes().toString().padStart(2, '0')
  const seconds = now.getSeconds().toString().padStart(2, '0')
  currentTime.value = `${hours}:${minutes}:${seconds}`
}

function getGreeting() {
  const hour = new Date().getHours()
  if (hour < 6) return '夜深了'
  if (hour < 9) return '早上好'
  if (hour < 12) return '上午好'
  if (hour < 14) return '中午好'
  if (hour < 18) return '下午好'
  return '晚上好'
}
</script>

<template>
  <div class="welcome-container">
    <div class="welcome-header">
      <div class="greeting">
        <h1>{{ getGreeting() }}，{{ username }}</h1>
        <p>欢迎回到商户管理中心</p>
      </div>
      <div class="time-display">
        <span class="time">{{ currentTime }}</span>
        <span class="date">{{ new Date().toLocaleDateString('zh-CN', { year: 'numeric', month: 'long', day: 'numeric', weekday: 'long' }) }}</span>
      </div>
    </div>

    <div class="stats-overview">
      <div class="stat-card">
        <div class="stat-icon" style="background: #ecf5ff; color: #409eff;">📦</div>
        <div class="stat-info">
          <span class="stat-value">0</span>
          <span class="stat-label">商品总数</span>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon" style="background: #f0f9eb; color: #67c23a;">📋</div>
        <div class="stat-info">
          <span class="stat-value">0</span>
          <span class="stat-label">今日订单</span>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon" style="background: #fdf6ec; color: #e6a23c;">💰</div>
        <div class="stat-info">
          <span class="stat-value">¥0</span>
          <span class="stat-label">今日营收</span>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon" style="background: #fef0f0; color: #f56c6c;">👤</div>
        <div class="stat-info">
          <span class="stat-value">0</span>
          <span class="stat-label">客户总数</span>
        </div>
      </div>
    </div>

    <div class="quick-actions">
      <h2>快捷入口</h2>
      <div class="actions-grid">
        <div v-for="action in quickActions" :key="action.title" class="action-card">
          <span class="action-icon">{{ action.icon }}</span>
          <div class="action-info">
            <span class="action-title">{{ action.title }}</span>
            <span class="action-desc">{{ action.desc }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.welcome-container {
  max-width: 1100px;
  margin: 0 auto;
  padding: 40px 24px;
}

.welcome-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 36px;
}

.greeting h1 {
  font-size: 28px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 8px 0;
}

.greeting p {
  font-size: 15px;
  color: #909399;
  margin: 0;
}

.time-display {
  text-align: right;
}

.time {
  display: block;
  font-size: 36px;
  font-weight: 300;
  color: #303133;
  font-variant-numeric: tabular-nums;
}

.date {
  font-size: 13px;
  color: #909399;
}

.stats-overview {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  margin-bottom: 36px;
}

.stat-card {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
  transition: box-shadow 0.2s;
}

.stat-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.stat-icon {
  width: 52px;
  height: 52px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  flex-shrink: 0;
}

.stat-info {
  display: flex;
  flex-direction: column;
}

.stat-value {
  font-size: 24px;
  font-weight: 600;
  color: #303133;
}

.stat-label {
  font-size: 13px;
  color: #909399;
  margin-top: 4px;
}

.quick-actions h2 {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 16px 0;
}

.actions-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.action-card {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 14px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
  cursor: pointer;
  transition: all 0.2s;
}

.action-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  transform: translateY(-2px);
}

.action-icon {
  font-size: 28px;
  flex-shrink: 0;
}

.action-info {
  display: flex;
  flex-direction: column;
}

.action-title {
  font-size: 15px;
  font-weight: 500;
  color: #303133;
}

.action-desc {
  font-size: 12px;
  color: #909399;
  margin-top: 2px;
}

@media (max-width: 768px) {
  .stats-overview,
  .actions-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .welcome-header {
    flex-direction: column;
    gap: 16px;
  }

  .time-display {
    text-align: left;
  }
}
</style>
