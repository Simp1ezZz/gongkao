<script setup>
import { ref, onMounted, computed } from 'vue'
import { historyApi, getUser } from '../utils/api.js'

const currentUser = getUser()
const isLoggedIn = !!currentUser

const loading = ref(false)
const summary = ref(null)
const list = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = 10
const activeType = ref('all')
const hasMore = computed(() => list.value.length < total.value)

const detailLoading = ref(false)
const detailData = ref(null)
const showDetail = ref(false)

async function loadSummary() {
  try {
    const res = await historyApi.summary()
    if (res.success) summary.value = res.data
  } catch {}
}

async function loadHistory(reset = false) {
  if (reset) {
    page.value = 1
    list.value = []
  }
  loading.value = true
  try {
    const res = await historyApi.list({
      page: page.value,
      pageSize,
      type: activeType.value
    })
    if (res.success) {
      if (reset) {
        list.value = res.data.list || []
      } else {
        list.value.push(...(res.data.list || []))
      }
      total.value = res.data.total
    }
  } catch {} finally {
    loading.value = false
  }
}

async function loadMore() {
  page.value++
  await loadHistory()
}

function switchType(type) {
  activeType.value = type
  showDetail.value = false
  detailData.value = null
  loadHistory(true)
}

async function viewDetail(sessionId) {
  detailLoading.value = true
  showDetail.value = true
  try {
    const res = await historyApi.getDetail(sessionId)
    if (res.success) detailData.value = res.data
  } catch {} finally {
    detailLoading.value = false
  }
}

function closeDetail() {
  showDetail.value = false
  detailData.value = null
}

function formatTime(seconds) {
  if (!seconds) return '0m'
  const h = Math.floor(seconds / 3600)
  const m = Math.floor((seconds % 3600) / 60)
  if (h > 0) return `${h}h${m > 0 ? m + 'm' : ''}`
  return `${m}m`
}

function formatDate(dt) {
  if (!dt) return ''
  return dt.replace('T', ' ').substring(0, 16)
}

onMounted(() => {
  if (isLoggedIn) {
    loadSummary()
    loadHistory(true)
  }
})
</script>

