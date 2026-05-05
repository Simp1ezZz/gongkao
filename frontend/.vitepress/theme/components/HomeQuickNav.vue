<template>
  <div class="quick-nav">
    <h2 class="section-title">📌 快捷导航</h2>
    <div class="nav-grid">
      <a v-for="item in navItems" :key="item.link" :href="item.link" class="nav-card">
        <div class="nav-icon">{{ item.emoji }}</div>
        <div class="nav-body">
          <h3>{{ item.title }}</h3>
          <p>{{ item.desc }}</p>
          <div class="nav-tags">
            <span v-for="tag in item.tags" :key="tag" class="nav-tag">{{ tag }}</span>
          </div>
        </div>
      </a>
    </div>
    <div class="home-footer">
      <p> BALA 公考 — 不负每一次努力，只为助你稳稳上岸、一战成公。</p>
    </div>
  </div>
</template>

<script setup>
const navItems = [
  {
    emoji: '💪', title: '专项练习', desc: '分模块刷题，哪里不会练哪里',
    tags: ['题库', '专项', '免费'], link: '/practice/special/'
  },
  {
    emoji: '🖊️', title: '行测题库', desc: '收录近6年真题，支持在线练习',
    tags: ['题库', '试卷', '免费'], link: '/题库/'
  },
  {
    emoji: '👤', title: '个人中心', desc: '个人信息管理，学习数据统计',
    tags: ['个人', '设置', '免费'], link: '/login/'
  },
]
</script>

<style scoped>
.quick-nav {
  max-width: 1152px;
  margin: 64px auto 0;
  padding: 0 24px;
}
.section-title {
  font-size: 24px;
  font-weight: 700;
  text-align: center;
  margin-bottom: 32px;
}
.nav-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}
@media (max-width: 768px) {
  .nav-grid { grid-template-columns: repeat(2, 1fr); }
}
@media (max-width: 480px) {
  .nav-grid { grid-template-columns: 1fr; }
}
.nav-card {
  display: block;
  padding: 20px;
  background: var(--vp-c-bg-soft);
  border: 1px solid var(--vp-c-divider);
  border-radius: 12px;
  text-decoration: none;
  color: inherit;
  transition: all 0.25s;
}
.nav-card:hover {
  box-shadow: 0 4px 16px rgba(0,0,0,0.08);
  border-color: var(--vp-c-brand);
  transform: translateY(-2px);
}
.nav-icon {
  font-size: 32px;
  margin-bottom: 8px;
}
.nav-body h3 {
  font-size: 16px;
  font-weight: 600;
  margin: 0 0 6px;
}
.nav-body p {
  font-size: 13px;
  color: var(--vp-c-text-2);
  margin: 0 0 10px;
}
.nav-tags {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}
.nav-tag {
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 10px;
  background: var(--vp-c-brand-dimm);
  color: var(--vp-c-brand);
}
.home-footer {
  text-align: center;
  padding: 48px 0 24px;
  color: var(--vp-c-text-2);
  font-size: 14px;
}
</style>
