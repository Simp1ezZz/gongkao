<!-- frontend/.vitepress/theme/components/Login.vue -->
<template>
  <div class="login-container">
    <div class="login-card">
      <!-- 已登录状态 -->
      <template v-if="loggedIn">
        <div class="user-info">
          <div class="user-avatar">{{ userInfo?.nickname?.charAt(0) || 'U' }}</div>
          <div class="user-detail">
            <p class="user-name">{{ userInfo?.nickname }}</p>
            <p class="user-email">{{ userInfo?.email }}</p>
          </div>
        </div>
        <button class="btn-brand btn-logout" @click="handleLogout">退出登录</button>
      </template>

      <!-- 未登录状态 -->
      <template v-else>
      <!-- Tab 切换 -->
      <div class="login-tabs">
        <button
          v-for="tab in visibleTabs"
          :key="tab.key"
          :class="['login-tab', { active: currentTab === tab.key }]"
          @click="currentTab = tab.key"
        >{{ tab.label }}</button>
      </div>

      <!-- 登录表单 -->
      <form v-if="currentTab === 'login'" @submit.prevent="handleLogin">
        <div class="form-group">
          <label>邮箱</label>
          <input v-model="loginForm.email" type="email" placeholder="请输入邮箱" required />
        </div>
        <div class="form-group">
          <label>密码</label>
          <input v-model="loginForm.password" type="password" placeholder="请输入密码" required />
        </div>
        <button type="submit" class="btn-brand" :disabled="loading">
          {{ loading ? '登录中...' : '登录' }}
        </button>
        <p class="form-link" @click="currentTab = 'forgot'">忘记密码？</p>
      </form>

      <!-- 注册表单 -->
      <form v-else-if="currentTab === 'register'" @submit.prevent="handleRegister">
        <div class="form-group">
          <label>邮箱</label>
          <input v-model="registerForm.email" type="email" placeholder="请输入邮箱" required />
        </div>
        <div class="form-group">
          <label>密码</label>
          <input v-model="registerForm.password" type="password" placeholder="6-20位密码" required minlength="6" />
        </div>
        <div class="form-group code-group">
          <label>验证码</label>
          <div class="code-row">
            <input v-model="registerForm.code" placeholder="验证码" required />
            <button type="button" class="btn-code" @click="handleSendCode('register')" :disabled="codeCooldown > 0">
              {{ codeCooldown > 0 ? codeCooldown + 's' : '发送验证码' }}
            </button>
          </div>
        </div>
        <button type="submit" class="btn-brand" :disabled="loading">
          {{ loading ? '注册中...' : '注册' }}
        </button>
      </form>

      <!-- 忘记密码 -->
      <form v-else @submit.prevent="handleResetPassword">
        <div class="form-group">
          <label>邮箱</label>
          <input v-model="resetForm.email" type="email" placeholder="请输入注册邮箱" required />
        </div>
        <div class="form-group code-group">
          <label>验证码</label>
          <div class="code-row">
            <input v-model="resetForm.code" placeholder="验证码" required />
            <button type="button" class="btn-code" @click="handleSendCode('reset_password')" :disabled="codeCooldown > 0">
              {{ codeCooldown > 0 ? codeCooldown + 's' : '发送验证码' }}
            </button>
          </div>
        </div>
        <div class="form-group">
          <label>新密码</label>
          <input v-model="resetForm.newPassword" type="password" placeholder="6-20位新密码" required minlength="6" />
        </div>
        <button type="submit" class="btn-brand" :disabled="loading">
          {{ loading ? '提交中...' : '重置密码' }}
        </button>
        <p class="form-link" @click="currentTab = 'login'">返回登录</p>
      </form>
      </template>

      <p v-if="message" :class="['login-message', messageType]">{{ message }}</p>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { api, getUser, isLoggedIn, logout } from '../utils/api.js'

const tabs = [
  { key: 'login', label: '登录' },
  { key: 'register', label: '注册' },
  { key: 'forgot', label: '忘记密码' }
]

// Tab 栏只显示登录和注册，忘记密码通过链接跳转
const visibleTabs = computed(() => tabs.filter(t => t.key !== 'forgot'))

const currentTab = ref('login')
const loading = ref(false)
const message = ref('')
const messageType = ref('error')
const codeCooldown = ref(0)

const loginForm = reactive({ email: '', password: '' })
const registerForm = reactive({ email: '', password: '', code: '' })
const resetForm = reactive({ email: '', code: '', newPassword: '' })

const loggedIn = ref(false)
const userInfo = ref(null)

onMounted(() => {
  if (isLoggedIn()) {
    loggedIn.value = true
    userInfo.value = getUser()
  }
})

function showMessage(text, type = 'error') {
  message.value = text
  messageType.value = type
  setTimeout(() => { message.value = '' }, 3000)
}

async function handleLogin() {
  loading.value = true
  try {
    const res = await api.post('/auth/login-password', loginForm)
    const data = res.data
    localStorage.setItem('token', data.accessToken)
    localStorage.setItem('refreshToken', data.refreshToken)
    localStorage.setItem('user', JSON.stringify(data))
    loggedIn.value = true
    userInfo.value = data
    showMessage('登录成功！', 'success')
  } catch (e) {
    showMessage(e.message || '登录失败')
  } finally {
    loading.value = false
  }
}

