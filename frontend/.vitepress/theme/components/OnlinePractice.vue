<template>
  <div class="online-practice" v-if="loaded">
    <!-- 登录弹窗 -->
    <Modal v-if="showLoginModal" @close="showLoginModal = false">
      <Login inline @login-success="onLoginSuccess" />
    </Modal>

    <!-- 顶部栏 -->
    <div class="top-bar">
      <a href="/题库/" class="btn-back" title="返回题库">←</a>
      <h1 class="paper-title">{{ paperDetail.title }}</h1>
      <button v-if="!result" class="btn-submit-top" @click="confirmSubmit">
        <span class="submit-icon">📤</span> 提交试卷
      </button>
      <button v-else class="btn-submitted" disabled>
        <span class="submit-icon">📤</span> 已提交
      </button>
    </div>

    <!-- 双栏布局 -->
    <div class="practice-layout">
      <!-- 左侧：题目区域 -->
      <div class="question-area">
        <!-- 暂停遮罩 -->
        <div v-if="pausedLocally" class="pause-overlay" @click="togglePause">
          <div class="pause-content">
            <span class="pause-icon">⏸️</span>
            <span class="pause-text">已暂停</span>
            <span class="pause-hint">点击此处继续答题</span>
          </div>
        </div>

        <!-- 材料区域 -->
        <div v-if="currentMaterial" class="material-panel">
          <h3>{{ currentMaterial.title }}</h3>
          <div class="material-content" v-html="renderLatex(currentMaterial.content)"></div>
        </div>

        <!-- 题目 -->
        <div class="question-panel" v-if="currentQuestion">
          <h3 class="question-content" v-html="renderLatex(currentQuestion.content)"></h3>

          <!-- 选项（选择题） -->
          <div v-if="hasOptions" class="options">
            <div v-for="opt in parsedOptions" :key="opt.label"
                 class="option-item"
                 :class="getOptionClass(opt.label)"
                 @click="selectAnswer(opt.label)">
              <span class="option-label">{{ opt.label }}.</span>
              <span class="option-body" v-html="opt.text"></span>
              <span v-if="result && currentResult && opt.label === currentResult.answer"
                    class="option-badge correct-badge">正确答案</span>
              <span v-if="result && currentResult && opt.label === currentResult.userAnswer && currentResult.userAnswer !== currentResult.answer"
                    class="option-badge wrong-badge">你的答案</span>
            </div>
          </div>

          <!-- 填空题 -->
          <div v-if="currentQuestion.type === 'fill_blank'" class="fill-blank">
            <input v-model="answers[currentQuestion.id]" placeholder="请输入答案" />
          </div>
        </div>

        <!-- 答案解析（提交后显示） -->
        <div v-if="result && currentResult" class="analysis-section"
             :class="{ expanded: expandedSet.has(currentIndex) }">
          <div class="analysis-header" @click="toggleAnalysis(currentIndex)">
            <span class="analysis-title">
              {{ currentResult.isCorrect === false ? '❌' : currentResult.isCorrect === true ? '✅' : '⚠️' }}
              答案解析
            </span>
            <span class="analysis-toggle">{{ expandedSet.has(currentIndex) ? '收起' : '展开' }}</span>
          </div>
          <div class="analysis-body" @click="!expandedSet.has(currentIndex) && toggleAnalysis(currentIndex)">
            <p><strong>正确答案：</strong>{{ currentResult.answer }}</p>
            <p><strong>你的答案：</strong>{{ currentResult.userAnswer || '未作答' }}</p>
            <div v-if="currentResult.explanation">
              <p><strong>解析：</strong></p>
              <div class="analysis-explanation" v-html="renderLatex(currentResult.explanation)"></div>
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
          <button v-if="currentIndex < questions.length - 1" @click="nextQuestion">下一题 →</button>
          <button v-if="result" class="btn-restart" @click="restartPractice">🔄 重新开始</button>
        </div>

        <!-- AI 深度分析按钮 -->
        <button v-if="result && currentResult && currentResult.isCorrect === false"
                class="btn-ai-analysis">❤️ AI 深度分析这道错题</button>
      </div>

      <!-- 右侧：答题卡面板 -->
      <div class="answer-card-wrapper">
        <div class="answer-card">
          <div class="card-header">
          <span class="card-title">答题卡</span>
          <span class="card-progress">{{ answeredCount }} / {{ questions.length }}</span>
        </div>

        <div class="card-controls">
          <button v-if="session?.status === 'ongoing' || (!session && timerRunning)"
                  class="btn-pause" @click="togglePause">⏸️ 暂停</button>
          <button v-else-if="session?.status === 'paused' || pausedLocally"
                  class="btn-resume" @click="togglePause">▶️ 继续</button>
          <div class="timer-display">
            <span>⏱️</span>
            <span class="timer-value">{{ formatTime(timeElapsed) }}</span>
          </div>
        </div>

        <div class="card-legend">
          <span class="legend-item"><span class="legend-dot current"></span>当前</span>
          <span class="legend-item"><span class="legend-dot answered"></span>已答</span>
          <template v-if="result">
            <span class="legend-item"><span class="legend-dot correct"></span>正确</span>
            <span class="legend-item"><span class="legend-dot wrong"></span>错误</span>
          </template>
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

        <!-- 提交后统计（答题卡下方） -->
        <div v-if="result" class="card-result">
          <div class="card-result-actions">
            <button class="btn-ai-sm">🤖 AI 智能分析</button>
            <a href="/practice/records/" class="link-records">📝 答题记录</a>
          </div>
          <div class="card-stats">
            <div class="cs-item">
              <span class="cs-value correct">{{ result.correctCount }}</span>
              <span class="cs-label">正确</span>
            </div>
            <div class="cs-item">
              <span class="cs-value wrong">{{ result.wrongCount }}</span>
              <span class="cs-label">错误</span>
            </div>
            <div class="cs-item">
              <span class="cs-value">{{ unansweredCount }}</span>
              <span class="cs-label">未答</span>
            </div>
            <div class="cs-item">
              <span class="cs-value">{{ result.accuracy }}%</span>
              <span class="cs-label">正确率</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 确认提交弹窗 -->
    <Modal v-if="showSubmitModal" @close="showSubmitModal = false">
      <div class="submit-confirm">
        <h3>确认提交？</h3>
        <p>已答 {{ answeredCount }} / {{ questions.length }} 题</p>
        <p v-if="answeredCount < questions.length" class="warn">
          还有 {{ questions.length - answeredCount }} 题未作答
        </p>
        <div class="submit-actions">
          <button @click="showSubmitModal = false">取消</button>
          <button class="btn-primary" @click="submitExam">确认提交</button>
        </div>
      </div>
    </Modal>

  </div>
  <div v-else class="loading">加载中...</div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { paperApi, sessionApi } from '../utils/api.js'
