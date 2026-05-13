<template>
  <div class="dashboard-page">
    <section class="card hero-card">
      <div>
        <div class="hero-badge">Admin Data Screen</div>
        <h2 class="hero-title">{{ greeting }}，{{ displayName }}</h2>
        <p class="hero-desc">数据大屏聚合关键业务指标、趋势统计与待办数据，便于统一查看管理态势。</p>
        <div class="hero-tags">
          <el-tag type="primary" effect="dark">管理员工作台</el-tag>
          <el-tag type="success" effect="plain">{{ currentDateText }}</el-tag>
          <el-tag type="warning" effect="plain">{{ currentWeekText }}</el-tag>
        </div>
      </div>
      <div class="hero-side">
        <div class="info-card">
          <div class="info-label">登录账号</div>
          <div class="info-value">{{ userStore.userInfo?.username || '-' }}</div>
          <div class="info-sub">姓名：{{ displayName }}</div>
        </div>
        <div class="info-card">
          <div class="info-label">当前时间</div>
          <div class="info-value">{{ currentTimeText }}</div>
          <div class="info-sub">{{ currentDateText }} · {{ currentWeekText }}</div>
        </div>
      </div>
    </section>

    <section class="portal-grid mt-3">
      <div class="card quick-apps-card">
        <div class="section-head">
          <div>
            <h3 class="section-title">
              <el-icon style="vertical-align: middle; margin-right: 0.5rem;"><Grid /></el-icon>
              全部应用
            </h3>
            <p class="section-subtitle">快速进入后台核心功能模块</p>
          </div>
          <el-tag type="info" effect="plain">{{ quickActions.length }} 个应用</el-tag>
        </div>
        <div class="quick-grid">
          <button 
            v-for="item in quickActions" 
            :key="item.path" 
            class="quick-item" 
            @click="goTo(item.path)"
            :class="{ 'is-active': $route.path === item.path }"
          >
            <span class="quick-icon" :style="{ background: item.bg, color: item.color }">
              <el-icon :size="20"><component :is="item.icon" /></el-icon>
            </span>
            <span class="quick-text">
              <strong>{{ item.label }}</strong>
              <small>{{ item.desc }}</small>
            </span>
          </button>
        </div>
      </div>

      <div class="card summary-card">
        <div class="section-head">
          <div>
            <h3 class="section-title">
              <el-icon style="vertical-align: middle; margin-right: 0.5rem;"><Monitor /></el-icon>
              大屏概览
            </h3>
            <p class="section-subtitle">核心指标与待办概览</p>
          </div>
        </div>
        <div class="summary-list">
          <div class="summary-item"><span>当前角色</span><strong>管理员</strong></div>
          <div class="summary-item"><span>待审核预约</span><strong class="warning">{{ data.pendingCount ?? 0 }}</strong></div>
          <div class="summary-item"><span>开放实验室</span><strong class="success">{{ data.openLabs ?? 0 }}</strong></div>
          <div class="summary-item"><span>黑名单人数</span><strong class="danger">{{ data.blacklistCount ?? 0 }}</strong></div>
        </div>
        <div class="summary-actions">
          <el-button type="primary" @click="goTo('/admin/approval')">处理审批</el-button>
          <el-button @click="goTo('/admin/labs')">查看实验室</el-button>
        </div>
      </div>

      <div class="card weather-portal-card">
        <div class="section-head weather-section-head">
          <div>
            <h3 class="section-title">
              <el-icon style="vertical-align: middle; margin-right: 0.5rem;"><Sunny /></el-icon>
              天气信息
            </h3>
            <p class="section-subtitle">
              <el-icon style="vertical-align: middle; margin-right: 0.25rem;"><Location /></el-icon>
              {{ weather.city }} · {{ weather.phenomenon }}
              <el-tag size="small" type="info" effect="plain" class="weather-source-tag">演示数据</el-tag>
            </p>
          </div>
          <el-button
            type="primary"
            link
            @click="weatherExpanded = !weatherExpanded"
          >
            {{ weatherExpanded ? '收起' : '更多' }}
            <el-icon class="weather-chevron" :class="{ 'is-open': weatherExpanded }" style="margin-left: 0.2rem;">
              <ArrowDown />
            </el-icon>
          </el-button>
        </div>
        <div class="weather-main">
          <div class="weather-icon-wrap" :style="{ background: weather.themeTint }">
            <el-icon :size="36" :color="weather.themeColor"><Sunny /></el-icon>
          </div>
          <div class="weather-body">
            <div class="weather-temp-row">
              <span class="weather-temp-now">{{ weather.tempNow }}°</span>
              <span class="weather-temp-unit">C</span>
            </div>
            <div class="weather-phenomenon">{{ weather.phenomenon }}，体感舒适</div>
            <div class="weather-range">{{ weather.tempMin }}° / {{ weather.tempMax }}° · 今日气温</div>
          </div>
        </div>
        <el-collapse-transition>
          <div v-show="weatherExpanded" class="weather-extra">
            <div class="weather-extra-grid">
              <div class="weather-extra-item">
                <span class="weather-extra-label">湿度</span>
                <strong>{{ weather.humidity }}%</strong>
              </div>
              <div class="weather-extra-item">
                <span class="weather-extra-label">气压</span>
                <strong>{{ weather.pressureHpa }} hPa</strong>
              </div>
              <div class="weather-extra-item">
                <span class="weather-extra-label">风力</span>
                <strong>{{ weather.windDir }} {{ weather.windScale }}</strong>
              </div>
              <div class="weather-extra-item">
                <span class="weather-extra-label">空气质量</span>
                <strong>{{ weather.aqiLevel }}</strong>
              </div>
            </div>
            <p class="weather-hint">首版无后端，以上为占位数据；后续可对接第三方气象接口并替换数据源。</p>
          </div>
        </el-collapse-transition>
      </div>
    </section>

    <div class="stat-cards mt-3">
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

    <!-- 公告展示区 -->
    <div class="card announcement-card mt-3">
      <div class="section-head">
        <div>
          <h3 class="section-title">
            <el-icon style="vertical-align: middle; margin-right: 0.5rem;"><Bell /></el-icon>
            系统公告
          </h3>
          <p class="section-subtitle">最新发布的系统公告和通知信息</p>
        </div>
        <el-button type="primary" link @click="goTo('/admin/announcements')">
          查看更多
          <el-icon style="margin-left: 0.25rem;"><ArrowRight /></el-icon>
        </el-button>
      </div>

      <div v-if="announcements.length" class="announcement-list">
        <div 
          v-for="item in announcements" 
          :key="item.id" 
          class="announcement-item"
          @click="goToAnnouncement(item.id)"
        >
          <div class="announcement-header">
            <div class="announcement-title-row">
              <el-tag 
                v-if="item.priority === 'high'" 
                type="danger" 
                size="small" 
                effect="dark"
                class="announcement-priority"
              >
                重要
              </el-tag>
              <el-tag 
                v-else-if="item.priority === 'medium'" 
                type="warning" 
                size="small" 
                effect="plain"
                class="announcement-priority"
              >
                通知
              </el-tag>
              <h4 class="announcement-title">{{ item.title }}</h4>
              <el-tag 
                v-if="isNew(item.publishTime)" 
                type="danger" 
                size="small" 
                effect="dark"
                round
                class="new-badge"
              >
                NEW
              </el-tag>
            </div>
            <span class="announcement-time">{{ formatAnnouncementTime(item.publishTime) }}</span>
          </div>
          <p class="announcement-summary">{{ item.summary || item.content?.substring(0, 80) + '...' }}</p>
          <div class="announcement-footer">
            <span class="announcement-author">
              <el-icon><User /></el-icon>
              {{ item.publisherName || '系统管理员' }}
            </span>
            <span class="announcement-views" v-if="item.viewCount">
              <el-icon><View /></el-icon>
              {{ item.viewCount }} 次浏览
            </span>
          </div>
        </div>
      </div>
      <el-empty v-else description="暂无系统公告" :image-size="120" />
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

        <div class="trend-chart-shell">
          <div class="trend-legend">
            <span><i class="legend-dot pending"></i>待审核</span>
            <span><i class="legend-dot approved"></i>通过</span>
            <span><i class="legend-dot rejected"></i>拒绝</span>
          </div>
          <div class="trend-axis-title">预约数</div>
          <div class="trend-chart-body">
            <div class="trend-y-axis">
              <span
                v-for="tick in trendTicks"
                :key="`tick-${tick}`"
                class="trend-y-tick"
                :style="{ bottom: `${trendTickBottom(tick)}%` }"
              >
                {{ tick }}
              </span>
            </div>

            <div class="trend-chart-wrap">
              <div class="trend-grid-lines">
                <span
                  v-for="tick in trendTicks"
                  :key="`grid-${tick}`"
                  class="trend-grid-line"
                  :style="{ bottom: `${trendTickBottom(tick)}%` }"
                ></span>
              </div>

              <div class="trend-chart">
                <div v-for="item in trendItems" :key="item.date" class="trend-group">
                  <div class="trend-bar-stack">
                    <div class="trend-total-label" :style="{ bottom: trendTotalOffset(item) }">
                      {{ item.totalCount }}
                    </div>
                    <span
                      v-if="item.pendingCount > 0"
                      class="trend-bar pending"
                      :style="{ height: trendSegmentHeight(item.pendingCount) }"
                    >
                      <em class="trend-segment-label">{{ item.pendingCount }}</em>
                    </span>
                    <span
                      v-if="item.approvedCount > 0"
                      class="trend-bar approved"
                      :style="{ height: trendSegmentHeight(item.approvedCount) }"
                    >
                      <em class="trend-segment-label">{{ item.approvedCount }}</em>
                    </span>
                    <span
                      v-if="item.rejectedCount > 0"
                      class="trend-bar rejected"
                      :style="{ height: trendSegmentHeight(item.rejectedCount) }"
                    >
                      <em class="trend-segment-label">{{ item.rejectedCount }}</em>
                    </span>
                  </div>
                  <div class="trend-date">{{ item.label }}</div>
                </div>
              </div>
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
import { computed, ref, onMounted, onBeforeUnmount } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { Grid, Monitor, Bell, ArrowRight, ArrowDown, User, View, Sunny, Location } from '@element-plus/icons-vue'
import dayjs from 'dayjs'
import { adminApi, reservationApi } from '@/api'
import { useUserStore } from '@/store/user'

