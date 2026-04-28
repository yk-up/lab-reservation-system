<template>
  <div class="page-container">
    <h2 class="page-title mb-2">我的预约</h2>

    <div class="filter-bar card-like mb-2">
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
      <div v-for="item in filteredList" :key="item.id" class="reservation-card">
        <div class="res-header">
          <h3 class="res-title">{{ item.title }}</h3>
          <span class="status-badge" :class="statusClass(item.status)">
            {{ statusText(item.status) }}
          </span>
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

        <div v-if="item.rejectReason" class="reject-reason">
          <el-icon color="#f56c6c"><WarningFilled /></el-icon>
          拒绝原因：{{ item.rejectReason }}
        </div>

        <div class="res-footer">
          <span class="create-time">申请时间：{{ formatDateTime(item.createTime) }}</span>
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

function formatDateTime(t) {
  return t ? dayjs(t).format('MM-DD HH:mm') : '-'
}

function formatTime(t) {
  return t ? dayjs(t).format('HH:mm') : '-'
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
.page-title { font-size: 1.25rem; font-weight: 600; color: #303133; }
.loading-wrap { padding: 1rem 0; }

.card-like {
  background: #fff;
  border-radius: 0.75rem;
  padding: 1rem 1.25rem;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
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
  background: #fff;
  border-radius: 0.75rem;
  padding: 1.25rem;
  box-shadow: 0 2px 8px rgba(0,0,0,0.06);
}

.res-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 0.75rem;
  gap: 0.5rem;
}

.res-title {
  font-size: 1rem;
  font-weight: 600;
  color: #303133;
  flex: 1;
}

.res-info {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
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
}

.create-time { font-size: 0.75rem; color: #c0c4cc; }
</style>