import { renderLatex } from '../utils/latex.js'
import Modal from './Modal.vue'
import Login from './Login.vue'

const loaded = ref(false)
const paperDetail = ref({})
const questions = ref([])
const materials = ref([])
const session = ref(null)
const currentIndex = ref(0)
const answers = ref({})
const timeElapsed = ref(0)
const result = ref(null)
const showSubmitModal = ref(false)
const showLoginModal = ref(false)
const timerRunning = ref(false)
const pausedLocally = ref(false)
const expandedSet = ref(new Set())

let timer = null
let saveTimer = null

function debouncedSave() {
  if (saveTimer) clearTimeout(saveTimer)
  saveTimer = setTimeout(saveProgress, 2000)
}

const currentQuestion = computed(() => questions.value[currentIndex.value])

const currentMaterial = computed(() => {
  const q = currentQuestion.value
  if (!q || !q.materialGroupId) return null
  return materials.value.find(m => m.id === q.materialGroupId)
})

const parsedOptions = computed(() => {
  const q = currentQuestion.value
  if (!q || !q.options) return []
  try { return JSON.parse(q.options) } catch { return [] }
})

const hasOptions = computed(() => {
  const q = currentQuestion.value
  return q && (q.type === 'single_choice' || q.type === 'multi_choice')
})

const answeredCount = computed(() => Object.keys(answers.value).length)

const moduleGroups = computed(() => {
  const groups = []
  let current = null
  questions.value.forEach((q, idx) => {
    if (!current || current.module !== q.module) {
      current = { module: q.module, items: [] }
      groups.push(current)
    }
    current.items.push({ index: idx, question: q })
  })
  return groups
})

