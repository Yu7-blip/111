<template>
  <div class="sidebar-container">
    <div class="logo">
      <span class="logo-text">管理后台</span>
    </div>
    <el-menu
      :default-active="activeMenu"
      router
      background-color="#304156"
      text-color="#bfcbd9"
      active-text-color="#409EFF"
      :collapse="isCollapse"
    >
      <el-menu-item index="/dashboard">
        <el-icon><DataAnalysis /></el-icon>
        <template #title>首页</template>
      </el-menu-item>

      <el-sub-menu index="user-group">
        <template #title>
          <el-icon><User /></el-icon>
          <span>用户管理</span>
        </template>
        <el-menu-item index="/users">用户列表</el-menu-item>
      </el-sub-menu>

      <el-sub-menu index="merchant-group">
        <template #title>
          <el-icon><Shop /></el-icon>
          <span>商家管理</span>
        </template>
        <el-menu-item index="/merchants">商家列表</el-menu-item>
        <el-menu-item v-if="isAdmin" index="/merchants/audit">商家审核</el-menu-item>
      </el-sub-menu>

      <el-menu-item v-if="isAdmin" index="/delivery">
        <el-icon><Van /></el-icon>
        <template #title>骑手管理</template>
      </el-menu-item>

      <el-menu-item index="/orders">
        <el-icon><Document /></el-icon>
        <template #title>订单管理</template>
      </el-menu-item>

      <el-menu-item v-if="isAdmin" index="/goods">
        <el-icon><Goods /></el-icon>
        <template #title>商品管理</template>
      </el-menu-item>

      <el-menu-item v-if="isAdmin" index="/evaluations">
        <el-icon><Star /></el-icon>
        <template #title>评价管理</template>
      </el-menu-item>

      <el-menu-item v-if="isAdmin" index="/feedback">
        <el-icon><ChatLineSquare /></el-icon>
        <template #title>反馈管理</template>
      </el-menu-item>

      <el-sub-menu v-if="isAdmin" index="marketing-group">
        <template #title>
          <el-icon><Discount /></el-icon>
          <span>营销活动</span>
        </template>
        <el-menu-item index="/marketing/full-reduce">满减活动</el-menu-item>
        <el-menu-item index="/marketing/coupons">平台优惠券</el-menu-item>
      </el-sub-menu>

      <el-sub-menu v-if="isAdmin" index="system-group">
        <template #title>
          <el-icon><Setting /></el-icon>
          <span>系统管理</span>
        </template>
        <el-menu-item index="/system/configs">系统配置</el-menu-item>
        <el-menu-item index="/system/admins">管理员管理</el-menu-item>
      </el-sub-menu>
    </el-menu>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'

defineProps({
  isCollapse: {
    type: Boolean,
    default: false
  }
})

const route = useRoute()
const userStore = useUserStore()
const activeMenu = computed(() => route.path)
const isAdmin = computed(() => userStore.userInfo?.role === 'admin')
</script>

<style scoped lang="scss">
.sidebar-container {
  height: 100%;
  background: #304156;
  overflow-y: auto;

  .logo {
    height: 60px;
    display: flex;
    align-items: center;
    justify-content: center;
    border-bottom: 1px solid rgba(255, 255, 255, 0.1);

    .logo-text {
      color: #fff;
      font-size: 18px;
      font-weight: 700;
      white-space: nowrap;
      letter-spacing: 2px;
    }
  }

  .el-menu {
    border-right: none;
  }
}
</style>
