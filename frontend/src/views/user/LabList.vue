<template>
  <div class="page-container">
    <div class="home-overview user-card mb-3">
      <div class="overview-head">
        <div>
          <h2 class="overview-title">{{ greetingText }}，{{ userDisplayName }}</h2>
          <p class="overview-subtitle">{{ todayText }} {{ weekText }} · {{ weatherText }}</p>
        </div>
      </div>
      <div class="overview-grid">
        <div class="overview-card">
          <div class="overview-label">未读消息</div>
          <div class="overview-value">{{ userStore.isLoggedIn ? unreadCount : '-' }}</div>
          <div class="overview-extra">
            <span>{{ userStore.isLoggedIn ? '及时查看审核与提醒通知' : '登录后查看通知消息' }}</span>
            <el-button type="primary" text @click="goNotices">
              {{ userStore.isLoggedIn ? '查看消息' : '去登录' }}
            </el-button>
          </div>
        </div>
        <div class="overview-card">
          <div class="overview-label">我的预约</div>
          <div class="overview-value">{{ userStore.isLoggedIn ? myReservations.length : '-' }}</div>
          <div class="overview-extra">
            <span v-if="userStore.isLoggedIn">今日 {{ todayReservationCount }} 条，待审核 {{ pendingReservationCount }} 条</span>
            <span v-else>登录后查看个人预约摘要</span>
            <el-button type="primary" text @click="goMyReservations">
              {{ userStore.isLoggedIn ? '查看预约' : '去登录' }}
            </el-button>
          </div>
        </div>
        <div class="overview-card">
          <div class="overview-label">今日提示</div>
          <div class="overview-tip">{{ todayTip }}</div>
        </div>
      </div>
    </div>

    <div class="page-header user-page-header mb-2">
      <div>
        <h2 class="page-title user-page-title">实验室列表</h2>
        <p class="page-subtitle user-page-subtitle">浏览实验室信息，并结合使用率参考选择预约对象</p>
      </div>
      <el-input
        v-model="searchText"
        placeholder="搜索名称或位置"
        :prefix-icon="Search"
        clearable
        style="width: 220px"
      />
    </div>

    <div class="usage-panel mb-3" v-if="usageList.length">
      <div class="usage-card-left card user-card usage-shell">
        <div class="usage-header">
          <div>
            <h3 class="usage-title">实验室使用率排行</h3>
            <p class="usage-subtitle">按预约次数统计，直观查看当前热门实验室</p>
          </div>
          <div class="usage-summary">
            <div class="summary-pill">总预约 {{ usageTotal }}</div>
          </div>
        </div>

        <div class="usage-rank-list">
          <div v-for="item in usageList.slice(0, 4)" :key="item.labId" class="usage-rank-item">
            <div class="usage-rank-info">
              <span class="rank-index" :class="rankClass(item.rank)">#{{ item.rank }}</span>
              <div class="rank-text">
                <div class="rank-name">{{ item.labName }}</div>
                <div class="rank-location">{{ item.location || '暂无位置信息' }}</div>
              </div>
            </div>
            <div class="usage-bar-area">
              <div class="usage-bar-bg">
                <div class="usage-bar" :style="{ width: `${barPercent(item)}%` }"></div>
              </div>
              <div class="usage-values">
                <span>{{ item.reservationCount }} 次</span>
                <span>{{ item.usageRate }}%</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div class="usage-card-right card user-card usage-shell">
        <div class="usage-header compact-header">
          <div>
            <h3 class="usage-title">使用率明细</h3>
            <p class="usage-subtitle">为预约选择提供参考依据</p>
          </div>
        </div>
        <el-table :data="usageList.slice(0, 6)" stripe size="small" style="width: 100%" class="usage-table">
          <el-table-column prop="rank" label="排名" width="70" />
          <el-table-column prop="labName" label="实验室名称" min-width="150" />
          <el-table-column prop="location" label="位置" min-width="140" show-overflow-tooltip />
          <el-table-column prop="reservationCount" label="预约次数" width="90" />
          <el-table-column label="使用率" width="100">
            <template #default="{ row }">
              <el-tag type="success" effect="light">{{ row.usageRate }}%</el-tag>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>

    <div v-else class="usage-empty mb-3">
      <div class="usage-empty-title">实验室使用率统计</div>
      <div class="usage-empty-text">当前预约数据较少，暂未生成排行展示。提交几条预约后，这里会自动显示热门实验室参考。</div>
    </div>

    <el-skeleton :loading="loading" animated :count="3">
      <template #template>
        <div class="lab-grid">
          <el-skeleton-item v-for="i in 3" :key="i" variant="rect" style="height: 180px; border-radius: 12px;" />
        </div>
      </template>
      <template #default>
        <div class="lab-grid">
          <div
            v-for="lab in labs"
            :key="lab.id"
            class="lab-card user-card user-card-hover"
            @click="goBook(lab)"
          >
            <div class="lab-card-header">
              <div class="lab-icon">
                <el-icon size="28" color="#409eff"><OfficeBuilding /></el-icon>
              </div>
              <el-tag :type="lab.status === 1 ? 'success' : 'info'" size="small">
                {{ lab.status === 1 ? '开放中' : '已关闭' }}
              </el-tag>
            </div>
            <h3 class="lab-name">{{ lab.name }}</h3>
            <p class="lab-location">
              <el-icon><Location /></el-icon> {{ lab.location || '暂无位置信息' }}
            </p>
            <p class="lab-desc">{{ lab.description || '暂无简介' }}</p>
            <div class="lab-footer">
              <span class="lab-capacity">
                <el-icon><User /></el-icon> 最多 {{ lab.capacity }} 人
              </span>
              <el-button
                type="primary"
                size="small"
                :disabled="lab.status !== 1"
                @click.stop="goBook(lab)"
              >
                立即预约
              </el-button>
            </div>
          </div>
        </div>

        <AppEmptyState
          v-if="labs.length === 0"
          :type="isSearchEmpty ? 'search' : 'reservation'"
          :title="isSearchEmpty ? '未找到相关实验室' : '暂无可用实验室'"
          :description="isSearchEmpty ? `没有找到与“${searchText.trim()}”相关的实验室，试试更换关键词。` : '当前暂时没有可展示的实验室，请稍后再来查看。'"
          :secondary-action-text="isSearchEmpty ? '清空搜索' : ''"
          :action-text="isSearchEmpty ? '查看全部实验室' : '返回上一页'"
          @secondary-action="clearSearch"
          @action="isSearchEmpty ? clearSearch() : goBack()"
        />
      </template>
    </el-skeleton>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import dayjs from 'dayjs'