const resultMap = computed(() => {
  if (!result.value) return {}
  const map = {}
  // API 返回的结果可能不包含未答题，用题目列表补全
  const apiResults = result.value.questions || []
  apiResults.forEach(q => { map[q.id] = q })
  questions.value.forEach(q => {
    if (!map[q.id]) {
      map[q.id] = {
        id: q.id,
        content: q.content,
        userAnswer: answers.value[q.id] || null,
        answer: q.answer || null,
        explanation: q.explanation || '',
        isCorrect: null
      }
    }
  })
  return map
})

const currentResult = computed(() => {
  const q = currentQuestion.value
  if (!q) return null
  return resultMap.value[q.id] || null
})

const unansweredCount = computed(() => {
  if (!result.value) return 0
  return questions.value.length - result.value.correctCount - result.value.wrongCount
})

function getNumBtnClass(item) {
  if (item.index === currentIndex.value) return 'current'
  if (result.value) {
    const r = resultMap.value[item.question.id]
    if (r) {
      if (r.isCorrect === true) return 'correct'
      if (r.isCorrect === false) return 'wrong'
    }
    return 'unanswered-result'
  }
  if (answers.value[item.question.id] != null) return 'answered'
  return ''
}

function toggleAnalysis(index) {
  const s = new Set(expandedSet.value)
  if (s.has(index)) s.delete(index)
  else s.add(index)
  expandedSet.value = s
}

function restartPractice() {
  result.value = null
  expandedSet.value = new Set()
  answers.value = {}
  currentIndex.value = 0
  timeElapsed.value = 0
  startTimer()
}

function formatTime(seconds) {
  const h = Math.floor(seconds / 3600)
  const m = Math.floor((seconds % 3600) / 60)
  const s = seconds % 60
  return h > 0
    ? `${h}:${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
    : `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
}

function selectAnswer(label) {
  if (result.value) return
  const qId = currentQuestion.value.id
  if (currentQuestion.value.type === 'multi_choice') {
    const current = answers.value[qId] || ''
    const selected = new Set(current ? current.split(',') : [])
    if (selected.has(label)) selected.delete(label)
    else selected.add(label)
    answers.value[qId] = [...selected].sort().join(',')
  } else {
    answers.value[qId] = label
    // 单选题：选择后自动跳转下一题
    if (currentIndex.value < questions.value.length - 1) {
      setTimeout(() => { currentIndex.value++ }, 300)
    }
  }
  if (session.value) debouncedSave()
}

function isOptionSelected(label) {
  const ans = answers.value[currentQuestion.value.id]
  if (!ans) return false
  if (currentQuestion.value.type === 'multi_choice') {
    return ans.split(',').includes(label)
  }
  return ans === label
}

function getOptionClass(label) {
  if (result.value && currentResult.value) {
    const r = currentResult.value
    const cls = []
    if (label === r.answer) cls.push('correct-option')
    if (label === r.userAnswer && r.userAnswer !== r.answer) cls.push('wrong-option')
    if (isOptionSelected(label) && !result.value) cls.push('selected')
    return cls
  }
  return isOptionSelected(label) ? 'selected' : ''
}

function prevQuestion() {
  if (currentIndex.value > 0) currentIndex.value--
}

function nextQuestion() {
  if (currentIndex.value < questions.value.length - 1) currentIndex.value++
}

function jumpToQuestion(index) {
  currentIndex.value = index
}

async function togglePause() {
  // 未登录本地暂停
  if (!session.value) {
    pausedLocally.value = !pausedLocally.value
    if (pausedLocally.value) stopTimer()
    else startTimer()
    return
  }
  const newStatus = session.value.status === 'ongoing' ? 'paused' : 'ongoing'
  try {
    const res = await sessionApi.update(session.value.id, {
      status: newStatus,
      timeElapsed: timeElapsed.value,
      currentIndex: currentIndex.value,
      answers: JSON.stringify(
        Object.entries(answers.value).map(([qId, ans]) => ({
          questionId: Number(qId), answer: ans
        }))
      )
    })
    if (res.success) session.value = res.data
    if (newStatus === 'paused') stopTimer()
    else startTimer()
  } catch (e) {
    console.error('暂停/恢复失败', e)
  }
}

