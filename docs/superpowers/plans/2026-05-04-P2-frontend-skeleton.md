# P2 — 前端骨架 + 首页 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 完成首页完整渲染（Hero + 特性卡片 + 18个快捷导航 + 底部横幅），全局导航/侧边栏配置就绪，所有页面占位路由可用，全局组件（Toast/Modal/Empty/BackToTop）就绪。

**Architecture:** 基于 VitePress 默认主题扩展，通过 theme/index.ts 注册全局组件，config.ts 配置导航和侧边栏。首页使用 custom CSS + Vue 组件实现特性卡片和导航网格。

**Tech Stack:** VitePress 2.0.0-alpha.15, Vue 3, CSS Variables (暗色模式支持)

**Spec:** `docs/superpowers/specs/2026-05-04-bala-gongkao-design.md`

---

## File Structure

```
frontend/
├── .vitepress/
│   ├── config.ts                    # 完整配置
│   └── theme/
│       ├── index.ts                  # 主题入口
│       ├── components/
│       │   ├── BackToTop.vue         # 返回顶部
│       │   ├── Toast.vue             # Toast 提示
│       │   ├── Empty.vue             # 空状态
│       │   └── Modal.vue             # 模态框
│       └── styles/
│           └── custom.css            # 自定义样式
├── pages/
│   ├── index.md                      # 首页
│   ├── 题库/
│   │   └── index.md
│   ├── essay-bank/
│   │   └── index.md
│   ├── practice/
│   │   ├── special/
│   │   │   └── index.md
│   │   ├── online/
│   │   │   └── index.md
│   │   └── history/
│   │       └── index.md
│   ├── shenlun-practice/
│   │   └── index.md
│   ├── idiom/
│   │   └── index.md
│   ├── high-freq-words/
│   │   └── index.md
│   ├── high-freq-idiom/
│   │   └── index.md
│   ├── wrong-questions/
│   │   └── index.md
│   ├── 今日热榜/
│   │   └── index.md
│   ├── checkin/
│   │   └── index.md
│   ├── history-today/
│   │   └── index.md
│   ├── news/
│   │   └── index.md
│   ├── login/
│   │   └── index.md                  # (P1 已创建)
│   ├── essay-review/
│   │   └── index.md
│   ├── ai-recommended/
│   │   └── index.md
│   ├── study-plan/
│   │   └── index.md
│   ├── secrets/
│   │   └── index.md
│   └── must-read/
│       └── index.md
└── public/
    ├── logo1.png
    └── footer-bg.png
```

---

## Task 1: VitePress 完整配置

**Files:**
- Modify: `frontend/.vitepress/config.ts`

- [ ] **Step 1: 更新 config.ts**

