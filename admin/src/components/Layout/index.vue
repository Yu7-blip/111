<template>
  <el-container class="layout-container">
    <el-aside :width="isCollapse ? '64px' : '220px'" class="layout-aside">
      <Sidebar :isCollapse="isCollapse" />
    </el-aside>
    <el-container>
      <el-header height="60px" class="layout-header">
        <Header :isCollapse="isCollapse" @toggle="isCollapse = !isCollapse" />
      </el-header>
      <el-main class="layout-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { ref } from 'vue'
import Sidebar from './Sidebar.vue'
import Header from './Header.vue'

const isCollapse = ref(false)
</script>

<style scoped lang="scss">
.layout-container {
  height: 100%;
}

.layout-aside {
  transition: width 0.3s;
  overflow: hidden;
}

.layout-header {
  padding: 0;
}

.layout-main {
  background: #f5f7fa;
  min-height: calc(100vh - 60px);
}
</style>