import { labApi, reservationApi, noticeApi, homeApi } from '@/api'
import { useUserStore } from '@/store/user'
import AppEmptyState from '@/components/AppEmptyState.vue'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(true)
const labs = ref([])
const usageList = ref([])
const usageTotal = ref(0)
const searchText = ref('')
const unreadCount = ref(0)
const myReservations = ref([])
const weatherText = ref('天气信息加载中')
const WEATHER_CACHE_KEY = 'user_home_weather_v1'
const WEATHER_CACHE_TTL = 15 * 60 * 1000

let searchDebounceTimer = null

const userDisplayName = computed(() => userStore.realName || '同学')
const currentHour = dayjs().hour()
const greetingText = computed(() => {
  if (currentHour < 6) return '夜深了'
  if (currentHour < 12) return '早上好'
  if (currentHour < 18) return '下午好'
  return '晚上好'
})
const todayText = computed(() => dayjs().format('YYYY年MM月DD日'))
const weekText = computed(() => {
  const weekdays = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
  return weekdays[dayjs().day()]
})
const todayReservationCount = computed(() => {
  const dayStart = dayjs().startOf('day')
  const dayEnd = dayStart.add(1, 'day')
  return myReservations.value.filter(item => {
    if (!item.startTime || !item.endTime) return false
    const start = dayjs(item.startTime)
    const end = dayjs(item.endTime)
    return start.isBefore(dayEnd) && end.isAfter(dayStart)
  }).length
})
const pendingReservationCount = computed(() => myReservations.value.filter(item => item.status === 0).length)
const todayTip = computed(() => {
  if (!userStore.isLoggedIn) return '登录后可查看个人预约与消息摘要。'
  if (unreadCount.value > 0) return `有 ${unreadCount.value} 条未读消息，建议优先处理。`
  if (todayReservationCount.value > 0) return `你今天有 ${todayReservationCount.value} 条预约安排，请提前准备。`
  if (pendingReservationCount.value > 0) return `当前有 ${pendingReservationCount.value} 条预约待审核，请留意审核结果。`
  return '今日暂无紧急事项，祝你学习顺利。'
})