```typescript
// frontend/.vitepress/config.ts
import { defineConfig } from 'vitepress'

export default defineConfig({
  title: 'BALA 公考',
  description: '上岸没烦恼',
  lang: 'zh-CN',
  cleanUrls: true,
  srcDir: 'pages',

  head: [
    ['link', { rel: 'icon', href: '/favicon.png' }]
  ],

  themeConfig: {
    logo: '/logo1.png',
    siteTitle: 'BALA 公考',

    nav: [
      { text: '主页', link: '/' },
      { text: '行测', link: '/题库/' },
      { text: '申论', link: '/essay-bank/' },
      { text: '专项练习', link: '/practice/special/' },
      { text: '个人中心', link: '/login/' },
    ],

    sidebar: {
      '/题库/': getSidebar(),
      '/essay-bank/': getSidebar(),
      '/practice/': getSidebar(),
      '/shenlun-practice/': getSidebar(),
      '/idiom/': getSidebar(),
      '/high-freq-words/': getSidebar(),
      '/high-freq-idiom/': getSidebar(),
      '/wrong-questions/': getSidebar(),
      '/今日热榜/': getSidebar(),
      '/checkin/': getSidebar(),
      '/history-today/': getSidebar(),
      '/news/': getSidebar(),
      '/login/': getSidebar(),
      '/essay-review/': getSidebar(),
      '/ai-recommended/': getSidebar(),
      '/study-plan/': getSidebar(),
      '/secrets/': getSidebar(),
      '/must-read/': getSidebar(),
    },

    darkModeSwitchLabel: '切换主题',
    returnToTopLabel: '返回顶部',
    sidebarMenuLabel: '菜单',
    outline: { label: '目录', level: [2, 3] },
    docFooter: { prev: '上一页', next: '下一页' },
    lastUpdated: { text: '更新于' },
    search: { provider: 'local' },
  },
})

function getSidebar() {
  return [
    {
      text: '题库练习',
      items: [
        { text: '💪 专项练习', link: '/practice/special/' },
        { text: '🖊️ 行测题库', link: '/题库/' },
        { text: '✍️ 申论题库', link: '/essay-bank/' },
        { text: '❎ 错题题库', link: '/wrong-questions/' },
      ]
    },
    {
      text: '学习工具',
      items: [
        { text: '🔍 成语查询', link: '/idiom/' },
        { text: '📝 高频词语', link: '/high-freq-words/' },
        { text: '📖 高频成语', link: '/high-freq-idiom/' },
        { text: '📺 每日新闻', link: '/news/' },
        { text: '🔥 实时热榜', link: '/今日热榜/' },
        { text: '📜 今日史事', link: '/history-today/' },
        { text: '📅 每日打卡', link: '/checkin/' },
      ]
    },
    {
      text: '个人中心',
      items: [
        { text: '👤 个人中心', link: '/login/' },
        { text: '📚 资料合集', link: 'https://my.feishu.cn/wiki/UuZWwzr6bivixHkPQ07c6QO7nGe?from=from_copylink' },
        { text: '📊 行测智能分析', link: '/practice/history/' },
        { text: '🤖 申论智能批改', link: '/essay-review/' },
        { text: '❤️ 错题AI补练', link: '/ai-recommended/' },
        { text: '💯 学习计划', link: '/study-plan/' },
      ]
    }
  ]
}
```

- [ ] **Step 2: 提交**

```bash
git add frontend/.vitepress/config.ts
git commit -m "feat(frontend): complete VitePress config with nav, sidebar, and all routes"
```

---

## Task 2: 全局工具组件

**Files:**
- Create: `frontend/.vitepress/theme/components/BackToTop.vue`
- Create: `frontend/.vitepress/theme/components/Toast.vue`
- Create: `frontend/.vitepress/theme/components/Empty.vue`
- Create: `frontend/.vitepress/theme/components/Modal.vue`

- [ ] **Step 1: 创建 BackToTop.vue**

```vue
<!-- frontend/.vitepress/theme/components/BackToTop.vue -->
<template>
  <transition name="fade">
    <button v-show="visible" class="back-to-top" @click="scrollToTop">↑</button>
  </transition>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'

const visible = ref(false)

function onScroll() {
  visible.value = window.scrollY > 200
}

function scrollToTop() {
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

onMounted(() => window.addEventListener('scroll', onScroll))
onUnmounted(() => window.removeEventListener('scroll', onScroll))
</script>

<style scoped>
.back-to-top {
  position: fixed;
  bottom: 40px;
  right: 40px;
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background: var(--vp-c-brand);
  color: #fff;
  font-size: 20px;
  border: none;
  cursor: pointer;
  box-shadow: 0 4px 12px rgba(0,0,0,0.15);
  z-index: 999;
  transition: transform 0.2s;
}
.back-to-top:hover { transform: translateY(-2px); }

.fade-enter-active, .fade-leave-active { transition: opacity 0.3s; }
.fade-enter-from, .fade-leave-to { opacity: 0; }
</style>
```

- [ ] **Step 2: 创建 Toast.vue**

