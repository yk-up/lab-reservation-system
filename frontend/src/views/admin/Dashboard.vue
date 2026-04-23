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

    <div class="card mt-3 trend-card">
      <div class="section-head">
        <div>
          <h3 class="section-title">预约趋势分析</h3>
          <p class="section-subtitle">支持按天/每7天分组统计，辅助排班和资源规划</p>
        </div>
        <div class="trend-controls">
          <el-radio-group v-model="trendGroupBy" size="small" @change="loadTrend">
            <el-radio-button label="daily">按天</el-radio-button>
            <el-radio-button label="weekly">每7天</el-radio-button>
          </el-radio-group>
          <el-radio-group v-model="trendDays" size="small" @change="loadTrend">
            <el-radio-button :label="7">近7天</el-radio-button>
            <el-radio-button :label="14">近14天</el-radio-button>
            <el-radio-button :label="30">近30天</el-radio-button>
          </el-radio-group>
        </div>
      </div>

      <div class="trend-kpis">
        <div class="kpi-item">
          <div class="kpi-label">总预约数</div>
          <div class="kpi-value">{{ trend.totalReservations ?? 0 }}</div>
        </div>
        <div class="kpi-item">
          <div class="kpi-label">通过 / 拒绝</div>
          <div class="kpi-value">{{ trend.approvedReservations ?? 0 }} / {{ trend.rejectedReservations ?? 0 }}</div>
        </div>
        <div class="kpi-item">
          <div class="kpi-label">{{ trendGroupBy === 'daily' ? '峰值日' : '峰值周期' }}</div>
          <div class="kpi-value">{{ trend.peakDate || '-' }}（{{ trend.peakCount ?? 0 }}）</div>
        </div>
        <div class="kpi-item">
          <div class="kpi-label">{{ trendGroupBy === 'daily' ? '较前一天' : '较上一周期' }}</div>
          <div class="kpi-value" :class="trend.dayOverDay >= 0 ? 'up' : 'down'">
            {{ trend.dayOverDay >= 0 ? '+' : '' }}{{ trend.dayOverDay ?? 0 }}%
          </div>
        </div>
      </div>

      <div v-if="trendPoints.length" class="trend-bars">
        <div v-for="item in trendPoints" :key="item.date" class="trend-row">
          <div class="trend-label">{{ item.label }}</div>
          <div class="trend-bar-bg">
            <div class="trend-bar-total" :style="{ width: `${trendBarPercent(item.totalCount)}%` }"></div>
            <div
              class="trend-bar-approved"
              :style="{ width: `${trendApprovedPercent(item.approvedCount, item.totalCount)}%` }"
            ></div>
          </div>
          <div class="trend-value">{{ item.totalCount }}</div>
        </div>
      </div>
      <el-empty v-else description="暂无趋势数据" />
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
const trendDays = ref(7)
const trendGroupBy = ref('weekly')
const trend = ref({})
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
const trendPoints = computed(() => trend.value?.points || [])
const trendMaxCount = computed(() => Math.max(...trendPoints.value.map(item => Number(item.totalCount) || 0), 1))

const formatDateTime = t => (t ? dayjs(t).format('MM-DD HH:mm') : '-')
const formatTime = t => (t ? dayjs(t).format('HH:mm') : '-')

function barPercent(item) {
  return Math.max(12, Math.round(((Number(item.reservationCount) || 0) / maxUsageCount.value) * 100))
}

function trendBarPercent(totalCount) {
  return Math.max(4, Math.round(((Number(totalCount) || 0) / trendMaxCount.value) * 100))
}

function trendApprovedPercent(approvedCount, totalCount) {
  const total = Number(totalCount) || 0
  if (!total) return 0
  return Math.round(((Number(approvedCount) || 0) / total) * 100)
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
  } finally {
    auditing.value = false
  }
}

async function loadTrend() {
  const trendRes = await adminApi.reservationTrend(trendDays.value, trendGroupBy.value)
  trend.value = trendRes.data || {}
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
.section-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1rem;
  margin-bottom: 1rem;
}
.trend-controls {
  display: flex;
  align-items: center;
  gap: 8px;
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
.trend-card {
  padding: 1.25rem;
}
.trend-kpis {
  display: grid;
  grid-template-columns: repeat(4, minmax(120px, 1fr));
  gap: 0.75rem;
  margin-bottom: 1rem;
}
.kpi-item {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  padding: 10px 12px;
}
.kpi-label {
  font-size: 12px;
  color: #64748b;
}
.kpi-value {
  margin-top: 4px;
  font-size: 16px;
  font-weight: 700;
  color: #1e293b;
}
.kpi-value.up {
  color: #16a34a;
}
.kpi-value.down {
  color: #dc2626;
}
.trend-bars {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.trend-row {
  display: grid;
  grid-template-columns: 116px 1fr 46px;
  gap: 8px;
  align-items: center;
}
.trend-label {
  color: #64748b;
  font-size: 12px;
}
.trend-bar-bg {
  position: relative;
  height: 12px;
  border-radius: 999px;
  background: #e2e8f0;
  overflow: hidden;
}
.trend-bar-total {
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  border-radius: 999px;
  background: linear-gradient(90deg, #60a5fa, #3b82f6);
}
.trend-bar-approved {
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  border-radius: 999px;
  background: linear-gradient(90deg, #34d399, #10b981);
  opacity: 0.85;
}
.trend-value {
  text-align: right;
  font-size: 12px;
  color: #334155;
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
@media (max-width: 1100px) {
  .usage-section {
    grid-template-columns: 1fr;
  }
}
@media (max-width: 768px) {
  .usage-row {
    grid-template-columns: 1fr;
  }
  .trend-kpis {
    grid-template-columns: repeat(2, minmax(120px, 1fr));
  }
  .trend-controls {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