const isSearchEmpty = computed(
  () => !!searchText.value.trim() && labs.value.length === 0 && !loading.value
)

const maxUsageCount = computed(() => Math.max(...usageList.value.map(item => Number(item.reservationCount) || 0), 1))

function barPercent(item) {
  const count = Number(item.reservationCount) || 0
  if (count <= 0) return 0
  return Math.max(12, Math.round((count / maxUsageCount.value) * 100))
}

function rankClass(rank) {
  if (rank === 1) return 'rank-top1'
  if (rank === 2) return 'rank-top2'
  if (rank === 3) return 'rank-top3'
  return ''
}

async function reloadLabsFromServer() {
  const kw = searchText.value.trim()
  const res = await labApi.list(kw ? { keyword: kw } : {})
  labs.value = res.data || []
}

watch(
  searchText,
  () => {
    if (searchDebounceTimer) clearTimeout(searchDebounceTimer)
    searchDebounceTimer = setTimeout(async () => {
      try {
        await reloadLabsFromServer()
      } catch {
        labs.value = []
      }
    }, 350)
  }
)

function clearSearch() {
  searchText.value = ''
  reloadLabsFromServer().catch(() => {})
}

function goBack() {
  router.back()
}

function goBook(lab) {
  if (lab.status !== 1) return
  router.push(`/labs/${lab.id}/book`)
}

function goNotices() {
  router.push(userStore.isLoggedIn ? '/notices' : '/login')
}

function goMyReservations() {
  router.push(userStore.isLoggedIn ? '/my-reservations' : '/login')
}

function inferWeatherFromSeason() {
  const month = dayjs().month() + 1
  if (month >= 3 && month <= 5) return '春季，注意早晚温差'
  if (month >= 6 && month <= 8) return '夏季，注意防暑补水'
  if (month >= 9 && month <= 11) return '秋季，天气较干燥'
  return '冬季，注意保暖防寒'
}

function normalizeWeather(payload) {
  const current = payload?.current_condition?.[0] || {}
  const nearestArea = payload?.nearest_area?.[0] || {}
  const city = nearestArea?.areaName?.[0]?.value || ''
  const desc = current?.lang_zh?.[0]?.value || current?.weatherDesc?.[0]?.value || ''
  const temp = current?.temp_C
  const feelsLike = current?.FeelsLikeC
  const humidity = current?.humidity

  const head = city ? `${city} ${desc}`.trim() : desc
  const tempText = temp != null ? `${temp}°C` : ''
  const feelText = feelsLike != null ? `体感${feelsLike}°C` : ''
  const humidityText = humidity != null ? `湿度${humidity}%` : ''
  const segments = [head, tempText, feelText, humidityText].filter(Boolean)
  return segments.length ? segments.join(' · ') : inferWeatherFromSeason()
}

function readCachedWeather() {
  try {
    const raw = localStorage.getItem(WEATHER_CACHE_KEY)
    if (!raw) return ''
    const parsed = JSON.parse(raw)
    if (!parsed?.text || !parsed?.at) return ''
    if (Date.now() - parsed.at > WEATHER_CACHE_TTL) return ''
    return parsed.text
  } catch {
    return ''
  }
}

function writeCachedWeather(text) {
  try {
    localStorage.setItem(WEATHER_CACHE_KEY, JSON.stringify({ text, at: Date.now() }))
  } catch {}
}

