<template>
  <div class="page-container">
    <div class="header-wrap user-page-header mb-2">
      <div>
        <h2 class="page-title user-page-title">消息通知</h2>
        <p class="user-page-subtitle">集中查看审核结果与预约提醒；可按类型、已读状态与关键词筛选（首版纯前端，基于当前列表数据）</p>
      </div>
      <div class="header-actions">
        <el-tag v-if="userStore.unreadCount > 0" type="danger" effect="dark">未读 {{ userStore.unreadCount }}</el-tag>
        <el-button size="small" text @click="readAll">全部已读</el-button>
      </div>
    </div>

    <div v-if="loading">
      <el-skeleton animated :count="4" />
    </div>

    <div v-else-if="notices.length === 0" class="mt-3">
      <AppEmptyState
        type="notice"
        title="暂无消息通知"
        :description="emptyDescription"
        :secondary-action-text="emptySecondaryText"
        :action-text="emptyPrimaryText"
        @secondary-action="goBack"
        @action="goEmptyPrimary"
      />
    </div>

    <template v-else>
      <div class="notice-toolbar user-card mb-2">
        <div class="toolbar-section">
          <span class="toolbar-label">类型</span>
          <el-radio-group v-model="filterType" size="small">
            <el-radio-button label="all">全部{{ countSuffix(typeCounts.all) }}</el-radio-button>
            <el-radio-button label="1">通过{{ countSuffix(typeCounts[1]) }}</el-radio-button>
            <el-radio-button label="2">拒绝{{ countSuffix(typeCounts[2]) }}</el-radio-button>
            <el-radio-button label="3">提醒{{ countSuffix(typeCounts[3]) }}</el-radio-button>
            <el-radio-button label="4">通知{{ countSuffix(typeCounts[4]) }}</el-radio-button>
          </el-radio-group>
        </div>
        <div class="toolbar-section">
          <span class="toolbar-label">状态</span>
          <el-radio-group v-model="filterRead" size="small">
            <el-radio-button label="all">全部{{ countSuffix(readCounts.all) }}</el-radio-button>
            <el-radio-button label="unread">未读{{ countSuffix(readCounts.unread) }}</el-radio-button>
            <el-radio-button label="read">已读{{ countSuffix(readCounts.read) }}</el-radio-button>
          </el-radio-group>
        </div>
        <div class="toolbar-section toolbar-search">
          <el-input
            v-model="keyword"
            clearable
            placeholder="搜索标题或正文"
            style="max-width: 280px"
          />
          <el-button size="small" text type="primary" :disabled="!filtersActive" @click="resetFilters">
            重置筛选
          </el-button>
        </div>
        <div class="toolbar-meta">
          符合条件的共 <strong>{{ filteredNotices.length }}</strong> 条
          <template v-if="filtersActive">（全量 {{ notices.length }} 条）</template>
        </div>
      </div>

      <div v-if="filteredNotices.length === 0" class="filter-empty user-card mt-2">
        <el-empty description="没有符合当前筛选条件的消息">
          <el-button type="primary" @click="resetFilters">清空筛选条件</el-button>
        </el-empty>
      </div>

      <div v-else class="notice-list">
        <div
          v-for="n in pagedNotices"
          :key="n.id"
          class="notice-item user-card user-card-hover"
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

      <div v-if="filteredNotices.length" class="list-footer">
        <el-pagination
          v-if="filteredNotices.length > pageSize"
          v-model:current-page="currentPage"
          :page-size="pageSize"
          layout="prev, pager, next"
          :total="filteredNotices.length"
          background
        />
        <el-button
          v-if="visibleCount < filteredNotices.length"
          class="load-more"
          plain
          @click="loadMore"
        >
          加载更多
        </el-button>
      </div>
    </template>
  </div>
</template>

