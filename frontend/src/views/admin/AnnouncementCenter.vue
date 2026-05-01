<template>
  <div class="announcement-page">
    <div class="page-head">
      <div>
        <h2 class="page-title">公告中心</h2>
        <p class="page-sub">系统发布公告统一查看；数据与工作台公告区同源（类型「系统公告」）。</p>
      </div>
      <el-button text type="primary" @click="$router.push('/admin/dashboard')">返回工作台</el-button>
    </div>

    <div v-if="loading" class="card inner-card">
      <el-skeleton animated :rows="8" />
    </div>

    <el-empty v-else-if="list.length === 0" description="暂无系统公告" :image-size="80" />

    <div v-else class="list-wrap">
      <div
        v-for="item in list"
        :key="item.id"
        class="card ann-row"
        @click="openDetail(item)"
      >
        <div class="ann-head">
          <span class="ann-title">{{ item.title }}</span>
          <span class="ann-time">{{ formatDateTime(item.createTime) }}</span>
        </div>
        <p class="ann-summary">{{ summaryLine(item.content) }}</p>
        <span class="ann-more">点击查看全文</span>
      </div>
    </div>

    <el-drawer v-model="drawerVisible" title="公告详情" size="440px">
      <template v-if="active">
        <div class="drawer-meta">{{ formatDateTime(active.createTime) }}</div>
        <h3 class="drawer-title">{{ active.title }}</h3>
        <p class="drawer-body">{{ active.content }}</p>
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import dayjs from 'dayjs'
import { adminApi } from '@/api'

const loading = ref(true)
const list = ref([])
const drawerVisible = ref(false)
const active = ref(null)

const formatDateTime = t => (t ? dayjs(t).format('YYYY-MM-DD HH:mm') : '-')

function summaryLine(content, max = 140) {
  const t = String(content || '').replace(/\s+/g, ' ').trim()
  if (t.length <= max) return t || '—'
  return `${t.slice(0, max)}…`
}

function openDetail(item) {
  active.value = item
  drawerVisible.value = true
}

onMounted(async () => {
  try {
    const res = await adminApi.announcements({ limit: 80 })
    list.value = Array.isArray(res.data) ? res.data : []
  } catch (e) {
    list.value = []
    ElMessage.error(e?.message || '加载公告失败')
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.announcement-page {
  max-width: 840px;
}
.page-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1rem;
  margin-bottom: 1.25rem;
}
.page-title {
  font-size: 1.125rem;
  font-weight: 600;
  color: #1e293b;
  margin: 0;
}
.page-sub {
  font-size: 0.82rem;
  color: #94a3b8;
  margin: 0.35rem 0 0;
  line-height: 1.5;
}
.card {
  background: #fff;
  border-radius: 0.75rem;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}
.inner-card {
  padding: 1.25rem;
}
.list-wrap {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}
.ann-row {
  padding: 1rem 1.15rem;
  cursor: pointer;
  transition: box-shadow 0.2s, border-color 0.2s;
  border: 1px solid transparent;
}
.ann-row:hover {
  border-color: #e8eefc;
  box-shadow: 0 4px 14px rgba(64, 158, 255, 0.12);
}
.ann-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 0.75rem;
}
.ann-title {
  font-weight: 600;
  color: #1e293b;
  font-size: 0.95rem;
}
.ann-time {
  font-size: 0.75rem;
  color: #94a3b8;
  flex-shrink: 0;
}
.ann-summary {
  margin: 0.65rem 0 0;
  font-size: 0.85rem;
  color: #64748b;
  line-height: 1.6;
}
.ann-more {
  display: inline-block;
  margin-top: 0.5rem;
  font-size: 0.75rem;
  color: #409eff;
}
.drawer-meta {
  font-size: 0.8rem;
  color: #94a3b8;
  margin-bottom: 0.5rem;
}
.drawer-title {
  font-size: 1rem;
  font-weight: 600;
  color: #1e293b;
  margin: 0 0 1rem;
  line-height: 1.4;
}
.drawer-body {
  margin: 0;
  font-size: 0.9rem;
  color: #475569;
  line-height: 1.75;
  white-space: pre-wrap;
}
@media (max-width: 768px) {
  .page-head {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
