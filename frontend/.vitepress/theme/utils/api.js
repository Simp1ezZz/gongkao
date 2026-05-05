// frontend/.vitepress/theme/utils/api.js
import axios from 'axios'

const API_BASE = import.meta.env.VITE_API_BASE_URL || '/api'
const AI_BASE = import.meta.env.VITE_AI_BASE_URL || '/ai'

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

// 试卷相关 API
export const paperApi = {
  list(params) {
    return api.get('/papers', { params })
  },
  getDetail(id) {
    return api.get(`/papers/${id}`)
  },
  getMaterials(paperId) {
    return api.get(`/papers/${paperId}/materials`)
  },
  getQuestionsByKnowledge(params) {
    return api.get('/papers/questions/by-knowledge', { params })
  },
  batchSubmit(data) {
    return api.post('/papers/user-answers/batch', data)
  },
  getMyAnswers(paperId) {
    return api.get(`/papers/${paperId}/my-answers`)
  },
}

// 地区 API
export const regionApi = {
  list() {
    return api.get('/regions')
  },
}

// 做题会话 API
export const sessionApi = {
  create(data) {
    return api.post('/sessions', data)
  },
  list() {
    return api.get('/sessions')
  },
  get(id) {
    return api.get(`/sessions/${id}`)
  },
  update(id, data) {
    return api.put(`/sessions/${id}`, data)
  },
  submit(id, data) {
    return api.post(`/sessions/${id}/submit`, data)
  },
}

export { api, aiApi, API_BASE, AI_BASE }

// Import API
export const importApi = {
  getModels() {
    return aiApi.get('/import/models')
  },
  parse(formData) {
    return aiApi.post('/import/parse', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      timeout: 600000, // 10 min for LLM processing
    })
  },
  reparseQuestion(formData) {
    return aiApi.post('/import/reparse-question', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      timeout: 120000,
    })
  },
  confirmImport(data) {
    return api.post('/admin/papers/import', data)
  },
}
