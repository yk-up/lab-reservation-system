<template>
  <div>
    <div class="flex-between mb-2">
      <h2 class="page-title">黑名单管理</h2>
      <el-button type="danger" :icon="Plus" @click="addDialog = true">加入黑名单</el-button>
    </div>

    <div class="card">
      <el-table :data="list" stripe v-loading="loading" style="width: 100%">
        <el-table-column prop="userId" label="用户ID" width="90" />
        <el-table-column label="申请人" width="180">
          <template #default="{ row }">
            <div>{{ row.realName || '-' }}</div>
            <div style="font-size: 12px; color: #909399;">{{ row.username || row.userId }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="reason" label="封禁原因" min-width="200" show-overflow-tooltip />
        <el-table-column label="到期时间" width="160">
          <template #default="{ row }">
            <span v-if="!row.expireTime" class="text-danger">永久</span>
            <span v-else>{{ formatDateTime(row.expireTime) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="封禁时间" width="140">
          <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button type="success" size="small" plain @click="removeFromBlacklist(row)">解封</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && list.length === 0" description="黑名单为空" />
    </div>

    <el-dialog v-model="addDialog" title="加入黑名单" width="420px">
      <el-form :model="addForm" ref="addFormRef" label-width="80px">
        <el-form-item label="用户ID" prop="userId" :rules="[{ required: true, message: '请输入用户ID' }]">
          <el-input-number v-model="addForm.userId" :min="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="封禁原因" prop="reason" :rules="[{ required: true, message: '请填写原因' }]">
          <el-input v-model="addForm.reason" type="textarea" :rows="2" placeholder="封禁原因" />
        </el-form-item>
        <el-form-item label="到期时间">
          <el-date-picker
            v-model="addForm.expireTime"
            type="datetime"
            placeholder="不填则永久封禁"
            style="width: 100%"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addDialog = false">取消</el-button>
        <el-button type="danger" :loading="saving" @click="confirmAdd">确认加入</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import dayjs from 'dayjs'
import { adminApi } from '@/api'

const loading = ref(true)
const list = ref([])
const addDialog = ref(false)
const saving = ref(false)
const addFormRef = ref()
const addForm = reactive({ userId: null, reason: '', expireTime: null })

const formatDateTime = (t) => t ? dayjs(t).format('YYYY-MM-DD HH:mm') : '-'

async function removeFromBlacklist(row) {
  await ElMessageBox.confirm(
    `确认解封用户 ${row.realName || '-'}（${row.username || row.userId}）？`,
    '解封确认',
    { type: 'warning' }
  )
  await adminApi.removeBlacklist(row.id)
  list.value = list.value.filter(r => r.id !== row.id)
  ElMessage.success('已解封')
}

async function confirmAdd() {
  await addFormRef.value.validate()
  saving.value = true
  try {
    await adminApi.addBlacklist({
      userId: addForm.userId,
      reason: addForm.reason,
      expireTime: addForm.expireTime
        ? dayjs(addForm.expireTime).format('YYYY-MM-DDTHH:mm:ss')
        : null
    })
    ElMessage.success('已加入黑名单')
    addDialog.value = false
    loadData()
  } finally {
    saving.value = false
  }
}

async function loadData() {
  loading.value = true
  try {
    const res = await adminApi.blacklist()
    list.value = res.data || []
  } finally {
    loading.value = false
  }
}

onMounted(loadData)
</script>

<style scoped>
.page-title { font-size: 1.25rem; font-weight: 600; }
.text-danger { color: #f56c6c; font-weight: 500; }
</style>
