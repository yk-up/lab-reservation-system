<template>
  <div class="page-container">
    <h2 class="page-title mb-2">我的预约</h2>

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
      <el-empty description="暂无预约记录" />
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
            <span>实验室 #{{ item.labId }}</span>
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
import { ElMessage, ElMessageBox } from 'element-plus'
import { WarningFilled } from '@element-plus/icons-vue'
import dayjs from 'dayjs'
import { reservationApi } from '@/api'

const loading = ref(true)
const list = ref([])
const activeTab = ref('all')
const cancelingId = ref(null)

const filteredList = computed(() => {
  if (activeTab.value === 'all') return list.value
  if (activeTab.value === 'other') return list.value.filter(r => r.status === 2 || r.status === 3)
  return list.value.filter(r => String(r.status) === activeTab.value)
})

const statusText = (s) => ['待审核', '已通过', '已拒绝', '已取消', '已完成'][s] ?? '未知'
const statusClass = (s) => ['status-pending', 'status-approved', 'status-rejected', 'status-canceled', 'status-finished'][s] ?? ''

function formatDateTime(t) {
  return t ? dayjs(t).format('MM-DD HH:mm') : '-'
}

function formatTime(t) {
  return t ? dayjs(t).format('HH:mm') : '-'
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
