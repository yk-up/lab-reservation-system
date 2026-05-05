<template>
  <div class="page-container">
    <div class="user-page-header mb-2">
      <div>
        <h2 class="page-title user-page-title">我的预约</h2>
        <p class="user-page-subtitle">按状态和时间快速查看预约进度，支持详情展开与操作</p>
      </div>
    </div>

    <div class="filter-bar card-like user-card user-filter-bar mb-2">
      <div class="filter-row">
        <span class="filter-label">预约日期</span>
        <el-date-picker
          v-model="filterDate"
          type="date"
          placeholder=""
          format="YYYY-MM-DD"
          value-format="YYYY-MM-DD"
          clearable
          style="width: 100%; max-width: 220px"
        />
      </div>
      <div class="filter-row">
        <span class="filter-label">实验室</span>
        <el-input v-model="filterLabName" placeholder="" clearable style="width: 100%; max-width: 280px" />
      </div>
      <el-button text type="primary" @click="clearFilters">清除筛选</el-button>
    </div>

    <el-tabs v-model="activeTab">
      <el-tab-pane label="全部" name="all" />
      <el-tab-pane label="待审核" name="0" />
      <el-tab-pane label="已通过" name="1" />
      <el-tab-pane label="已取消/拒绝" name="other" />
    </el-tabs>

    <div v-if="loading" class="loading-wrap">
      <el-skeleton animated :count="3" />
    </div>

    <div v-else-if="filteredList.length === 0" class="mt-3">
      <AppEmptyState
        type="reservation"
        :title="emptyTitle"
        :description="emptyDescription"
        :action-text="hasActiveFilters ? '' : '去预约实验室'"
        @action="goToLabs"
      />
    </div>

    <div v-else class="reservation-list mt-2">
      <div v-for="item in filteredList" :key="item.id" class="reservation-card user-card user-card-hover">
        <div class="res-header">
          <h3 class="res-title">{{ item.title }}</h3>
          <div class="res-header-right">
            <span class="status-badge" :class="statusClass(item.status)">
              {{ statusText(item.status) }}
            </span>
            <span class="time-tip">{{ reservationTimeHint(item) }}</span>
          </div>
        </div>

        <div class="res-info">
          <div class="info-item">
            <el-icon><OfficeBuilding /></el-icon>
            <span>{{ displayLabName(item) }}</span>
          </div>
          <div class="info-item">
            <el-icon><Clock /></el-icon>
            <span>{{ formatDateTime(item.startTime) }} ~ {{ formatTime(item.endTime) }}</span>
          </div>
          <div class="info-item">
            <el-icon><User /></el-icon>
            <span>{{ item.attendees }} 人参与</span>
          </div>
        </div>

        <div class="res-tags">
          <el-tag size="small" effect="plain" :type="reminderTagType(item)">
            {{ reminderTagText(item) }}
          </el-tag>
          <el-tag size="small" type="info" effect="plain">
            时段：{{ formatDateTimeFull(item.startTime) }} ~ {{ formatTime(item.endTime) }}
          </el-tag>
        </div>

        <div v-if="item.rejectReason" class="reject-reason">
          <el-icon color="#f56c6c"><WarningFilled /></el-icon>
          拒绝原因：{{ item.rejectReason }}
        </div>

        <div class="res-footer">
          <span class="create-time">申请时间：{{ formatDateTime(item.createTime) }}</span>
          <div class="footer-actions">
            <el-button size="small" text type="primary" @click="openDetail(item)">查看详情</el-button>
            <el-button
              v-if="item.status === 0 || item.status === 1"
              type="danger"
              size="small"
              plain
              :loading="cancelingId === item.id"
              @click="cancelReservation(item)"
            >
              取消预约
            </el-button>
          </div>
        </div>
      </div>
    </div>

    <el-dialog v-model="detailVisible" title="预约详情" width="560px">
      <template v-if="currentReservation">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="预约主题">{{ currentReservation.title || '-' }}</el-descriptions-item>
          <el-descriptions-item label="实验室">{{ displayLabName(currentReservation) }}</el-descriptions-item>
          <el-descriptions-item label="预约时段">
            {{ formatDateTimeFull(currentReservation.startTime) }} ~ {{ formatDateTimeFull(currentReservation.endTime) }}
          </el-descriptions-item>
          <el-descriptions-item label="人数">{{ currentReservation.attendees || 0 }} 人</el-descriptions-item>
          <el-descriptions-item label="当前状态">
            <el-tag size="small" :type="reminderTagType(currentReservation)">
              {{ statusText(currentReservation.status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="状态提醒">{{ reminderTagText(currentReservation) }}</el-descriptions-item>
          <el-descriptions-item label="时间提示">{{ reservationTimeHint(currentReservation) }}</el-descriptions-item>
          <el-descriptions-item label="申请时间">{{ formatDateTimeFull(currentReservation.createTime) }}</el-descriptions-item>
          <el-descriptions-item v-if="currentReservation.rejectReason" label="拒绝原因">
            {{ currentReservation.rejectReason }}
          </el-descriptions-item>
        </el-descriptions>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { WarningFilled } from '@element-plus/icons-vue'
import dayjs from 'dayjs'
import { reservationApi } from '@/api'
import AppEmptyState from '@/components/AppEmptyState.vue'

const router = useRouter()
const loading = ref(true)
const list = ref([])
const activeTab = ref('all')
const cancelingId = ref(null)
const detailVisible = ref(false)
const currentReservation = ref(null)
/** 仅按自然日筛选：预约时段与该日有交集即显示 */
const filterDate = ref(null)
const filterLabName = ref('')

const filteredList = computed(() => {
  let rows = list.value
  if (activeTab.value === 'other') rows = rows.filter(r => r.status === 2 || r.status === 3)
  else if (activeTab.value !== 'all') rows = rows.filter(r => String(r.status) === activeTab.value)

  const kw = filterLabName.value.trim().toLowerCase()
  if (kw) {
    rows = rows.filter(r => (displayLabName(r).toLowerCase()).includes(kw))
  }

  if (filterDate.value) {
    const dayStart = dayjs(filterDate.value).startOf('day')
    const dayEndExclusive = dayStart.add(1, 'day')
    rows = rows.filter(r => {
      if (!r.startTime || !r.endTime) return false
      const st = dayjs(r.startTime)
      const et = dayjs(r.endTime)
      return st.isBefore(dayEndExclusive) && et.isAfter(dayStart)
    })
  }

  return rows
})

const hasActiveFilters = computed(() => {
  return Boolean(filterDate.value || filterLabName.value.trim())
})

const emptyTitle = computed(() => {
  if (hasActiveFilters.value && list.value.length > 0) return '没有符合筛选条件的预约'
  if (activeTab.value === 'all') return '暂无预约记录'
  return '当前分类下暂无预约记录'
})

const emptyDescription = computed(() => {
  if (hasActiveFilters.value && list.value.length > 0) {
    return '试试换一天、改实验室名称，或点击「清除筛选」。'
  }
  if (activeTab.value === 'all') return '你还没有提交过实验室预约，先去挑选一个合适的实验室吧。'
  return '当前标签下还没有对应的预约记录，可以切换其他分类查看。'
})

function clearFilters() {
  filterDate.value = null
  filterLabName.value = ''
}

/** 仅展示实验室中文名称（接口 lab_name），不展示数字 ID */
function displayLabName(item) {
  const n = item?.labName
  if (n != null && String(n).trim() !== '') return String(n).trim()
  return '未获取实验室名称'
}

const statusText = (s) => ['待审核', '已通过', '已拒绝', '已取消', '已完成'][s] ?? '未知'
const statusClass = (s) => ['status-pending', 'status-approved', 'status-rejected', 'status-canceled', 'status-finished'][s] ?? ''

function safeDayjs(t) {
  return t ? dayjs(t) : null
}

function betweenNowText(target) {
  const now = dayjs()
  const diff = target.diff(now)
  const absMinutes = Math.max(0, Math.round(Math.abs(diff) / 60000))
  const days = Math.floor(absMinutes / (24 * 60))
  const hours = Math.floor((absMinutes % (24 * 60)) / 60)
  const minutes = absMinutes % 60
  const text = []
  if (days) text.push(`${days}天`)
  if (hours) text.push(`${hours}小时`)
  if (!days && minutes) text.push(`${minutes}分钟`)
  const delta = text.length ? text.join('') : '1分钟内'
  return diff >= 0 ? `${delta}后` : `${delta}前`
}

function reservationTimeHint(item) {
  const now = dayjs()
  const start = safeDayjs(item.startTime)
  const end = safeDayjs(item.endTime)
  if (!start || !end) return '时间信息不完整'
  if (item.status === 0) return `提交于 ${betweenNowText(safeDayjs(item.createTime) || now)}`
  if (item.status === 2) return '已拒绝，请查看原因后重新预约'
  if (item.status === 3) return '已取消，不再占用时段'
  if (item.status === 4) return '该预约流程已完成'
  if (end.isBefore(now)) return `已结束 ${betweenNowText(end)}`
  if (start.isAfter(now)) return `距开始 ${betweenNowText(start)}`
  return `进行中，距结束 ${betweenNowText(end)}`
}

function reminderTagText(item) {
  const now = dayjs()
  const start = safeDayjs(item.startTime)
  const end = safeDayjs(item.endTime)
  if (item.status === 0) return '等待管理员审核'
  if (item.status === 2) return '审核未通过'
  if (item.status === 3) return '预约已取消'
  if (item.status === 4) return '预约已完成'
  if (!start || !end) return '请核对预约时段'
  if (start.isAfter(now)) return '即将开始'
  if (end.isBefore(now)) return '预约已结束'
  return '预约进行中'
}

function reminderTagType(item) {
  if (item.status === 0) return 'warning'
  if (item.status === 2 || item.status === 3) return 'danger'
  if (item.status === 4) return 'success'
  const now = dayjs()
  const start = safeDayjs(item.startTime)
  const end = safeDayjs(item.endTime)
  if (!start || !end) return 'info'
  if (start.isAfter(now)) return 'info'
  if (end.isBefore(now)) return 'success'
  return 'primary'
}

function formatDateTime(t) {
  return t ? dayjs(t).format('MM-DD HH:mm') : '-'
}

function formatDateTimeFull(t) {
  return t ? dayjs(t).format('YYYY-MM-DD HH:mm') : '-'
}

function formatTime(t) {
  return t ? dayjs(t).format('HH:mm') : '-'
}

function openDetail(item) {
  currentReservation.value = item
  detailVisible.value = true
}

function goToLabs() {
  router.push('/labs')
}

async function cancelReservation(item) {
  await ElMessageBox.confirm(`确认取消预约「${item.title}」？`, '取消确认', { type: 'warning' })
  cancelingId.value = item.id
  try {
    await reservationApi.cancel(item.id)
    ElMessage.success('预约已取消')
    item.status = 3
    item.cancelTime = new Date().toISOString()
  } finally {
    cancelingId.value = null
  }
}

onMounted(async () => {
  try {
    const res = await reservationApi.myList()
    list.value = res.data || []
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.loading-wrap { padding: 1rem 0; }

.card-like {
  border-radius: var(--user-radius-lg);
  padding: 1rem 1.25rem;
}
.filter-bar {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-end;
  gap: 0.75rem 1rem;
}
.filter-row {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  flex-wrap: wrap;
}
.filter-label {
  font-size: 0.875rem;
  color: #606266;
  white-space: nowrap;
}

.reservation-list {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.reservation-card {
  border-radius: var(--user-radius-lg);
  padding: 1.25rem;
}

.res-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 0.75rem;
  gap: 0.5rem;
}
.res-header-right {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 0.25rem;
}

.res-title {
  font-size: 1rem;
  font-weight: 600;
  color: #303133;
  flex: 1;
}
.time-tip {
  font-size: 0.75rem;
  color: #909399;
}

.res-info {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
  margin-bottom: 0.75rem;
}
.res-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
  margin-bottom: 0.75rem;
}

.info-item {
  display: flex;
  align-items: center;
  gap: 0.3rem;
  font-size: 0.8rem;
  color: #606266;
}

.reject-reason {
  display: flex;
  align-items: flex-start;
  gap: 0.4rem;
  font-size: 0.8rem;
  color: #f56c6c;
  background: #fef0f0;
  padding: 0.5rem 0.75rem;
  border-radius: 0.375rem;
  margin-bottom: 0.75rem;
}

.res-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 0.75rem;
  flex-wrap: wrap;
}

.create-time { font-size: 0.75rem; color: #c0c4cc; }
.footer-actions {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}
</style>
