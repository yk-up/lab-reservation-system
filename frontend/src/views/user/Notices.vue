<template>
  <div class="page-container">
    <div class="flex-between mb-2 header-wrap">
      <h2 class="page-title">消息通知</h2>
      <div class="header-actions">
        <el-tag v-if="userStore.unreadCount > 0" type="danger" effect="dark">未读 {{ userStore.unreadCount }}</el-tag>
        <el-button size="small" text @click="readAll">全部已读</el-button>
      </div>
    </div>

    <div v-if="loading">
      <el-skeleton animated :count="4" />
    </div>

    <div v-else-if="notices.length === 0" class="mt-3">
      <el-empty description="暂无消息" />
    </div>

    <template v-else>
      <div class="notice-list">
        <div
          v-for="n in notices"
          :key="n.id"
          class="notice-item"
          :class="{ unread: n.isRead === 0 }"
          @click="handleNoticeClick(n)"
        >
          <div class="notice-dot" v-if="n.isRead === 0" />
          <el-icon :color="typeColor(n.type)" size="20">
            <component :is="typeIcon(n.type)" />
          </el-icon>
          <div class="notice-body">
            <div class="notice-head">
              <div class="notice-title">{{ n.title }}</div>
              <div class="notice-tags">
                <el-tag size="small" :type="statusTagType(n.type)" effect="light">{{ statusTagText(n.type) }}</el-tag>
                <el-tag size="small" :type="n.isRead === 0 ? 'danger' : 'info'" effect="plain">
                  {{ n.isRead === 0 ? '未读' : '已读' }}
                </el-tag>
              </div>
            </div>
            <div class="notice-content">{{ n.content }}</div>
            <div class="notice-footer">
              <span class="notice-time">{{ formatTime(n.createTime) }}</span>
              <el-button v-if="noticeTargetLabel(n)" text type="primary" @click.stop="handleNoticeClick(n)">
                {{ noticeTargetLabel(n) }}
              </el-button>
            </div>
          </div>
        </div>
      </div>

      <div class="list-footer">
        <el-pagination
          v-if="total > pageSize"
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          layout="total, sizes, prev, pager, next"
          :page-sizes="[10, 20, 50]"
          :total="total"
          background
          @current-change="loadNotices"
          @size-change="onSizeChange"
        />
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import dayjs from 'dayjs'
import { noticeApi } from '@/api'
import { useUserStore } from '@/store/user'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(true)
const notices = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
let timer = null

const typeIcon = (t) => ({ 1: 'CircleCheckFilled', 2: 'CircleCloseFilled', 3: 'AlarmClock', 4: 'Bell' }[t] || 'Bell')
const typeColor = (t) => ({ 1: '#67c23a', 2: '#f56c6c', 3: '#409eff', 4: '#909399' }[t] || '#909399')
const formatTime = (t) => (t ? dayjs(t).format('MM-DD HH:mm') : '')
const statusTagText = (t) => ({ 1: '成功', 2: '失败', 3: '提醒', 4: '通知' }[t] || '通知')
const statusTagType = (t) => ({ 1: 'success', 2: 'danger', 3: 'primary', 4: 'info' }[t] || 'info')

function noticeTargetPath(n) {
  if (n.type === 1 || n.type === 2) return '/my-reservations'
  if (n.type === 3) return '/my-reservations'
  return '/notices'
}

function noticeTargetLabel(n) {
  if (n.type === 1) return '查看我的预约'
  if (n.type === 2) return '查看拒绝详情'
  if (n.type === 3) return '查看预约安排'
  return ''
}

async function loadNotices() {
  const res = await noticeApi.list({
    page: currentPage.value,
    pageSize: pageSize.value
  })
  const data = res.data || {}
  notices.value = data.list || []
  total.value = data.total || 0
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

async function handleNoticeClick(n) {
  await markRead(n)
  const path = noticeTargetPath(n)
  if (path && router.currentRoute.value.path !== path) {
    router.push(path)
  }
}

async function readAll() {
  await noticeApi.readAll()
  notices.value.forEach(n => {
    n.isRead = 1
  })
  userStore.setUnreadCount(0)
  ElMessage.success('已全部标为已读')
}

function onSizeChange() {
  currentPage.value = 1
  loadNotices()
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
.header-wrap, .header-actions, .notice-head, .notice-tags, .notice-footer, .list-footer {
  display: flex;
  align-items: center;
}
.header-actions, .notice-tags, .list-footer {
  gap: 0.5rem;
}
.notice-list {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
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
  transition: all 0.2s;
}
.notice-item:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 16px rgba(64, 158, 255, 0.12);
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
.notice-head {
  justify-content: space-between;
  gap: 0.75rem;
  margin-bottom: 0.375rem;
}
.notice-title { font-size: 0.95rem; font-weight: 600; color: #303133; }
.notice-content { font-size: 0.82rem; color: #606266; line-height: 1.6; margin-bottom: 0.5rem; }
.notice-footer {
  justify-content: space-between;
  gap: 1rem;
}
.notice-time { font-size: 0.75rem; color: #c0c4cc; }
.list-footer {
  justify-content: space-between;
  margin-top: 1rem;
  flex-wrap: wrap;
}
@media (max-width: 768px) {
  .notice-head, .notice-footer, .header-wrap {
    flex-direction: column;
    align-items: flex-start;
  }
  .list-footer {
    justify-content: center;
  }
}
</style>