const router = useRouter()
const userStore = useUserStore()
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
const now = ref(dayjs())
const announcements = ref([])
const weatherExpanded = ref(false)
/** 首版静态天气；后续可改为 fetch 第三方 API（如 Open-Meteo / 和风等） */
const weather = ref({
  city: '上海',
  phenomenon: '晴',
  tempNow: 24,
  tempMin: 18,
  tempMax: 26,
  humidity: 62,
  pressureHpa: 1008,
  windDir: '东南风',
  windScale: '3 级',
  aqiLevel: '良',
  themeColor: '#d97706',
  themeTint: 'linear-gradient(135deg, #fff7ed 0%, #ffedd5 100%)'
})
let clockTimer = null

const statCards = [
  { key: 'totalLabs', label: '实验室总数', icon: 'OfficeBuilding', bg: '#ecf5ff', color: '#409eff' },
  { key: 'openLabs', label: '开放中', icon: 'Check', bg: '#f0f9eb', color: '#67c23a' },
  { key: 'pendingCount', label: '待审核预约', icon: 'Clock', bg: '#fdf6ec', color: '#e6a23c' },
  { key: 'blacklistCount', label: '黑名单人数', icon: 'UserFilled', bg: '#fef0f0', color: '#f56c6c' }
]

const quickActions = [
  { path: '/admin/screen', label: '数据大屏', desc: '查看全局统计分析', icon: 'DataAnalysis', bg: '#e0f7fa', color: '#0891b2' },
  { path: '/admin/workbench', label: '工作台', desc: '查看后台首页与核心数据', icon: 'HomeFilled', bg: '#e0edff', color: '#2563eb' },
  { path: '/admin/notices', label: '消息通知', desc: '查看系统消息通知', icon: 'ChatDotRound', bg: '#f4f0ff', color: '#9333ea' },
  { path: '/admin/announcements', label: '公告中心', desc: '查看并管理系统公告', icon: 'Bell', bg: '#e8f3ff', color: '#409eff' },
  { path: '/admin/approval', label: '审批中心', desc: '处理预约审批申请', icon: 'DocumentChecked', bg: '#edf9ee', color: '#67c23a' },
  { path: '/admin/labs', label: '实验室管理', desc: '维护实验室信息', icon: 'OfficeBuilding', bg: '#fff5e8', color: '#e6a23c' },
  { path: '/admin/blacklist', label: '黑名单管理', desc: '处理限制账号', icon: 'UserFilled', bg: '#feeeee', color: '#f56c6c' }
]