async function handleRegister() {
  loading.value = true
  try {
    const res = await api.post('/auth/register', registerForm)
    const data = res.data
    localStorage.setItem('token', data.accessToken)
    localStorage.setItem('refreshToken', data.refreshToken)
    localStorage.setItem('user', JSON.stringify(data))
    loggedIn.value = true
    userInfo.value = data
    showMessage('注册成功！', 'success')
  } catch (e) {
    showMessage(e.message || '注册失败')
  } finally {
    loading.value = false
  }
}

async function handleResetPassword() {
  loading.value = true
  try {
    await api.post('/auth/reset-password', resetForm)
    showMessage('密码重置成功，请登录', 'success')
    currentTab.value = 'login'
    loginForm.email = resetForm.email
  } catch (e) {
    showMessage(e.message || '重置失败')
  } finally {
    loading.value = false
  }
}

async function handleSendCode(type) {
  const email = type === 'register' ? registerForm.email : resetForm.email
  if (!email) { showMessage('请先输入邮箱'); return }
  try {
    await api.post('/auth/send-code', { email, type })
    showMessage('验证码已发送', 'success')
    codeCooldown.value = 60
    const timer = setInterval(() => {
      codeCooldown.value--
      if (codeCooldown.value <= 0) clearInterval(timer)
    }, 1000)
  } catch (e) {
    showMessage(e.message || '发送失败')
  }
}

function handleLogout() {
  logout()
  loggedIn.value = false
  userInfo.value = null
  loginForm.email = ''
  loginForm.password = ''
}
</script>

<style scoped>
.login-container {
  max-width: 440px;
  margin: 40px auto;
  padding: 0 20px;
}

.login-card {
  background: var(--vp-c-bg);
  border: 1px solid var(--vp-c-divider);
  border-radius: 12px;
  padding: 32px;
  box-shadow: 0 4px 24px rgba(0,0,0,0.08);
}

.user-info {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 24px;
}

.user-avatar {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background: var(--vp-c-brand);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  font-weight: 600;
}

.user-detail {
  flex: 1;
}

.user-name {
  font-size: 16px;
  font-weight: 600;
  color: var(--vp-c-text-1);
  margin: 0;
}

.user-email {
  font-size: 13px;
  color: var(--vp-c-text-2);
  margin: 4px 0 0;
}

.btn-logout {
  background: var(--vp-c-bg-soft);
  color: var(--vp-c-text-2);
  border: 1px solid var(--vp-c-divider);
}

.btn-logout:hover {
  color: #f56565;
  border-color: #f56565;
}

.login-tabs {
  display: flex;
  gap: 0;
  margin-bottom: 24px;
  border-bottom: 2px solid var(--vp-c-divider);
}

.login-tab {
  flex: 1;
  padding: 12px;
  background: none;
  border: none;
  font-size: 15px;
  color: var(--vp-c-text-2);
  cursor: pointer;
  border-bottom: 2px solid transparent;
  margin-bottom: -2px;
  transition: all 0.2s;
}

.login-tab.active {
  color: var(--vp-c-brand);
  border-bottom-color: var(--vp-c-brand);
  font-weight: 600;
}

.form-group {
  margin-bottom: 16px;
}

.form-group label {
  display: block;
  margin-bottom: 6px;
  font-size: 14px;
  color: var(--vp-c-text-1);
  font-weight: 500;
}

.form-group input {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid var(--vp-c-divider);
  border-radius: 8px;
  font-size: 14px;
  background: var(--vp-c-bg-soft);
  color: var(--vp-c-text-1);
  box-sizing: border-box;
  transition: border-color 0.2s;
}

.form-group input:focus {
  outline: none;
  border-color: var(--vp-c-brand);
}

.code-row {
  display: flex;
  gap: 8px;
}

.code-row input {
  flex: 1;
}

.btn-code {
  white-space: nowrap;
  padding: 10px 16px;
  background: var(--vp-c-bg-soft);
  border: 1px solid var(--vp-c-divider);
  border-radius: 8px;
  font-size: 13px;
  color: var(--vp-c-brand);
  cursor: pointer;
  transition: all 0.2s;
}

.btn-code:disabled {
  color: var(--vp-c-text-3);
  cursor: not-allowed;
}

.btn-brand {
  width: 100%;
  padding: 12px;
  background: var(--vp-c-brand);
  color: #fff;
  border: none;
  border-radius: 8px;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: opacity 0.2s;
  margin-top: 8px;
}

.btn-brand:hover { opacity: 0.9; }
.btn-brand:disabled { opacity: 0.6; cursor: not-allowed; }

.form-link {
  text-align: center;
  margin-top: 16px;
  font-size: 13px;
  color: var(--vp-c-brand);
  cursor: pointer;
}

.login-message {
  text-align: center;
  margin-top: 12px;
  font-size: 14px;
  padding: 8px;
  border-radius: 6px;
}

.login-message.success {
  color: #10b981;
  background: rgba(16, 185, 129, 0.1);
}

.login-message.error {
  color: #f56565;
  background: rgba(245, 101, 101, 0.1);
}
</style>
