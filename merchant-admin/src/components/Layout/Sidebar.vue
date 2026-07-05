<template>
  <div class="sidebar">
    <div class="logo">
      <span v-if="!isCollapse">商家管理中心</span>
      <span v-else>商家</span>
    </div>
    <el-menu
      :default-active="activeMenu"
      :collapse="isCollapse"
      :collapse-transition="false"
      background-color="#304156"
      text-color="#bfcbd9"
      active-text-color="#409eff"
      router
    >
      <el-menu-item index="/dashboard">
        <el-icon><DataAnalysis /></el-icon>
        <template #title>数据看板</template>
      </el-menu-item>
      <el-menu-item index="/shop">
        <el-icon><Shop /></el-icon>
        <template #title>店铺信息</template>
      </el-menu-item>
      <el-menu-item index="/goods">
        <el-icon><Goods /></el-icon>
        <template #title>商品管理</template>
      </el-menu-item>
      <el-menu-item index="/order">
        <el-icon><List /></el-icon>
        <template #title>订单管理</template>
      </el-menu-item>
      <el-menu-item index="/coupons">
        <el-icon><Ticket /></el-icon>
        <template #title>优惠券管理</template>
      </el-menu-item>
      <el-menu-item index="/evaluations">
        <el-icon><Star /></el-icon>
        <template #title>店铺评价</template>
      </el-menu-item>
    </el-menu>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'

defineProps({
  isCollapse: Boolean
})

const route = useRoute()
const activeMenu = computed(() => '/' + route.path.split('/')[1])
</script>

<style scoped>
.sidebar {
  height: 100%;
  display: flex;
  flex-direction: column;
}
.logo {
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 16px;
  font-weight: 600;
  border-bottom: 1px solid rgba(255,255,255,0.1);
}
.el-menu {
  border-right: none;
}
</style>