<template>
  <div class="practice-history">
    <!-- 未登录 -->
    <div v-if="!isLoggedIn" class="login-prompt">
      <h3>请先登录</h3>
      <p>登录后可查看做题历史记录</p>
      <a href="/login/" class="btn-primary">去登录</a>
    </div>

    <template v-else>
      <!-- 统计概览 -->
      <div v-if="summary" class="summary-card">
        <div class="stat-item">
          <span class="stat-value">{{ summary.totalSessions }}</span>
          <span class="stat-label">总练习</span>
        </div>
        <div class="stat-item">
          <span class="stat-value">{{ summary.totalQuestions }}</span>
          <span class="stat-label">总做题</span>
        </div>
        <div class="stat-item">
          <span class="stat-value">{{ summary.avgAccuracy }}%</span>
          <span class="stat-label">平均正确率</span>
        </div>
        <div class="stat-item">
          <span class="stat-value">{{ formatTime(summary.totalTimeElapsed) }}</span>
          <span class="stat-label">总用时</span>
        </div>
      </div>

      <!-- 详情页 -->
      <template v-if="showDetail">
        <div class="detail-header">
          <button class="btn-back" @click="closeDetail">&larr; 返回列表</button>
          <h3 v-if="detailData">{{ detailData.paperTitle }}</h3>
        </div>

        <div v-if="detailLoading" class="loading-section">
          <div class="spinner"></div>
          <p>加载中...</p>
        </div>

        <template v-if="detailData && !detailLoading">
          <div class="detail-summary">
            <span>正确率 {{ detailData.summary.accuracy }}%</span>
            <span>答对 {{ detailData.summary.correctCount }}</span>
            <span>答错 {{ detailData.summary.wrongCount }}</span>
            <span>用时 {{ formatTime(detailData.timeElapsed) }}</span>
          </div>

          <div class="question-list">
            <div
              v-for="q in detailData.questions"
              :key="q.questionId"
              :class="['question-item', {
                correct: q.isCorrect === true,
                wrong: q.isCorrect === false,
                unanswered: q.isCorrect === null
              }]"
            >
              <div class="question-header">
                <span class="question-num">{{ q.sortOrder }}</span>
                <span :class="['question-badge', {
                  'badge-correct': q.isCorrect === true,
                  'badge-wrong': q.isCorrect === false,
                  'badge-unanswered': q.isCorrect === null
                }]">
                  {{ q.isCorrect === true ? '对' : q.isCorrect === false ? '错' : '未答' }}
                </span>
              </div>
              <div class="question-content" v-html="q.content"></div>
              <div class="answer-row">
                <span class="your-answer">你的答案：{{ q.userAnswer || '未作答' }}</span>
                <span class="correct-answer">正确答案：{{ q.correctAnswer }}</span>
              </div>
              <details v-if="q.explanation" class="explanation-block">
                <summary>查看解析</summary>
                <div v-html="q.explanation"></div>
              </details>
            </div>
          </div>
        </template>
      </template>

      <!-- 列表页 -->
      <template v-else>
        <!-- 筛选 Tab -->
        <div class="filter-tabs">
          <button :class="['tab-btn', { active: activeType === 'all' }]" @click="switchType('all')">全部</button>
          <button :class="['tab-btn', { active: activeType === 'paper' }]" @click="switchType('paper')">整卷练习</button>
          <button :class="['tab-btn', { active: activeType === 'special' }]" @click="switchType('special')">专项练习</button>
        </div>

        <!-- 记录列表 -->
        <div v-if="loading && list.length === 0" class="loading-section">
          <div class="spinner"></div>
          <p>加载中...</p>
        </div>

        <div v-else-if="list.length === 0" class="empty-state">
          <p>暂无做题记录</p>
        </div>

        <div v-else class="history-list">
          <div
            v-for="item in list"
            :key="item.sessionId"
            class="history-card"
            @click="viewDetail(item.sessionId)"
          >
            <div class="card-title">
              <span class="card-type-badge" :class="item.type">
                {{ item.type === 'paper' ? '整卷' : '专项' }}
              </span>
              {{ item.paperTitle }}
            </div>
            <div class="card-stats">
              <span>正确率 {{ item.accuracy }}%</span>
              <span>用时 {{ formatTime(item.timeElapsed) }}</span>
              <span>{{ item.totalQuestions }}题</span>
            </div>
            <div class="card-footer">
              <span class="card-date">{{ formatDate(item.submittedAt) }}</span>
              <span class="card-arrow">&rarr;</span>
            </div>
          </div>
        </div>

        <button
          v-if="hasMore && !loading"
          class="btn-load-more"
          @click="loadMore"
        >加载更多</button>
      </template>
    </template>
  </div>
</template>

<style scoped>
.practice-history {
  max-width: 900px;
  margin: 0 auto;
  padding: 20px;
}

/* 登录提示 */
.login-prompt {
  text-align: center;
  padding: 80px 24px;
  background: var(--vp-c-bg-soft);
  border-radius: 12px;
}
.login-prompt h3 { margin-bottom: 8px; }
.login-prompt p { color: var(--vp-c-text-2); margin-bottom: 24px; }

/* 统计概览 */
.summary-card {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 24px;
  background: var(--vp-c-bg-soft);
  border-radius: 12px;
  padding: 20px;
}
.stat-item {
  text-align: center;
}
.stat-value {
  display: block;
  font-size: 24px;
  font-weight: 700;
  color: var(--vp-c-brand);
}
.stat-label {
  display: block;
  font-size: 13px;
  color: var(--vp-c-text-3);
  margin-top: 4px;
}

/* 筛选 Tab */
.filter-tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 20px;
}
.tab-btn {
  padding: 8px 16px;
  border: 1px solid var(--vp-c-divider);
  border-radius: 20px;
  background: var(--vp-c-bg);
  color: var(--vp-c-text-2);
  cursor: pointer;
  font-size: 14px;
}
.tab-btn.active {
  background: var(--vp-c-brand);
  color: white;
  border-color: var(--vp-c-brand);
}

