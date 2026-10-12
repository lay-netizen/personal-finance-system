import { createRouter, createWebHistory } from 'vue-router'
import Dashboard from '../pages/Dashboard.vue'
import Transactions from '../pages/Transactions.vue'
import Analysis from '../pages/Analysis.vue'
import Accounts from '../pages/Accounts.vue'
import Categories from '../pages/Categories.vue'
import Login from '../pages/Login.vue'
import Register from '../pages/Register.vue'
import Profile from '../pages/Profile.vue'
import authStore from '../store/auth.js'

const routes = [
  { path: '/', name: 'dashboard', component: Dashboard, meta: { requiresAuth: true } },
  { path: '/transactions', name: 'transactions', component: Transactions, meta: { requiresAuth: true } },
  { path: '/analysis', name: 'analysis', component: Analysis, meta: { requiresAuth: true } },
  { path: '/accounts', name: 'accounts', component: Accounts, meta: { requiresAuth: true } },
  { path: '/categories', name: 'categories', component: Categories, meta: { requiresAuth: true } },
  { path: '/profile', name: 'profile', component: Profile, meta: { requiresAuth: true } },
  { path: '/login', name: 'login', component: Login, meta: { requiresAuth: false } },
  { path: '/register', name: 'register', component: Register, meta: { requiresAuth: false } },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

// 路由守卫：检查登录状态
router.beforeEach(async (to, from, next) => {
  // 如果需要认证
  if (to.meta.requiresAuth) {
    // 先检查本地存储是否有用户信息
    const localUser = localStorage.getItem('user')
    if (localUser) {
      try {
        // 验证会话是否有效
        const authenticated = await authStore.checkAuth()
        if (authenticated) {
          next()
        } else {
          // 会话失效，清除本地存储
          localStorage.removeItem('user')
          next({ name: 'login', query: { redirect: to.fullPath } })
        }
      } catch (error) {
        localStorage.removeItem('user')
        next({ name: 'login', query: { redirect: to.fullPath } })
      }
    } else {
      // 未登录，跳转到登录页，保存目标路径
      next({ name: 'login', query: { redirect: to.fullPath } })
    }
  } else {
    // 登录/注册页面，如果已登录则跳转到首页
    if (to.name === 'login' || to.name === 'register') {
      const localUser = localStorage.getItem('user')
      if (localUser) {
        try {
          const authenticated = await authStore.checkAuth()
          if (authenticated) {
            next({ name: 'dashboard' })
            return
          }
        } catch (error) {
          localStorage.removeItem('user')
        }
      }
    }
    next()
  }
})

export default router