async function saveProgress() {
  if (!session.value || session.value.status === 'submitted') return
  try {
    await sessionApi.update(session.value.id, {
      timeElapsed: timeElapsed.value,
      currentIndex: currentIndex.value,
      answers: JSON.stringify(
        Object.entries(answers.value).map(([qId, ans]) => ({
          questionId: Number(qId), answer: ans
        }))
      )
    })
  } catch (e) {
    console.error('保存进度失败', e)
  }
}

function confirmSubmit() {
  if (!localStorage.getItem('token')) {
    saveLocalState()
    showLoginModal.value = true
    return
  }
  showSubmitModal.value = true
}

function getSpecialStateKey() {
  const params = new URLSearchParams(window.location.search)
  const module = params.get('module')
  const count = params.get('count')
  if (!module || !count) return null
  return `specialState_${module}_${count}`
}

function saveLocalState() {
  const state = {
    answers: answers.value,
    currentIndex: currentIndex.value,
    timeElapsed: timeElapsed.value
  }
  // For special practice, save with module+count key for resume capability
  const specialKey = getSpecialStateKey()
  if (specialKey) {
    state.questions = questions.value
    state.questionIds = questions.value.map(q => q.id)
    localStorage.setItem(specialKey, JSON.stringify(state))
  }
  // Also save generic key for login-redirect recovery
  localStorage.setItem('practiceState', JSON.stringify({ answers: answers.value, currentIndex: currentIndex.value, timeElapsed: timeElapsed.value }))
}

async function onLoginSuccess() {
  showLoginModal.value = false
  const params = new URLSearchParams(window.location.search)
  const paperId = params.get('paperId')
  if (!paperId) return

  try {
    const sessionRes = await sessionApi.create({ paperId: Number(paperId) })
    if (sessionRes.success) {
      session.value = sessionRes.data
      // 合并本地已有答案（本地优先）
      if (sessionRes.data.answers) {
        try {
          const savedAnswers = JSON.parse(sessionRes.data.answers)
          savedAnswers.forEach(a => {
            if (!answers.value[a.questionId]) {
              answers.value[a.questionId] = a.answer
            }
          })
        } catch {}
      }
      // 同步本地计时器到服务端
      await sessionApi.update(sessionRes.data.id, {
        timeElapsed: timeElapsed.value,
        currentIndex: currentIndex.value,
        answers: JSON.stringify(
          Object.entries(answers.value).map(([qId, ans]) => ({
            questionId: Number(qId), answer: ans
          }))
        )
      })
    }
  } catch (e) {
    console.warn('创建会话失败', e)
  }
}

async function submitExam() {
  showSubmitModal.value = false
  stopTimer()

  const answerItems = Object.entries(answers.value).map(([qId, ans]) => ({
    questionId: Number(qId), answer: ans
  }))
  const hasToken = !!localStorage.getItem('token')

  if (hasToken) {
    try {
      if (session.value) {
        await sessionApi.submit(session.value.id, {
          timeElapsed: timeElapsed.value,
          answers: JSON.stringify(answerItems)
        })
      }
      const res = await paperApi.batchSubmit({
        sessionId: session.value?.id || null,
        answers: answerItems
      })
      if (res.success) {
        result.value = res.data
        return
      }
    } catch (e) {
      console.error('提交失败', e)
    }
  }

  // 未登录或 API 失败：本地生成结果
  buildLocalResult()
}

function buildLocalResult() {
  let correctCount = 0
  let wrongCount = 0
  const resultQuestions = questions.value.map(q => {
    const userAnswer = answers.value[q.id] || null
    return {
      id: q.id,
      content: q.content,
      userAnswer,
      answer: null,
      explanation: '',
      isCorrect: null
    }
  })
  result.value = {
    correctCount,
    wrongCount: 0,
    accuracy: 0,
    questions: resultQuestions
  }
}

function startTimer() {
  stopTimer()
  timerRunning.value = true
  timer = setInterval(() => { timeElapsed.value++ }, 1000)
}

function stopTimer() {
  if (timer) { clearInterval(timer); timer = null }
  if (saveTimer) { clearTimeout(saveTimer); saveTimer = null }
  timerRunning.value = false
}