```vue
<!-- frontend/.vitepress/theme/components/Toast.vue -->
<template>
  <transition name="slide-up">
    <div v-if="visible" :class="['toast', type]">{{ message }}</div>
  </transition>
</template>

<script setup>
import { ref } from 'vue'

const visible = ref(false)
const message = ref('')
const type = ref('info')
let timer = null

function show(msg, duration = 3000, t = 'info') {
  message.value = msg
  type.value = t
  visible.value = true
  if (timer) clearTimeout(timer)
  timer = setTimeout(() => { visible.value = false }, duration)
}

defineExpose({ show })
</script>

<style scoped>
.toast {
  position: fixed;
  bottom: 40px;
  left: 50%;
  transform: translateX(-50%);
  padding: 12px 24px;
  border-radius: 8px;
  font-size: 14px;
  z-index: 9999;
  box-shadow: 0 4px 16px rgba(0,0,0,0.12);
  max-width: 400px;
  text-align: center;
}
.toast.info { background: var(--vp-c-brand); color: #fff; }
.toast.success { background: #10b981; color: #fff; }
.toast.error { background: #f56565; color: #fff; }

.slide-up-enter-active, .slide-up-leave-active { transition: all 0.3s; }
.slide-up-enter-from, .slide-up-leave-to { opacity: 0; transform: translate(-50%, 20px); }
</style>
```

- [ ] **Step 3: 创建 Empty.vue**

```vue
<!-- frontend/.vitepress/theme/components/Empty.vue -->
<template>
  <div class="empty-state">
    <div class="empty-icon">{{ icon }}</div>
    <p class="empty-text">{{ text }}</p>
    <a v-if="link" :href="link" class="empty-link">{{ linkText }}</a>
  </div>
</template>

<script setup>
defineProps({
  icon: { type: String, default: '📭' },
  text: { type: String, default: '暂无数据' },
  link: { type: String, default: '' },
  linkText: { type: String, default: '去看看' }
})
</script>

<style scoped>
.empty-state {
  text-align: center;
  padding: 60px 20px;
}
.empty-icon { font-size: 48px; margin-bottom: 16px; }
.empty-text { color: var(--vp-c-text-2); font-size: 15px; margin-bottom: 12px; }
.empty-link {
  color: var(--vp-c-brand);
  font-size: 14px;
  text-decoration: none;
}
.empty-link:hover { text-decoration: underline; }
</style>
```

- [ ] **Step 4: 创建 Modal.vue**

```vue
<!-- frontend/.vitepress/theme/components/Modal.vue -->
<template>
  <teleport to="body">
    <transition name="modal">
      <div v-if="modelValue" class="modal-overlay" @click.self="handleOverlayClick">
        <div class="modal-content" :style="{ maxWidth: width }">
          <div class="modal-header">
            <h3>{{ title }}</h3>
            <button class="modal-close" @click="close">&times;</button>
          </div>
          <div class="modal-body">
            <slot />
          </div>
          <div v-if="$slots.footer" class="modal-footer">
            <slot name="footer" />
          </div>
        </div>
      </div>
    </transition>
  </teleport>
</template>

<script setup>
const props = defineProps({
  modelValue: { type: Boolean, default: false },
  title: { type: String, default: '' },
  width: { type: String, default: '560px' },
  closeOnOverlay: { type: Boolean, default: true }
})

const emit = defineEmits(['update:modelValue'])

function close() {
  emit('update:modelValue', false)
}

function handleOverlayClick() {
  if (props.closeOnOverlay) close()
}
</script>

<style scoped>
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0,0,0,0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 10000;
  padding: 20px;
}

.modal-content {
  background: var(--vp-c-bg);
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(0,0,0,0.2);
  width: 100%;
  max-height: 80vh;
  overflow-y: auto;
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid var(--vp-c-divider);
}

.modal-header h3 {
  margin: 0;
  font-size: 16px;
  color: var(--vp-c-text-1);
}

.modal-close {
  background: none;
  border: none;
  font-size: 24px;
  color: var(--vp-c-text-2);
  cursor: pointer;
  line-height: 1;
}

.modal-body { padding: 20px; }

.modal-footer {
  padding: 12px 20px;
  border-top: 1px solid var(--vp-c-divider);
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.modal-enter-active, .modal-leave-active { transition: opacity 0.25s; }
.modal-enter-from, .modal-leave-to { opacity: 0; }
</style>
```