const displayName = computed(() => userStore.realName || userStore.userInfo?.username || '管理员')
const greeting = computed(() => {
  const hour = now.value.hour()
  if (hour < 6) return '凌晨好'
  if (hour < 11) return '上午好'
  if (hour < 14) return '中午好'
  if (hour < 18) return '下午好'
  return '晚上好'
})
const currentDateText = computed(() => now.value.format('YYYY年MM月DD日'))
const currentTimeText = computed(() => now.value.format('HH:mm:ss'))
const currentWeekText = computed(() => ['星期日', '星期一', '星期二', '星期三', '星期四', '星期五', '星期六'][now.value.day()])

const maxUsageCount = computed(() => Math.max(...usageList.value.map(item => Number(item.reservationCount) || 0), 1))
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
const maxTrendCount = computed(() => Math.max(...trendItems.value.map(item => item.totalCount), 1))
const trendAxisMax = computed(() => maxTrendCount.value + 1)
const trendTicks = computed(() => Array.from({ length: trendAxisMax.value + 1 }, (_, index) => index).reverse())

const formatDateTime = t => (t ? dayjs(t).format('MM-DD HH:mm') : '-')
const formatTime = t => (t ? dayjs(t).format('HH:mm') : '-')
const formatMetric = value => {
  const num = Number(value) || 0
  return Number.isInteger(num) ? String(num) : num.toFixed(1)
}