async function init() {
  const params = new URLSearchParams(window.location.search)
  const paperId = params.get('paperId')
  const questionIds = params.get('questionIds')

  if (!paperId && !questionIds) {
    loaded.value = true
    return
  }

  try {
    if (paperId) {
      const detailRes = await paperApi.getDetail(paperId)
      if (!detailRes.success) { loaded.value = true; return }
      paperDetail.value = detailRes.data
      questions.value = detailRes.data.questions || []
      materials.value = detailRes.data.materials || []
    } else {
      paperDetail.value = {
        title: '专项练习',
        questionCount: questionIds.split(',').length,
        year: new Date().getFullYear(),
        regionName: ''
      }
      const cached = localStorage.getItem('specialQuestions')
      if (cached) {
        try {
          questions.value = JSON.parse(cached)
          localStorage.removeItem('specialQuestions')
        } catch {}
      }
    }
    loaded.value = true

    // Restore practice state
    const specialKey = getSpecialStateKey()
    let restoredFromSpecial = false
    if (specialKey) {
      const savedSpecial = localStorage.getItem(specialKey)
      if (savedSpecial) {
        try {
          const state = JSON.parse(savedSpecial)
          if (state.answers) answers.value = state.answers
          if (state.currentIndex != null) currentIndex.value = state.currentIndex
          if (state.timeElapsed != null) timeElapsed.value = state.timeElapsed
          restoredFromSpecial = true
        } catch {}
      }
    }

    // Fallback: restore generic state (for login-redirect recovery)
    if (!restoredFromSpecial) {
      const savedState = localStorage.getItem('practiceState')
      if (savedState) {
        try {
          const state = JSON.parse(savedState)
          if (state.answers) answers.value = state.answers
          if (state.currentIndex != null) currentIndex.value = state.currentIndex
          if (state.timeElapsed != null) timeElapsed.value = state.timeElapsed
        } catch {}
      }
    }
    localStorage.removeItem('practiceState')

    // 始终启动前端计时器（无论是否登录）
    startTimer()

    // 已登录 + paperId 模式：创建/恢复会话
    if (paperId && localStorage.getItem('token')) {
      try {
        const sessionRes = await sessionApi.create({ paperId: Number(paperId) })
        if (sessionRes.success) {
          session.value = sessionRes.data
          if (sessionRes.data.answers) {
            try {
              const savedAnswers = JSON.parse(sessionRes.data.answers)
              if (!restoredFromSpecial) {
                savedAnswers.forEach(a => { answers.value[a.questionId] = a.answer })
              }
            } catch {}
          }
          if (!restoredFromSpecial) {
            currentIndex.value = sessionRes.data.currentIndex || 0
            timeElapsed.value = sessionRes.data.timeElapsed || 0
          }
          if (sessionRes.data.status === 'paused') {
            stopTimer()
            pausedLocally.value = true
          }
        }
      } catch (e) {
        console.warn('创建会话失败，切换到浏览模式', e)
      }
    }

    // 已登录 + 专项练习模式：创建会话
    if (!paperId && questionIds && localStorage.getItem('token')) {
      const moduleName = params.get('module') || ''
      const count = params.get('count') || questionIds.split(',').length
      try {
        const sessionRes = await sessionApi.create({
          module: moduleName,
          questionCount: Number(count)
        })
        if (sessionRes.success) {
          session.value = sessionRes.data
        }
      } catch (e) {
        console.warn('创建专项练习会话失败', e)
      }
    }
  } catch (e) {
    console.error('加载试卷失败', e)
    loaded.value = true
  }
}

function saveBeforeUnload() {
  if (!result.value) saveLocalState()
}

onMounted(() => {
  window.addEventListener('beforeunload', saveBeforeUnload)
  init()
})
onUnmounted(() => {
  window.removeEventListener('beforeunload', saveBeforeUnload)
  stopTimer()
  if (!result.value) {
    saveLocalState()
  } else {
    // Submitted — clear any saved special practice state
    const specialKey = getSpecialStateKey()
    if (specialKey) localStorage.removeItem(specialKey)
  }
})
</script>

<style scoped>
.online-practice { max-width: 100%; padding: 0; }

