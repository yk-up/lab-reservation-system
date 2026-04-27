<template>
  <div class="empty-state">
    <div class="icon-shell" :class="`type-${props.type}`">
      <el-icon size="34">
        <component :is="iconName" />
      </el-icon>
    </div>

    <h3 class="empty-title">{{ props.title }}</h3>
    <p v-if="props.description" class="empty-description">{{ props.description }}</p>

    <div v-if="hasActions" class="empty-actions">
      <el-button
        v-if="props.secondaryActionText"
        plain
        @click="$emit('secondary-action')"
      >
        {{ props.secondaryActionText }}
      </el-button>
      <el-button
        v-if="props.actionText"
        type="primary"
        @click="$emit('action')"
      >
        {{ props.actionText }}
      </el-button>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  type: {
    type: String,
    default: 'default'
  },
  title: {
    type: String,
    default: '暂无数据'
  },
  description: {
    type: String,
    default: ''
  },
  actionText: {
    type: String,
    default: ''
  },
  secondaryActionText: {
    type: String,
    default: ''
  }
})

defineEmits(['action', 'secondary-action'])

const iconMap = {
  default: 'Box',
  search: 'Search',
  reservation: 'Calendar',
  audit: 'DocumentChecked',
  notice: 'Bell',
  blacklist: 'CircleCloseFilled'
}

const iconName = computed(() => iconMap[props.type] || iconMap.default)
const hasActions = computed(() => Boolean(props.actionText || props.secondaryActionText))
</script>

<style scoped>
.empty-state {
  margin: 1rem 0 0;
  padding: 2rem 1.5rem;
  border-radius: 16px;
  border: 1px dashed #d9e2ef;
  background: linear-gradient(180deg, #ffffff 0%, #f8fbff 100%);
  text-align: center;
}

.icon-shell {
  width: 72px;
  height: 72px;
  margin: 0 auto 1rem;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #ecf5ff;
  color: #409eff;
}

.type-search {
  background: #eef6ff;
  color: #3b82f6;
}

.type-reservation {
  background: #effaf3;
  color: #67c23a;
}

.type-audit {
  background: #fff7e6;
  color: #e6a23c;
}

.type-notice {
  background: #f4f4ff;
  color: #7c6cff;
}

.type-blacklist {
  background: #fef0f0;
  color: #f56c6c;
}

.empty-title {
  margin: 0;
  font-size: 1.1rem;
  font-weight: 600;
  color: #303133;
}

.empty-description {
  max-width: 540px;
  margin: 0.75rem auto 0;
  line-height: 1.7;
  font-size: 0.9rem;
  color: #606266;
}

.empty-actions {
  margin-top: 1.25rem;
  display: flex;
  justify-content: center;
  gap: 0.75rem;
  flex-wrap: wrap;
}
</style>