function ensureArray(value, fallbackKeys = []) {
  if (Array.isArray(value)) return value
  for (const key of fallbackKeys) {
    if (Array.isArray(value?.[key])) return value[key]
  }
  return []
}

function goTo(path) {
  router.push(path)
}

function goToAnnouncement(id) {
  router.push(`/admin/announcements/${id}`)
}

function formatAnnouncementTime(time) {
  if (!time) return '-'
  const date = dayjs(time)
  const diffDays = dayjs().diff(date, 'day')
  
  if (diffDays === 0) {
    const diffHours = dayjs().diff(date, 'hour')
    if (diffHours === 0) {
      const diffMinutes = dayjs().diff(date, 'minute')
      return diffMinutes <= 1 ? '刚刚' : `${diffMinutes}分钟前`
    }
    return `${diffHours}小时前`
  }
  
  if (diffDays === 1) return '昨天'
  if (diffDays < 7) return `${diffDays}天前`
  
  return date.format('YYYY-MM-DD')
}

function isNew(time) {
  if (!time) return false
  const diffHours = dayjs().diff(dayjs(time), 'hour')
  return diffHours <= 24
}

function resolveAnnouncementPriority(item) {
  const time = item?.createTime || item?.publishTime
  if (!time) return 'normal'
  const diffDays = dayjs().diff(dayjs(time), 'day')
  if (diffDays <= 1) return 'high'
  if (diffDays <= 3) return 'medium'
  return 'normal'
}

function barPercent(item) {
  return Math.max(12, Math.round(((Number(item.reservationCount) || 0) / maxUsageCount.value) * 100))
}

function trendSegmentHeight(value) {
  const num = Number(value) || 0
  if (num <= 0) return '0%'
  return `${(num / trendAxisMax.value) * 100}%`
}

