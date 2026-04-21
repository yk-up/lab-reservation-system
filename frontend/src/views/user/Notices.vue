<template>
  <div class="page-container">
    <div class="flex-between mb-2">
      <h2 class="page-title">消息通知</h2>
      <el-button size="small" text @click="readAll">全部已读</el-button>
    </div>

    <div v-if="loading">
      <el-skeleton animated :count="4" />
    </div>

    <div v-else-if="notices.length === 0" class="mt-3">
      <el-empty description="暂无消息" />
    </div>

    <div v-else class="notice-list">
      <div
        v-for="n in notices"
        :key="n.id"
        class="notice-item"
        :class="{ unread: n.isRead === 0 }"
        @click="markRead(n)"
      >
        <div class="notice-dot" v-if="n.isRead === 0" />
        <el-icon :color="typeColor(n.type)" size="20">
          <component :is="typeIcon(n.type)" />
        </el-icon>
        <div class="notice-body">
          <div class="notice-title">{{ n.title }}</div>
          <div class="notice-content">{{ n.content }}</div>
          <div class="notice-time">{{ formatTime(n.createTime) }}</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import dayjs from 'dayjs'
import { noticeApi } from '@/api'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()
const loading = ref(true)
const notices = ref([])
let timer = null

const typeIcon = (t) => ({ 1: 'CircleCheckFilled', 2: 'CircleCloseFilled', 3: 'AlarmClock', 4: 'Bell' }[t] || 'Bell')
const typeColor = (t) => ({ 1: '#67c23a', 2: '#f56c6c', 3: '#e6a23c', 4: '#409eff' }[t] || '#909399')
const formatTime = (t) => t ? dayjs(t).format('MM-DD HH:mm') : ''

async function loadNotices() {
  const res = await noticeApi.list()
  notices.value = res.data || []
}

async function loadUnread() {
  const previousCount = userStore.unreadCount
  const res = await noticeApi.unreadCount()
  const nextCount = res.data || 0
  userStore.setUnreadCount(nextCount)

  if (nextCount !== previousCount) {
    await loadNotices()
  }
}

async function markRead(n) {
  if (n.isRead === 1) return
  n.isRead = 1
  userStore.setUnreadCount(userStore.unreadCount - 1)
  noticeApi.read(n.id).catch(() => {})
}

async function readAll() {
  await noticeApi.readAll()
  notices.value.forEach(n => { n.isRead = 1 })
  userStore.setUnreadCount(0)
  ElMessage.success('已全部标为已读')
}

onMounted(async () => {
  try {
    await loadNotices()
    await loadUnread()
    timer = setInterval(() => {
      loadUnread().catch(() => {})
    }, 30000)
  } finally {
    loading.value = false
  }
})

onUnmounted(() => {
  clearInterval(timer)
})
</script>

<style scoped>
.page-title { font-size: 1.25rem; font-weight: 600; }

.notice-list {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.notice-item {
  background: #fff;
  border-radius: 0.75rem;
  padding: 1rem 1.25rem;
  display: flex;
  align-items: flex-start;
  gap: 0.75rem;
  cursor: pointer;
  position: relative;
  box-shadow: 0 1px 6px rgba(0,0,0,0.05);
  border: 1px solid transparent;
  transition: border-color 0.2s;
}

.notice-item.unread {
  border-color: #a0cfff;
  background: #f0f7ff;
}

.notice-dot {
  position: absolute;
  top: 0.75rem;
  right: 0.75rem;
  width: 8px;
  height: 8px;
  background: #f56c6c;
  border-radius: 50%;
}

.notice-body { flex: 1; }
.notice-title { font-size: 0.9rem; font-weight: 600; color: #303133; margin-bottom: 0.25rem; }
.notice-content { font-size: 0.8rem; color: #606266; line-height: 1.5; margin-bottom: 0.375rem; }
.notice-time { font-size: 0.75rem; color: #c0c4cc; }
</style>
