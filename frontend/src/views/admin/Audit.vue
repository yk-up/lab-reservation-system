<template>
  <div>
    <div class="flex-between mb-2">
      <h2 class="page-title">预约审核</h2>
      <el-button :icon="Refresh" @click="loadData">刷新</el-button>
    </div>

    <div class="card">
      <el-table :data="list" stripe v-loading="loading" style="width: 100%">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="title" label="预约用途" min-width="150" show-overflow-tooltip />
        <el-table-column label="实验室" width="120">
          <template #default="{ row }">实验室 #{{ row.labId }}</template>
        </el-table-column>
        <el-table-column label="预约时间" min-width="190">
          <template #default="{ row }">
            {{ formatDateTime(row.startTime) }} ~ {{ formatTime(row.endTime) }}
          </template>
        </el-table-column>
        <el-table-column prop="attendees" label="人数" width="70" />
        <el-table-column label="申请时间" width="120">
          <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button type="success" size="small" @click="approve(row)" :loading="processingId === row.id + '_1'">
              通过
            </el-button>
            <el-button type="danger" size="small" plain @click="openReject(row)">拒绝</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && list.length === 0" description="暂无待审核预约" />
    </div>

    <el-dialog v-model="rejectVisible" title="拒绝预约" width="420px">
      <p style="margin-bottom: 0.75rem; font-size: 0.875rem; color: #606266;">
        预约：<strong>{{ currentItem?.title }}</strong>
      </p>
      <el-input
        v-model="rejectReason"
        type="textarea"
        :rows="3"
        placeholder="请填写拒绝原因，将通知给申请人（选填）"
      />
      <template #footer>
        <el-button @click="rejectVisible = false">取消</el-button>
        <el-button type="danger" :loading="!!processingId" @click="confirmReject">确认拒绝</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
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

const formatDateTime = (t) => t ? dayjs(t).format('MM-DD HH:mm') : '-'
const formatTime = (t) => t ? dayjs(t).format('HH:mm') : '-'

async function loadData() {
  loading.value = true
  try {
    const res = await reservationApi.pending()
    list.value = res.data || []
  } finally {
    loading.value = false
  }
}

async function approve(row) {
  processingId.value = row.id + '_1'
  try {
    await reservationApi.audit(row.id, { status: 1 })
    ElMessage.success('已通过审核，通知已发送给申请人')
    list.value = list.value.filter(r => r.id !== row.id)
  } finally {
    processingId.value = ''
  }
}

function openReject(row) {
  currentItem.value = row
  rejectReason.value = ''
  rejectVisible.value = true
}

async function confirmReject() {
  processingId.value = currentItem.value.id + '_2'
  try {
    await reservationApi.audit(currentItem.value.id, {
      status: 2,
      rejectReason: rejectReason.value
    })
    ElMessage.success('已拒绝，通知已发送给申请人')
    list.value = list.value.filter(r => r.id !== currentItem.value.id)
    rejectVisible.value = false
  } finally {
    processingId.value = ''
  }
}

onMounted(loadData)
</script>

<style scoped>
.page-title { font-size: 1.25rem; font-weight: 600; }
</style>