/* === 顶部栏 === */
.top-bar {
  display: flex; align-items: center; gap: 12px;
  padding: 10px 20px; background: var(--vp-c-bg-soft);
  border-bottom: 1px solid var(--vp-c-divider);
  margin-bottom: 16px;
}
.btn-back {
  font-size: 20px; text-decoration: none; color: var(--vp-c-text-1);
  padding: 4px 8px; border-radius: 4px; transition: background 0.2s;
}
.btn-back:hover { background: var(--vp-c-bg); }
.paper-title {
  flex: 1; font-size: 16px; font-weight: 600; margin: 0;
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}
.btn-submit-top {
  display: flex; align-items: center; gap: 4px;
  padding: 8px 16px; background: var(--vp-c-brand); color: #fff;
  border: none; border-radius: 6px; font-size: 14px; font-weight: 600;
  cursor: pointer; white-space: nowrap; transition: opacity 0.2s;
}
.btn-submit-top:hover { opacity: 0.9; }
.submit-icon { font-size: 14px; }
.btn-submitted {
  display: flex; align-items: center; gap: 4px;
  padding: 8px 16px; background: var(--vp-c-bg-soft);
  color: var(--vp-c-text-3); border: 1px solid var(--vp-c-divider);
  border-radius: 6px; font-size: 14px; font-weight: 600;
  cursor: not-allowed; white-space: nowrap; opacity: 0.7;
}

/* === 双栏布局 === */
.practice-layout {
  display: flex; gap: 20px; padding: 0 20px;
  align-items: flex-start;
}

/* === 左侧题目区 === */
.question-area { flex: 1; min-width: 0; position: relative; }
.pause-overlay {
  position: absolute; inset: 0; z-index: 10;
  background: rgba(0, 0, 0, 0.6); border-radius: 12px;
  display: flex; align-items: center; justify-content: center;
  cursor: pointer;
}
.pause-content {
  display: flex; flex-direction: column; align-items: center; gap: 8px;
  color: #fff;
}
.pause-icon { font-size: 48px; }
.pause-text { font-size: 20px; font-weight: 600; }
.pause-hint { font-size: 14px; opacity: 0.8; }
.material-panel {
  padding: 16px; background: var(--vp-c-bg-soft);
  border-radius: 8px; margin-bottom: 16px;
  max-height: 300px; overflow-y: auto;
}
.material-panel h3 { margin: 0 0 8px; font-size: 15px; }
.question-panel {
  padding: 24px; background: var(--vp-c-bg-soft); border-radius: 8px;
}
.question-content {
  font-size: 15px; line-height: 1.8; margin: 0 0 20px; font-weight: 500;
}
.question-content img { max-width: 200px; max-height: 80px; vertical-align: middle; }
.question-content img[flag="tex"] { display: inline !important; }
.options { display: flex; flex-direction: column; gap: 10px; }
.option-item {
  padding: 12px 14px; border: 1px solid var(--vp-c-divider);
  border-radius: 8px; cursor: pointer; transition: all 0.2s;
}
.option-item:hover { border-color: var(--vp-c-brand); }
.option-item.selected {
  border-color: var(--vp-c-brand); background: var(--vp-c-brand-dimm);
}
.option-label { font-weight: 600; margin-right: 4px; }
.option-body { line-height: 1.6; }
.option-body img { max-width: 200px; max-height: 80px; vertical-align: middle; }
.option-body img[flag="tex"] { display: inline !important; }
.fill-blank input {
  width: 100%; padding: 10px 12px; border: 1px solid var(--vp-c-divider);
  border-radius: 6px; font-size: 14px; background: var(--vp-c-bg);
  color: var(--vp-c-text-1);
}

