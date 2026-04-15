<template>
  <div class="admin-layout">
    <!-- 侧边栏 -->
    <aside class="sidebar" :class="{ collapsed: sidebarCollapsed }">
      <div class="sidebar-header">
        <el-icon size="24" color="#fff"><School /></el-icon>
        <span v-if="!sidebarCollapsed" class="sidebar-title">管理后台</span>
      </div>

      <el-menu
        :default-active="route.path"
        router
        background-color="#1e293b"
        text-color="#94a3b8"
        active-text-color="#ffffff"
        :collapse="sidebarCollapsed"
      >
        <el-menu-item v-for="item in menuItems" :key="item.path" :index="item.path">
          <el-icon><component :is="item.icon" /></el-icon>
          <template #title>{{ item.label }}</template>
        </el-menu-item>
      </el-menu>

      <div class="sidebar-footer">
        <el-button text style="color: #94a3b8;" @click="goHome">
          <el-icon><House /></el-icon>
          <span v-if="!sidebarCollapsed">返回前台</span>
        </el-button>
      </div>
    </aside>

    <!-- 主内容区 -->
    <div class="admin-main">
      <!-- 顶部栏 -->
      <header class="admin-header">
        <el-button text :icon="sidebarCollapsed ? Expand : Fold" @click="sidebarCollapsed = !sidebarCollapsed" />
        <span class="header-title">{{ currentTitle }}</span>
        <div class="header-right">
          <span class="admin-name">{{ userStore.realName }}</span>
          <el-button type="danger" size="small" plain @click="handleLogout">退出</el-button>
        </div>
      </header>

      <!-- 页面内容 -->
      <main class="admin-content">
        <router-view />
      </main>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { School, House, Expand, Fold } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const sidebarCollapsed = ref(false)

const menuItems = [
  { path: '/admin/dashboard', label: '数据看板', icon: 'DataAnalysis' },
  { path: '/admin/audit', label: '预约审核', icon: 'DocumentChecked' },
  { path: '/admin/labs', label: '实验室管理', icon: 'OfficeBuilding' },
  { path: '/admin/blacklist', label: '黑名单管理', icon: 'UserFilled' }
]

const currentTitle = computed(() => {
  return menuItems.find(m => m.path === route.path)?.label || '管理后台'
})

function goHome() {
  router.push('/labs')
}

function handleLogout() {
  userStore.logout()
  ElMessage.success('已退出登录')
  router.push('/login')
}
</script>

<style scoped>
.admin-layout {
  display: flex;
  min-height: 100vh;
}

.sidebar {
  width: 220px;
  min-height: 100vh;
  background: #1e293b;
  display: flex;
  flex-direction: column;
  transition: width 0.3s;
  flex-shrink: 0;
}

.sidebar.collapsed {
  width: 64px;
}

.sidebar-header {
  height: 4rem;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  border-bottom: 1px solid #334155;
  padding: 0 1rem;
}

.sidebar-title {
  color: #fff;
  font-size: 1rem;
  font-weight: 700;
  white-space: nowrap;
}

.el-menu {
  border-right: none;
  flex: 1;
}

.sidebar-footer {
  padding: 1rem;
  border-top: 1px solid #334155;
}

.admin-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  background: #f1f5f9;
}

.admin-header {
  height: 3.5rem;
  background: #fff;
  border-bottom: 1px solid #e2e8f0;
  display: flex;
  align-items: center;
  padding: 0 1.5rem;
  gap: 1rem;
  box-shadow: 0 1px 4px rgba(0,0,0,0.06);
}

.header-title {
  flex: 1;
  font-size: 1rem;
  font-weight: 600;
  color: #1e293b;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.admin-name {
  font-size: 0.875rem;
  color: #475569;
}

.admin-content {
  flex: 1;
  padding: 1.5rem;
  overflow-y: auto;
}

@media (max-width: 768px) {
  .sidebar { width: 64px; }
  .sidebar-title { display: none; }
}
</style>
