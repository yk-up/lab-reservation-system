<template>
  <div>
    <div class="flex-between mb-2 header-row">
      <h2 class="page-title">预约审核</h2>
      <div class="header-actions">
        <el-button @click="loadPendingOnly">仅待审核</el-button>
        <el-button :icon="Refresh" @click="loadData">刷新</el-button>
      </div>
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
      <div class="mb-2 table-toolbar">
        <el-button
          type="success"
          plain
          :disabled="selectedPendingIds.length === 0"
          :loading="processingBatch"
          @click="batchApprove"
        >
          批量通过（{{ selectedPendingIds.length }}）
        </el-button>
        <el-button
          type="danger"
          plain
          :disabled="selectedPendingIds.length === 0"
          :loading="processingBatch"
          @click="openBatchReject"
        >
          批量拒绝（{{ selectedPendingIds.length }}）
        </el-button>
      </div>

      <el-table :data="pagedList" stripe v-loading="loading" style="width: 100%" @selection-change="onSelectionChange">
        <el-table-column type="selection" width="50" :selectable="canSelectRow" />
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
        <el-table-column label="操作" width="230" fixed="right">
          <template #default="{ row }">
            <div class="action-group">
              <template v-if="row.status === 0">
                <el-button type="success" size="small" @click="approve(row)" :loading="processingId === row.id + '_1'">
                  通过
                </el-button>
                <el-button type="danger" size="small" plain @click="openReject(row)">拒绝</el-button>
              </template>
              <span v-else class="processed-text">已处理</span>
              <el-button size="small" @click="openDetail(row)">详情</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
      <AppEmptyState
        v-if="!loading && list.length === 0"
        :type="isFilterEmpty ? 'search' : 'audit'"
        :title="isFilterEmpty ? '未找到符合条件的审核记录' : '暂无审核记录'"
        :description="isFilterEmpty ? '当前筛选条件下没有匹配的审核记录，建议重置后重新查询。' : '当前还没有需要展示的审核记录，可以稍后刷新再查看。'"
        :secondary-action-text="isFilterEmpty ? '重置筛选' : ''"
        action-text="刷新列表"
        @secondary-action="resetFilters"
        @action="loadData"
      />

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

    <el-dialog v-model="detailVisible" title="预约申请详情" width="560px">
      <div v-if="detailItem" class="detail-panel">
        <div class="detail-grid">
          <div class="detail-item">
            <span class="detail-label">预约用途</span>
            <span class="detail-value">{{ detailItem.title || '-' }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">当前状态</span>
            <el-tag :type="statusTagType(detailItem.status)" effect="light">{{ statusText(detailItem.status) }}</el-tag>
          </div>
          <div class="detail-item">
            <span class="detail-label">申请人</span>
            <span class="detail-value">{{ detailItem.realName || '-' }}（{{ detailItem.username || detailItem.userId }}）</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">实验室</span>
            <span class="detail-value">{{ detailItem.labName || `实验室 #${detailItem.labId}` }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">预约时间</span>
            <span class="detail-value">{{ formatFullDateTime(detailItem.startTime) }} - {{ formatFullDateTime(detailItem.endTime) }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">参与人数</span>
            <span class="detail-value">{{ detailItem.attendees || 0 }} 人</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">申请时间</span>
            <span class="detail-value">{{ formatFullDateTime(detailItem.createTime) }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">更新时间</span>
            <span class="detail-value">{{ formatFullDateTime(detailItem.updateTime) }}</span>
          </div>
        </div>

        <div class="detail-block">
          <div class="detail-label">申请备注</div>
          <div class="detail-content">{{ detailItem.remark || '未填写备注' }}</div>
        </div>

        <div v-if="detailItem.rejectReason" class="detail-block">
          <div class="detail-label">拒绝原因</div>
          <div class="detail-content reject-text">{{ detailItem.rejectReason }}</div>
        </div>
      </div>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="batchRejectVisible" title="批量拒绝预约" width="420px">
      <p style="margin-bottom: 0.75rem; font-size: 0.875rem; color: #606266;">
        本次将拒绝 <strong>{{ selectedPendingIds.length }}</strong> 条待审核预约。
      </p>
      <el-input
        v-model="batchRejectReason"
        type="textarea"
        :rows="3"
        maxlength="200"
        show-word-limit
        placeholder="请填写拒绝原因，将通知给申请人（建议填写）"
      />
      <template #footer>
        <el-button @click="batchRejectVisible = false">取消</el-button>
        <el-button type="danger" :loading="processingBatch" @click="confirmBatchReject">确认批量拒绝</el-button>
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
import AppEmptyState from '@/components/AppEmptyState.vue'

const loading = ref(true)
const list = ref([])
const rejectVisible = ref(false)
const rejectReason = ref('')
const currentItem = ref(null)
const detailVisible = ref(false)
const detailItem = ref(null)
const processingId = ref('')
const processingBatch = ref(false)
const selectedRows = ref([])
const batchRejectVisible = ref(false)
const batchRejectReason = ref('')
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
const selectedPendingIds = computed(() =>
  selectedRows.value.filter(row => row.status === 0).map(row => row.id)
)
const isFilterEmpty = computed(() => Boolean(filters.keyword || filters.dateRange?.length))

const formatDateTime = t => (t ? dayjs(t).format('MM-DD HH:mm') : '-')
const formatFullDateTime = t => (t ? dayjs(t).format('YYYY-MM-DD HH:mm') : '-')
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

async function loadPendingOnly() {
  loading.value = true
  try {
    const res = await reservationApi.pending()
    list.value = res.data || []
    currentPage.value = 1
    filters.status = 0
    selectedRows.value = []
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

function canSelectRow(row) {
  return row.status === 0
}

function onSelectionChange(rows) {
  selectedRows.value = rows || []
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

function openDetail(row) {
  detailItem.value = row
  detailVisible.value = true
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

async function batchApprove() {
  if (selectedPendingIds.value.length === 0) {
    ElMessage.warning('请先选择待审核记录')
    return
  }
  processingBatch.value = true
  try {
    await reservationApi.batchAudit({
      ids: selectedPendingIds.value,
      status: 1
    })
    ElMessage.success(`批量通过成功，共处理 ${selectedPendingIds.value.length} 条`)
    await loadData()
  } finally {
    processingBatch.value = false
  }
}

function openBatchReject() {
  if (selectedPendingIds.value.length === 0) {
    ElMessage.warning('请先选择待审核记录')
    return
  }
  batchRejectReason.value = ''
  batchRejectVisible.value = true
}

async function confirmBatchReject() {
  processingBatch.value = true
  try {
    await reservationApi.batchAudit({
      ids: selectedPendingIds.value,
      status: 2,
      rejectReason: batchRejectReason.value
    })
    ElMessage.success(`批量拒绝成功，共处理 ${selectedPendingIds.value.length} 条`)
    batchRejectVisible.value = false
    await loadData()
  } finally {
    processingBatch.value = false
  }
}

onMounted(loadData)
</script>

<style scoped>
.page-title { font-size: 1.25rem; font-weight: 600; }
.header-row { align-items: center; }
.header-actions { display: flex; gap: 8px; }
.filter-card { padding-bottom: 0.25rem; }
.filter-form { display: flex; flex-wrap: wrap; gap: 0.25rem; }
.table-toolbar { display: flex; gap: 8px; }
.action-group {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.processed-text {
  color: #909399;
  font-size: 12px;
}
.detail-panel {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.detail-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px 20px;
}
.detail-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.detail-label {
  font-size: 12px;
  color: #909399;
}
.detail-value {
  color: #303133;
  line-height: 1.5;
  word-break: break-word;
}
.detail-block {
  padding: 12px 14px;
  border-radius: 10px;
  background: #f7f9fc;
}
.detail-content {
  margin-top: 6px;
  color: #303133;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
}
.reject-text {
  color: #f56c6c;
}
.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 1rem;
}

@media (max-width: 768px) {
  .detail-grid {
    grid-template-columns: 1fr;
  }
}
</style>
