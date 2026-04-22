<template>
  <div>
    <div class="flex-between mb-2 header-row">
      <h2 class="page-title">预约审核</h2>
      <el-button :icon="Refresh" @click="loadData">刷新</el-button>
    </div>

    <div class="card filter-card mb-2">
      <el-form :inline="true" :model="filters" class="filter-form">
        <el-form-item>
          <el-input
            v-model="filters.keyword"
            placeholder="搜索姓名 / 用户名 / 实验室 / 用途"
            clearable
            style="width: 260px"
            @keyup.enter="loadData"
          />
        </el-form-item>
        <el-form-item>
          <el-select v-model="filters.status" placeholder="状态筛选" clearable style="width: 140px">
            <el-option label="待审核" :value="0" />
            <el-option label="已通过" :value="1" />
            <el-option label="已拒绝" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-date-picker
            v-model="filters.dateRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            style="width: 360px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadData">查询</el-button>
          <el-button @click="resetFilters">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="card">
      <el-table :data="pagedList" stripe v-loading="loading" style="width: 100%">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column label="申请人" width="180">
          <template #default="{ row }">
            <div>{{ row.realName || '-' }}</div>
            <div style="font-size: 12px; color: #909399;">{{ row.username || row.userId }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="预约用途" min-width="150" show-overflow-tooltip />
        <el-table-column label="实验室" width="150">
          <template #default="{ row }">{{ row.labName || `实验室 #${row.labId}` }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" effect="light">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="预约时间" min-width="190">
          <template #default="{ row }">
            {{ formatDateTime(row.startTime) }} ~ {{ formatTime(row.endTime) }}
          </template>
        </el-table-column>
        <el-table-column prop="attendees" label="人数" width="90">
          <template #default="{ row }">
            <span>{{ row.attendees }} 人</span>
          </template>
        </el-table-column>
        <el-table-column label="申请时间" width="140">
          <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <template v-if="row.status === 0">
              <el-button type="success" size="small" @click="approve(row)" :loading="processingId === row.id + '_1'">
                通过
              </el-button>
              <el-button type="danger" size="small" plain @click="openReject(row)">拒绝</el-button>
            </template>
            <span v-else style="color: #909399; font-size: 12px">已处理</span>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && list.length === 0" description="暂无预约记录" />

      <div class="pagination-wrap" v-if="list.length > pageSize">
        <el-pagination
          v-model:current-page="currentPage"
          :page-size="pageSize"
          layout="total, prev, pager, next"
          :total="list.length"
          background
        />
      </div>
    </div>

    <el-dialog v-model="rejectVisible" title="拒绝预约" width="420px">
      <p style="margin-bottom: 0.75rem; font-size: 0.875rem; color: #606266;">
        预约：<strong>{{ currentItem?.title }}</strong>
      </p>
      <el-alert
        type="warning"
        :closable="false"
        show-icon
        style="margin-bottom: 12px"
        title="请尽量填写明确拒绝原因，方便申请人理解并修改后重新提交。"
      />
      <el-input
        v-model="rejectReason"
        type="textarea"
        :rows="3"
        maxlength="200"
        show-word-limit
        placeholder="请填写拒绝原因，将通知给申请人（建议填写）"
      />
      <template #footer>
        <el-button @click="rejectVisible = false">取消</el-button>
        <el-button type="danger" :loading="!!processingId" @click="confirmReject">确认拒绝</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, reactive, ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import dayjs from 'dayjs'
import { reservationApi } from '@/api'

const loading = ref(true)
const list = ref([])
const rejectVisible = ref(false)
const rejectReason = ref('')
const currentItem = ref(null)
const processingId = ref('')
const currentPage = ref(1)
const pageSize = 8

const filters = reactive({
  keyword: '',
  status: 0,
  dateRange: []
})

const pagedList = computed(() => {
  const start = (currentPage.value - 1) * pageSize
  return list.value.slice(start, start + pageSize)
})

const formatDateTime = t => (t ? dayjs(t).format('MM-DD HH:mm') : '-')
const formatTime = t => (t ? dayjs(t).format('HH:mm') : '-')
const statusText = s => ['待审核', '已通过', '已拒绝', '已取消', '已完成'][s] ?? '未知'
const statusTagType = s => ({ 0: 'warning', 1: 'success', 2: 'danger', 3: 'info', 4: '' }[s] || 'info')

async function loadData() {
  loading.value = true
  try {
    const [start, end] = filters.dateRange || []
    const res = await reservationApi.adminList({
      status: filters.status,
      keyword: filters.keyword || undefined,
      startTime: start ? dayjs(start).format('YYYY-MM-DDTHH:mm:ss') : undefined,
      endTime: end ? dayjs(end).format('YYYY-MM-DDTHH:mm:ss') : undefined
    })
    list.value = res.data || []
    currentPage.value = 1
  } finally {
    loading.value = false
  }
}

function resetFilters() {
  filters.keyword = ''
  filters.status = 0
  filters.dateRange = []
  loadData()
}

async function approve(row) {
  processingId.value = row.id + '_1'
  try {
    await reservationApi.audit(row.id, { status: 1 })
    ElMessage.success('已通过审核，通知已发送给申请人')
    row.status = 1
  } finally {
    processingId.value = ''
  }
}

function openReject(row) {
  currentItem.value = row
  rejectReason.value = row.rejectReason || ''
  rejectVisible.value = true
}

async function confirmReject() {
  if (!rejectReason.value.trim()) {
    ElMessage.warning('建议填写拒绝原因，便于申请人查看')
  }
  processingId.value = currentItem.value.id + '_2'
  try {
    await reservationApi.audit(currentItem.value.id, {
      status: 2,
      rejectReason: rejectReason.value
    })
    ElMessage.success('已拒绝，通知已发送给申请人')
    currentItem.value.status = 2
    currentItem.value.rejectReason = rejectReason.value
    rejectVisible.value = false
  } finally {
    processingId.value = ''
  }
}

onMounted(loadData)
</script>

<style scoped>
.page-title { font-size: 1.25rem; font-weight: 600; }
.header-row { align-items: center; }
.filter-card { padding-bottom: 0.25rem; }
.filter-form { display: flex; flex-wrap: wrap; gap: 0.25rem; }
.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 1rem;
}
</style>
