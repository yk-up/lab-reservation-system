<template>
  <div>
    <div class="flex-between mb-2">
      <h2 class="page-title">实验室管理</h2>
      <el-button type="primary" :icon="Plus" @click="openDialog()">新增实验室</el-button>
    </div>

    <div class="card">
      <el-table :data="labs" stripe v-loading="loading" style="width: 100%">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="name" label="实验室名称" min-width="150" />
        <el-table-column prop="location" label="位置" min-width="150" show-overflow-tooltip />
        <el-table-column prop="capacity" label="容量" width="80" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">
              {{ row.status === 1 ? '开放' : '已关闭' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="openDialog(row)">编辑</el-button>
            <el-button
              size="small"
              :type="row.status === 1 ? 'warning' : 'success'"
              plain
              @click="toggleStatus(row)"
            >
              {{ row.status === 1 ? '关闭' : '开放' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑实验室' : '新增实验室'" width="500px">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="80px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" placeholder="实验室名称" />
        </el-form-item>
        <el-form-item label="位置" prop="location">
          <el-input v-model="form.location" placeholder="如：教学楼3楼301" />
        </el-form-item>
        <el-form-item label="容量" prop="capacity">
          <el-input-number v-model="form.capacity" :min="1" :max="500" />
        </el-form-item>
        <el-form-item label="简介">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="实验室简介（选填）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveLab">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { labApi } from '@/api'

const loading = ref(true)
const labs = ref([])
const dialogVisible = ref(false)
const isEdit = ref(false)
const saving = ref(false)
const formRef = ref()

const form = reactive({ id: null, name: '', location: '', capacity: 30, description: '' })
const rules = {
  name: [{ required: true, message: '请输入实验室名称', trigger: 'blur' }],
  capacity: [{ required: true, message: '请填写容量', trigger: 'blur' }]
}

function openDialog(row = null) {
  isEdit.value = !!row
  if (row) {
    Object.assign(form, { id: row.id, name: row.name, location: row.location, capacity: row.capacity, description: row.description })
  } else {
    Object.assign(form, { id: null, name: '', location: '', capacity: 30, description: '' })
  }
  dialogVisible.value = true
}

async function saveLab() {
  await formRef.value.validate()
  saving.value = true
  try {
    if (isEdit.value) {
      await labApi.update(form.id, form)
      ElMessage.success('更新成功')
      const idx = labs.value.findIndex(l => l.id === form.id)
      if (idx !== -1) Object.assign(labs.value[idx], form)
    } else {
      await labApi.add(form)
      ElMessage.success('新增成功')
      loadData()
    }
    dialogVisible.value = false
  } finally {
    saving.value = false
  }
}

async function toggleStatus(row) {
  const newStatus = row.status === 1 ? 0 : 1
  await labApi.toggleStatus(row.id, newStatus)
  row.status = newStatus
  ElMessage.success(newStatus === 1 ? '已开放' : '已关闭')
}

async function loadData() {
  loading.value = true
  try {
    const res = await labApi.listAll()
    labs.value = res.data || []
  } finally {
    loading.value = false
  }
}

onMounted(loadData)
</script>

<style scoped>
.page-title { font-size: 1.25rem; font-weight: 600; }
</style>
