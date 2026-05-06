<script setup>
import { ref, onMounted, computed, reactive } from 'vue'
import { historyApi, getUser } from '../utils/api.js'
import { renderLatex } from '../utils/latex.js'

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
const currentIndex = ref(0)
const expandedSet = ref(new Set())

const currentQuestion = computed(() => {
  if (!detailData.value) return null
  return detailData.value.questions[currentIndex.value] || null
})

const parsedOptions = computed(() => {
  const q = currentQuestion.value
  if (!q || !q.options) return []
  try { return JSON.parse(q.options) } catch { return [] }
})

const hasOptions = computed(() => {
  const q = currentQuestion.value
  return q && (q.type === 'single_choice' || q.type === 'multi_choice' || !q.type)
})

const moduleGroups = computed(() => {
  if (!detailData.value) return []
  const groups = []
  let current = null
  detailData.value.questions.forEach((q, idx) => {
    if (!current || current.module !== q.module) {
      current = { module: q.module || '其他', items: [] }
      groups.push(current)
    }
    current.items.push({ index: idx, question: q })
  })
  return groups
})

function getNumBtnClass(item) {
  if (item.index === currentIndex.value) return 'current'
  const q = item.question
  if (q.isCorrect === true) return 'correct'
  if (q.isCorrect === false) return 'wrong'
  return 'unanswered-result'
}

function getOptionClass(label) {
  const q = currentQuestion.value
  if (!q) return ''
  const cls = []
  if (label === q.correctAnswer) cls.push('correct-option')
  if (label === q.userAnswer && q.userAnswer !== q.correctAnswer) cls.push('wrong-option')
  return cls
}

function jumpToQuestion(index) {
  currentIndex.value = index
}

function prevQuestion() {
  if (currentIndex.value > 0) currentIndex.value--
}

function nextQuestion() {
  if (detailData.value && currentIndex.value < detailData.value.questions.length - 1) currentIndex.value++
}

