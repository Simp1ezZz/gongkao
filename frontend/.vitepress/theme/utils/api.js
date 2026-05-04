// frontend/.vitepress/theme/utils/api.js
import axios from 'axios'

const API_BASE = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api'
const AI_BASE = import.meta.env.VITE_AI_BASE_URL || 'http://localhost:8000/ai'

const api = axios.create({
  baseURL: API_BASE,
  timeout: 30000,
  headers: { 'Content-Type': 'application/json' }
})

const aiApi = axios.create({
  baseURL: AI_BASE,
  timeout: 120000,
  headers: { 'Content-Type': 'application/json' }
})

// 请求拦截器：附加 Token
function addTokenInterceptor(instance) {
  instance.interceptors.request.use(config => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  })
}

// 响应拦截器：Token 过期自动刷新
function addRefreshInterceptor(instance) {
  instance.interceptors.response.use(
    response => {
      const data = response.data
      if (data.success === false) {
        return Promise.reject(new Error(data.message || '请求失败'))
      }
      return data
    },
    async error => {
      const originalRequest = error.config

      if (error.response?.status === 401 && !originalRequest._retry) {
        originalRequest._retry = true
        const refreshToken = localStorage.getItem('refreshToken')

        if (refreshToken) {
          try {
            const res = await axios.post(`${API_BASE}/auth/refresh`, { refreshToken })
            if (res.data.success) {
              const { accessToken, refreshToken: newRefresh } = res.data.data
              localStorage.setItem('token', accessToken)
              localStorage.setItem('refreshToken', newRefresh)
              originalRequest.headers.Authorization = `Bearer ${accessToken}`
              return instance(originalRequest)
            }
          } catch {
            // refresh 失败，清除登录状态
          }
        }

        localStorage.removeItem('token')
        localStorage.removeItem('refreshToken')
        localStorage.removeItem('user')
        window.location.href = '/login/'
      }

      return Promise.reject(new Error(error.response?.data?.message || error.message))
    }
  )
}

addTokenInterceptor(api)
addTokenInterceptor(aiApi)
addRefreshInterceptor(api)

// 用户状态管理
export function getUser() {
  const userStr = localStorage.getItem('user')
  return userStr ? JSON.parse(userStr) : null
}

export function isLoggedIn() {
  return !!localStorage.getItem('token')
}

export function logout() {
  const token = localStorage.getItem('token')
  if (token) {
    api.post('/auth/logout').catch(() => {})
  }
  localStorage.removeItem('token')
  localStorage.removeItem('refreshToken')
  localStorage.removeItem('user')
}

export { api, aiApi, API_BASE, AI_BASE }
