<template>
  <div class="login-page">
    <div class="background-decoration" aria-hidden="true">
      <span class="glow glow-1"></span>
      <span class="glow glow-2"></span>
      <span class="glow glow-3"></span>
      <span class="grid-mask"></span>
    </div>
    <div class="login-card">
      <div class="login-header">
        <div class="logo">
          <el-icon size="40" color="#409eff"><School /></el-icon>
        </div>
        <h1>实验室预约系统</h1>
        <p class="subtitle">高校实验室在线预约平台</p>
      </div>

      <el-form :model="form" :rules="rules" ref="formRef" label-position="top">
        <el-form-item prop="username">
          <el-input
            v-model="form.username"
            placeholder="请输入学号 / 工号"
            size="large"
            :prefix-icon="User"
          />
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="请输入密码"
            size="large"
            :prefix-icon="Lock"
            show-password
            @keyup.enter="handleLogin"
          />
        </el-form-item>
        <el-button
          type="primary"
          size="large"
          :loading="loading"
          class="login-btn"
          @click="handleLogin"
        >
          登录
        </el-button>
      </el-form>

      <div class="login-footer">
        <span>还没有账号？</span>
        <router-link to="/register">立即注册</router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { authApi } from '@/api'
import { useUserStore } from '@/store/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const formRef = ref()
const loading = ref(false)

const form = reactive({ username: '', password: '' })

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function handleLogin() {
  await formRef.value.validate()
  loading.value = true
  try {
    const res = await authApi.login(form)
    userStore.setLoginData(res.data)
    ElMessage.success(`欢迎回来，${res.data.realName}`)
    const redirect = route.query.redirect || (res.data.role === 1 ? '/admin/dashboard' : '/labs')
    router.push(redirect)
  } catch (error) {
    // request 拦截器已统一提示错误，这里兜底避免未处理 Promise 异常
    if (!error?.message) {
      ElMessage.error('登录失败，请稍后重试')
    }
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
  background:
    linear-gradient(135deg, rgba(7, 15, 35, 0.5) 0%, rgba(14, 31, 64, 0.72) 48%, rgba(6, 12, 28, 0.82) 100%),
    radial-gradient(circle at 20% 20%, rgba(97, 170, 255, 0.24), transparent 28%),
    radial-gradient(circle at 80% 18%, rgba(112, 87, 255, 0.2), transparent 30%),
    radial-gradient(circle at 50% 100%, rgba(31, 221, 255, 0.12), transparent 32%),
    url('https://images.unsplash.com/photo-1541339907198-e08756dedf3f?auto=format&fit=crop&w=1920&q=80')
      center center / cover no-repeat;
  padding: 1rem;
}

.login-page::before {
  content: '';
  position: absolute;
  inset: 0;
  background:
    linear-gradient(115deg, rgba(255, 255, 255, 0.08), transparent 36%),
    radial-gradient(circle at 18% 20%, rgba(255, 255, 255, 0.18), transparent 32%),
    radial-gradient(circle at 82% 18%, rgba(255, 255, 255, 0.14), transparent 30%);
  pointer-events: none;
}

.login-page::after {
  content: '';
  position: absolute;
  inset: 0;
  background:
    linear-gradient(180deg, rgba(10, 18, 36, 0.08) 0%, rgba(10, 18, 36, 0.5) 100%),
    radial-gradient(circle at center, transparent 0%, rgba(6, 12, 24, 0.24) 100%);
  pointer-events: none;
}

.background-decoration {
  position: absolute;
  inset: 0;
  z-index: 1;
  pointer-events: none;
}

.glow {
  position: absolute;
  border-radius: 50%;
  filter: blur(8px);
  opacity: 0.85;
  animation: floatGlow 16s ease-in-out infinite;
}

.glow-1 {
  width: 24rem;
  height: 24rem;
  top: -8rem;
  left: -6rem;
  background: radial-gradient(circle, rgba(118, 196, 255, 0.34) 0%, rgba(118, 196, 255, 0) 72%);
}

.glow-2 {
  width: 22rem;
  height: 22rem;
  right: -5rem;
  top: 12%;
  background: radial-gradient(circle, rgba(150, 108, 255, 0.3) 0%, rgba(150, 108, 255, 0) 72%);
  animation-delay: -5s;
}

.glow-3 {
  width: 26rem;
  height: 26rem;
  bottom: -10rem;
  left: 32%;
  background: radial-gradient(circle, rgba(59, 214, 255, 0.2) 0%, rgba(59, 214, 255, 0) 70%);
  animation-delay: -9s;
}

.grid-mask {
  position: absolute;
  inset: 0;
  opacity: 0.18;
  background-image:
    linear-gradient(rgba(255, 255, 255, 0.1) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.1) 1px, transparent 1px);
  background-size: 52px 52px;
  mask-image: radial-gradient(circle at center, rgba(0, 0, 0, 0.85), transparent 78%);
}

.login-card {
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.9), rgba(255, 255, 255, 0.82));
  backdrop-filter: blur(14px);
  border-radius: 1.25rem;
  padding: 2.5rem 2rem;
  width: 100%;
  max-width: 400px;
  box-shadow:
    0 24px 70px rgba(3, 10, 28, 0.35),
    0 10px 30px rgba(21, 59, 126, 0.12);
  border: 1px solid rgba(255, 255, 255, 0.6);
  position: relative;
  z-index: 2;
}

.login-card::before {
  content: '';
  position: absolute;
  inset: 0;
  border-radius: inherit;
  padding: 1px;
  background: linear-gradient(145deg, rgba(255, 255, 255, 0.8), rgba(255, 255, 255, 0.16));
  -webkit-mask:
    linear-gradient(#fff 0 0) content-box,
    linear-gradient(#fff 0 0);
  -webkit-mask-composite: xor;
  mask-composite: exclude;
  pointer-events: none;
}

.login-header {
  text-align: center;
  margin-bottom: 2rem;
}

.logo {
  margin-bottom: 0.75rem;
}

.login-header h1 {
  font-size: 1.5rem;
  font-weight: 700;
  color: #303133;
  margin-bottom: 0.25rem;
}

.subtitle {
  font-size: 0.875rem;
  color: #909399;
}

.login-btn {
  width: 100%;
  margin-top: 0.5rem;
  height: 2.9rem;
  font-size: 1rem;
  border: none;
  background: linear-gradient(135deg, #356dff 0%, #5b5df0 100%);
  box-shadow: 0 12px 30px rgba(65, 92, 241, 0.32);
}

.login-btn:hover {
  transform: translateY(-1px);
  background: linear-gradient(135deg, #2f63f0 0%, #5355e4 100%);
  box-shadow: 0 16px 34px rgba(65, 92, 241, 0.36);
}

.login-footer {
  text-align: center;
  margin-top: 1.5rem;
  font-size: 0.875rem;
  color: #606266;
}

.login-footer a {
  color: #409eff;
  text-decoration: none;
  margin-left: 0.25rem;
}

@keyframes floatGlow {
  0%,
  100% {
    transform: translate3d(0, 0, 0) scale(1);
  }
  50% {
    transform: translate3d(0, -14px, 0) scale(1.05);
  }
}

@media (max-width: 768px) {
  .grid-mask {
    display: none;
  }

  .login-card {
    padding: 2.2rem 1.4rem;
  }
}
</style>
