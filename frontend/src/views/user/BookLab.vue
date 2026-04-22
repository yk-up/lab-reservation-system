<template>
  <div class="page-container">
    <div class="flex-between mb-2">
      <el-button :icon="ArrowLeft" text @click="router.back()">返回</el-button>
      <h2 class="page-title">预约实验室</h2>
      <span />
    </div>

    <div class="book-layout">
      <div class="book-left">
        <div class="card mb-2" v-if="lab">
          <div class="lab-info-header">
            <el-icon size="24" color="#409eff"><OfficeBuilding /></el-icon>
            <div>
              <h3>{{ lab.name }}</h3>
              <p class="text-gray">{{ lab.location }}</p>
            </div>
          </div>
          <p class="lab-desc">{{ lab.description }}</p>
          <div class="lab-meta">
            <el-tag size="small" type="success">开放中</el-tag>
            <span class="text-gray" style="font-size: 0.8rem;">最多 {{ lab.capacity }} 人</span>
          </div>
        </div>

        <div class="card">
          <h4 class="section-title">选择日期</h4>
          <el-calendar v-model="selectedDate" @change="onDateChange">
            <template #date-cell="{ data }">
              <div class="cal-cell" :class="{ 'cal-selected': isSelectedDate(data.day) }">
                {{ data.day.split('-')[2] }}
              </div>
            </template>
          </el-calendar>
        </div>
      </div>

      <div class="book-right">
        <div class="card mb-2">
          <h4 class="section-title">
            {{ formatDate(selectedDate) }} 空闲时段
            <el-button size="small" text :icon="Refresh" :loading="slotsLoading" @click="loadSlots">刷新</el-button>
          </h4>
          <div v-if="slotsLoading" class="slots-loading">
            <el-icon class="is-loading"><Loading /></el-icon> 加载中...
          </div>
          <div v-else class="slots-grid">
            <div
              v-for="slot in slots"
              :key="slot.start"
              class="slot-item"
              :class="{
                'slot-free': !slot.occupied,
                'slot-busy': slot.occupied,
                'slot-active': isSlotInRange(slot)
              }"
              @click="!slot.occupied && selectSlot(slot)"
            >
              {{ formatTime(slot.start) }}
            </div>
          </div>
          <div class="slots-legend">
            <span class="legend-item"><span class="dot dot-free"></span>空闲</span>
            <span class="legend-item"><span class="dot dot-busy"></span>已占用</span>
            <span class="legend-item"><span class="dot dot-active"></span>已选择</span>
          </div>
        </div>

        <div class="card">
          <h4 class="section-title">填写预约信息</h4>
          <el-alert
            type="info"
            :closable="false"
            show-icon
            style="margin-bottom: 12px"
            title="请选择未来时间，单次预约时长 30 分钟到 8 小时之间，参与人数不能超过实验室容量。"
          />
          <el-form :model="form" :rules="rules" ref="formRef" label-position="top">
            <el-form-item label="预约用途" prop="title">
              <el-input v-model="form.title" placeholder="简要说明预约用途，如：毕业设计实验" maxlength="100" show-word-limit />
            </el-form-item>
            <el-row :gutter="12">
              <el-col :span="12">
                <el-form-item label="开始时间" prop="startTime">
                  <el-date-picker
                    v-model="form.startTime"
                    type="datetime"
                    placeholder="开始时间"
                    format="MM-DD HH:mm"
                    value-format="YYYY-MM-DDTHH:mm:ss"
                    :disabled-date="disabledDate"
                    :disabled-hours="disabledHours"
                    :disabled-minutes="disabledMinutes"
                    style="width: 100%"
                  />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="结束时间" prop="endTime">
                  <el-date-picker
                    v-model="form.endTime"
                    type="datetime"
                    placeholder="结束时间"
                    format="MM-DD HH:mm"
                    value-format="YYYY-MM-DDTHH:mm:ss"
                    :disabled-date="disabledDate"
                    :disabled-hours="disabledHours"
                    :disabled-minutes="disabledMinutes"
                    style="width: 100%"
                  />
                </el-form-item>
              </el-col>
            </el-row>

            <div v-if="conflictTip" class="conflict-tip">
              <el-icon color="#f56c6c"><WarningFilled /></el-icon>
              {{ conflictTip }}
            </div>
            <div v-else-if="form.startTime && form.endTime" class="ok-tip">
              <el-icon color="#67c23a"><CircleCheckFilled /></el-icon>
              该时段暂无冲突，可以预约
            </div>

            <el-form-item label="参与人数" prop="attendees">
              <el-input-number v-model="form.attendees" :min="1" :max="lab?.capacity || 100" />
              <div class="field-tip">当前实验室最多容纳 {{ lab?.capacity || '-' }} 人，请合理填写参与人数。</div>
            </el-form-item>
            <el-form-item label="备注说明">
              <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="选填" />
            </el-form-item>

            <el-button
              type="primary"
              :loading="submitting"
              :disabled="!!conflictTip"
              style="width: 100%; height: 2.5rem;"
              @click="submitReservation"
            >
              提交预约申请
            </el-button>
          </el-form>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, Refresh, Loading, WarningFilled, CircleCheckFilled } from '@element-plus/icons-vue'