function trendTotalOffset(item) {
  const total = Number(item.totalCount) || 0
  return `calc(${(total / trendAxisMax.value) * 100}% + 6px)`
}

function trendTickBottom(tick) {
  return (tick / trendAxisMax.value) * 100
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

function mapAnnouncementRows(rows) {
  if (!Array.isArray(rows)) return []
  return rows.map(item => {
    const content = String(item?.content || '').replace(/\s+/g, ' ').trim()
    return {
      ...item,
      publishTime: item?.createTime || item?.publishTime,
      summary: content.length > 80 ? `${content.slice(0, 80)}...` : content,
      priority: resolveAnnouncementPriority(item),
      publisherName: item?.publisherName || '系统管理员'
    }
  })
}

function applyScreenBundle(screen) {
  const s = screen && typeof screen === 'object' ? screen : {}
  data.value = s.dashboard || {}
  usageList.value = ensureArray(s.usage?.ranking, ['list', 'rows'])
  usageTotal.value = Number(s.usage?.totalReservations) || 0
  trendSeries.value = ensureArray(s.trend?.series)
  trendSummary.value = s.trend?.summary || {}
  const annRows = ensureArray(s.announcementsTop, ['list', 'rows'])
  announcements.value = mapAnnouncementRows(annRows)
}

async function loadDashboardMain() {
  const days = Number(trendRange.value)
  const opts = { skipErrorToast: true }
  try {
    const screenRes = await adminApi.screenStats(
      { days, announcementLimit: 5 },
      opts
    )
    applyScreenBundle(screenRes.data)
    return
  } catch {
    // 聚合接口不可用（旧后端/404）时降级为多次请求
  }
  const [dashRes, usageRes, trendRes, annRes] = await Promise.all([
    adminApi.dashboard(opts),
    adminApi.labUsage(opts),
    adminApi.reservationTrend({ days }, opts),
    adminApi.dashboardAnnouncements({ limit: 5 }, opts)
  ])
  data.value = dashRes.data || {}
  usageList.value = ensureArray(usageRes.data?.ranking, ['list', 'rows'])
  usageTotal.value = Number(usageRes.data?.totalReservations) || 0
  trendSeries.value = ensureArray(trendRes.data?.series)
  trendSummary.value = trendRes.data?.summary || {}
  const rawAnn = annRes.data
  const annRows = Array.isArray(rawAnn) ? rawAnn : ensureArray(rawAnn, ['list', 'rows'])
  announcements.value = mapAnnouncementRows(annRows)
}

async function loadTrend() {
  const res = await adminApi.reservationTrend({ days: Number(trendRange.value) })
  trendSeries.value = ensureArray(res.data?.series)
  trendSummary.value = res.data?.summary || {}
}

onMounted(async () => {
  clockTimer = window.setInterval(() => {
    now.value = dayjs()
  }, 1000)
  try {
    await loadDashboardMain()
  } catch (error) {
    data.value = {}
    usageList.value = []
    usageTotal.value = 0
    trendSeries.value = []
    trendSummary.value = {}
    announcements.value = []
    ElMessage.error(error?.message || '加载大屏数据失败，请确认已登录管理员且后端已更新')
  }
  try {
    const pendingRes = await reservationApi.pending()
    pendingList.value = ensureArray(pendingRes.data, ['list', 'rows'])
  } catch {
    pendingList.value = []
  }
})
onBeforeUnmount(() => {
  if (clockTimer) {
    window.clearInterval(clockTimer)
    clockTimer = null
  }
})
</script>

<style scoped>
.dashboard-page {
  display: flex;
  flex-direction: column;
}
.hero-card {
  display: grid;
  grid-template-columns: 1.4fr 0.8fr;
  gap: 1rem;
  padding: 1.5rem;
  background: linear-gradient(135deg, #1d4ed8, #2563eb 40%, #38bdf8);
  color: #fff;
}
.hero-badge {
  display: inline-block;
  padding: 4px 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.16);
  font-size: 12px;
}
.hero-title {
  margin-top: 0.9rem;
  font-size: 2rem;
  font-weight: 700;
}
.hero-desc {
  margin-top: 0.8rem;
  color: rgba(255, 255, 255, 0.9);
  line-height: 1.7;
}
.hero-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
  margin-top: 1rem;
}
.hero-side {
  display: grid;
  gap: 1rem;
}
.info-card {
  padding: 1rem 1.1rem;
  border-radius: 1rem;
  background: rgba(255, 255, 255, 0.16);
  border: 1px solid rgba(255, 255, 255, 0.18);
}
.info-label {
  font-size: 0.82rem;
  color: rgba(255, 255, 255, 0.78);
}
.info-value {
  margin-top: 0.35rem;
  font-size: 1.35rem;
  font-weight: 700;
}
.info-sub {
  margin-top: 0.35rem;
  font-size: 0.85rem;
  color: rgba(255, 255, 255, 0.88);
}
.portal-grid {
  display: grid;
  grid-template-columns: 1.15fr 0.95fr minmax(260px, 0.9fr);
  gap: 1rem;
}
.weather-portal-card {
  position: relative;
  overflow: hidden;
}
.weather-portal-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
  background: linear-gradient(90deg, #38bdf8, #f59e0b, #fbbf24);
}
.weather-section-head .section-subtitle {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 0.35rem;
}
.weather-source-tag {
  margin-left: 0.25rem;
}
.weather-main {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 0.25rem 0 0.5rem;
}
.weather-icon-wrap {
  width: 4.25rem;
  height: 4.25rem;
  border-radius: 1rem;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  border: 1px solid rgba(251, 191, 36, 0.35);
}
.weather-body {
  min-width: 0;
}
.weather-temp-row {
  display: flex;
  align-items: flex-start;
  line-height: 1;
}
.weather-temp-now {
  font-size: 2.35rem;
  font-weight: 700;
  color: #0f172a;
  letter-spacing: -0.03em;
}
.weather-temp-unit {
  font-size: 1rem;
  font-weight: 600;
  color: #64748b;
  margin-left: 0.15rem;
  margin-top: 0.35rem;
}
.weather-phenomenon {
  margin-top: 0.35rem;
  font-size: 0.95rem;
  font-weight: 600;
  color: #334155;
}
.weather-range {
  margin-top: 0.2rem;
  font-size: 0.8rem;
  color: #94a3b8;
}
.weather-extra {
  margin-top: 0.75rem;
  padding-top: 0.9rem;
  border-top: 1px solid #e2e8f0;
}
.weather-extra-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.65rem 1rem;
}
.weather-extra-item {
  display: flex;
  flex-direction: column;
  gap: 0.2rem;
  padding: 0.65rem 0.75rem;
  border-radius: 0.75rem;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
}
.weather-extra-label {
  font-size: 0.75rem;
  color: #94a3b8;
}
.weather-extra-item strong {
  font-size: 0.9rem;
  color: #1e293b;
  font-weight: 600;
}
.weather-hint {
  margin: 0.75rem 0 0;
  font-size: 0.72rem;
  color: #94a3b8;
  line-height: 1.5;
}
.weather-chevron {
  transition: transform 0.2s ease;
}
.weather-chevron.is-open {
  transform: rotate(180deg);
}
.quick-apps-card {
  position: relative;
  overflow: hidden;
}
.quick-apps-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
  background: linear-gradient(90deg, #409eff, #67c23a, #e6a23c);
}
.summary-card {
  position: relative;
  overflow: hidden;
}
.summary-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
  background: linear-gradient(90deg, #67c23a, #409eff);
}
.quick-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.9rem;
}
.quick-item {
  border: 1px solid #e5e7eb;
  border-radius: 1rem;
  background: #fff;
  padding: 1rem;
  display: flex;
  gap: 0.75rem;
  text-align: left;
  cursor: pointer;
  transition: all 0.3s ease;
  position: relative;
  overflow: hidden;
}
.quick-item::before {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, transparent 0%, rgba(64, 158, 255, 0.05) 100%);
  opacity: 0;
  transition: opacity 0.3s ease;
}
.quick-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  border-color: #409eff;
}
.quick-item:hover::before {
  opacity: 1;
}
.quick-item.is-active {
  border-color: #409eff;
  background: linear-gradient(135deg, #f0f9ff 0%, #e0f2fe 100%);
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.2);
}
.quick-item.is-active .quick-text strong {
  color: #409eff;
}
.quick-icon {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: transform 0.3s ease;
}
.quick-item:hover .quick-icon {
  transform: scale(1.1);
}
.quick-text {
  display: flex;
  flex-direction: column;
  position: relative;
  z-index: 1;
}
.quick-text strong {
  color: #0f172a;
  font-size: 0.95rem;
  transition: color 0.3s ease;
}
.quick-text small {
  margin-top: 0.2rem;
  color: #64748b;
  font-size: 0.8rem;
}
.summary-list {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}
.summary-item {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  padding: 0.9rem 1rem;
  border-radius: 0.9rem;
  background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
  border: 1px solid #e2e8f0;
  transition: all 0.3s ease;
}
.summary-item:hover {
  background: linear-gradient(135deg, #f1f5f9 0%, #e2e8f0 100%);
  transform: translateX(4px);
}
.summary-item span {
  color: #64748b;
  font-size: 0.9rem;
}
.summary-item strong {
  font-size: 1.1rem;
  font-weight: 700;
}
.summary-actions {
  display: flex;
  gap: 0.75rem;
  margin-top: 1rem;
  flex-wrap: wrap;
}
.warning {
  color: #d97706;
}
.success {
  color: #059669;
}
.danger {
  color: #dc2626;
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
  border: 1px solid #f1f5f9;
  transition: all 0.3s ease;
  position: relative;
  overflow: hidden;
}
.stat-card::before {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, transparent 0%, rgba(0, 0, 0, 0.02) 100%);
  opacity: 0;
  transition: opacity 0.3s ease;
}
.stat-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 16px rgba(0,0,0,0.12);
  border-color: #e2e8f0;
}
.stat-card:hover::before {
  opacity: 1;
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
.announcement-card {
  position: relative;
  overflow: hidden;
}
.announcement-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
  background: linear-gradient(90deg, #f59e0b, #ef4444, #ec4899);
}
.announcement-list {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}
.announcement-item {
  padding: 1.25rem;
  border-radius: 0.75rem;
  background: linear-gradient(135deg, #fefefe 0%, #f9fafb 100%);
  border: 1px solid #e5e7eb;
  cursor: pointer;
  transition: all 0.3s ease;
  position: relative;
  overflow: hidden;
}
.announcement-item::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 4px;
  background: linear-gradient(180deg, #409eff, #67c23a);
  opacity: 0;
  transition: opacity 0.3s ease;
}
.announcement-item:hover {
  transform: translateX(4px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  border-color: #409eff;
}
.announcement-item:hover::before {
  opacity: 1;
}
.announcement-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 1rem;
  margin-bottom: 0.75rem;
}
.announcement-title-row {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  flex: 1;
  min-width: 0;
}
.announcement-priority {
  flex-shrink: 0;
}
.announcement-title {
  font-size: 1rem;
  font-weight: 600;
  color: #1e293b;
  margin: 0;
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.new-badge {
  flex-shrink: 0;
  animation: pulse 2s ease-in-out infinite;
}
@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.6; }
}
.announcement-time {
  font-size: 0.8rem;
  color: #94a3b8;
  white-space: nowrap;
  flex-shrink: 0;
}
.announcement-summary {
  font-size: 0.9rem;
  color: #64748b;
  line-height: 1.6;
  margin: 0 0 0.75rem 0;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.announcement-footer {
  display: flex;
  align-items: center;
  gap: 1.5rem;
  font-size: 0.8rem;
  color: #94a3b8;
}
.announcement-author,
.announcement-views {
  display: flex;
  align-items: center;
  gap: 0.35rem;
}
.announcement-author .el-icon,
.announcement-views .el-icon {
  font-size: 0.9rem;
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
  margin-bottom: 0.9rem;
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
.trend-chart-shell {
  margin-top: 0.25rem;
  padding: 0;
  border-radius: 0;
  background: transparent;
  box-shadow: none;
}
.trend-axis-title {
  margin-bottom: 0.7rem;
  font-size: 0.95rem;
  font-weight: 600;
  color: #475569;
  letter-spacing: 0.02em;
}
.trend-chart-body {
  display: grid;
  grid-template-columns: 56px 1fr;
  gap: 0.75rem;
  align-items: stretch;
}
.trend-y-axis {
  position: relative;
  height: 260px;
}
.trend-y-tick {
  position: absolute;
  right: 0;
  transform: translateY(50%);
  font-size: 0.8rem;
  color: #94a3b8;
}
.trend-chart-wrap {
  position: relative;
  height: 260px;
  overflow-x: auto;
  overflow-y: hidden;
  border-left: 1px solid #e8edf5;
  border-bottom: 1px solid #e8edf5;
}
.trend-grid-lines {
  position: absolute;
  inset: 0;
  pointer-events: none;
}
.trend-grid-line {
  position: absolute;
  left: 0;
  right: 0;
  border-top: 1px solid #e8edf5;
}
.trend-chart {
  position: relative;
  z-index: 1;
  min-width: max(100%, 760px);
  height: 100%;
  display: flex;
  justify-content: space-around;
  align-items: stretch;
  gap: 1.4rem;
  padding: 0 0.8rem;
}
.trend-group {
  flex: 1;
  min-width: 76px;
  max-width: 104px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: flex-end;
}
.trend-total-label {
  position: absolute;
  left: 50%;
  transform: translateX(-50%);
  font-size: 0.9rem;
  font-weight: 700;
  color: #334155;
  text-shadow: none;
  white-space: nowrap;
  pointer-events: none;
}
.trend-bar-stack {
  position: relative;
  width: 78%;
  height: 220px;
  display: flex;
  flex-direction: column-reverse;
  justify-content: flex-start;
  align-items: stretch;
  overflow: visible;
  filter: drop-shadow(0 8px 16px rgba(15, 23, 42, 0.08));
}
.trend-bar {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 0;
  transition: height 0.2s ease;
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.18),
    inset 0 -1px 0 rgba(255, 255, 255, 0.08);
}
.trend-bar + .trend-bar {
  margin-bottom: -3px;
}
.trend-bar::before {
  content: '';
  position: absolute;
  left: 0;
  right: 0;
  top: -6px;
  height: 12px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.18), rgba(255, 255, 255, 0));
  opacity: 0.55;
  pointer-events: none;
}
.trend-bar.pending {
  background: linear-gradient(180deg, #7cbcff 0%, #4d9fff 55%, #409eff 100%);
  border-radius: 0.5rem 0.5rem 0 0;
}
.trend-bar.approved {
  background: linear-gradient(180deg, #98dd7e 0%, #75cc4d 55%, #67c23a 100%);
}
.trend-bar.rejected {
  background: linear-gradient(180deg, #f9aaaa 0%, #f67979 55%, #f56c6c 100%);
  border-radius: 0 0 0.5rem 0.5rem;
}
.trend-segment-label {
  font-style: normal;
  font-size: 0.82rem;
  font-weight: 700;
  color: white;
  text-shadow: 0 2px 4px rgba(15, 23, 42, 0.3);
}
.trend-date {
  margin-top: 0.9rem;
  text-align: center;
  font-size: 0.85rem;
  color: #64748b;
}
@media (max-width: 1280px) {
  .portal-grid {
    grid-template-columns: 1fr 1fr;
  }
  .weather-portal-card {
    grid-column: 1 / -1;
  }
}
@media (max-width: 1100px) {
  .portal-grid {
    grid-template-columns: 1fr;
  }
  .weather-portal-card {
    grid-column: auto;
  }
  .usage-section {
    grid-template-columns: 1fr;
  }
  .trend-summary {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
@media (max-width: 768px) {
  .hero-card {
    grid-template-columns: 1fr;
  }
  .quick-grid {
    grid-template-columns: 1fr;
  }
  .stat-cards {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
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
  .trend-chart {
    min-width: 640px;
  }
}
</style>