/* === 翻页按钮 === */
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
.btn-restart {
  padding: 10px 20px; border: 1px solid var(--vp-c-brand);
  border-radius: 6px; cursor: pointer; background: var(--vp-c-brand);
  color: #fff; font-size: 14px; transition: all 0.2s;
}
.btn-restart:hover { opacity: 0.9; }
.btn-ai-analysis {
  display: block; width: 100%; margin-top: 12px;
  padding: 10px; border: 1px solid #f56c6c; border-radius: 6px;
  background: transparent; color: #f56c6c; font-size: 14px;
  cursor: pointer; transition: all 0.2s;
}
.btn-ai-analysis:hover { background: #f56c6c; color: #fff; }

/* === 答案解析 === */
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
.analysis-section.expanded .analysis-body {
  max-height: none;
}
.analysis-body p { margin: 6px 0; }
.analysis-body img { max-width: 200px; max-height: 80px; vertical-align: middle; }
.analysis-body img[flag="tex"] { display: inline !important; }
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

/* === 提交后选项样式 === */
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

/* === 右侧答题卡 === */
.answer-card-wrapper {
  width: 280px; flex-shrink: 0;
  position: sticky; top: 72px;
}
.answer-card {
  background: var(--vp-c-bg-soft); border-radius: 10px;
  border: 1px solid var(--vp-c-divider);
}
.answer-card::-webkit-scrollbar { width: 4px; }
.answer-card::-webkit-scrollbar-thumb { background: var(--vp-c-divider); border-radius: 2px; }
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
.btn-pause, .btn-resume {
  padding: 4px 10px; border: 1px solid var(--vp-c-divider);
  border-radius: 4px; cursor: pointer; background: var(--vp-c-bg);
  font-size: 13px; white-space: nowrap;
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
.legend-dot.current { background: var(--vp-c-brand); }
.legend-dot.answered { background: #67c23a; }
.legend-dot.correct { background: #67c23a; }
.legend-dot.wrong { background: #f56c6c; }
.card-grid {
  padding: 12px 16px;
  max-height: calc(100vh - 480px); overflow-y: auto;
}
.card-grid::-webkit-scrollbar { width: 4px; }
.card-grid::-webkit-scrollbar-thumb { background: var(--vp-c-divider); border-radius: 2px; }
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
.num-btn.answered {
  background: #67c23a; color: #fff; border-color: #67c23a;
}
.num-btn.correct {
  background: #67c23a; color: #fff; border-color: #67c23a;
}
.num-btn.wrong {
  background: #f56c6c; color: #fff; border-color: #f56c6c;
}
.num-btn.unanswered-result {
  background: var(--vp-c-bg); color: var(--vp-c-text-3);
  border-color: var(--vp-c-divider); opacity: 0.6;
}

/* === 答题卡提交后统计 === */
.card-result {
  background: var(--vp-c-bg-soft); border-radius: 10px;
  border: 1px solid var(--vp-c-divider);
  padding: 12px 16px; margin-top: 12px;
}
.card-result-actions {
  display: flex; gap: 10px; margin-bottom: 10px;
}
.btn-ai-sm {
  padding: 6px 12px; border: 1px solid var(--vp-c-brand);
  border-radius: 6px; background: var(--vp-c-brand); color: #fff;
  font-size: 12px; cursor: pointer;
}
.link-records {
  padding: 6px 12px; border: 1px solid var(--vp-c-divider);
  border-radius: 6px; background: var(--vp-c-bg); color: var(--vp-c-text-1);
  font-size: 12px; text-decoration: none;
}
.card-stats {
  display: grid; grid-template-columns: repeat(4, 1fr); gap: 8px;
}
.cs-item { text-align: center; }
.cs-value {
  display: block; font-size: 18px; font-weight: 700;
  color: var(--vp-c-text-1);
}
.cs-value.correct { color: #67c23a; }
.cs-value.wrong { color: #f56c6c; }
.cs-label { font-size: 11px; color: var(--vp-c-text-3); }

/* === 提交弹窗 === */
.submit-confirm { text-align: center; }
.submit-confirm h3 { margin: 0 0 12px; }
.submit-confirm .warn { color: #e6a23c; }
.submit-actions { display: flex; gap: 12px; justify-content: center; margin-top: 16px; }
.btn-primary {
  background: var(--vp-c-brand); color: #fff; border: none;
  padding: 8px 24px; border-radius: 6px; cursor: pointer; font-size: 14px;
}
.loading { text-align: center; padding: 40px; color: var(--vp-c-text-2); }

/* === 响应式 === */
@media (max-width: 960px) {
  .practice-layout { flex-direction: column; }
  .answer-card-wrapper {
    width: 100%; position: static;
  }
  .answer-card {
    max-height: none;
  }
  .number-grid { gap: 4px; }
  .num-btn { width: 28px; height: 28px; font-size: 11px; }
}
</style>