function toggleAnalysis(index) {
  const s = new Set(expandedSet.value)
  if (s.has(index)) s.delete(index)
  else s.add(index)
  expandedSet.value = s
}

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
  currentIndex.value = 0
  expandedSet.value = new Set()
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
      <a href="/login/" class="btn-go-login">去登录</a>
    </div>

    <template v-else>
      <!-- ====== 详情页：复用 OnlinePractice 提交后的布局 ====== -->
      <template v-if="showDetail">
        <!-- 顶部栏 -->
        <div class="top-bar">
          <button class="btn-back-link" @click="closeDetail" title="返回列表">←</button>
          <h1 class="paper-title">{{ detailData?.paperTitle || '加载中...' }}</h1>
          <button class="btn-submitted" disabled>
            <span class="submit-icon">📤</span> 已提交
          </button>
        </div>

        <div v-if="detailLoading" class="loading-section">
          <div class="spinner"></div>
          <p>加载中...</p>
        </div>

        <!-- 双栏布局 -->
        <div v-if="detailData && !detailLoading" class="practice-layout">
          <!-- 左侧：题目区域 -->
          <div class="question-area">
            <!-- 题目 -->
            <div class="question-panel" v-if="currentQuestion">
              <h3 class="question-content" v-html="renderLatex(currentQuestion.content)"></h3>

              <!-- 选项 -->
              <div v-if="hasOptions" class="options">
                <div v-for="opt in parsedOptions" :key="opt.label"
                     class="option-item"
                     :class="getOptionClass(opt.label)">
                  <span class="option-label">{{ opt.label }}.</span>
                  <span class="option-body" v-html="opt.text"></span>
                  <span v-if="opt.label === currentQuestion.correctAnswer"
                        class="option-badge correct-badge">正确答案</span>
                  <span v-if="opt.label === currentQuestion.userAnswer && currentQuestion.userAnswer !== currentQuestion.correctAnswer"
                        class="option-badge wrong-badge">你的答案</span>
                </div>
              </div>

              <!-- 非选择题的答案显示 -->
              <div v-if="!hasOptions" class="answer-display">
                <p><strong>你的答案：</strong>{{ currentQuestion.userAnswer || '未作答' }}</p>
              </div>
            </div>

            <!-- 答案解析 -->
            <div v-if="currentQuestion" class="analysis-section"
                 :class="{ expanded: expandedSet.has(currentIndex) }">
              <div class="analysis-header" @click="toggleAnalysis(currentIndex)">
                <span class="analysis-title">
                  {{ currentQuestion.isCorrect === false ? '❌' : currentQuestion.isCorrect === true ? '✅' : '⚠️' }}
                  答案解析
                </span>
                <span class="analysis-toggle">{{ expandedSet.has(currentIndex) ? '收起' : '展开' }}</span>
              </div>
              <div class="analysis-body" @click="!expandedSet.has(currentIndex) && toggleAnalysis(currentIndex)">
                <p><strong>正确答案：</strong>{{ currentQuestion.correctAnswer }}</p>
                <p><strong>你的答案：</strong>{{ currentQuestion.userAnswer || '未作答' }}</p>
                <div v-if="currentQuestion.explanation">
                  <p><strong>解析：</strong></p>
                  <div class="analysis-explanation" v-html="renderLatex(currentQuestion.explanation)"></div>
                </div>
                <p v-if="currentQuestion.module"><strong>💡 知识点：</strong>{{ currentQuestion.module }}</p>
                <div v-if="!expandedSet.has(currentIndex)" class="analysis-expand-hint">
                  <span>点击展开查看完整解析 ▼</span>
                </div>
              </div>
            </div>

            <!-- 翻页按钮 -->
            <div class="nav-buttons">
              <button :disabled="currentIndex <= 0" @click="prevQuestion">← 上一题</button>
              <button v-if="detailData.questions && currentIndex < detailData.questions.length - 1" @click="nextQuestion">下一题 →</button>
            </div>
          </div>

          <!-- 右侧：答题卡面板 -->
          <div class="answer-card-wrapper">
            <div class="answer-card">
              <div class="card-header">
                <span class="card-title">答题卡</span>
                <span class="card-progress">{{ detailData.questions?.length || 0 }} 题</span>
              </div>

              <div class="card-controls">
                <div class="timer-display">
                  <span>⏱️</span>
                  <span class="timer-value">{{ formatTime(detailData.timeElapsed) }}</span>
                </div>
              </div>

              <div class="card-legend">
                <span class="legend-item"><span class="legend-dot correct"></span>正确</span>
                <span class="legend-item"><span class="legend-dot wrong"></span>错误</span>
              </div>

              <div class="card-grid">
                <div v-for="group in moduleGroups" :key="group.module" class="module-group">
                  <div class="module-label">
                    {{ group.module }}
                    <span class="module-range">{{ group.items[0].index + 1 }}-{{ group.items[group.items.length - 1].index + 1 }}</span>
                  </div>
                  <div class="number-grid">
                    <button v-for="item in group.items" :key="item.index"
                            :class="['num-btn', getNumBtnClass(item)]"
                            @click="jumpToQuestion(item.index)">
                      {{ item.index + 1 }}
                    </button>
                  </div>
                </div>
              </div>
            </div>

            <!-- 统计 -->
            <div class="card-result">
              <div class="card-result-actions">
                <a href="/practice/history/" class="link-records">📝 返回列表</a>
              </div>
              <div class="card-stats">
                <div class="cs-item">
                  <span class="cs-value correct">{{ detailData.summary.correctCount }}</span>
                  <span class="cs-label">正确</span>
                </div>
                <div class="cs-item">
                  <span class="cs-value wrong">{{ detailData.summary.wrongCount }}</span>
                  <span class="cs-label">错误</span>
                </div>
                <div class="cs-item">
                  <span class="cs-value">{{ detailData.summary.unansweredCount }}</span>
                  <span class="cs-label">未答</span>
                </div>
                <div class="cs-item">
                  <span class="cs-value">{{ detailData.summary.accuracy }}%</span>
                  <span class="cs-label">正确率</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </template>

      <!-- ====== 列表页 ====== -->
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
            <div class="history-card-title">
              <span class="card-type-badge" :class="item.type">
                {{ item.type === 'paper' ? '整卷' : '专项' }}
              </span>
              {{ item.paperTitle }}
            </div>
            <div class="history-card-stats">
              <span>正确率 {{ item.accuracy }}%</span>
              <span>用时 {{ formatTime(item.timeElapsed) }}</span>
              <span>{{ item.totalQuestions }}题</span>
            </div>
            <div class="history-card-footer">
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
.practice-history { max-width: 100%; padding: 0; }

