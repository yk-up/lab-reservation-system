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
          <h3 class="section-title">预约趋势统计分析</h3>
          <p class="section-subtitle">按申请日期观察近一段时间的预约总量、通过量和拒绝量变化</p>
        </div>
        <el-radio-group v-model="trendRange" size="small" @change="loadTrend">
          <el-radio-button label="7">近7天</el-radio-button>
          <el-radio-button label="15">近15天</el-radio-button>
          <el-radio-button label="30">近30天</el-radio-button>
        </el-radio-group>
      </div>

      <template v-if="trendSeries.length">
        <div class="trend-summary">
          <div class="trend-summary-card">
            <div class="trend-summary-label">预约总量</div>
            <div class="trend-summary-value">{{ trendSummary.totalCount ?? 0 }}</div>
          </div>
          <div class="trend-summary-card">
            <div class="trend-summary-label">平均每日</div>
            <div class="trend-summary-value">{{ formatMetric(trendSummary.avgDaily) }}</div>
          </div>
          <div class="trend-summary-card">
            <div class="trend-summary-label">通过率</div>
            <div class="trend-summary-value">{{ formatMetric(trendSummary.passRate) }}%</div>
          </div>
          <div class="trend-summary-card">
            <div class="trend-summary-label">峰值日期</div>
            <div class="trend-summary-value">{{ trendSummary.peakLabel || '-' }}</div>
          </div>
        </div>

        <div class="trend-analysis">
          近 {{ trendSummary.days || trendRange }} 天共收到
          <strong>{{ trendSummary.totalCount ?? 0 }}</strong> 条预约申请，
          其中通过 <strong>{{ trendSummary.approvedCount ?? 0 }}</strong> 条，
          拒绝 <strong>{{ trendSummary.rejectedCount ?? 0 }}</strong> 条；
          预约高峰出现在 <strong>{{ trendSummary.peakLabel || '-' }}</strong>，
          当日共有 <strong>{{ trendSummary.peakCount ?? 0 }}</strong> 条申请。
        </div>

        <div class="trend-legend">
          <span><i class="legend-dot total"></i>总量</span>
          <span><i class="legend-dot approved"></i>通过</span>
          <span><i class="legend-dot rejected"></i>拒绝</span>
        </div>

        <div class="trend-chart-wrap">
          <div class="trend-chart">
            <div v-for="item in trendSeries" :key="item.date" class="trend-group">
              <div class="trend-bars">
                <span class="trend-bar total" :style="{ height: trendBarHeight(item.totalCount) }"></span>
                <span class="trend-bar approved" :style="{ height: trendBarHeight(item.approvedCount) }"></span>
                <span class="trend-bar rejected" :style="{ height: trendBarHeight(item.rejectedCount) }"></span>
              </div>
              <div class="trend-total">{{ item.totalCount }}</div>
              <div class="trend-date">{{ item.label }}</div>
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
import { computed, ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import dayjs from 'dayjs'
import { adminApi, reservationApi } from '@/api'

const data = ref({})
const pendingList = ref([])
const usageList = ref([])
const usageTotal = ref(0)
const trendRange = ref('7')
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
const maxTrendCount = computed(() => Math.max(...trendSeries.value.map(item => Number(item.totalCount) || 0), 1))

const formatDateTime = t => (t ? dayjs(t).format('MM-DD HH:mm') : '-')
const formatTime = t => (t ? dayjs(t).format('HH:mm') : '-')
const formatMetric = value => {
  const num = Number(value) || 0
  return Number.isInteger(num) ? String(num) : num.toFixed(1)
}

function barPercent(item) {
  return Math.max(12, Math.round(((Number(item.reservationCount) || 0) / maxUsageCount.value) * 100))
}

function trendBarHeight(value) {
  const num = Number(value) || 0
  if (num <= 0) return '8px'
  return `${Math.max(10, Math.round((num / maxTrendCount.value) * 100))}%`
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
.trend-analysis {
  margin-bottom: 1rem;
  padding: 0.9rem 1rem;
  border-radius: 0.85rem;
  background: #f8fafc;
  color: #475569;
  line-height: 1.75;
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
.trend-chart-wrap {
  overflow-x: auto;
  padding-bottom: 0.25rem;
  display: flex;
  justify-content: center;
}
.trend-chart {
  width: max-content;
  min-width: 100%;
  display: flex;
  gap: 0.85rem;
  align-items: flex-end;
  justify-content: center;
}
.trend-group {
  width: 42px;
  flex-shrink: 0;
}
.trend-bars {
  height: 220px;
  display: flex;
  align-items: flex-end;
  justify-content: center;
  gap: 4px;
  padding: 0 2px;
}
.trend-bar {
  width: 10px;
  min-height: 8px;
  border-radius: 999px 999px 0 0;
  transition: height 0.2s ease;
}
.trend-bar.total {
  background: linear-gradient(180deg, #79bbff, #409eff);
}
.trend-bar.approved {
  background: linear-gradient(180deg, #95d475, #67c23a);
}
.trend-bar.rejected {
  background: linear-gradient(180deg, #f89898, #f56c6c);
}
.trend-total {
  margin-top: 0.5rem;
  text-align: center;
  font-size: 0.8rem;
  font-weight: 600;
  color: #334155;
}
.trend-date {
  margin-top: 0.25rem;
  text-align: center;
  font-size: 0.75rem;
  color: #94a3b8;
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
  .trend-summary {
    grid-template-columns: 1fr;
  }
}
</style>
