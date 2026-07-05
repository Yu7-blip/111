<template>
  <div class="header-bar">
    <div class="header-left">
      <el-icon class="collapse-btn" @click="toggleCollapse" :size="22">
        <Fold v-if="!isCollapse" />
        <Expand v-else />
      </el-icon>
      <el-breadcrumb separator="/">
        <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
        <el-breadcrumb-item v-if="currentTitle">{{ currentTitle }}</el-breadcrumb-item>
      </el-breadcrumb>
    </div>
    <div class="header-right">
      <span class="username">{{ username }}</span>
      <el-button type="danger" text @click="handleLogout">退出登录</el-button>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const props = defineProps({ isCollapse: Boolean })
const emit = defineEmits(['update:isCollapse'])

const route = useRoute()
const router = useRouter()
const currentTitle = computed(() => route.meta.title || '')
const username = computed(() => {
  const info = sessionStorage.getItem('userInfo')
  return info ? JSON.parse(info).username : '管理员'
})

function toggleCollapse() {
  emit('update:isCollapse', !props.isCollapse)
}

function handleLogout() {
  sessionStorage.clear()
  router.push('/login')
}
</script>

<style scoped>
.header-bar {
  width: 100%;
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}
.collapse-btn {
  cursor: pointer;
}
.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}
.username {
  color: #606266;
  font-size: 14px;
}
</style>
