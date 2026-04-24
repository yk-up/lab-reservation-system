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
          <span><i class="legend-dot pending"></i>待审核</span>
          <span><i class="legend-dot approved"></i>通过</span>
          <span><i class="legend-dot rejected"></i>拒绝</span>
        </div>

        <div class="trend-chart-wrap">
          <svg
            class="trend-chart-svg"
            :viewBox="`0 0 ${chartWidth} ${chartHeight}`"
            :style="{ width: `${chartWidth}px` }"
            preserveAspectRatio="xMinYMin meet"
          >
            <defs>
              <linearGradient id="barGradientPending" x1="0" y1="0" x2="0" y2="1">
                <stop offset="0%" stop-color="#66b1ff" />
                <stop offset="100%" stop-color="#409eff" />
              </linearGradient>
              <linearGradient id="barGradientApproved" x1="0" y1="0" x2="0" y2="1">
                <stop offset="0%" stop-color="#85ce61" />
                <stop offset="100%" stop-color="#67c23a" />
              </linearGradient>
              <linearGradient id="barGradientRejected" x1="0" y1="0" x2="0" y2="1">
                <stop offset="0%" stop-color="#f89898" />
                <stop offset="100%" stop-color="#f56c6c" />
              </linearGradient>
            </defs>

            <g>
              <line
                v-for="tick in chartTicks"
                :key="`grid-${tick.value}`"
                :x1="chartPadding.left"
                :y1="tick.y"
                :x2="chartWidth - chartPadding.right"
                :y2="tick.y"
                class="chart-grid-line"
              />
              <line
                v-for="point in chartAxisPoints"
                :key="`xgrid-${point.key}`"
                :x1="point.x"
                :y1="chartPadding.top"
                :x2="point.x"
                :y2="chartHeight - chartPadding.bottom"
                class="chart-axis-line vertical"
              />
            </g>

            <line
              :x1="chartPadding.left"
              :y1="chartHeight - chartPadding.bottom"
              :x2="chartWidth - chartPadding.right"
              :y2="chartHeight - chartPadding.bottom"
              class="chart-axis-line"
            />
            <line
              :x1="chartPadding.left"
              :y1="chartPadding.top"
              :x2="chartPadding.left"
              :y2="chartHeight - chartPadding.bottom"
              class="chart-axis-line"
            />

            <text :x="6" :y="14" class="chart-axis-title">预约数</text>

            <g>
              <text
                v-for="tick in chartTicks"
                :key="`ylabel-${tick.value}`"
                :x="chartPadding.left - 12"
                :y="tick.y + 4"
                class="chart-y-label"
              >
                {{ tick.label }}
              </text>
            </g>

            <g>
              <rect
                v-for="bar in chartBars"
                :key="`bar-${bar.key}`"
                :x="bar.x"
                :y="bar.y"
                :width="bar.width"
                :height="bar.renderHeight"
                rx="4"
                :class="['chart-bar', bar.seriesKey]"
              >
                <title>{{ bar.tooltip }}</title>
              </rect>
            </g>

            <g>
              <text
                v-for="label in chartSegmentLabels"
                :key="`segment-${label.key}`"
                :x="label.x"
                :y="label.y"
                :text-anchor="label.anchor"
                :class="['chart-segment-label', label.seriesKey, { outside: label.outside }]"
              >
                {{ label.text }}
              </text>
            </g>

            <g>
              <text
                v-for="label in chartBarTopLabels"
                :key="`top-${label.key}`"
                :x="label.x"
                :y="label.y"
                text-anchor="middle"
                class="chart-bar-top-label"
              >
                {{ label.text }}
              </text>
            </g>

            <g>
              <text
                v-for="point in chartAxisPoints"
                :key="`xlabel-${point.key}`"
                :x="point.labelX"
                :y="point.labelY"
                :text-anchor="point.labelAnchor"
                dominant-baseline="middle"
                class="chart-x-label"
              >
                <tspan
                  v-for="(line, lineIndex) in point.labelLines"
                  :key="`${point.key}-line-${lineIndex}`"
                  :x="point.labelX"
                  :dy="lineIndex === 0 ? (point.labelLines.length > 1 ? -7 : 0) : 14"
                >
                  {{ line }}
                </tspan>
              </text>
            </g>
          </svg>
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
const chartPadding = {
  top: 24,
  right: 28,
  bottom: 58,
  left: 62
}
const chartHeight = 280
const displayTrendSeries = computed(() => {
  if (isDailyTrendView.value) {
    return trendSeries.value.map(item => ({
      key: item.date,
      label: item.label,
      fullLabel: item.date,
      labelLines: [item.label],
      totalCount: Number(item.totalCount) || 0,
      approvedCount: Number(item.approvedCount) || 0,
      rejectedCount: Number(item.rejectedCount) || 0,
      pendingCount: Math.max(
        0,
        (Number(item.totalCount) || 0) - (Number(item.approvedCount) || 0) - (Number(item.rejectedCount) || 0)
      )
    }))
  }

  const groups = []
  for (let i = 0; i < trendSeries.value.length; i += trendGroupSize.value) {
    const chunk = trendSeries.value.slice(i, i + trendGroupSize.value)
    if (!chunk.length) continue

    const totalCount = chunk.reduce((sum, item) => sum + (Number(item.totalCount) || 0), 0)
    const approvedCount = chunk.reduce((sum, item) => sum + (Number(item.approvedCount) || 0), 0)
    const rejectedCount = chunk.reduce((sum, item) => sum + (Number(item.rejectedCount) || 0), 0)

    groups.push({
      key: `${chunk[0].date}-${chunk[chunk.length - 1].date}`,
      label: `${chunk[0].label} ~ ${chunk[chunk.length - 1].label}`,
      fullLabel: `${chunk[0].date} ~ ${chunk[chunk.length - 1].date}`,
      labelLines: [chunk[0].label, `~ ${chunk[chunk.length - 1].label}`],
      totalCount,
      approvedCount,
      rejectedCount,
      pendingCount: Math.max(0, totalCount - approvedCount - rejectedCount)
    })
  }
  return groups
})
const maxTrendCount = computed(() =>
  Math.max(
    ...displayTrendSeries.value.map(item => Number(item.totalCount) || 0),
    1
  )
)
const chartLabelSlotWidth = computed(() => {
  const maxLabelLength = Math.max(...displayTrendSeries.value.map(item => (item.label || '').length), 0)
  return Math.max(120, maxLabelLength * 10)
})
const chartWidth = computed(() =>
  Math.max(820, displayTrendSeries.value.length * chartLabelSlotWidth.value + chartPadding.left + chartPadding.right)
)
const chartPlotWidth = computed(() => chartWidth.value - chartPadding.left - chartPadding.right)
const chartPlotHeight = computed(() => chartHeight - chartPadding.top - chartPadding.bottom)
const chartEdgeOffset = 32
const chartAxisPoints = computed(() =>
  displayTrendSeries.value.map((item, index) => {
    const isSingle = displayTrendSeries.value.length === 1
    const innerWidth = Math.max(0, chartPlotWidth.value - chartEdgeOffset * 2)
    const stepX = displayTrendSeries.value.length > 1 ? innerWidth / (displayTrendSeries.value.length - 1) : 0
    const x =
      isSingle
        ? chartPadding.left + chartPlotWidth.value / 2
        : chartPadding.left + chartEdgeOffset + stepX * index
    return {
      ...item,
      x,
      labelX: isSingle
        ? x
        : index === 0
          ? chartPadding.left + 2
          : index === displayTrendSeries.value.length - 1
            ? chartWidth.value - chartPadding.right - 2
            : x,
      labelAnchor: isSingle
        ? 'middle'
        : index === 0
          ? 'start'
          : index === displayTrendSeries.value.length - 1
            ? 'end'
            : 'middle',
      labelY: chartHeight - chartPadding.bottom / 2
    }
  })
)
const chartMaxValue = computed(() => {
  const rawMax = maxTrendCount.value
  return Math.max(1, rawMax + 1)
})
const chartTicks = computed(() => {
  const max = chartMaxValue.value
  const step = 1
  const values = []

  for (let value = max; value >= 0; value -= step) {
    values.push(value)
  }

  if (values[values.length - 1] !== 0) {
    values.push(0)
  }

  return values.map(value => {
    const ratio = max === 0 ? 0 : value / max
    const y = chartPadding.top + chartPlotHeight.value - ratio * chartPlotHeight.value
    return {
      value,
      y,
      label: String(value)
    }
  })
})
const chartBars = computed(() => {
  const seriesDefs = [
    { key: 'pending', label: '待审核', field: 'pendingCount' },
    { key: 'approved', label: '通过', field: 'approvedCount' },
    { key: 'rejected', label: '拒绝', field: 'rejectedCount' }
  ]
  const estimatedStep =
    chartAxisPoints.value.length > 1
      ? (chartPlotWidth.value - chartEdgeOffset * 2) / (chartAxisPoints.value.length - 1)
      : chartPlotWidth.value / 2
  const barWidth = Math.max(24, Math.min(42, estimatedStep > 0 ? estimatedStep / 3.2 : 28))
  const overlap = 1.5

  return chartAxisPoints.value.flatMap(point => {
    let stackTop = chartPadding.top + chartPlotHeight.value
    return seriesDefs.map((def, index) => {
      const value = Number(point[def.field]) || 0
      const ratio = chartMaxValue.value === 0 ? 0 : value / chartMaxValue.value
      const height = Math.max(value > 0 ? 2 : 0, ratio * chartPlotHeight.value)
      const y = stackTop - height
      const x = point.x - barWidth / 2
      stackTop = y
      return {
        key: `${point.key}-${def.key}`,
        seriesKey: def.key,
        x,
        y,
        width: barWidth,
        height,
        value,
        renderHeight: value > 0 && index > 0 ? height + overlap : height,
        tooltip: `${point.fullLabel} ${def.label}: ${value}`
      }
    })
  })
})
const chartSegmentLabels = computed(() =>
  chartBars.value
    .filter(bar => ['pending', 'approved', 'rejected'].includes(bar.seriesKey) && bar.value > 0)
    .map(bar => {
      const outside = bar.height < 18
      return {
        key: bar.key,
        seriesKey: bar.seriesKey,
        text: bar.value,
        x: outside ? bar.x + bar.width + 6 : bar.x + bar.width / 2,
        y: outside ? bar.y + 10 : bar.y + bar.height / 2 + 1,
        anchor: outside ? 'start' : 'middle',
        outside
      }
    })
)
const chartBarTopLabels = computed(() =>
  chartAxisPoints.value.map(point => {
    const total = Number(point.totalCount) || 0
    const ratio = chartMaxValue.value === 0 ? 0 : total / chartMaxValue.value
    const topY = chartPadding.top + chartPlotHeight.value - ratio * chartPlotHeight.value
    return {
      key: point.key,
      x: point.x,
      y: Math.max(chartPadding.top + 10, topY - 10),
      text: total
    }
  })
)
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
.legend-dot.pending { background: #409eff; }
.legend-dot.approved { background: #67c23a; }
.legend-dot.rejected { background: #f56c6c; }
.trend-chart-wrap {
  overflow-x: auto;
  padding-bottom: 0.25rem;
}
.trend-chart-svg {
  min-width: 820px;
  height: 280px;
  display: block;
}
.chart-grid-line {
  stroke: #e8edf5;
  stroke-width: 1;
}
.chart-axis-line {
  stroke: #d6dfeb;
  stroke-width: 1;
}
.chart-axis-line.vertical {
  stroke: #eef3f9;
}
.chart-y-label {
  fill: #94a3b8;
  font-size: 11px;
  text-anchor: end;
}
.chart-axis-title {
  fill: #64748b;
  font-size: 12px;
  font-weight: 600;
}
.chart-x-label {
  fill: #64748b;
  font-size: 11px;
  text-anchor: middle;
}
.chart-bar {
  stroke: none;
  opacity: 0.96;
}
.chart-bar.pending {
  fill: url(#barGradientPending);
}
.chart-bar.approved {
  fill: url(#barGradientApproved);
}
.chart-bar.rejected {
  fill: url(#barGradientRejected);
}
.chart-segment-label {
  font-size: 11px;
  font-weight: 700;
  dominant-baseline: middle;
  paint-order: stroke;
  stroke: rgba(255, 255, 255, 0.9);
  stroke-width: 3px;
  stroke-linejoin: round;
}
.chart-segment-label.pending {
  fill: #1d4ed8;
}
.chart-segment-label.approved {
  fill: #166534;
}
.chart-segment-label.rejected {
  fill: #b91c1c;
}
.chart-segment-label.outside.pending {
  fill: #2563eb;
}
.chart-segment-label.outside.approved {
  fill: #16a34a;
}
.chart-segment-label.outside.rejected {
  fill: #dc2626;
}
.chart-bar-top-label {
  fill: #1e293b;
  font-size: 12px;
  font-weight: 700;
  text-anchor: middle;
  dominant-baseline: middle;
  paint-order: stroke;
  stroke: rgba(255, 255, 255, 0.92);
  stroke-width: 3px;
  stroke-linejoin: round;
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
  .trend-chart-svg {
    min-width: 700px;
  }
}
</style>
