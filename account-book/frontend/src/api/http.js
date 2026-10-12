import axios from 'axios'

const http = axios.create({
  baseURL: '/',
  timeout: 15000,
  withCredentials: true,
})

http.interceptors.response.use(
  (res) => res,
  (err) => {
    // 如果是401未授权，清除本地存储
    if (err.response && err.response.status === 401) {
      localStorage.removeItem('user')
      // 路由跳转由调用方处理，避免循环依赖
    }
    return Promise.reject(err)
  }
)

export default http