import dayjs from 'dayjs'
import { labApi, reservationApi } from '@/api'
import { useUserStore } from '@/store/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const labId = Number(route.params.id)

const lab = ref(null)
const selectedDate = ref(new Date())
const slots = ref([])
const slotsLoading = ref(false)
const formRef = ref()
const submitting = ref(false)
const conflictTip = ref('')
const selectedStartSlot = ref(null)

const form = reactive({
  title: '',
  startTime: '',
  endTime: '',
  attendees: 1,
  remark: ''
})

const rules = {
  title: [{ required: true, message: '请填写预约用途', trigger: 'blur' }],
  startTime: [{ required: true, message: '请选择开始时间', trigger: 'change' }],
  endTime: [{ required: true, message: '请选择结束时间', trigger: 'change' }],
  attendees: [{ required: true, message: '请填写参与人数', trigger: 'change' }]
}

function formatDate(d) {
  return dayjs(d).format('YYYY年MM月DD日')
}

function formatTime(isoStr) {
  return dayjs(isoStr).format('HH:mm')
}

function isSelectedDate(day) {
  return dayjs(selectedDate.value).format('YYYY-MM-DD') === day
}

function disabledDate(d) {
  return dayjs(d).isBefore(dayjs(), 'day')
}

function disabledHours() {
  return []
}

function disabledMinutes(hour) {
  const now = dayjs()
  const selected = dayjs(form.startTime || form.endTime || new Date())
  if (selected.isSame(now, 'day') && hour === now.hour()) {
    return Array.from({ length: now.minute() }, (_, i) => i)
  }
  return []
}

function isSlotInRange(slot) {
  if (!form.startTime || !form.endTime) return false
  const s = dayjs(slot.start)
  return s.isAfter(dayjs(form.startTime).subtract(1, 'minute')) && s.isBefore(dayjs(form.endTime))
}

function selectSlot(slot) {
  if (!selectedStartSlot.value) {
    selectedStartSlot.value = slot
    form.startTime = dayjs(slot.start).format('YYYY-MM-DDTHH:mm:ss')
    form.endTime = dayjs(slot.start).add(30, 'minute').format('YYYY-MM-DDTHH:mm:ss')
  } else {
    const clickedTime = dayjs(slot.start)
    const startTime = dayjs(selectedStartSlot.value.start)
    if (clickedTime.isAfter(startTime)) {
      form.endTime = dayjs(slot.end).format('YYYY-MM-DDTHH:mm:ss')
    } else {
      selectedStartSlot.value = slot
      form.startTime = dayjs(slot.start).format('YYYY-MM-DDTHH:mm:ss')
      form.endTime = dayjs(slot.start).add(30, 'minute').format('YYYY-MM-DDTHH:mm:ss')
    }
  }
}

function onDateChange() {
  loadSlots()
}

async function loadSlots() {
  slotsLoading.value = true
  try {
    const dateStr = dayjs(selectedDate.value).format('YYYY-MM-DD')
    const res = await labApi.getSlots(labId, dateStr)
    slots.value = res.data || []
  } finally {
    slotsLoading.value = false
  }
}