async function fetchJsonWithTimeout(url, timeout = 3500) {
  const controller = new AbortController()
  const timer = setTimeout(() => controller.abort(), timeout)
  try {
    const res = await fetch(url, { signal: controller.signal })
    if (!res.ok) throw new Error('weather unavailable')
    return await res.json()
  } finally {
    clearTimeout(timer)
  }
}

async function fetchWeatherSummary() {
  const cached = readCachedWeather()
  if (cached) weatherText.value = cached
  try {
    const payload = await fetchJsonWithTimeout('https://wttr.in/?format=j1')
    const summary = normalizeWeather(payload)
    weatherText.value = summary
    writeCachedWeather(summary)
  } catch {
    if (!cached) weatherText.value = inferWeatherFromSeason()
  }
}

async function fetchHomeDigest() {
  if (!userStore.isLoggedIn) {
    unreadCount.value = 0
    myReservations.value = []
    return
  }
  try {
    const [noticeRes, reservationRes] = await Promise.all([
      noticeApi.unreadCount(),
      reservationApi.myList()
    ])
    unreadCount.value = noticeRes.data || 0
    myReservations.value = reservationRes.data || []
    userStore.setUnreadCount(unreadCount.value)
  } catch {
    unreadCount.value = 0
    myReservations.value = []
  }
}

