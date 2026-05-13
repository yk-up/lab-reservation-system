<template>
  <div class="welcome-carousel user-card">
    <el-carousel
      ref="carouselRef"
      :interval="interval"
      height="320px"
      indicator-position="none"
      :arrow="'hover'"
      class="welcome-carousel__track"
      @change="onCarouselChange"
    >
      <el-carousel-item v-for="slide in slides" :key="slide.id">
        <div class="welcome-carousel__slide">
          <img
            class="welcome-carousel__image"
            :src="slide.image"
            :alt="slide.title"
            loading="lazy"
          />
          <div class="welcome-carousel__overlay" />
          <div class="welcome-carousel__content">
            <h3 class="welcome-carousel__title">{{ slide.title }}</h3>
            <p class="welcome-carousel__desc">{{ slide.description }}</p>
            <el-button
              v-if="slide.action"
              type="primary"
              size="large"
              class="welcome-carousel__cta"
              @click="onCta(slide)"
            >
              {{ slide.buttonText }}
            </el-button>
          </div>
        </div>
      </el-carousel-item>
    </el-carousel>
    <div class="welcome-carousel__pager" role="tablist" aria-label="轮播场景切换">
      <button
        v-for="(slide, index) in slides"
        :key="slide.id"
        type="button"
        class="welcome-carousel__pager-btn"
        :class="{ 'is-active': activeIndex === index }"
        :aria-selected="activeIndex === index"
        :aria-label="slide.pagerLabel || slide.title"
        @click="goToSlide(index)"
      >
        {{ slide.pagerLabel || slide.title }}
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { welcomeCarouselSlides, welcomeCarouselIntervalMs } from '@/config/welcomeCarousel'
import { useUserStore } from '@/store/user'

const router = useRouter()
const userStore = useUserStore()

const slides = welcomeCarouselSlides
const interval = welcomeCarouselIntervalMs
const carouselRef = ref()
const activeIndex = ref(0)

function onCarouselChange(index) {
  activeIndex.value = Number(index)
}

function goToSlide(index) {
  activeIndex.value = index
  carouselRef.value?.setActiveItem?.(index)
}

function onCta(slide) {
  const action = slide.action
  if (!action) return

  if (action.type === 'scroll') {
    const el = document.getElementById(action.elementId)
    el?.scrollIntoView({ behavior: 'smooth', block: 'start' })
    return
  }

  if (action.type === 'route') {
    if (action.requiresAuth && !userStore.isLoggedIn) {
      router.push({ path: '/login', query: { redirect: action.path } })
      return
    }
    router.push({ path: action.path, query: action.query })
  }
}
</script>

<style scoped>
.welcome-carousel {
  overflow: hidden;
  border-radius: 12px;
  padding: 0;
  margin-bottom: 1rem;
}
.welcome-carousel__track {
  border-radius: 12px;
  overflow: hidden;
}
.welcome-carousel__track :deep(.el-carousel__container) {
  border-radius: 12px;
}
.welcome-carousel__slide {
  position: relative;
  height: 320px;
  border-radius: 12px;
  overflow: hidden;
}
.welcome-carousel__image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}
.welcome-carousel__overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(
    105deg,
    rgba(15, 23, 42, 0.72) 0%,
    rgba(15, 23, 42, 0.45) 42%,
    rgba(15, 23, 42, 0.2) 100%
  );
  pointer-events: none;
}
.welcome-carousel__content {
  position: absolute;
  left: 0;
  right: 38%;
  bottom: 0;
  top: 0;
  padding: 2rem 2rem 2rem 2.25rem;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: flex-start;
  z-index: 1;
  max-width: 560px;
}
.welcome-carousel__title {
  margin: 0;
  font-size: 1.5rem;
  font-weight: 700;
  color: #fff;
  line-height: 1.35;
  text-shadow: 0 2px 16px rgba(0, 0, 0, 0.35);
}
.welcome-carousel__desc {
  margin: 0.85rem 0 0;
  font-size: 0.95rem;
  line-height: 1.65;
  color: rgba(255, 255, 255, 0.92);
  text-shadow: 0 1px 8px rgba(0, 0, 0, 0.35);
}
.welcome-carousel__cta {
  margin-top: 1.25rem;
  font-weight: 600;
}
.welcome-carousel__pager {
  display: flex;
  justify-content: center;
  align-items: center;
  flex-wrap: wrap;
  gap: 0.5rem;
  padding: 0.75rem 0.5rem 0.25rem;
  background: linear-gradient(180deg, rgba(248, 250, 252, 0.96), #fff);
  border-top: 1px solid #e2e8f0;
}
.welcome-carousel__pager-btn {
  border: 1px solid #cbd5e1;
  background: #fff;
  color: #475569;
  font-size: 0.8rem;
  font-weight: 500;
  padding: 0.45rem 0.9rem;
  border-radius: 999px;
  cursor: pointer;
  transition: color 0.2s ease, background 0.2s ease, border-color 0.2s ease, box-shadow 0.2s ease;
}
.welcome-carousel__pager-btn:hover {
  border-color: #409eff;
  color: #2563eb;
}
.welcome-carousel__pager-btn.is-active {
  background: linear-gradient(135deg, #2563eb, #3b82f6);
  border-color: transparent;
  color: #fff;
  box-shadow: 0 2px 10px rgba(37, 99, 235, 0.35);
}
@media (max-width: 768px) {
  .welcome-carousel__content {
    right: 0;
    max-width: none;
    padding: 1.5rem 1.25rem;
  }
  .welcome-carousel__title {
    font-size: 1.2rem;
  }
  .welcome-carousel__pager-btn {
    flex: 1 1 auto;
    min-width: 5.5rem;
  }
}
</style>