- [ ] **Step 5: 更新 theme/index.ts 注册所有全局组件**

```typescript
// frontend/.vitepress/theme/index.ts
import DefaultTheme from 'vitepress/theme'
import './styles/custom.css'
import Login from './components/Login.vue'
import BackToTop from './components/BackToTop.vue'
import Toast from './components/Toast.vue'
import Empty from './components/Empty.vue'
import Modal from './components/Modal.vue'

export default {
  ...DefaultTheme,
  enhanceApp({ app }) {
    app.component('Login', Login)
    app.component('BackToTop', BackToTop)
    app.component('Toast', Toast)
    app.component('Empty', Empty)
    app.component('Modal', Modal)
  }
}
```

- [ ] **Step 6: 提交**

```bash
git add frontend/.vitepress/theme/
git commit -m "feat(frontend): add global components (BackToTop, Toast, Empty, Modal) and theme setup"
```

---

## Task 3: 自定义样式

**Files:**
- Create: `frontend/.vitepress/theme/styles/custom.css`

- [ ] **Step 1: 创建 custom.css**

```css
/* frontend/.vitepress/theme/styles/custom.css */

/* ===== 全局基础 ===== */
:root {
  --color-xingce: #3b82f6;
  --color-shenlun: #10b981;
  --color-changshi: #f59e0b;
  --color-gongji: #8b5cf6;
  --color-success: #48bb78;
  --color-warning: #ed8936;
  --color-danger: #f56565;
}

/* ===== 通用卡片 ===== */
.gk-card {
  background: var(--vp-c-bg-soft);
  border: 1px solid var(--vp-c-divider);
  border-radius: 12px;
  padding: 20px;
  transition: transform 0.2s, box-shadow 0.2s;
}

.gk-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(0,0,0,0.1);
}

.gk-card-clickable {
  cursor: pointer;
  text-decoration: none;
  color: inherit;
  display: block;
}

/* ===== 按钮 ===== */
.btn-brand {
  padding: 10px 20px;
  background: var(--vp-c-brand);
  color: #fff;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  cursor: pointer;
  transition: opacity 0.2s;
}

.btn-brand:hover { opacity: 0.9; }

.btn-alt {
  padding: 10px 20px;
  background: transparent;
  color: var(--vp-c-brand);
  border: 1px solid var(--vp-c-brand);
  border-radius: 8px;
  font-size: 14px;
  cursor: pointer;
  transition: background 0.2s;
}

.btn-alt:hover { background: var(--vp-c-brand-dimm); }

.btn-sm {
  padding: 6px 12px;
  font-size: 12px;
  border-radius: 6px;
}

.btn-danger {
  padding: 6px 12px;
  background: var(--color-danger);
  color: #fff;
  border: none;
  border-radius: 6px;
  font-size: 12px;
  cursor: pointer;
}

/* ===== 标签 ===== */
.gk-tag {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
  background: var(--vp-c-bg-soft);
  color: var(--vp-c-text-2);
  border: 1px solid var(--vp-c-divider);
}

/* ===== 加载状态 ===== */
.gk-loading {
  text-align: center;
  padding: 40px;
  color: var(--vp-c-text-2);
}

.gk-loading .spinner {
  display: inline-block;
  width: 24px;
  height: 24px;
  border: 3px solid var(--vp-c-divider);
  border-top-color: var(--vp-c-brand);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
  margin-bottom: 8px;
}

@keyframes spin { to { transform: rotate(360deg); } }

/* ===== 进度条 ===== */
.gk-progress {
  height: 8px;
  background: var(--vp-c-divider);
  border-radius: 4px;
  overflow: hidden;
}

.gk-progress-bar {
  height: 100%;
  border-radius: 4px;
  transition: width 0.3s;
}

.gk-progress-bar.green { background: var(--color-success); }
.gk-progress-bar.orange { background: var(--color-warning); }
.gk-progress-bar.red { background: var(--color-danger); }
.gk-progress-bar.blue { background: var(--color-xingce); }

/* ===== 评分颜色 ===== */
.score-excellent { color: var(--color-success); }
.score-good { color: var(--color-warning); }
.score-improve { color: var(--color-danger); }

/* ===== 搜索框 ===== */
.gk-search {
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

.gk-search:focus {
  outline: none;
  border-color: var(--vp-c-brand);
}

/* ===== Tab 切换 ===== */
.gk-tabs {
  display: flex;
  gap: 0;
  border-bottom: 2px solid var(--vp-c-divider);
  margin-bottom: 20px;
  overflow-x: auto;
}

.gk-tab {
  padding: 10px 16px;
  background: none;
  border: none;
  border-bottom: 2px solid transparent;
  margin-bottom: -2px;
  font-size: 14px;
  color: var(--vp-c-text-2);
  cursor: pointer;
  white-space: nowrap;
  transition: all 0.2s;
}

.gk-tab.active {
  color: var(--vp-c-brand);
  border-bottom-color: var(--vp-c-brand);
  font-weight: 600;
}

/* ===== 响应式网格 ===== */
.gk-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
}

@media (max-width: 768px) {
  .gk-grid {
    grid-template-columns: 1fr;
  }
}
```

