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

    <div class="card announcements-card mt-3">
      <div class="section-head">
        <div>
          <h3 class="section-title">系统公告</h3>
          <p class="section-subtitle">最新发布的系统公告摘要；完整列表请前往公告中心</p>
        </div>
        <el-button type="primary" link @click="goAnnouncementCenter">
          查看更多
          <el-icon class="el-icon--right"><ArrowRight /></el-icon>
        </el-button>
      </div>

      <div v-if="announcementsLoading" class="announcements-skel">
        <el-skeleton :rows="3" animated />
      </div>
      <template v-else-if="dashboardAnnouncements.length">
        <ul class="announcement-items">
          <li
            v-for="item in dashboardAnnouncements"
            :key="item.id"
            class="announcement-item announcement-item--click"
            role="link"
            tabindex="0"
            @click="openAnnouncementDetail(item)"
            @keydown.enter="openAnnouncementDetail(item)"
          >
            <div class="announcement-item-main">
              <span class="announcement-item-title">{{ item.title }}</span>
              <span class="announcement-item-time">{{ formatPublishTime(item.createTime) }}</span>
            </div>
            <p class="announcement-item-summary">{{ announcementSummary(item.content) }}</p>
          </li>
        </ul>
      </template>
      <el-empty v-else description="暂无系统公告" :image-size="64" />
    </div>

    <div class="usage-section mt-3">
      <div class="card usage-card">
        <div class="section-head">
          <div>
            <h3 class="section-title">实验室使用率排行</h3>
            <p class="section-subtitle">按预约次数统计；下方图表为各实验室使用率（占全部预约的百分比）</p>
          </div>
          <el-tag type="primary" effect="dark">总预约 {{ usageTotal }}</el-tag>
        </div>

        <div v-if="usageList.length" ref="usageChartRef" class="echart-box" />
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
          <p class="section-subtitle">按申请日期观察预约变化；下图按日（或每 7 天）堆叠展示待审核、通过、拒绝数量</p>
        </div>
        <div class="trend-toolbar">
          <el-radio-group v-model="trendDisplayMode" size="small">
            <el-radio-button value="daily">每天</el-radio-button>
            <el-radio-button value="weekly">每7天</el-radio-button>
          </el-radio-group>
          <el-radio-group v-model="trendRange" size="small" @change="loadTrend">
            <el-radio-button value="7">近7天</el-radio-button>
            <el-radio-button value="15">近15天</el-radio-button>
            <el-radio-button value="30">近30天</el-radio-button>
          </el-radio-group>
        </div>
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

        <div ref="trendChartRef" class="echart-box echart-box--trend" />
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
import { computed, ref, onMounted, onBeforeUnmount, nextTick, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowRight } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import dayjs from 'dayjs'
import * as echarts from 'echarts'
import { adminApi, reservationApi } from '@/api'

const router = useRouter()

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
const usageChartRef = ref(null)
const trendChartRef = ref(null)
let usageChart = null
let trendChart = null

const dashboardAnnouncements = ref([])
const announcementsLoading = ref(true)

const statCards = [
  { key: 'totalLabs', label: '实验室总数', icon: 'OfficeBuilding', bg: '#ecf5ff', color: '#409eff' },
  { key: 'openLabs', label: '开放中', icon: 'Check', bg: '#f0f9eb', color: '#67c23a' },
  { key: 'pendingCount', label: '待审核预约', icon: 'Clock', bg: '#fdf6ec', color: '#e6a23c' },
  { key: 'blacklistCount', label: '黑名单人数', icon: 'UserFilled', bg: '#fef0f0', color: '#f56c6c' }
]

const trendItems = computed(() => {
  const normalized = trendSeries.value.map(item => {
    const totalCount = Number(item.totalCount) || 0
    const approvedCount = Number(item.approvedCount) || 0
    const rejectedCount = Number(item.rejectedCount) || 0
    const pendingCount = Math.max(0, totalCount - approvedCount - rejectedCount)
    return {
      ...item,
      totalCount,
      approvedCount,
      rejectedCount,
      pendingCount
    }
  })

  if (trendDisplayMode.value === 'daily') return normalized

  const grouped = []
  for (let i = 0; i < normalized.length; i += 7) {
    const chunk = normalized.slice(i, i + 7)
    if (!chunk.length) continue

    grouped.push({
      date: `${chunk[0].date}-${chunk[chunk.length - 1].date}`,
      label: `${chunk[0].label} ~ ${chunk[chunk.length - 1].label}`,
      totalCount: chunk.reduce((sum, item) => sum + item.totalCount, 0),
      approvedCount: chunk.reduce((sum, item) => sum + item.approvedCount, 0),
      rejectedCount: chunk.reduce((sum, item) => sum + item.rejectedCount, 0),
      pendingCount: chunk.reduce((sum, item) => sum + item.pendingCount, 0)
    })
  }

  return grouped
})