onMounted(async () => {
  try {
    const homeRes = await homeApi.overview()
    await Promise.allSettled([fetchHomeDigest(), fetchWeatherSummary()])
    labs.value = homeRes.data?.labs || []
    usageList.value = homeRes.data?.usageStats?.ranking || []
    usageTotal.value = homeRes.data?.usageStats?.totalReservations || 0
  } catch (error) {
    labs.value = []
    usageList.value = []
    usageTotal.value = 0
    ElMessage.error(error?.message || '加载实验室数据失败，请稍后重试')
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.page-header {
  align-items: center;
}
.home-overview {
  border-radius: 1rem;
  padding: 1.2rem 1.25rem;
  background: linear-gradient(140deg, #ecf5ff, #f5f7fa 60%, #f0f9eb);
  border-color: #d9ecff;
}
.overview-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 0.75rem;
  margin-bottom: 0.95rem;
}
.overview-title {
  font-size: 1.15rem;
  font-weight: 700;
  color: #1f2937;
}
.overview-subtitle {
  font-size: 0.82rem;
  color: #64748b;
  margin-top: 0.25rem;
}
.overview-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0.75rem;
}
.overview-card {
  background: #ffffffd9;
  border-radius: 0.85rem;
  border: 1px solid #e5e7eb;
  padding: 0.85rem 0.95rem;
  display: flex;
  flex-direction: column;
  gap: 0.45rem;
  min-height: 110px;
}
.overview-label {
  font-size: 0.78rem;
  color: #64748b;
}
.overview-value {
  font-size: 1.35rem;
  line-height: 1.2;
  font-weight: 700;
  color: #2563eb;
}
.overview-extra {
  margin-top: auto;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.6rem;
  font-size: 0.75rem;
  color: #64748b;
}
.overview-tip {
  font-size: 0.86rem;
  line-height: 1.6;
  color: #334155;
}
.usage-panel {
  display: grid;
  grid-template-columns: 1.08fr 1fr;
  gap: 1rem;
}
.usage-shell {
  background: linear-gradient(180deg, #ffffff 0%, #fbfdff 100%);
  border-color: #edf2f7;
  box-shadow: var(--user-shadow-sm);
}
.usage-card-left,
.usage-card-right {
  padding: 1.15rem 1.2rem;
  border-radius: 1rem;
}
.usage-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 1rem;
  margin-bottom: 1rem;
}
.compact-header {
  margin-bottom: 0.75rem;
}
.usage-title {
  font-size: 1rem;
  font-weight: 700;
  color: #1e293b;
}
.usage-subtitle {
  font-size: 0.78rem;
  color: #94a3b8;
  margin-top: 0.25rem;
}
.summary-pill {
  padding: 0.45rem 0.8rem;
  background: linear-gradient(135deg, #e0f2fe, #dbeafe);
  color: #2563eb;
  border-radius: 999px;
  font-size: 0.78rem;
  font-weight: 700;
  white-space: nowrap;
}
.usage-rank-list {
  display: flex;
  flex-direction: column;
  gap: 0.95rem;
}
.usage-rank-item {
  display: grid;
  grid-template-columns: 230px 1fr;
  gap: 0.8rem;
  align-items: center;
}
.usage-rank-info {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}
.rank-text {
  min-width: 0;
}
.rank-index {
  width: 2rem;
  height: 2rem;
  border-radius: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #60a5fa, #3b82f6);
  color: #fff;
  font-size: 0.78rem;
  font-weight: 700;
  flex-shrink: 0;
  box-shadow: 0 6px 16px rgba(59, 130, 246, 0.28);
}
.rank-top1 {
  background: linear-gradient(135deg, #f59e0b, #fbbf24);
  box-shadow: 0 6px 16px rgba(245, 158, 11, 0.28);
}
.rank-top2 {
  background: linear-gradient(135deg, #94a3b8, #cbd5e1);
  box-shadow: 0 6px 16px rgba(148, 163, 184, 0.28);
}
.rank-top3 {
  background: linear-gradient(135deg, #fb7185, #f43f5e);
  box-shadow: 0 6px 16px rgba(244, 63, 94, 0.24);
}
.rank-name {
  font-size: 0.92rem;
  font-weight: 700;
  color: #1f2937;
}
.rank-location {
  font-size: 0.75rem;
  color: #94a3b8;
  margin-top: 0.2rem;
}
.usage-bar-area {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}
.usage-bar-bg {
  flex: 1;
  height: 0.72rem;
  background: #eef2f7;
  border-radius: 999px;
  overflow: hidden;
}
.usage-bar {
  height: 100%;
  border-radius: 999px;
  background: linear-gradient(90deg, #4da3ff, #67c23a);
}
.usage-values {
  min-width: 92px;
  display: flex;
  justify-content: space-between;
  gap: 0.4rem;
  font-size: 0.8rem;
  color: #475569;
  font-weight: 500;
}
.usage-table :deep(.el-table__header th) {
  background: #f8fafc;
  color: #64748b;
}
.usage-empty {
  border-radius: 1rem;
  background: linear-gradient(180deg, #ffffff 0%, #f8fbff 100%);
  border: 1px dashed #cbd5e1;
  padding: 1.25rem 1.35rem;
  color: #64748b;
}
.usage-empty-title {
  font-size: 0.98rem;
  font-weight: 700;
  color: #334155;
  margin-bottom: 0.35rem;
}
.usage-empty-text {
  font-size: 0.82rem;
  line-height: 1.7;
}
.lab-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 1rem;
}
.lab-card {
  border-radius: 0.9rem;
  padding: 1.25rem;
  cursor: pointer;
}
.lab-card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 0.75rem;
}
.lab-icon {
  width: 2.9rem;
  height: 2.9rem;
  background: #ecf5ff;
  border-radius: 0.65rem;
  display: flex;
  align-items: center;
  justify-content: center;
}
.lab-name {
  font-size: 1.05rem;
  font-weight: 700;
  color: #303133;
  margin-bottom: 0.5rem;
}
.lab-location {
  font-size: 0.8rem;
  color: #909399;
  display: flex;
  align-items: center;
  gap: 0.25rem;
  margin-bottom: 0.5rem;
}
.lab-desc {
  font-size: 0.8rem;
  color: #606266;
  line-height: 1.5;
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  margin-bottom: 0.75rem;
  min-height: 2.4em;
}
.lab-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.lab-capacity {
  font-size: 0.8rem;
  color: #909399;
  display: flex;
  align-items: center;
  gap: 0.25rem;
}
@media (max-width: 960px) {
  .overview-grid {
    grid-template-columns: 1fr;
  }
  .usage-panel {
    grid-template-columns: 1fr;
  }
}
@media (max-width: 640px) {
  .usage-rank-item {
    grid-template-columns: 1fr;
  }
  .usage-header {
    flex-direction: column;
    align-items: flex-start;
  }
}
@media (max-width: 480px) {
  .lab-grid {
    grid-template-columns: 1fr;
  }
}
</style>