- [ ] **Step 2: 提交**

```bash
git add frontend/.vitepress/theme/styles/custom.css
git commit -m "feat(frontend): add global custom CSS with card, button, tag, loading styles"
```

---

## Task 4: 首页 Hero + 特性卡片 + 快捷导航

**Files:**
- Modify: `frontend/pages/index.md`

- [ ] **Step 1: 创建完整首页**

```markdown
---
layout: home
hero:
  name: "BALA 公考"
  text: "上岸没烦恼"
  tagline: "不负每一次努力，只为助你稳稳上岸、一战成公。"
  image:
    src: /logo1.png
    alt: BALA 公考
  actions:
    - theme: brand
      text: 个人中心
      link: /login/
    - theme: alt
      text: 进站必读
      link: /must-read/
    - theme: alt
      text: 上岸秘籍
      link: /secrets/
---

<HomeFeatures />
<QuickNav />
<FooterBanner />
```

- [ ] **Step 2: 创建 HomeFeatures.vue**

```vue
<!-- frontend/.vitepress/theme/components/HomeFeatures.vue -->
<template>
  <div class="home-features">
    <div class="container">
      <div class="features-grid">
        <a href="/practice/history/" class="feature-card gk-card gk-card-clickable">
          <div class="feature-icon">📊</div>
          <h3>行测智能分析</h3>
          <p>AI智能诊断学习情况，分析各模块正确率，提供个性化提升建议，助您精准突破薄弱环节。</p>
        </a>
        <a href="https://my.feishu.cn/wiki/UuZWwzr6bivixHkPQ07c6QO7nGe?from=from_copylink"
           target="_blank" class="feature-card gk-card gk-card-clickable">
          <div class="feature-icon">📚</div>
          <h3>资料合集</h3>
          <p>汇集公考学习资料，包含行测、申论、面试等各类资源，助力高效备考，一站式学习。</p>
        </a>
        <a href="/essay-review/" class="feature-card gk-card gk-card-clickable">
          <div class="feature-icon">🤖</div>
          <h3>申论智能批改</h3>
          <p>AI智能批改申论作文，提供专业点评与改进建议，帮助您快速提升申论写作水平。</p>
        </a>
      </div>
    </div>
  </div>
</template>

<style scoped>
.home-features { padding: 40px 24px; }
.container { max-width: 1152px; margin: 0 auto; }

.features-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
}

@media (max-width: 768px) {
  .features-grid { grid-template-columns: 1fr; }
}

.feature-icon { font-size: 32px; margin-bottom: 12px; }

.feature-card h3 {
  font-size: 18px;
  margin: 0 0 8px;
  color: var(--vp-c-text-1);
}

.feature-card p {
  font-size: 14px;
  color: var(--vp-c-text-2);
  margin: 0;
  line-height: 1.6;
}
</style>
```

- [ ] **Step 3: 创建 QuickNav.vue**

