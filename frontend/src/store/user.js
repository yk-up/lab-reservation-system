import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const userInfo = ref(JSON.parse(localStorage.getItem('userInfo') || 'null'))
  const unreadCount = ref(0)

  const isLoggedIn = computed(() => !!token.value)
  const isAdmin = computed(() => userInfo.value?.role === 1)
  const realName = computed(() => userInfo.value?.realName || '')

  function setLoginData(data) {
    token.value = data.token
    userInfo.value = {
      userId: data.userId,
      username: data.username,
      realName: data.realName,
      role: data.role
    }
    localStorage.setItem('token', data.token)
    localStorage.setItem('userInfo', JSON.stringify(userInfo.value))
  }

  function setUnreadCount(count) {
    unreadCount.value = Math.max(0, Number(count) || 0)
  }

  function logout() {
    token.value = ''
    userInfo.value = null
    unreadCount.value = 0
    localStorage.removeItem('token')
    localStorage.removeItem('userInfo')
  }

  return {
    token,
    userInfo,
    unreadCount,
    isLoggedIn,
    isAdmin,
    realName,
    setLoginData,
    setUnreadCount,
    logout
  }
})
