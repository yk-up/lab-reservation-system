<template>
  <div class="workbench-page">
    <section class="card hero">
      <div class="hero-main">
        <p class="hero-badge">Admin Workbench</p>
        <h2 class="hero-title">{{ greeting }}，{{ displayName }}</h2>
        <p class="hero-sub">欢迎进入管理后台工作台，可快速处理审批、公告和通知等核心任务。</p>
      </div>
      <div class="hero-meta">
        <div class="meta-item">
          <span class="meta-label">当前日期</span>
          <strong class="meta-value">{{ currentDate }}</strong>
        </div>
        <div class="meta-item">
          <span class="meta-label">当前时间</span>
          <strong class="meta-value">{{ currentTime }}</strong>
        </div>
      </div>
    </section>

    <section class="card mt-3">
      <div class="section-head">
        <h3>工作台概览</h3>
        <span class="section-sub">今日优先处理事项</span>
      </div>
      <div class="summary-list">
        <div class="summary-item"><span>当前角色</span><strong>管理员</strong></div>
        <div class="summary-item"><span>待审核预约</span><strong class="warning">{{ overview.pendingCount ?? 0 }}</strong></div>
        <div class="summary-item"><span>开放实验室</span><strong class="success">{{ overview.openLabs ?? 0 }}</strong></div>
        <div class="summary-item"><span>黑名单人数</span><strong class="danger">{{ overview.blacklistCount ?? 0 }}</strong></div>
      </div>
      <div class="summary-actions">
        <el-button type="primary" @click="go('/admin/approval')">处理审批</el-button>
        <el-button @click="go('/admin/labs')">查看实验室</el-button>
      </div>
    </section>

    <section class="card mt-3">
      <div class="section-head">
        <h3>快捷入口</h3>
      </div>
      <div class="quick-grid">
        <button
          v-for="item in quickEntries"
          :key="item.path"
          class="quick-item"
          @click="go(item.path)"
        >
          <el-icon :size="18"><component :is="item.icon" /></el-icon>
          <span>{{ item.label }}</span>
        </button>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import dayjs from 'dayjs'
import { ElMessage } from 'element-plus'
import { adminApi } from '@/api'
import { useUserStore } from '@/store/user'

const router = useRouter()
const userStore = useUserStore()
const overview = ref({})
const now = ref(dayjs())
let timer = null

const quickEntries = [
  { path: '/admin/screen', label: '数据大屏', icon: 'DataAnalysis' },
  { path: '/admin/notices', label: '消息通知', icon: 'ChatDotRound' },
  { path: '/admin/announcements', label: '公告中心', icon: 'Bell' },
  { path: '/admin/approval', label: '审批中心', icon: 'DocumentChecked' },
  { path: '/admin/labs', label: '实验室管理', icon: 'OfficeBuilding' }
]

const displayName = computed(() => userStore.realName || userStore.userInfo?.username || '管理员')
const greeting = computed(() => {
  const hour = now.value.hour()
  if (hour < 6) return '凌晨好'
  if (hour < 11) return '上午好'
  if (hour < 14) return '中午好'
  if (hour < 18) return '下午好'
  return '晚上好'
})
const currentDate = computed(() => now.value.format('YYYY-MM-DD dddd'))
const currentTime = computed(() => now.value.format('HH:mm:ss'))

function go(path) {
  router.push(path)
}

onMounted(async () => {
  timer = window.setInterval(() => {
    now.value = dayjs()
  }, 1000)

  try {
    const res = await adminApi.dashboard()
    overview.value = res.data || {}
  } catch (error) {
    ElMessage.error(error?.message || '加载工作台数据失败')
  }
})

onBeforeUnmount(() => {
  if (timer) {
    window.clearInterval(timer)
    timer = null
  }
})
</script>

<style scoped>
.workbench-page {
  display: flex;
  flex-direction: column;
}
.card {
  background: #fff;
  border-radius: 0.9rem;
  box-shadow: 0 2px 10px rgba(15, 23, 42, 0.06);
  padding: 1.2rem;
}
.hero {
  display: grid;
  grid-template-columns: 1.2fr 0.8fr;
  gap: 1rem;
  background: linear-gradient(135deg, #1d4ed8, #3b82f6);
  color: #fff;
}
.hero-badge {
  margin: 0;
  display: inline-block;
  font-size: 0.75rem;
  padding: 0.2rem 0.65rem;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.15);
}
.hero-title {
  margin: 0.6rem 0 0;
  font-size: 1.5rem;
}
.hero-sub {
  margin: 0.6rem 0 0;
  line-height: 1.7;
  color: rgba(255, 255, 255, 0.92);
}
.hero-meta {
  display: grid;
  gap: 0.75rem;
}
.meta-item {
  background: rgba(255, 255, 255, 0.12);
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 0.8rem;
  padding: 0.9rem 1rem;
}
.meta-label {
  display: block;
  font-size: 0.8rem;
  opacity: 0.85;
}
.meta-value {
  display: block;
  margin-top: 0.35rem;
}
.section-head {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  margin-bottom: 0.9rem;
}
.section-head h3 {
  margin: 0;
  font-size: 1rem;
}
.section-sub {
  font-size: 0.8rem;
  color: #94a3b8;
}
.summary-list {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.8rem;
}
.summary-item {
  border: 1px solid #e2e8f0;
  border-radius: 0.75rem;
  padding: 0.9rem 1rem;
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.summary-item span {
  color: #64748b;
}
.summary-item strong {
  font-size: 1.35rem;
  color: #1e293b;
}
.summary-actions {
  display: flex;
  gap: 0.75rem;
  margin-top: 1rem;
}
.warning {
  color: #d97706 !important;
}
.success {
  color: #059669 !important;
}
.danger {
  color: #dc2626 !important;
}
.quick-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0.75rem;
}
.quick-item {
  border: 1px solid #e2e8f0;
  border-radius: 0.75rem;
  background: #fff;
  height: 3rem;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.45rem;
  cursor: pointer;
}
.quick-item:hover {
  border-color: #93c5fd;
  color: #2563eb;
}
@media (max-width: 900px) {
  .hero {
    grid-template-columns: 1fr;
  }
  .quick-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
@media (max-width: 640px) {
  .summary-list {
    grid-template-columns: 1fr;
  }
  .quick-grid {
    grid-template-columns: 1fr;
  }
}
</style>