```vue
<!-- frontend/.vitepress/theme/components/QuickNav.vue -->
<template>
  <div class="quick-nav">
    <div class="container">
      <h2 class="section-title">📌 快捷导航</h2>
      <div class="nav-grid">
        <a v-for="item in navItems" :key="item.title"
           :href="item.link" :target="item.external ? '_blank' : undefined"
           class="nav-card gk-card gk-card-clickable">
          <div class="nav-icon">{{ item.icon }}</div>
          <div class="nav-info">
            <div class="nav-title">{{ item.title }}</div>
            <div class="nav-desc">{{ item.desc }}</div>
          </div>
          <div class="nav-tags">
            <span v-for="tag in item.tags" :key="tag" class="gk-tag">{{ tag }}</span>
          </div>
        </a>
      </div>
    </div>
  </div>
</template>

<script setup>
const navItems = [
  { icon: '💪', title: '专项练习', link: '/practice/special/', desc: '分模块刷题，哪里不会练哪里', tags: ['题库', '专项'] },
  { icon: '🖊️', title: '行测题库', link: '/题库/', desc: '收录近6年真题，支持在线练习', tags: ['题库', '试卷'] },
  { icon: '✍️', title: '申论题库', link: '/essay-bank/', desc: '规范作答训练，支持智能批改', tags: ['申论', '题库'] },
  { icon: '📝', title: '高频词语', link: '/high-freq-words/', desc: '申论常用词汇，助力申论写作', tags: ['词汇', '短语'] },
  { icon: '📖', title: '高频成语', link: '/high-freq-idiom/', desc: '常考成语解析，助力言语理解', tags: ['成语', '解析'] },
  { icon: '❎', title: '错题题库', link: '/wrong-questions/', desc: '自动归集错题，针对性复习', tags: ['错题', '复习'] },
  { icon: '🔍', title: '成语查询', link: '/idiom/', desc: '收录近3万四字成语，用法清晰易懂', tags: ['成语', '词语'] },
  { icon: '🔥', title: '实时热榜', link: '/今日热榜/', desc: '实时热门话题，紧跟考试动态', tags: ['热点', '推荐'] },
  { icon: '📅', title: '每日打卡', link: '/checkin/', desc: '记录学习进度，养成良好习惯', tags: ['打卡', '记录'] },
  { icon: '📜', title: '今日史事', link: '/history-today/', desc: '当天重要历史事件，增长知识储备', tags: ['历史', '知识'] },
  { icon: '👤', title: '个人中心', link: '/login/', desc: '个人信息管理，学习数据统计', tags: ['个人', '设置'] },
  { icon: '📺', title: '每日新闻', link: '/news/', desc: '关注时事热点，把握政策走向', tags: ['新闻', '时政'] },
  { icon: '📊', title: '行测智能分析', link: '/practice/history/', desc: 'AI智能诊断学习情况，提供个性化提升建议', tags: ['AI', '分析'] },
  { icon: '🤖', title: '申论智能批改', link: '/essay-review/', desc: 'AI逐句批改，对标高分范文', tags: ['AI', '批改'] },
  { icon: '📚', title: '资料合集', link: 'https://my.feishu.cn/wiki/UuZWwzr6bivixHkPQ07c6QO7nGe?from=from_copylink', external: true, desc: '汇集公考学习资料，助力高效备考', tags: ['资料', '文档'] },
  { icon: '❤️', title: '错题AI补练', link: '/ai-recommended/', desc: '基于错题智能分析，AI生成类似题目补练', tags: ['AI', '推荐'] },
  { icon: '💯', title: '学习计划', link: '/study-plan/', desc: 'AI分析弱项，智能生成个性化学习计划', tags: ['AI', '计划'] },
  { icon: '🎁', title: '上岸秘籍', link: '/secrets/', desc: '行测申论技巧合集，助你成功上岸', tags: ['技巧', '秘籍'] },
]
</script>

<style scoped>
.quick-nav { padding: 40px 24px 60px; }
.container { max-width: 1152px; margin: 0 auto; }

.section-title {
  font-size: 24px;
  text-align: center;
  margin-bottom: 32px;
  color: var(--vp-c-text-1);
}

.nav-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 16px;
}

.nav-card {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 16px;
}

.nav-icon { font-size: 28px; }

.nav-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--vp-c-text-1);
}

.nav-desc {
  font-size: 13px;
  color: var(--vp-c-text-2);
  line-height: 1.5;
}

.nav-tags {
  display: flex;
  gap: 6px;
  margin-top: 4px;
}

@media (max-width: 768px) {
  .nav-grid { grid-template-columns: 1fr 1fr; }
}
@media (max-width: 480px) {
  .nav-grid { grid-template-columns: 1fr; }
}
</style>
```