watch([() => form.startTime, () => form.endTime, () => form.attendees], ([start, end, attendees]) => {
  conflictTip.value = ''
  if (!start || !end) return
  if (dayjs(start).isBefore(dayjs())) {
    conflictTip.value = '开始时间不能早于当前时间'
    return
  }
  if (dayjs(end).isBefore(dayjs(start)) || dayjs(end).isSame(dayjs(start))) {
    conflictTip.value = '结束时间必须晚于开始时间'
    return
  }
  const durationMin = dayjs(end).diff(dayjs(start), 'minute')
  if (durationMin < 30) {
    conflictTip.value = '预约时长不能少于30分钟'
    return
  }
  if (durationMin > 480) {
    conflictTip.value = '单次预约不能超过8小时'
    return
  }
  if (lab.value && attendees > lab.value.capacity) {
    conflictTip.value = `参与人数不能超过实验室容量（最多 ${lab.value.capacity} 人）`
    return
  }
  const conflict = slots.value.some(slot => {
    if (!slot.occupied) return false
    const slotStart = dayjs(slot.start)
    const slotEnd = dayjs(slot.end)
    return slotStart.isBefore(dayjs(end)) && slotEnd.isAfter(dayjs(start))
  })
  if (conflict) {
    conflictTip.value = '所选时段内存在已预约时段，请重新选择'
  }
})

async function submitReservation() {
  if (!userStore.isLoggedIn) {
    router.push('/login')
    return
  }
  await formRef.value.validate()
  if (conflictTip.value) return

  submitting.value = true
  try {
    await reservationApi.create({
      labId,
      title: form.title,
      startTime: form.startTime,
      endTime: form.endTime,
      attendees: form.attendees,
      remark: form.remark
    })
    ElMessage.success('预约申请已提交，等待管理员审核')
    router.push('/my-reservations')
  } finally {
    submitting.value = false
  }
}

;(async () => {
  const res = await labApi.list()
  lab.value = (res.data || []).find(l => l.id === labId) || null
  loadSlots()
})()
</script>

<style scoped>
.book-layout {
  display: grid;
  grid-template-columns: 1fr 1.5fr;
  gap: 1rem;
  align-items: start;
}
.section-title {
  font-size: 0.95rem;
  font-weight: 600;
  color: #303133;
  margin-bottom: 0.75rem;
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.lab-info-header {
  display: flex;
  gap: 0.75rem;
  align-items: flex-start;
  margin-bottom: 0.75rem;
}
.lab-info-header h3 { font-size: 1rem; font-weight: 600; }
.text-gray { font-size: 0.8rem; color: #909399; margin-top: 0.2rem; }
.lab-desc { font-size: 0.8rem; color: #606266; margin-bottom: 0.5rem; }
.lab-meta { display: flex; align-items: center; gap: 0.75rem; }
.slots-loading {
  text-align: center;
  padding: 1.5rem;
  color: #909399;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
}
.slots-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 0.4rem;
  margin-bottom: 0.75rem;
}
.slot-item {
  padding: 0.3rem 0.25rem;
  text-align: center;
  font-size: 0.7rem;
  border-radius: 0.375rem;
  cursor: pointer;
  border: 1px solid transparent;
  transition: all 0.15s;
  white-space: nowrap;
}
.slot-free { background: #f0f9eb; color: #67c23a; border-color: #c2e7b0; }
.slot-free:hover { background: #67c23a; color: #fff; }
.slot-busy { background: #fef0f0; color: #c0c4cc; border-color: #fde2e2; cursor: not-allowed; }
.slot-active { background: #409eff !important; color: #fff !important; border-color: #409eff !important; }
.slots-legend { display: flex; gap: 1rem; font-size: 0.75rem; color: #909399; }
.legend-item { display: flex; align-items: center; gap: 0.3rem; }
.dot { width: 10px; height: 10px; border-radius: 50%; display: inline-block; }
.dot-free { background: #67c23a; }
.dot-busy { background: #f56c6c; }
.dot-active { background: #409eff; }
.conflict-tip {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  font-size: 0.8rem;
  color: #f56c6c;
  background: #fef0f0;
  padding: 0.5rem 0.75rem;
  border-radius: 0.375rem;
  margin-bottom: 0.75rem;
}
.ok-tip {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  font-size: 0.8rem;
  color: #67c23a;
  background: #f0f9eb;
  padding: 0.5rem 0.75rem;
  border-radius: 0.375rem;
  margin-bottom: 0.75rem;
}
.field-tip {
  margin-top: 0.4rem;
  font-size: 12px;
  color: #909399;
}
@media (max-width: 768px) {
  .book-layout { grid-template-columns: 1fr; }
  .slots-grid { grid-template-columns: repeat(6, 1fr); }
}
</style>
