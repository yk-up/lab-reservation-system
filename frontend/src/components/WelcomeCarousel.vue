<template>
  <div class="welcome-carousel user-card">
    <el-carousel
      :interval="interval"
      height="320px"
      indicator-position="outside"
      class="welcome-carousel__track"
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
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { welcomeCarouselSlides, welcomeCarouselIntervalMs } from '@/config/welcomeCarousel'
import { useUserStore } from '@/store/user'

const router = useRouter()
const userStore = useUserStore()

const slides = welcomeCarouselSlides
const interval = welcomeCarouselIntervalMs

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
@media (max-width: 768px) {
  .welcome-carousel__content {
    right: 0;
    max-width: none;
    padding: 1.5rem 1.25rem;
  }
  .welcome-carousel__title {
    font-size: 1.2rem;
  }
}
</style>