/* === 登录提示 === */
.login-prompt {
  text-align: center; padding: 80px 24px;
  background: var(--vp-c-bg-soft); border-radius: 12px;
  max-width: 400px; margin: 40px auto;
}
.login-prompt h3 { margin-bottom: 8px; }
.login-prompt p { color: var(--vp-c-text-2); margin-bottom: 24px; }
.btn-go-login {
  padding: 10px 24px; border-radius: 6px; font-size: 14px; font-weight: 600;
  background: var(--vp-c-brand); color: #fff; text-decoration: none;
  border: none; cursor: pointer;
}

/* === 列表页 === */
.summary-card {
  display: grid; grid-template-columns: repeat(4, 1fr);
  gap: 16px; margin: 0 20px 24px; background: var(--vp-c-bg-soft);
  border-radius: 12px; padding: 20px;
}
.stat-item { text-align: center; }
.stat-value { display: block; font-size: 24px; font-weight: 700; color: var(--vp-c-brand); }
.stat-label { display: block; font-size: 13px; color: var(--vp-c-text-3); margin-top: 4px; }

.filter-tabs { display: flex; gap: 8px; margin: 0 20px 20px; }
.tab-btn {
  padding: 8px 16px; border: 1px solid var(--vp-c-divider); border-radius: 20px;
  background: var(--vp-c-bg); color: var(--vp-c-text-2); cursor: pointer; font-size: 14px;
}
.tab-btn.active { background: var(--vp-c-brand); color: white; border-color: var(--vp-c-brand); }

