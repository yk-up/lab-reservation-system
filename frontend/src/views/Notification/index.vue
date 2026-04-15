<template>
  <div class="page-container notification-page">
    <div class="page-header mb-2">
      <h2 class="page-title">消息中心</h2>
    </div>

    <div class="card list-card">
      <div class="list-header">
        <span class="list-title">消息列表</span>
      </div>

      <div class="list-body">
        <el-empty description="暂无消息" />
      </div>

      <div class="pagination-wrapper" v-if="showPagination">
        <el-pagination
          background
          layout="prev, pager, next"
          :total="total"
          :page-size="pageSize"
          :current-page="currentPage"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { getNotificationList, getUnreadCount, markAllRead, markAsRead } from '@/api/notification'

const messages = ref([])
const loading = ref(false)
const unreadCount = ref(0)

const showPagination = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const loadMessages = async () => {
  loading.value = true
  try {
    const res = await getNotificationList({ page: 1, size: 10 })
    messages.value = res.data
  } finally {
    loading.value = false
  }
}

const loadUnread = async () => {
  const res = await getUnreadCount()
  unreadCount.value = res.data
}

const handleRead = async (item) => {
  if (item.read) return
  await markAsRead(item.id)
  item.read = true
  loadUnread()
}

const handleReadAll = async () => {
  await markAllRead()
  messages.value.forEach(m => m.read = true)
  unreadCount.value = 0
}

onMounted(() => {
  loadMessages()
  loadUnread()
})
</script>

<style scoped>
.notification-page {
  display: flex;
  flex-direction: column;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.page-title {
  font-size: 1.25rem;
  font-weight: 600;
  color: #303133;
}

.list-card {
  min-height: 420px;
  display: flex;
  flex-direction: column;
}

.list-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 1rem;
  border-bottom: 1px solid #ebeef5;
}

.list-title {
  font-size: 1rem;
  font-weight: 500;
  color: #303133;
}

.list-body {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 2rem 0;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  padding-top: 1rem;
  border-top: 1px solid #ebeef5;
}
</style>
