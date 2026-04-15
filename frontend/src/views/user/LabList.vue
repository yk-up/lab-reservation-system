<template>
  <div class="page-container">
    <div class="page-header mb-2">
      <h2 class="page-title">实验室列表</h2>
      <el-input
        v-model="searchText"
        placeholder="搜索实验室名称"
        :prefix-icon="Search"
        clearable
        style="width: 220px"
      />
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
const searchText = ref('')

const filteredLabs = computed(() =>
  labs.value.filter(lab =>
    !searchText.value || lab.name.includes(searchText.value)
  )
)

function goBook(lab) {
  if (lab.status !== 1) return
  router.push(`/labs/${lab.id}/book`)
}

onMounted(async () => {
  try {
    const res = await labApi.list()
    labs.value = res.data || []
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
  font-size: 1.25rem;
  font-weight: 600;
  color: #303133;
}

.lab-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 1rem;
}

.lab-card {
  background: #fff;
  border-radius: 0.75rem;
  padding: 1.25rem;
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  border: 1px solid transparent;
}

.lab-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(64, 158, 255, 0.15);
  border-color: #409eff33;
}

.lab-card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 0.75rem;
}

.lab-icon {
  width: 2.75rem;
  height: 2.75rem;
  background: #ecf5ff;
  border-radius: 0.5rem;
  display: flex;
  align-items: center;
  justify-content: center;
}

.lab-name {
  font-size: 1.05rem;
  font-weight: 600;
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

@media (max-width: 480px) {
  .lab-grid {
    grid-template-columns: 1fr;
  }
}
</style>