.history-list { display: flex; flex-direction: column; gap: 12px; padding: 0 20px; }
.history-card {
  background: var(--vp-c-bg-soft); border-radius: 10px; padding: 16px 20px;
  cursor: pointer; transition: box-shadow 0.2s;
}
.history-card:hover { box-shadow: 0 2px 12px rgba(0,0,0,0.08); }
.history-card-title {
  font-weight: 600; font-size: 15px; margin-bottom: 8px;
  display: flex; align-items: center; gap: 8px;
}
.card-type-badge {
  display: inline-block; padding: 2px 8px; border-radius: 4px;
  font-size: 12px; font-weight: 500;
}
.card-type-badge.paper { background: #dbeafe; color: #1d4ed8; }
.card-type-badge.special { background: #fef3c7; color: #b45309; }
:root.dark .card-type-badge.paper { background: #1e3a5f; color: #93c5fd; }
:root.dark .card-type-badge.special { background: #422006; color: #fbbf24; }
.history-card-stats { display: flex; gap: 16px; font-size: 13px; color: var(--vp-c-text-2); }
.history-card-footer { display: flex; justify-content: space-between; align-items: center; margin-top: 8px; }
.card-date { font-size: 13px; color: var(--vp-c-text-3); }
.card-arrow { color: var(--vp-c-text-3); font-size: 16px; }
.btn-load-more {
  display: block; width: calc(100% - 40px); margin: 16px 20px;
  padding: 12px; border: 1px dashed var(--vp-c-divider); border-radius: 8px;
  background: none; color: var(--vp-c-text-2); font-size: 14px; cursor: pointer;
}
.btn-load-more:hover { background: var(--vp-c-bg-soft); }

/* === 详情页：复用 OnlinePractice 布局和样式 === */

/* 顶部栏 */
.top-bar {
  display: flex; align-items: center; gap: 12px;
  padding: 10px 20px; background: var(--vp-c-bg-soft);
  border-bottom: 1px solid var(--vp-c-divider); margin-bottom: 16px;
}
.btn-back-link {
  font-size: 20px; color: var(--vp-c-text-1); background: none; border: none;
  padding: 4px 8px; border-radius: 4px; cursor: pointer; transition: background 0.2s;
}
.btn-back-link:hover { background: var(--vp-c-bg); }
.paper-title {
  flex: 1; font-size: 16px; font-weight: 600; margin: 0;
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}
.btn-submitted {
  display: flex; align-items: center; gap: 4px;
  padding: 8px 16px; background: var(--vp-c-bg-soft);
  color: var(--vp-c-text-3); border: 1px solid var(--vp-c-divider);
  border-radius: 6px; font-size: 14px; font-weight: 600;
  cursor: not-allowed; white-space: nowrap; opacity: 0.7;
}
.submit-icon { font-size: 14px; }

/* 双栏布局 */
.practice-layout {
  display: flex; gap: 20px; padding: 0 20px; align-items: flex-start;
}

/* 左侧题目区 */
.question-area { flex: 1; min-width: 0; }
.question-panel {
  padding: 24px; background: var(--vp-c-bg-soft); border-radius: 8px;
}
.question-content {
  font-size: 15px; line-height: 1.8; margin: 0 0 20px; font-weight: 500;
}
.question-content :deep(img) { max-width: 200px; max-height: 80px; vertical-align: middle; }
.question-content :deep(img[flag="tex"]) { display: inline !important; }
.options { display: flex; flex-direction: column; gap: 10px; }
.option-item {
  padding: 12px 14px; border: 1px solid var(--vp-c-divider);
  border-radius: 8px; transition: all 0.2s;
}
.option-label { font-weight: 600; margin-right: 4px; }
.option-body { line-height: 1.6; }
.option-body :deep(img) { max-width: 200px; max-height: 80px; vertical-align: middle; }
.option-body :deep(img[flag="tex"]) { display: inline !important; }

/* 提交后选项样式 */
.correct-option {
  border-color: #67c23a !important;
  background: rgba(103, 194, 58, 0.08) !important;
}
.wrong-option {
  border-color: #f56c6c !important;
  background: rgba(245, 108, 108, 0.08) !important;
}
.option-badge {
  font-size: 11px; padding: 1px 6px; border-radius: 3px;
  margin-left: 6px; font-weight: 600; white-space: nowrap;
}
.correct-badge { background: #67c23a; color: #fff; }
.wrong-badge { background: #f56c6c; color: #fff; }
.answer-display { margin-top: 12px; font-size: 14px; }

/* 答案解析 */
.analysis-section {
  margin-top: 12px; border: 1px solid var(--vp-c-divider);
  border-radius: 8px; overflow: hidden;
}
.analysis-header {
  display: flex; justify-content: space-between; align-items: center;
  padding: 10px 14px; cursor: pointer;
  background: var(--vp-c-bg-soft); transition: background 0.2s;
}
.analysis-header:hover { background: var(--vp-c-bg); }
.analysis-title { font-size: 14px; font-weight: 600; }
.analysis-toggle { font-size: 12px; color: var(--vp-c-text-2); }
.analysis-body {
  padding: 14px; border-top: 1px solid var(--vp-c-divider);
  font-size: 14px; line-height: 1.7;
  max-height: 240px; overflow: hidden; position: relative;
  transition: max-height 0.3s ease;
}
.analysis-section.expanded .analysis-body { max-height: none; }
.analysis-body p { margin: 6px 0; }
.analysis-body :deep(img) { max-width: 200px; max-height: 80px; vertical-align: middle; }
.analysis-body :deep(img[flag="tex"]) { display: inline !important; }
.analysis-explanation { margin-top: 4px; }
.analysis-expand-hint {
  position: absolute; bottom: 0; left: 0; right: 0;
  height: 48px; display: flex; align-items: flex-end; justify-content: center;
  padding-bottom: 6px;
  background: linear-gradient(transparent, var(--vp-c-bg-soft));
  cursor: pointer;
}
.analysis-expand-hint span {
  font-size: 12px; color: var(--vp-c-brand); font-weight: 500;
  background: var(--vp-c-bg-soft); padding: 0 8px; border-radius: 4px;
}

/* 翻页按钮 */
.nav-buttons {
  display: flex; justify-content: space-between; margin-top: 16px; gap: 8px;
}
.nav-buttons button {
  padding: 10px 20px; border: 1px solid var(--vp-c-divider);
  border-radius: 6px; cursor: pointer; background: var(--vp-c-bg);
  color: var(--vp-c-text-1); font-size: 14px; transition: all 0.2s;
}
.nav-buttons button:hover { border-color: var(--vp-c-brand); }
.nav-buttons button:disabled { opacity: 0.4; cursor: not-allowed; }

/* 右侧答题卡 */
.answer-card-wrapper {
  width: 280px; flex-shrink: 0;
  position: sticky; top: 72px;
}
.answer-card {
  background: var(--vp-c-bg-soft); border-radius: 10px;
  border: 1px solid var(--vp-c-divider);
}
.card-header {
  display: flex; justify-content: space-between; align-items: center;
  padding: 14px 16px; border-bottom: 1px solid var(--vp-c-divider);
}
.card-title { font-size: 15px; font-weight: 700; }
.card-progress { font-size: 13px; color: var(--vp-c-text-2); }
.card-controls {
  display: flex; align-items: center; gap: 10px;
  padding: 10px 16px; border-bottom: 1px solid var(--vp-c-divider);
}
.timer-display {
  display: flex; align-items: center; gap: 4px; margin-left: auto;
}
.timer-value {
  font-size: 16px; font-weight: 600;
  font-variant-numeric: tabular-nums; color: var(--vp-c-text-1);
}
.card-legend {
  display: flex; gap: 16px; padding: 8px 16px;
  border-bottom: 1px solid var(--vp-c-divider); font-size: 12px;
  color: var(--vp-c-text-2);
}
.legend-item { display: flex; align-items: center; gap: 4px; }
.legend-dot {
  display: inline-block; width: 10px; height: 10px; border-radius: 2px;
}
.legend-dot.correct { background: #67c23a; }
.legend-dot.wrong { background: #f56c6c; }
.card-grid {
  padding: 12px 16px;
  max-height: calc(100vh - 400px); overflow-y: auto;
}
.module-group { margin-bottom: 12px; }
.module-group:last-child { margin-bottom: 0; }
.module-label {
  font-size: 12px; color: var(--vp-c-text-2); margin-bottom: 6px;
  display: flex; justify-content: space-between;
}
.module-range { color: var(--vp-c-text-3); }
.number-grid { display: flex; flex-wrap: wrap; gap: 6px; }
.num-btn {
  width: 32px; height: 32px; border: 1px solid var(--vp-c-divider);
  border-radius: 4px; background: var(--vp-c-bg); color: var(--vp-c-text-1);
  font-size: 12px; cursor: pointer; transition: all 0.15s;
  display: flex; align-items: center; justify-content: center;
}
.num-btn:hover { border-color: var(--vp-c-brand); }
.num-btn.current {
  background: var(--vp-c-brand); color: #fff;
  border-color: var(--vp-c-brand); font-weight: 700;
}
.num-btn.correct { background: #67c23a; color: #fff; border-color: #67c23a; }
.num-btn.wrong { background: #f56c6c; color: #fff; border-color: #f56c6c; }
.num-btn.unanswered-result {
  background: var(--vp-c-bg); color: var(--vp-c-text-3);
  border-color: var(--vp-c-divider); opacity: 0.6;
}

/* 答题卡统计 */
.card-result {
  background: var(--vp-c-bg-soft); border-radius: 10px;
  border: 1px solid var(--vp-c-divider);
  padding: 12px 16px; margin-top: 12px;
}
.card-result-actions {
  display: flex; gap: 10px; margin-bottom: 10px;
}
.link-records {
  padding: 6px 12px; border: 1px solid var(--vp-c-divider);
  border-radius: 6px; background: var(--vp-c-bg); color: var(--vp-c-text-1);
  font-size: 12px; text-decoration: none;
}
.card-stats { display: grid; grid-template-columns: repeat(4, 1fr); gap: 8px; }
.cs-item { text-align: center; }
.cs-value { display: block; font-size: 18px; font-weight: 700; color: var(--vp-c-text-1); }
.cs-value.correct { color: #67c23a; }
.cs-value.wrong { color: #f56c6c; }
.cs-label { font-size: 11px; color: var(--vp-c-text-3); }

/* 加载 / 空状态 */
.loading-section { text-align: center; padding: 60px 24px; }
.spinner {
  width: 32px; height: 32px; border: 3px solid var(--vp-c-divider);
  border-top-color: var(--vp-c-brand); border-radius: 50%;
  animation: spin 0.8s linear infinite; margin: 0 auto 12px;
}
@keyframes spin { to { transform: rotate(360deg); } }
.empty-state { text-align: center; padding: 60px 24px; color: var(--vp-c-text-3); }

/* 响应式 */
@media (max-width: 960px) {
  .practice-layout { flex-direction: column; }
  .answer-card-wrapper { width: 100%; position: static; }
}
@media (max-width: 640px) {
  .summary-card { grid-template-columns: repeat(2, 1fr); }
  .stat-value { font-size: 20px; }
  .history-card-stats { flex-wrap: wrap; gap: 8px; }
}
</style>
</template>