const formatDateTime = t => (t ? dayjs(t).format('MM-DD HH:mm') : '-')
const formatPublishTime = t => (t ? dayjs(t).format('YYYY-MM-DD HH:mm') : '-')
const formatTime = t => (t ? dayjs(t).format('HH:mm') : '-')
const formatMetric = value => {
  const num = Number(value) || 0
  return Number.isInteger(num) ? String(num) : num.toFixed(1)
}

function announcementSummary(content, maxLen = 120) {
  const t = String(content || '').replace(/\s+/g, ' ').trim()
  if (t.length <= maxLen) return t || '—'
  return `${t.slice(0, maxLen)}…`
}

function goAnnouncementCenter() {
  router.push({ name: 'AdminAnnouncementCenter' })
}

function openAnnouncementDetail(item) {
  if (!item?.id) return
  router.push({
    name: 'AdminAnnouncementDetail',
    params: { id: String(item.id) }
  })
}

function ensureArray(value, fallbackKeys = []) {
  if (Array.isArray(value)) return value
  for (const key of fallbackKeys) {
    if (Array.isArray(value?.[key])) return value[key]
  }
  return []
}

function shortLabLabel(row) {
  const name = row.labName || `实验室#${row.labId}`
  return name.length > 16 ? `${name.slice(0, 16)}…` : name
}

function syncUsageChart() {
  const el = usageChartRef.value
  if (!el || !usageList.value.length) {
    usageChart?.dispose()
    usageChart = null
    return
  }
  if (!usageChart) usageChart = echarts.init(el)
  const rows = usageList.value
  const categories = rows.map(r => `#${r.rank} ${shortLabLabel(r)}`)
  const rates = rows.map(r => Number(r.usageRate) || 0)
  const counts = rows.map(r => Number(r.reservationCount) || 0)
  usageChart.setOption(
    {
      tooltip: {
        trigger: 'axis',
        axisPointer: { type: 'shadow' },
        formatter(params) {
          const i = params[0].dataIndex
          const r = rows[i]
          const loc = r.location ? `<br/>${r.location}` : ''
          return `${r.labName || '-'}${loc}<br/>预约 <strong>${counts[i]}</strong> 次 · 使用率 <strong>${rates[i]}%</strong>`
        }
      },
      grid: { left: 4, right: 20, top: 8, bottom: 4, containLabel: true },
      xAxis: {
        type: 'value',
        max: 100,
        axisLabel: { formatter: '{value}%' },
        splitLine: { lineStyle: { type: 'dashed', color: '#e2e8f0' } }
      },
      yAxis: {
        type: 'category',
        data: categories,
        inverse: true,
        axisTick: { show: false },
        axisLine: { lineStyle: { color: '#cbd5e1' } }
      },
      series: [
        {
          name: '使用率',
          type: 'bar',
          data: rates,
          barMaxWidth: 26,
          itemStyle: {
            borderRadius: [0, 6, 6, 0],
            color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
              { offset: 0, color: '#409eff' },
              { offset: 1, color: '#67c23a' }
            ])
          }
        }
      ]
    },
    true
  )
}

