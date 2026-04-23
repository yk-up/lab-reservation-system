<template>
  <div class="login-page">
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
    linear-gradient(180deg, rgba(28, 42, 74, 0.42) 0%, rgba(23, 36, 66, 0.55) 100%),
    url('https://images.unsplash.com/photo-1541339907198-e08756dedf3f?auto=format&fit=crop&w=1920&q=80')
      center center / cover no-repeat;
  padding: 1rem;
}

.login-page::before {
  content: '';
  position: absolute;
  inset: 0;
  background:
    radial-gradient(circle at 18% 20%, rgba(255, 255, 255, 0.24), transparent 40%),
    radial-gradient(circle at 82% 18%, rgba(255, 255, 255, 0.2), transparent 36%);
  pointer-events: none;
}

.login-page::after {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, rgba(10, 18, 36, 0.08) 0%, rgba(10, 18, 36, 0.45) 100%);
  pointer-events: none;
}

.login-card {
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(8px);
  border-radius: 1rem;
  padding: 2.5rem 2rem;
  width: 100%;
  max-width: 400px;
  box-shadow: 0 22px 60px rgba(0, 0, 0, 0.28);
  border: 1px solid rgba(255, 255, 255, 0.55);
  position: relative;
  z-index: 2;
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
  height: 2.75rem;
  font-size: 1rem;
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
</style>