- [ ] **Step 4: 创建 FooterBanner.vue**

```vue
<!-- frontend/.vitepress/theme/components/FooterBanner.vue -->
<template>
  <div class="footer-banner">
    <img src="/footer-bg.png" alt="BALA公考" class="footer-img" />
  </div>
</template>

<style scoped>
.footer-banner {
  width: 100%;
  text-align: center;
  padding: 20px;
}
.footer-img {
  max-width: 100%;
  height: auto;
  border-radius: 12px;
}
</style>
```

- [ ] **Step 5: 注册新组件到 theme/index.ts**

更新 `frontend/.vitepress/theme/index.ts`：

```typescript
// frontend/.vitepress/theme/index.ts
import DefaultTheme from 'vitepress/theme'
import './styles/custom.css'
import Login from './components/Login.vue'
import BackToTop from './components/BackToTop.vue'
import Toast from './components/Toast.vue'
import Empty from './components/Empty.vue'
import Modal from './components/Modal.vue'
import HomeFeatures from './components/HomeFeatures.vue'
import QuickNav from './components/QuickNav.vue'
import FooterBanner from './components/FooterBanner.vue'

export default {
  ...DefaultTheme,
  enhanceApp({ app }) {
    app.component('Login', Login)
    app.component('BackToTop', BackToTop)
    app.component('Toast', Toast)
    app.component('Empty', Empty)
    app.component('Modal', Modal)
    app.component('HomeFeatures', HomeFeatures)
    app.component('QuickNav', QuickNav)
    app.component('FooterBanner', FooterBanner)
  }
}
```

- [ ] **Step 6: 准备占位图片**

```bash
# 创建占位 logo 和 footer 图片（开发阶段用纯色占位）
# 实际图片从原站下载后替换
touch frontend/public/logo1.png
touch frontend/public/footer-bg.png
touch frontend/public/favicon.png
```

- [ ] **Step 7: 提交**

```bash
git add frontend/
git commit -m "feat(frontend): add homepage with Hero, features, 18 quick nav cards, and footer"
```

---

## Task 5: 所有页面占位路由

**Files:**
- Create: 20 个 `.md` 占位页面文件

- [ ] **Step 1: 创建所有占位页面**

```bash
# 创建所有目录
mkdir -p "frontend/pages/题库"
mkdir -p frontend/pages/essay-bank
mkdir -p frontend/pages/practice/special
mkdir -p frontend/pages/practice/online
mkdir -p frontend/pages/practice/history
mkdir -p frontend/pages/shenlun-practice
mkdir -p frontend/pages/idiom
mkdir -p frontend/pages/high-freq-words
mkdir -p frontend/pages/high-freq-idiom
mkdir -p frontend/pages/wrong-questions
mkdir -p "frontend/pages/今日热榜"
mkdir -p frontend/pages/checkin
mkdir -p frontend/pages/history-today
mkdir -p frontend/pages/news
mkdir -p frontend/pages/essay-review
mkdir -p frontend/pages/ai-recommended
mkdir -p frontend/pages/study-plan
mkdir -p frontend/pages/secrets
mkdir -p frontend/pages/must-read
```

每个目录下的 `index.md` 使用统一模板：

