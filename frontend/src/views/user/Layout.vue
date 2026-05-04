<template>
  <div class="user-layout">
    <!-- 顶部导航栏（桌面端） -->
    <header class="top-nav hide-mobile-false">
      <div class="nav-inner">
        <div class="nav-brand">
          <el-icon size="22" color="#409eff"><School /></el-icon>
          <span>实验室预约</span>
        </div>
        <nav class="nav-links">
          <router-link to="/labs">实验室列表</router-link>
          <router-link to="/my-reservations">我的预约</router-link>
          <router-link to="/notices">
            消息通知
            <el-badge v-if="userStore.unreadCount > 0" :value="userStore.unreadCount" :max="99" />
          </router-link>
        </nav>
        <div class="nav-user" v-if="userStore.isLoggedIn">
          <el-dropdown @command="handleCommand">
            <span class="user-name">
              {{ userStore.realName }}
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item v-if="userStore.isAdmin" command="admin">
                  进入管理后台
                </el-dropdown-item>
                <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
        <div v-else>
          <router-link to="/login">
            <el-button type="primary" size="small">登录</el-button>
          </router-link>
        </div>
      </div>
    </header>

    <!-- 页面内容 -->
    <main class="main-content has-bottom-nav">
      <router-view />
    </main>

    <!-- 底部导航栏（移动端） -->
    <nav class="bottom-nav">
      <div
        v-for="item in navItems"
        :key="item.path"
        class="bottom-nav-item"
        :class="{ active: isActive(item.path) }"
        @click="navigate(item)"
      >
        <el-icon class="nav-icon"><component :is="item.icon" /></el-icon>
        <span>{{ item.label }}</span>
        <el-badge
          v-if="item.badge && userStore.unreadCount > 0"
          :value="userStore.unreadCount"
          :max="99"
          class="notice-badge"
        />
      </div>
    </nav>
  </div>
</template>

<script setup>
import { onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElNotification } from 'element-plus'
import { School, ArrowDown } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'
import { noticeApi } from '@/api'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
let timer = null

const navItems = [
  { path: '/labs', label: '实验室', icon: 'OfficeBuilding' },
  { path: '/my-reservations', label: '我的预约', icon: 'Calendar', requiresAuth: true },
  { path: '/notices', label: '消息', icon: 'Bell', requiresAuth: true, badge: true }
]

function isActive(path) {
  return route.path === path || route.path.startsWith(path + '/')
}

function navigate(item) {
  if (item.requiresAuth && !userStore.isLoggedIn) {
    router.push('/login')
    return
  }
  router.push(item.path)
}

function handleCommand(cmd) {
  if (cmd === 'logout') {
    userStore.logout()
    ElMessage.success('已退出登录')
    router.push('/login')
  } else if (cmd === 'admin') {
    router.push('/admin/workbench')
  }
}

function openNoticePage() {
  if (route.path !== '/notices') {
    router.push('/notices')
  }
}

async function fetchUnreadCount(showToast = false) {
  if (!userStore.isLoggedIn) {
    userStore.setUnreadCount(0)
    return
  }
  try {
    const previousCount = userStore.unreadCount
    const res = await noticeApi.unreadCount()
    const nextCount = res.data || 0
    userStore.setUnreadCount(nextCount)

    if (showToast && nextCount > previousCount) {
      const diff = nextCount - previousCount
      ElNotification({
        title: '新消息提醒',
        message: `你有 ${diff} 条新通知，点击查看`,
        type: 'info',
        duration: 3000,
        onClick: openNoticePage
      })
    }
  } catch {}
}

onMounted(async () => {
  await fetchUnreadCount(false)
  timer = setInterval(() => {
    fetchUnreadCount(true).catch(() => {})
  }, 30000)
})

onUnmounted(() => {
  clearInterval(timer)
})
</script>

<style scoped>
.user-layout {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
}

.top-nav {
  background: #fff;
  border-bottom: 1px solid #ebeef5;
  position: sticky;
  top: 0;
  z-index: 50;
  box-shadow: 0 2px 8px rgba(0,0,0,0.06);
}

.nav-inner {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 1.5rem;
  height: 3.5rem;
  display: flex;
  align-items: center;
  gap: 2rem;
}

.nav-brand {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 1.1rem;
  font-weight: 700;
  color: #303133;
  white-space: nowrap;
}

.nav-links {
  display: flex;
  gap: 1.5rem;
  flex: 1;
}

.nav-links a {
  color: #606266;
  text-decoration: none;
  font-size: 0.9rem;
  padding: 0.25rem 0;
  border-bottom: 2px solid transparent;
  transition: all 0.2s;
  position: relative;
}

.nav-links a.router-link-active,
.nav-links a:hover {
  color: #409eff;
  border-bottom-color: #409eff;
}

.nav-user {
  cursor: pointer;
}

.user-name {
  display: flex;
  align-items: center;
  gap: 0.25rem;
  font-size: 0.9rem;
  color: #303133;
}

.main-content {
  flex: 1;
  background: #f5f7fa;
}

.notice-badge {
  position: absolute;
  top: -4px;
  right: -8px;
}

/* 移动端隐藏顶部导航链接 */
@media (max-width: 768px) {
  .nav-links { display: none; }
  .nav-inner { padding: 0 1rem; }
}
</style>
