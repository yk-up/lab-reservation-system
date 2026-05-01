import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/store/user'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { public: true }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/Register.vue'),
    meta: { public: true }
  },
  // 用户端路由
  {
    path: '/',
    component: () => import('@/views/user/Layout.vue'),
    children: [
      { path: '', redirect: '/labs' },
      {
        path: 'labs',
        name: 'LabList',
        component: () => import('@/views/user/LabList.vue'),
        meta: { title: '实验室列表' }
      },
      {
        path: 'labs/:id/book',
        name: 'BookLab',
        component: () => import('@/views/user/BookLab.vue'),
        meta: { title: '预约实验室' }
      },
      {
        path: 'my-reservations',
        name: 'MyReservations',
        component: () => import('@/views/user/MyReservations.vue'),
        meta: { title: '我的预约', requiresAuth: true }
      },
      {
        path: 'notices',
        name: 'Notices',
        component: () => import('@/views/user/Notices.vue'),
        meta: { title: '消息通知', requiresAuth: true }
      }
    ]
  },
  // 管理端路由
  {
    path: '/admin',
    component: () => import('@/views/admin/Layout.vue'),
    meta: { requiresAuth: true, requiresAdmin: true },
    children: [
      { path: '', redirect: '/admin/dashboard' },
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/admin/Dashboard.vue'),
        meta: { title: '数据看板' }
      },
      {
        path: 'announcements',
        name: 'AdminAnnouncementCenter',
        component: () => import('@/views/admin/AnnouncementCenter.vue'),
        meta: { title: '公告中心' }
      },
      {
        path: 'audit',
        name: 'Audit',
        component: () => import('@/views/admin/Audit.vue'),
        meta: { title: '预约审核' }
      },
      {
        path: 'labs',
        name: 'AdminLabs',
        component: () => import('@/views/admin/LabManage.vue'),
        meta: { title: '实验室管理' }
      },
      {
        path: 'blacklist',
        name: 'Blacklist',
        component: () => import('@/views/admin/Blacklist.vue'),
        meta: { title: '黑名单管理' }
      }
    ]
  },
  { path: '/:pathMatch(.*)*', redirect: '/' }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 })
})

// 路由守卫
router.beforeEach((to, from, next) => {
  const userStore = useUserStore()

  if (to.meta.requiresAuth && !userStore.isLoggedIn) {
    return next({ path: '/login', query: { redirect: to.fullPath } })
  }

  if (to.meta.requiresAdmin && !userStore.isAdmin) {
    return next('/')
  }

  next()
})

export default router
