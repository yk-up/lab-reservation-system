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
      <AppEmptyState
        v-if="!loading && list.length === 0"
        type="blacklist"
        title="暂无黑名单数据"
        description="当前没有被加入黑名单的用户。如需限制预约，可通过右上角按钮手动添加。"
        secondary-action-text="返回上一页"
        action-text="加入黑名单"
        @secondary-action="goBack"
        @action="addDialog = true"
      />
    </div>

    <el-dialog v-model="addDialog" title="加入黑名单" width="480px" @open="onAddDialogOpen">
      <el-form :model="addForm" ref="addFormRef" label-width="88px">
        <el-form-item label="选择用户" prop="userId" :rules="[{ required: true, message: '请检索并选择用户' }]">
          <el-select
            v-model="addForm.userId"
            filterable
            remote
            reserve-keyword
            clearable
            placeholder="输入用户名、姓名或用户ID检索"
            :remote-method="remoteSearchUser"
            :loading="userSearchLoading"
            style="width: 100%"
          >
            <el-option
              v-for="u in userOptions"
              :key="u.id"
              :label="formatUserOption(u)"
              :value="u.id"
            />
          </el-select>
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
import { ref, reactive, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import dayjs from 'dayjs'
import { adminApi } from '@/api'
import AppEmptyState from '@/components/AppEmptyState.vue'

const router = useRouter()
const loading = ref(true)
const list = ref([])
const addDialog = ref(false)
const saving = ref(false)
const addFormRef = ref()
const addForm = reactive({ userId: null, reason: '', expireTime: null })
const userSearchLoading = ref(false)
const userOptions = ref([])

const formatDateTime = (t) => t ? dayjs(t).format('YYYY-MM-DD HH:mm') : '-'

function formatUserOption(u) {
  const name = u.realName ? `${u.username}（${u.realName}）` : u.username
  return `${name} · ID ${u.id}`
}

function onAddDialogOpen() {
  addForm.userId = null
  addForm.reason = ''
  addForm.expireTime = null
  userOptions.value = []
  nextTick(() => addFormRef.value?.clearValidate())
}

async function remoteSearchUser(query) {
  const q = (query || '').trim()
  const selectedId = addForm.userId
  const keepSelected =
    selectedId != null ? userOptions.value.find((u) => u.id === selectedId) : null
  if (!q) {
    userOptions.value = keepSelected ? [keepSelected] : []
    return
  }
  userSearchLoading.value = true
  try {
    const res = await adminApi.searchUsers({ keyword: q, limit: 20 })
    let list = res.data || []
    if (keepSelected && !list.some((u) => u.id === keepSelected.id)) {
      list = [keepSelected, ...list]
    }
    userOptions.value = list
  } finally {
    userSearchLoading.value = false
  }
}

function goBack() {
  router.back()
}

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
