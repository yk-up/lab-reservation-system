<template>
  <div class="login-page">
    <div class="login-card">
      <div class="login-header">
        <h1>注册账号</h1>
        <p class="subtitle">创建您的实验室预约账号</p>
      </div>

      <el-form :model="form" :rules="rules" ref="formRef" label-position="top">
        <el-form-item label="学号 / 工号" prop="username">
          <el-input v-model="form.username" placeholder="请输入学号或工号" size="large" />
        </el-form-item>
        <el-form-item label="真实姓名" prop="realName">
          <el-input v-model="form.realName" placeholder="请输入真实姓名" size="large" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" placeholder="至少6位密码"
            size="large" show-password />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" placeholder="选填" size="large" />
        </el-form-item>
        <el-button type="primary" size="large" :loading="loading" class="login-btn" @click="handleRegister">
          注册
        </el-button>
      </el-form>

      <div class="login-footer">
        <span>已有账号？</span>
        <router-link to="/login">立即登录</router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { authApi } from '@/api'

const router = useRouter()
const formRef = ref()
const loading = ref(false)

const form = reactive({ username: '', realName: '', password: '', email: '' })

const rules = {
  username: [{ required: true, message: '请输入学号/工号', trigger: 'blur' }],
  realName: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少6位', trigger: 'blur' }
  ]
}

async function handleRegister() {
  await formRef.value.validate()
  loading.value = true
  try {
    await authApi.register(form)
    ElMessage.success('注册成功，请登录')
    router.push('/login')
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
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 1rem;
}
.login-card {
  background: #fff;
  border-radius: 1rem;
  padding: 2.5rem 2rem;
  width: 100%;
  max-width: 400px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.15);
}
.login-header { text-align: center; margin-bottom: 1.5rem; }
.login-header h1 { font-size: 1.5rem; font-weight: 700; color: #303133; }
.subtitle { font-size: 0.875rem; color: #909399; margin-top: 0.25rem; }
.login-btn { width: 100%; margin-top: 0.5rem; height: 2.75rem; font-size: 1rem; }
.login-footer { text-align: center; margin-top: 1.5rem; font-size: 0.875rem; color: #606266; }
.login-footer a { color: #409eff; text-decoration: none; margin-left: 0.25rem; }
</style>
