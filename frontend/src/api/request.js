import axios from 'axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/user'
import router from '@/router'

const request = axios.create({
  baseURL: '/api',
  timeout: 10000
})

// 请求拦截器：自动附加 JWT Token
request.interceptors.request.use(
  config => {
    const userStore = useUserStore()
    if (userStore.token) {
      config.headers['Authorization'] = `Bearer ${userStore.token}`
    }
    return config
  },
  error => Promise.reject(error)
)

// 响应拦截器：统一处理错误
request.interceptors.response.use(
  response => {
    const res = response.data
    if (res.code === 200) {
      return res
    }
    if (res.code === 401) {
      const userStore = useUserStore()
      userStore.logout()
      router.push('/login')
      ElMessage.error('登录已过期，请重新登录')
      return Promise.reject(new Error(res.message))
    }
    if (res.code === 403) {
      ElMessage.error(res.message || '权限不足')
      return Promise.reject(Object.assign(new Error(res.message || '权限不足'), { code: res.code }))
    }
    if (res.code >= 40000 && res.code < 50000) {
      ElMessage.warning(res.message || '请求参数或业务状态错误')
      return Promise.reject(Object.assign(new Error(res.message || '请求参数或业务状态错误'), { code: res.code }))
    }
    ElMessage.error(res.message || '请求失败')
    return Promise.reject(Object.assign(new Error(res.message || '请求失败'), { code: res.code }))
  },
  error => {
    if (error.response?.status === 401) {
      const userStore = useUserStore()
      userStore.logout()
      router.push('/login')
    }
    ElMessage.error(error.response?.data?.message || '网络请求失败')
    return Promise.reject(error)
  }
)

export default request