```markdown
---
layout: page
---

<div class="placeholder-page">
  <div class="placeholder-icon">🚧</div>
  <h1>PAGE_TITLE</h1>
  <p>该功能正在开发中，敬请期待...</p>
</div>

<style scoped>
.placeholder-page {
  text-align: center;
  padding: 80px 20px;
}
.placeholder-icon { font-size: 48px; margin-bottom: 16px; }
.placeholder-page h1 { font-size: 24px; margin-bottom: 12px; }
.placeholder-page p { color: var(--vp-c-text-2); }
</style>
```

各页面标题列表：

| 文件路径 | PAGE_TITLE |
|---------|-----------|
| `pages/题库/index.md` | 🖊️ 行测题库 |
| `pages/essay-bank/index.md` | ✍️ 申论题库 |
| `pages/practice/special/index.md` | 💪 专项练习 |
| `pages/practice/online/index.md` | 📝 在线练习 |
| `pages/practice/history/index.md` | 📊 行测智能分析 |
| `pages/shenlun-practice/index.md` | ✍️ 申论练习 |
| `pages/idiom/index.md` | 🔍 成语查询 |
| `pages/high-freq-words/index.md` | 📝 高频词语 |
| `pages/high-freq-idiom/index.md` | 📖 高频成语 |
| `pages/wrong-questions/index.md` | ❎ 错题题库 |
| `pages/今日热榜/index.md` | 🔥 实时热榜 |
| `pages/checkin/index.md` | 📅 每日打卡 |
| `pages/history-today/index.md` | 📜 今日史事 |
| `pages/news/index.md` | 📺 每日新闻 |
| `pages/essay-review/index.md` | 🤖 申论智能批改 |
| `pages/ai-recommended/index.md` | ❤️ 错题AI补练 |
| `pages/study-plan/index.md` | 💯 学习计划 |
| `pages/secrets/index.md` | 🎁 上岸秘籍 |
| `pages/must-read/index.md` | 📋 进站必读 |

`pages/login/index.md` 已在 P1 创建，内容为 `<Login />`。

- [ ] **Step 2: 提交**

```bash
git add frontend/pages/
git commit -m "feat(frontend): add placeholder pages for all 21 routes"
```

---

## Task 6: 验证

**Files:** 无新增

- [ ] **Step 1: 启动前端开发服务器**

```bash
cd D:/CODE/gongkao/frontend
npm run dev
```

- [ ] **Step 2: 验证首页**

浏览器打开 http://localhost:5173/ ，确认:
- Hero 区域显示 "BALA 公考" + "上岸没烦恼"
- 三个特性卡片水平排列，可点击
- 18 个快捷导航卡片网格显示
- 点击卡片跳转正确

- [ ] **Step 3: 验证导航和侧边栏**

- 顶部导航: 主页、行测、申论、专项练习、个人中心均可点击
- 进入任意内页，侧边栏三组显示正确
- 资料合集点击后新窗口打开飞书链接

- [ ] **Step 4: 验证暗色模式**

点击右上角主题切换按钮:
- 切换到暗色模式 → 背景变深，文字变浅
- 切换回亮色模式 → 恢复正常

- [ ] **Step 5: 验证移动端响应式**

浏览器宽度缩小到 375px:
- 导航折叠为汉堡菜单
- 快捷导航网格变为单列
- 特性卡片变为单列

- [ ] **Step 6: 验证所有路由**

逐个点击侧边栏和导航的所有链接，确认每个页面都能正常显示（"开发中"占位内容）。

---

## 验收 Checklist

- [ ] 首页 Hero 区域完整渲染
- [ ] 3 个特性卡片水平排列，可点击跳转
- [ ] 18 个快捷导航卡片网格显示，emoji 图标正确
- [ ] 所有 21 个路由可访问（无 404）
- [ ] 顶部导航 5 个链接正常跳转
- [ ] 侧边栏三组（题库练习、学习工具、个人中心）完整
- [ ] 暗色模式切换正常
- [ ] 移动端响应式正常（汉堡菜单、单列布局）
- [ ] 返回顶部按钮滚动后出现
- [ ] BackToTop、Toast、Empty、Modal 组件已注册
