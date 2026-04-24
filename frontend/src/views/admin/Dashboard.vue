<template>
  <div>
    <div class="stat-cards">
      <div class="stat-card" v-for="card in statCards" :key="card.label">
        <div class="stat-icon" :style="{ background: card.bg }">
          <el-icon size="24" :color="card.color"><component :is="card.icon" /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ data[card.key] ?? '-' }}</div>
          <div class="stat-label">{{ card.label }}</div>
        </div>
      </div>
    </div>

    <div class="usage-section mt-3">
      <div class="card usage-card">
        <div class="section-head">
          <div>
            <h3 class="section-title">实验室使用率排行</h3>
            <p class="section-subtitle">按预约次数统计，直观查看热门实验室</p>
          </div>
          <el-tag type="primary" effect="dark">总预约 {{ usageTotal }}</el-tag>
        </div>

        <div v-if="usageList.length" class="usage-chart">
          <div v-for="item in usageList.slice(0, 6)" :key="item.labId" class="usage-row">
            <div class="usage-label">
              <span class="rank-badge">#{{ item.rank }}</span>
              <div>
                <div class="lab-name">{{ item.labName }}</div>
                <div class="lab-location">{{ item.location || '暂无位置信息' }}</div>
              </div>
            </div>
            <div class="usage-bar-wrap">
              <div class="usage-bar-bg">
                <div class="usage-bar" :style="{ width: `${barPercent(item)}%` }"></div>
              </div>
              <div class="usage-meta">
                <span>{{ item.reservationCount }} 次</span>
                <span>{{ item.usageRate }}%</span>
              </div>
            </div>
          </div>
        </div>
        <el-empty v-else description="暂无实验室使用统计" />
      </div>

      <div class="card ranking-card">
        <h3 class="section-title">使用率明细</h3>
        <el-table :data="usageList" stripe style="width: 100%">
          <el-table-column prop="rank" label="排名" width="80" />
          <el-table-column prop="labName" label="实验室名称" min-width="160" />
          <el-table-column prop="location" label="位置" min-width="150" show-overflow-tooltip />
          <el-table-column prop="reservationCount" label="预约次数" width="100" />
          <el-table-column label="使用率" width="120">
            <template #default="{ row }">
              <el-tag type="success" effect="light">{{ row.usageRate }}%</el-tag>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>

    <div class="card trend-card mt-3">
      <div class="section-head">
        <div>
          <h3 class="section-title">预约趋势分析</h3>
          <p class="section-subtitle">按创建时间统计预约趋势，辅助排班和资源规划</p>
        </div>
        <div class="trend-toolbar">
          <el-radio-group v-model="trendDisplayMode" size="small">
            <el-radio-button label="daily">按天展示</el-radio-button>
            <el-radio-button label="weekly">按周展示</el-radio-button>
          </el-radio-group>
          <el-radio-group v-model="trendRange" size="small" @change="loadTrend">
            <el-radio-button label="7">近7天</el-radio-button>
            <el-radio-button label="15">近15天</el-radio-button>
            <el-radio-button label="30">近30天</el-radio-button>
          </el-radio-group>
        </div>
      </div>

      <template v-if="trendSeries.length">
        <div class="trend-summary">
          <div class="trend-summary-card">
            <div class="trend-summary-label">总预约数</div>
            <div class="trend-summary-value">{{ trendSummary.totalCount ?? 0 }}</div>
          </div>
          <div class="trend-summary-card">
            <div class="trend-summary-label">通过 / 拒绝</div>
            <div class="trend-summary-value">{{ trendSummary.approvedCount ?? 0 }} / {{ trendSummary.rejectedCount ?? 0 }}</div>
          </div>
          <div class="trend-summary-card">
            <div class="trend-summary-label">峰值日</div>
            <div class="trend-summary-value">
              {{ trendSummary.peakDate || '-' }}
              <span v-if="trendSummary.peakCount" class="trend-summary-sub">（{{ trendSummary.peakCount }}）</span>
            </div>
          </div>
          <div class="trend-summary-card">
            <div class="trend-summary-label">{{ trendChangeLabel }}</div>
            <div class="trend-summary-value" :class="trendChangeClass">{{ trendChangeText }}</div>
          </div>
        </div>

        <div class="trend-legend">
          <span><i class="legend-dot total"></i>总量</span>
          <span><i class="legend-dot approved"></i>通过</span>
          <span><i class="legend-dot rejected"></i>拒绝</span>
        </div>

        <div class="trend-list">
          <div v-for="item in displayTrendSeries" :key="item.key" class="trend-row">
            <div class="trend-date">{{ item.label }}</div>
            <div class="trend-track">
              <div class="trend-total-bar" :style="{ width: trendTotalWidth(item) }">
                <span class="trend-segment approved" :style="{ width: trendApprovedWidth(item) }"></span>
                <span class="trend-segment rejected" :style="{ width: trendRejectedWidth(item), left: trendApprovedWidth(item) }"></span>
              </div>
            </div>
            <div class="trend-values">
              <span class="trend-value total">
                <i class="trend-value-dot total"></i>
                总 {{ item.totalCount }}
              </span>
              <span class="trend-value approved">
                <i class="trend-value-dot approved"></i>
                通过 {{ item.approvedCount }}
              </span>
              <span class="trend-value rejected">
                <i class="trend-value-dot rejected"></i>
                拒绝 {{ item.rejectedCount }}
              </span>
            </div>
          </div>
        </div>
      </template>
      <el-empty v-else description="暂无趋势统计数据" />
    </div>

    <div class="card mt-3">
      <h3 style="margin-bottom: 1rem; font-size: 1rem; font-weight: 600;">待审核预约</h3>
      <el-table :data="pendingList" stripe style="width: 100%">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column label="申请人" width="180">
          <template #default="{ row }">
            <div>{{ row.realName || '-' }}</div>
            <div style="font-size: 12px; color: #909399;">{{ row.username || row.userId }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="预约用途" min-width="150" show-overflow-tooltip />
        <el-table-column label="实验室" width="140">
          <template #default="{ row }">{{ row.labName || `实验室 #${row.labId}` }}</template>
        </el-table-column>
        <el-table-column label="时间" min-width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.startTime) }} ~ {{ formatTime(row.endTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button type="success" size="small" @click="approve(row)">通过</el-button>
            <el-button type="danger" size="small" plain @click="openReject(row)">拒绝</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="pendingList.length === 0" description="暂无待审核预约" />
    </div>

    <el-dialog v-model="rejectDialog" title="填写拒绝原因" width="400px">
      <el-input v-model="rejectReason" type="textarea" :rows="3" placeholder="请填写拒绝原因（选填）" />
      <template #footer>
        <el-button @click="rejectDialog = false">取消</el-button>
        <el-button type="danger" :loading="auditing" @click="confirmReject">确认拒绝</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, ref, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import dayjs from 'dayjs'
import { adminApi, reservationApi } from '@/api'

const data = ref({})
const pendingList = ref([])
const usageList = ref([])
const usageTotal = ref(0)
const trendRange = ref('7')
const trendDisplayMode = ref('daily')
const trendSeries = ref([])
const trendSummary = ref({})
const rejectDialog = ref(false)
const rejectReason = ref('')
const currentRow = ref(null)
const auditing = ref(false)

const statCards = [
  { key: 'totalLabs', label: '实验室总数', icon: 'OfficeBuilding', bg: '#ecf5ff', color: '#409eff' },
  { key: 'openLabs', label: '开放中', icon: 'Check', bg: '#f0f9eb', color: '#67c23a' },
  { key: 'pendingCount', label: '待审核预约', icon: 'Clock', bg: '#fdf6ec', color: '#e6a23c' },
  { key: 'blacklistCount', label: '黑名单人数', icon: 'UserFilled', bg: '#fef0f0', color: '#f56c6c' }
]

const maxUsageCount = computed(() => Math.max(...usageList.value.map(item => Number(item.reservationCount) || 0), 1))
const isDailyTrendView = computed(() => trendDisplayMode.value === 'daily')
const trendGroupSize = computed(() => (isDailyTrendView.value ? 1 : 7))
const displayTrendSeries = computed(() => {
  if (isDailyTrendView.value) {
    return trendSeries.value.map(item => ({
      key: item.date,
      label: item.label,
      totalCount: Number(item.totalCount) || 0,
      approvedCount: Number(item.approvedCount) || 0,
      rejectedCount: Number(item.rejectedCount) || 0
    }))
  }

  const groups = []
  for (let i = 0; i < trendSeries.value.length; i += trendGroupSize.value) {
    const chunk = trendSeries.value.slice(i, i + trendGroupSize.value)
    if (!chunk.length) continue

    groups.push({
      key: `${chunk[0].date}-${chunk[chunk.length - 1].date}`,
      label: `${chunk[0].label} ~ ${chunk[chunk.length - 1].label}`,
      totalCount: chunk.reduce((sum, item) => sum + (Number(item.totalCount) || 0), 0),
      approvedCount: chunk.reduce((sum, item) => sum + (Number(item.approvedCount) || 0), 0),
      rejectedCount: chunk.reduce((sum, item) => sum + (Number(item.rejectedCount) || 0), 0)
    })
  }
  return groups
})
const maxTrendCount = computed(() => Math.max(...displayTrendSeries.value.map(item => Number(item.totalCount) || 0), 1))
const trendChange = computed(() => {
  if (!displayTrendSeries.value.length) return { text: '0%', state: 'flat' }
  const last = Number(displayTrendSeries.value[displayTrendSeries.value.length - 1]?.totalCount) || 0
  const prev = Number(displayTrendSeries.value[displayTrendSeries.value.length - 2]?.totalCount) || 0

  if (prev === 0) {
    if (last === 0) return { text: '0%', state: 'flat' }
    return { text: '+100%', state: 'up' }
  }

  const diff = ((last - prev) / prev) * 100
  const rounded = Math.round(diff * 10) / 10
  return {
    text: `${rounded > 0 ? '+' : ''}${formatMetric(rounded)}%`,
    state: rounded > 0 ? 'up' : rounded < 0 ? 'down' : 'flat'
  }
})
const trendChangeLabel = computed(() => (isDailyTrendView.value ? '较前一天' : '较前7天'))
const trendChangeText = computed(() => trendChange.value.text)
const trendChangeClass = computed(() => ({
  'is-up': trendChange.value.state === 'up',
  'is-down': trendChange.value.state === 'down',
  'is-flat': trendChange.value.state === 'flat'
}))

const formatDateTime = t => (t ? dayjs(t).format('MM-DD HH:mm') : '-')
const formatTime = t => (t ? dayjs(t).format('HH:mm') : '-')
const formatMetric = value => {
  const num = Number(value) || 0
  return Number.isInteger(num) ? String(num) : num.toFixed(1)
}

function barPercent(item) {
  return Math.max(12, Math.round(((Number(item.reservationCount) || 0) / maxUsageCount.value) * 100))
}

function trendTotalWidth(item) {
  const total = Number(item.totalCount) || 0
  if (total <= 0) return '0%'
  return `${Math.max(4, (total / maxTrendCount.value) * 100)}%`
}

function trendApprovedWidth(item) {
  const total = Number(item.totalCount) || 0
  const approved = Number(item.approvedCount) || 0
  if (total <= 0 || approved <= 0) return '0%'
  return `${(approved / total) * 100}%`
}

function trendRejectedWidth(item) {
  const total = Number(item.totalCount) || 0
  const rejected = Number(item.rejectedCount) || 0
  if (total <= 0 || rejected <= 0) return '0%'
  return `${(rejected / total) * 100}%`
}

function openReject(row) {
  currentRow.value = row
  rejectReason.value = ''
  rejectDialog.value = true
}

async function approve(row) {
  auditing.value = true
  try {
    await reservationApi.audit(row.id, { status: 1 })
    ElMessage.success('已通过审核')
    pendingList.value = pendingList.value.filter(r => r.id !== row.id)
    data.value.pendingCount = Math.max(0, (data.value.pendingCount || 0) - 1)
    await loadTrend()
  } finally {
    auditing.value = false
  }
}

async function confirmReject() {
  auditing.value = true
  try {
    await reservationApi.audit(currentRow.value.id, { status: 2, rejectReason: rejectReason.value })
    ElMessage.success('已拒绝')
    pendingList.value = pendingList.value.filter(r => r.id !== currentRow.value.id)
    data.value.pendingCount = Math.max(0, (data.value.pendingCount || 0) - 1)
    rejectDialog.value = false
    await loadTrend()
  } finally {
    auditing.value = false
  }
}

async function loadTrend() {
  const res = await adminApi.reservationTrend({ days: Number(trendRange.value) })
  trendSeries.value = res.data?.series || []
  trendSummary.value = res.data?.summary || {}
}

watch(trendRange, value => {
  trendDisplayMode.value = Number(value) === 7 ? 'daily' : 'weekly'
})

onMounted(async () => {
  const [dashRes, pendingRes, usageRes] = await Promise.all([
    adminApi.dashboard(),
    reservationApi.pending(),
    adminApi.labUsage()
  ])
  data.value = dashRes.data || {}
  pendingList.value = pendingRes.data || []
  usageList.value = usageRes.data?.ranking || []
  usageTotal.value = usageRes.data?.totalReservations || 0
  await loadTrend()
})
</script>

<style scoped>
.stat-cards {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 1rem;
}
.stat-card {
  background: #fff;
  border-radius: 0.75rem;
  padding: 1.25rem;
  display: flex;
  align-items: center;
  gap: 1rem;
  box-shadow: 0 2px 8px rgba(0,0,0,0.06);
}
.stat-icon {
  width: 3rem;
  height: 3rem;
  border-radius: 0.75rem;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.stat-value {
  font-size: 1.75rem;
  font-weight: 700;
  color: #1e293b;
  line-height: 1;
}
.stat-label {
  font-size: 0.8rem;
  color: #94a3b8;
  margin-top: 0.25rem;
}
.usage-section {
  display: grid;
  grid-template-columns: 1.2fr 1fr;
  gap: 1rem;
}
.usage-card, .ranking-card {
  padding: 1.25rem;
}
.trend-card {
  padding: 1.25rem;
}
.trend-toolbar {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  flex-wrap: wrap;
}
.section-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1rem;
  margin-bottom: 1rem;
}
.section-title {
  font-size: 1rem;
  font-weight: 600;
  color: #1e293b;
}
.section-subtitle {
  font-size: 0.8rem;
  color: #94a3b8;
  margin-top: 0.25rem;
}
.usage-chart {
  display: flex;
  flex-direction: column;
  gap: 0.9rem;
}
.usage-row {
  display: grid;
  grid-template-columns: 220px 1fr;
  gap: 1rem;
  align-items: center;
}
.usage-label {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  min-width: 0;
}
.rank-badge {
  width: 2rem;
  height: 2rem;
  border-radius: 999px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #409eff, #79bbff);
  color: white;
  font-size: 0.8rem;
  font-weight: 700;
  flex-shrink: 0;
}
.lab-name {
  font-size: 0.9rem;
  font-weight: 600;
  color: #303133;
}
.lab-location {
  font-size: 0.75rem;
  color: #909399;
  margin-top: 0.2rem;
}
.usage-bar-wrap {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}
.usage-bar-bg {
  flex: 1;
  height: 0.7rem;
  background: #edf2f7;
  border-radius: 999px;
  overflow: hidden;
}
.usage-bar {
  height: 100%;
  border-radius: 999px;
  background: linear-gradient(90deg, #409eff, #67c23a);
}
.usage-meta {
  display: flex;
  gap: 0.5rem;
  align-items: center;
  min-width: 90px;
  justify-content: space-between;
  color: #606266;
  font-size: 0.8rem;
}
.trend-summary {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 0.9rem;
  margin-bottom: 1rem;
}
.trend-summary-card {
  border-radius: 0.9rem;
  padding: 1rem;
  background: linear-gradient(180deg, #f8fbff 0%, #f3f7ff 100%);
  border: 1px solid #e8eefc;
}
.trend-summary-label {
  font-size: 0.8rem;
  color: #94a3b8;
}
.trend-summary-value {
  margin-top: 0.5rem;
  font-size: 1.5rem;
  font-weight: 700;
  color: #1e293b;
}
.trend-summary-sub {
  margin-left: 0.15rem;
  font-size: 0.95em;
}
.trend-summary-value.is-up {
  color: #16a34a;
}
.trend-summary-value.is-down {
  color: #dc2626;
}
.trend-summary-value.is-flat {
  color: #475569;
}
.trend-legend {
  display: flex;
  gap: 1rem;
  align-items: center;
  font-size: 0.8rem;
  color: #64748b;
  margin-bottom: 0.75rem;
}
.legend-dot {
  display: inline-block;
  width: 10px;
  height: 10px;
  border-radius: 999px;
  margin-right: 6px;
}
.legend-dot.total { background: #409eff; }
.legend-dot.approved { background: #67c23a; }
.legend-dot.rejected { background: #f56c6c; }
.trend-list {
  display: flex;
  flex-direction: column;
  gap: 0.9rem;
}
.trend-row {
  display: grid;
  grid-template-columns: 110px 1fr 170px;
  align-items: center;
  gap: 0.9rem;
}
.trend-track {
  position: relative;
  height: 18px;
  border-radius: 999px;
  background: #dce5f1;
  overflow: hidden;
}
.trend-total-bar {
  position: relative;
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, #6aa7f8, #3f7fe6);
  transition: width 0.2s ease;
}
.trend-segment {
  position: absolute;
  top: 0;
  bottom: 0;
}
.trend-segment.approved {
  background: linear-gradient(90deg, #2fc3b0, #56ce9d);
  border-radius: 999px;
}
.trend-segment.rejected {
  background: linear-gradient(90deg, #ff9999, #f56c6c);
}
.trend-values {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 0.45rem;
}
.trend-value {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  padding: 0.18rem 0.55rem;
  border-radius: 999px;
  font-size: 0.78rem;
  line-height: 1.2;
  font-weight: 600;
  background: #f8fafc;
  border: 1px solid transparent;
}
.trend-value.total {
  color: #334155;
  background: #f8fafc;
  border-color: #e2e8f0;
}
.trend-value.approved {
  color: #16a34a;
  background: #f0fdf4;
  border-color: #bbf7d0;
}
.trend-value.rejected {
  color: #dc2626;
  background: #fef2f2;
  border-color: #fecaca;
}
.trend-value-dot {
  width: 7px;
  height: 7px;
  border-radius: 999px;
  flex-shrink: 0;
}
.trend-value-dot.total {
  background: #409eff;
}
.trend-value-dot.approved {
  background: #67c23a;
}
.trend-value-dot.rejected {
  background: #f56c6c;
}
.trend-date {
  font-size: 0.9rem;
  color: #64748b;
}
@media (max-width: 1100px) {
  .usage-section {
    grid-template-columns: 1fr;
  }
  .trend-summary {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
@media (max-width: 768px) {
  .usage-row {
    grid-template-columns: 1fr;
  }
  .section-head {
    flex-direction: column;
    align-items: stretch;
  }
  .trend-toolbar {
    width: 100%;
  }
  .trend-summary {
    grid-template-columns: 1fr;
  }
  .trend-row {
    grid-template-columns: 88px 1fr 128px;
  }
  .trend-values {
    justify-content: flex-start;
  }
}
</style>
