import { ref } from 'vue'
import http from '../api/http'

const isAuthenticated = ref(false)
const user = ref(null)

// 检查登录状态
async function checkAuth() {
  try {
    const response = await http.get('/api/auth/check')
    if (response.status === 200 && response.data) {
      isAuthenticated.value = true
      user.value = response.data
      return true
    }
    isAuthenticated.value = false
    user.value = null
    return false
  } catch (error) {
    // 如果是401，说明未登录，这是正常情况
    if (error.response && error.response.status === 401) {
      isAuthenticated.value = false
      user.value = null
      return false
    }
    // 其他错误也视为未登录
    isAuthenticated.value = false
    user.value = null
    return false
  }
}

// 登录
function login(userData) {
  isAuthenticated.value = true
  user.value = userData
}

// 退出
function logout() {
  isAuthenticated.value = false
  user.value = null
  http.post('/api/auth/logout').catch(() => {})
}

export default {
  isAuthenticated,
  user,
  checkAuth,
  login,
  logout
}