<script setup>
import { computed, ref, watch, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import dayjs from 'dayjs'
import { noticeApi } from '@/api'
import { useUserStore } from '@/store/user'
import AppEmptyState from '@/components/AppEmptyState.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

/** 管理端 LABRES-07：复用同一页面，由路由名区分行为与跳转 */
const isAdminNotices = computed(() => route.name === 'AdminNotices')

const emptyDescription = computed(() =>
  isAdminNotices.value
    ? '当前账号暂无站内消息。若您同时作为用户预约过实验室，审核与提醒会推送至此处。'
    : '当前还没有新的通知消息，预约审核结果和提醒会第一时间显示在这里。'
)
const emptySecondaryText = computed(() =>
  isAdminNotices.value ? '返回工作台' : '返回上一页'
)
const emptyPrimaryText = computed(() =>
  isAdminNotices.value ? '前往预约审核' : '查看我的预约'
)
const loading = ref(true)
const notices = ref([])
const currentPage = ref(1)
const pageSize = 6
const visibleCount = ref(12)
/** 首版前端筛选：all | '1'..'4' 对应 notice.type */
const filterType = ref('all')
const filterRead = ref('all')
const keyword = ref('')
let timer = null

const typeIcon = (t) => ({ 1: 'CircleCheckFilled', 2: 'CircleCloseFilled', 3: 'AlarmClock', 4: 'Bell' }[t] || 'Bell')
const typeColor = (t) => ({ 1: '#67c23a', 2: '#f56c6c', 3: '#409eff', 4: '#909399' }[t] || '#909399')
const formatTime = (t) => (t ? dayjs(t).format('MM-DD HH:mm') : '')
const statusTagText = (t) => ({ 1: '成功', 2: '失败', 3: '提醒', 4: '通知' }[t] || '通知')
const statusTagType = (t) => ({ 1: 'success', 2: 'danger', 3: 'primary', 4: 'info' }[t] || 'info')

const typeCounts = computed(() => {
  const m = { all: notices.value.length, 1: 0, 2: 0, 3: 0, 4: 0 }
  for (const n of notices.value) {
    if (m[n.type] != null) m[n.type] += 1
  }
  return m
})

const readCounts = computed(() => {
  let unread = 0
  for (const n of notices.value) {
    if (n.isRead === 0) unread += 1
  }
  return { all: notices.value.length, unread, read: notices.value.length - unread }
})

const filteredNotices = computed(() => {
  let list = notices.value
  if (filterType.value !== 'all') {
    const t = Number(filterType.value)
    list = list.filter(n => n.type === t)
  }
  if (filterRead.value === 'unread') list = list.filter(n => n.isRead === 0)
  if (filterRead.value === 'read') list = list.filter(n => n.isRead === 1)
  const kw = keyword.value.trim().toLowerCase()
  if (kw) {
    list = list.filter(n => {
      const title = String(n.title || '').toLowerCase()
      const content = String(n.content || '').toLowerCase()
      return title.includes(kw) || content.includes(kw)
    })
  }
  return list
})

const filtersActive = computed(
  () => filterType.value !== 'all' || filterRead.value !== 'all' || !!keyword.value.trim()
)

function countSuffix(n) {
  return n > 0 ? ` (${n})` : ''
}

function resetFilters() {
  filterType.value = 'all'
  filterRead.value = 'all'
  keyword.value = ''
  currentPage.value = 1
  visibleCount.value = 12
}

watch([filterType, filterRead, keyword], () => {
  currentPage.value = 1
  visibleCount.value = 12
})

watch(filteredNotices, list => {
  const maxPage = Math.max(1, Math.ceil(Math.min(list.length, visibleCount.value) / pageSize))
  if (currentPage.value > maxPage) currentPage.value = maxPage
})

const visibleNotices = computed(() => filteredNotices.value.slice(0, visibleCount.value))
const pagedNotices = computed(() => {
  const start = (currentPage.value - 1) * pageSize
  return visibleNotices.value.slice(start, start + pageSize)
})

function noticeTargetPath(n) {
  if (isAdminNotices.value) {
    if (n.type === 1 || n.type === 2 || n.type === 3) return '/admin/approval'
    if (n.type === 4) return '/admin/announcements'
    return '/admin/notices'
  }
  if (n.type === 1 || n.type === 2 || n.type === 3) return '/my-reservations'
  return '/notices'
}

function noticeTargetLabel(n) {
  if (isAdminNotices.value) {
    if (n.type === 1) return '审批中心'
    if (n.type === 2) return '审批中心'
    if (n.type === 3) return '审批中心'
    if (n.type === 4) return '公告中心'
    return ''
  }
  if (n.type === 1) return '查看我的预约'
  if (n.type === 2) return '查看拒绝详情'
  if (n.type === 3) return '查看预约安排'
  return ''
}

function goBack() {
  if (isAdminNotices.value) {
    router.push('/admin/workbench')
    return
  }
  router.back()
}

function goEmptyPrimary() {
  if (isAdminNotices.value) {
    router.push('/admin/approval')
    return
  }
  router.push('/my-reservations')
}

async function loadNotices() {
  const res = await noticeApi.list()
  notices.value = res.data || []
  const len = filteredNotices.value.length
  const maxPage = Math.max(1, Math.ceil(Math.min(len, visibleCount.value) / pageSize))
  if (currentPage.value > maxPage) currentPage.value = maxPage
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
  const cur = router.currentRoute.value.path
  if (path && cur !== path) {
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

function loadMore() {
  visibleCount.value = Math.min(visibleCount.value + 12, filteredNotices.value.length)
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
.filter-empty {
  padding: 2rem 1rem;
  border-radius: var(--user-radius-lg);
}
.notice-toolbar {
  padding: 1rem 1.25rem;
  border-radius: var(--user-radius-lg);
  display: flex;
  flex-direction: column;
  gap: 0.85rem;
}
.toolbar-section {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.5rem 0.75rem;
}
.toolbar-label {
  font-size: 0.8rem;
  color: #909399;
  min-width: 2.25rem;
  flex-shrink: 0;
}
.toolbar-search {
  justify-content: flex-start;
}
.toolbar-meta {
  font-size: 0.8rem;
  color: #909399;
  padding-top: 0.15rem;
}
.toolbar-meta strong {
  color: #606266;
}
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
  border-radius: var(--user-radius-lg);
  padding: 1rem 1.25rem;
  display: flex;
  align-items: flex-start;
  gap: 0.75rem;
  cursor: pointer;
  position: relative;
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
.load-more { margin-left: auto; }
@media (max-width: 768px) {
  .notice-head, .notice-footer, .header-wrap {
    flex-direction: column;
    align-items: flex-start;
  }
  .list-footer {
    justify-content: center;
  }
  .load-more { margin-left: 0; }
}
</style>