/* 历史卡片 */
.history-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.history-card {
  background: var(--vp-c-bg-soft);
  border-radius: 10px;
  padding: 16px 20px;
  cursor: pointer;
  transition: box-shadow 0.2s;
}
.history-card:hover {
  box-shadow: 0 2px 12px rgba(0,0,0,0.08);
}
.card-title {
  font-weight: 600;
  font-size: 15px;
  margin-bottom: 8px;
  display: flex;
  align-items: center;
  gap: 8px;
}
.card-type-badge {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
}
.card-type-badge.paper {
  background: #dbeafe;
  color: #1d4ed8;
}
.card-type-badge.special {
  background: #fef3c7;
  color: #b45309;
}
:root.dark .card-type-badge.paper {
  background: #1e3a5f;
  color: #93c5fd;
}
:root.dark .card-type-badge.special {
  background: #422006;
  color: #fbbf24;
}
.card-stats {
  display: flex;
  gap: 16px;
  font-size: 13px;
  color: var(--vp-c-text-2);
}
.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 8px;
}
.card-date {
  font-size: 13px;
  color: var(--vp-c-text-3);
}
.card-arrow {
  color: var(--vp-c-text-3);
  font-size: 16px;
}

/* 详情页 */
.detail-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
}
.btn-back {
  background: none;
  border: none;
  color: var(--vp-c-brand);
  cursor: pointer;
  font-size: 14px;
  padding: 4px 8px;
}
.btn-back:hover { text-decoration: underline; }
.detail-header h3 {
  font-size: 16px;
}

.detail-summary {
  display: flex;
  gap: 20px;
  padding: 12px 16px;
  background: var(--vp-c-bg-soft);
  border-radius: 8px;
  margin-bottom: 20px;
  font-size: 14px;
  color: var(--vp-c-text-2);
}

/* 题目列表 */
.question-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.question-item {
  background: var(--vp-c-bg-soft);
  border-radius: 8px;
  padding: 14px 16px;
  border-left: 3px solid var(--vp-c-divider);
}
.question-item.correct { border-left-color: #22c55e; }
.question-item.wrong { border-left-color: #ef4444; }
.question-item.unanswered { border-left-color: var(--vp-c-text-3); }

.question-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}
.question-num {
  font-weight: 700;
  font-size: 14px;
}
.question-badge {
  font-size: 11px;
  padding: 1px 6px;
  border-radius: 4px;
  font-weight: 600;
}
.badge-correct { background: #dcfce7; color: #166534; }
.badge-wrong { background: #fef2f2; color: #b91c1c; }
.badge-unanswered { background: var(--vp-c-default-soft); color: var(--vp-c-text-3); }
:root.dark .badge-correct { background: #052e16; color: #86efac; }
:root.dark .badge-wrong { background: #450a0a; color: #fca5a5; }

.question-content {
  font-size: 14px;
  line-height: 1.6;
  margin-bottom: 8px;
}
.question-content :deep(img) {
  display: inline;
  max-width: 100%;
  max-height: 200px;
}

.answer-row {
  display: flex;
  gap: 16px;
  font-size: 13px;
  margin-bottom: 4px;
}
.your-answer { color: var(--vp-c-text-2); }
.correct-answer { color: #22c55e; font-weight: 600; }

.explanation-block {
  margin-top: 8px;
  font-size: 13px;
}
.explanation-block summary {
  cursor: pointer;
  color: var(--vp-c-brand);
  font-size: 13px;
}
.explanation-block :deep(img) {
  display: inline;
  max-width: 100%;
  max-height: 200px;
}

/* 按钮 */
.btn-primary {
  display: inline-block;
  padding: 10px 24px;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 600;
  background: var(--vp-c-brand);
  color: white;
  text-decoration: none;
  border: none;
  cursor: pointer;
}

.btn-load-more {
  display: block;
  width: 100%;
  padding: 12px;
  margin-top: 16px;
  border: 1px dashed var(--vp-c-divider);
  border-radius: 8px;
  background: none;
  color: var(--vp-c-text-2);
  font-size: 14px;
  cursor: pointer;
}
.btn-load-more:hover { background: var(--vp-c-bg-soft); }

/* 加载 / 空状态 */
.loading-section {
  text-align: center;
  padding: 60px 24px;
}
.spinner {
  width: 32px; height: 32px;
  border: 3px solid var(--vp-c-divider);
  border-top-color: var(--vp-c-brand);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
  margin: 0 auto 12px;
}
@keyframes spin { to { transform: rotate(360deg); } }

.empty-state {
  text-align: center;
  padding: 60px 24px;
  color: var(--vp-c-text-3);
}

@media (max-width: 640px) {
  .summary-card {
    grid-template-columns: repeat(2, 1fr);
  }
  .stat-value { font-size: 20px; }
  .card-stats { flex-wrap: wrap; gap: 8px; }
  .detail-summary { flex-wrap: wrap; gap: 8px; }
  .answer-row { flex-direction: column; gap: 4px; }
}
</style>