function syncTrendChart() {
  const el = trendChartRef.value
  const items = trendItems.value
  if (!el || !items.length) {
    trendChart?.dispose()
    trendChart = null
    return
  }
  if (!trendChart) trendChart = echarts.init(el)
  const labels = items.map(i => i.label)
  const pending = items.map(i => i.pendingCount)
  const approved = items.map(i => i.approvedCount)
  const rejected = items.map(i => i.rejectedCount)
  const needZoom = labels.length > 12
  trendChart.setOption(
    {
      tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
      legend: {
        data: ['待审核', '通过', '拒绝'],
        top: 0,
        textStyle: { color: '#64748b' }
      },
      grid: {
        left: 44,
        right: 16,
        top: 40,
        bottom: needZoom ? 52 : 28,
        containLabel: false
      },
      xAxis: {
        type: 'category',
        data: labels,
        axisLabel: {
          rotate: labels.length > 8 ? 32 : 0,
          interval: 0,
          color: '#64748b',
          hideOverlap: true
        },
        axisLine: { lineStyle: { color: '#e2e8f0' } }
      },
      yAxis: {
        type: 'value',
        name: '预约数',
        nameTextStyle: { color: '#64748b', padding: [0, 0, 0, 8] },
        splitLine: { lineStyle: { type: 'dashed', color: '#e2e8f0' } },
        axisLine: { show: false }
      },
      dataZoom: needZoom
        ? [{ type: 'slider', xAxisIndex: 0, height: 18, bottom: 6, start: 0, end: 100 }]
        : [],
      series: [
        { name: '待审核', type: 'bar', stack: 'day', data: pending, itemStyle: { color: '#409eff' } },
        { name: '通过', type: 'bar', stack: 'day', data: approved, itemStyle: { color: '#67c23a' } },
        { name: '拒绝', type: 'bar', stack: 'day', data: rejected, itemStyle: { color: '#f56c6c' } }
      ]
    },
    true
  )
}

function resizeCharts() {
  usageChart?.resize()
  trendChart?.resize()
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
  trendSeries.value = ensureArray(res.data?.series)
  trendSummary.value = res.data?.summary || {}
  await nextTick()
  syncTrendChart()
  resizeCharts()
}

watch(usageList, () => nextTick(() => syncUsageChart()), { deep: true })
watch(trendItems, () => nextTick(() => syncTrendChart()), { deep: true })

onMounted(async () => {
  try {
    const [dashRes, pendingRes, usageRes, annRes] = await Promise.all([
      adminApi.dashboard(),
      reservationApi.pending(),
      adminApi.labUsage(),
      adminApi.dashboardAnnouncements({ limit: 5 }).catch(() => ({ data: [] }))
    ])
    data.value = dashRes.data || {}
    pendingList.value = ensureArray(pendingRes.data, ['list', 'rows'])
    usageList.value = ensureArray(usageRes.data?.ranking, ['list', 'rows'])
    usageTotal.value = Number(usageRes.data?.totalReservations) || 0
    dashboardAnnouncements.value = Array.isArray(annRes.data) ? annRes.data : []
    await loadTrend()
  } catch (error) {
    pendingList.value = []
    usageList.value = []
    trendSeries.value = []
    trendSummary.value = {}
    dashboardAnnouncements.value = []
    ElMessage.error(error?.message || '加载看板数据失败')
  } finally {
    announcementsLoading.value = false
  }
  await nextTick()
  syncUsageChart()
  syncTrendChart()
  window.addEventListener('resize', resizeCharts)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeCharts)
  usageChart?.dispose()
  trendChart?.dispose()
  usageChart = null
  trendChart = null
})
</script>

<style scoped>
.announcements-card {
  padding: 1.25rem;
}
.announcements-skel {
  padding: 0.25rem 0;
}
.announcement-items {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 0;
}
.announcement-item {
  padding: 0.9rem 0;
  border-bottom: 1px solid #f1f5f9;
}
.announcement-item--click {
  cursor: pointer;
  border-radius: 0.35rem;
  margin: 0 -0.35rem;
  padding-left: 0.35rem;
  padding-right: 0.35rem;
  outline: none;
}
.announcement-item--click:hover {
  background: #f8fafc;
}
.announcement-item:last-child {
  border-bottom: none;
  padding-bottom: 0;
}
.announcement-item:first-child {
  padding-top: 0;
}
.announcement-item-main {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 0.75rem;
}
.announcement-item-title {
  font-weight: 600;
  font-size: 0.92rem;
  color: #1e293b;
}
.announcement-item-time {
  font-size: 0.75rem;
  color: #94a3b8;
  flex-shrink: 0;
}
.announcement-item-summary {
  margin: 0.45rem 0 0;
  font-size: 0.82rem;
  color: #64748b;
  line-height: 1.55;
}

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
.echart-box {
  width: 100%;
  height: 320px;
}
.echart-box--trend {
  height: 380px;
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
@media (max-width: 1100px) {
  .usage-section {
    grid-template-columns: 1fr;
  }
  .trend-summary {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
@media (max-width: 768px) {
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
  .echart-box {
    height: 280px;
  }
  .echart-box--trend {
    height: 320px;
  }
}
</style>
