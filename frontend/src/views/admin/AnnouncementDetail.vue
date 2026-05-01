<template>
  <div class="detail-page">
    <div class="detail-toolbar">
      <el-button text type="primary" :icon="ArrowLeft" @click="goBack">返回列表</el-button>
    </div>

    <div v-if="loading" class="card detail-card detail-card--loading">
      <el-skeleton animated :rows="6" />
    </div>

    <el-result
      v-else-if="!notice"
      icon="warning"
      title="无法加载公告"
      :sub-title="errorMsg"
    >
      <template #extra>
        <el-button type="primary" @click="goBack">返回公告中心</el-button>
      </template>
    </el-result>

    <article v-else class="card detail-card">
      <header class="detail-head">
        <el-tag type="info" size="small" effect="plain">系统公告</el-tag>
        <h1 class="detail-title">{{ notice.title }}</h1>
        <div class="detail-meta">{{ formatFullTime(notice.createTime) }}</div>
      </header>
      <div class="detail-body">{{ notice.content }}</div>
    </article>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft } from '@element-plus/icons-vue'
import dayjs from 'dayjs'
import { adminApi } from '@/api'

const route = useRoute()
const router = useRouter()

const loading = ref(true)
const notice = ref(null)
const errorMsg = ref('')

const formatFullTime = t => (t ? dayjs(t).format('YYYY-MM-DD HH:mm') : '-')

function goBack() {
  router.push({ name: 'AdminAnnouncementCenter' })
}

async function load(id) {
  loading.value = true
  notice.value = null
  errorMsg.value = ''
  try {
    const res = await adminApi.announcementDetail(id)
    notice.value = res.data || null
    if (!notice.value) errorMsg.value = '未返回有效数据'
  } catch (e) {
    errorMsg.value = e?.message || e?.response?.data?.message || '请求失败'
  } finally {
    loading.value = false
  }
}

watch(
  () => route.params.id,
  async id => {
    const num = Number(id)
    if (!id || Number.isNaN(num) || num <= 0) {
      loading.value = false
      notice.value = null
      errorMsg.value = '无效的公告 ID'
      return
    }
    await load(num)
  },
  { immediate: true }
)
</script>

<style scoped>
.detail-page {
  max-width: 720px;
}
.detail-toolbar {
  margin-bottom: 1rem;
}
.detail-card {
  padding: 1.5rem 1.65rem;
  background: #fff;
  border-radius: 0.75rem;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}
.detail-card--loading {
  min-height: 200px;
}
.detail-head {
  margin-bottom: 1.25rem;
  padding-bottom: 1rem;
  border-bottom: 1px solid #f1f5f9;
}
.detail-title {
  margin: 0.85rem 0 0;
  font-size: 1.35rem;
  font-weight: 700;
  color: #1e293b;
  line-height: 1.35;
}
.detail-meta {
  margin-top: 0.75rem;
  font-size: 0.82rem;
  color: #94a3b8;
}
.detail-body {
  margin: 0;
  font-size: 0.95rem;
  color: #475569;
  line-height: 1.85;
  white-space: pre-wrap;
}
</style>
