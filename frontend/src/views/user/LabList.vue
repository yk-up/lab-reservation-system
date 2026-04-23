<template>
  <div class="page-container">
    <div class="page-header mb-2">
      <div>
        <h2 class="page-title">实验室列表</h2>
        <p class="page-subtitle">浏览实验室信息，并结合使用率参考选择预约对象</p>
      </div>
      <el-input
        v-model="searchText"
        placeholder="搜索实验室名称"
        :prefix-icon="Search"
        clearable
        style="width: 220px"
      />
    </div>

    <div class="usage-panel mb-3" v-if="usageList.length">
      <div class="usage-card-left card usage-shell">
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

      <div class="usage-card-right card usage-shell">
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
            v-for="lab in filteredLabs"
            :key="lab.id"
            class="lab-card"
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

        <el-empty v-if="filteredLabs.length === 0" description="暂无可用实验室" />
      </template>
    </el-skeleton>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Search } from '@element-plus/icons-vue'
import { labApi } from '@/api'

const router = useRouter()
const loading = ref(true)
const labs = ref([])
const usageList = ref([])
const usageTotal = ref(0)
const searchText = ref('')

const filteredLabs = computed(() =>
  labs.value.filter(lab => !searchText.value || lab.name.includes(searchText.value))
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

function goBook(lab) {
  if (lab.status !== 1) return
  router.push(`/labs/${lab.id}/book`)
}

onMounted(async () => {
  try {
    const [labRes, usageRes] = await Promise.all([
      labApi.list(),
      labApi.usage()
    ])
    labs.value = labRes.data || []
    usageList.value = usageRes.data?.ranking || []
    usageTotal.value = usageRes.data?.totalReservations || 0
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 0.75rem;
}
.page-title {
  font-size: 1.4rem;
  font-weight: 700;
  color: #1f2937;
}
.page-subtitle {
  font-size: 0.82rem;
  color: #94a3b8;
  margin-top: 0.25rem;
}
.usage-panel {
  display: grid;
  grid-template-columns: 1.08fr 1fr;
  gap: 1rem;
}
.usage-shell {
  background: linear-gradient(180deg, #ffffff 0%, #fbfdff 100%);
  border: 1px solid #edf2f7;
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.06);
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
  background: #fff;
  border-radius: 0.9rem;
  padding: 1.25rem;
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  border: 1px solid transparent;
}
.lab-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 10px 28px rgba(64, 158, 255, 0.15);
  border-color: #409eff33;
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
