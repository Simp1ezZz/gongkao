<template>
  <div class="online-practice" v-if="loaded">
    <!-- 顶部信息栏 -->
    <div class="top-bar">
      <div class="paper-info">
        <h2>{{ paperDetail.title }}</h2>
        <span class="meta">
          {{ paperDetail.questionCount }}题 |
          {{ paperDetail.year }}年 |
          {{ paperDetail.regionName || '通用' }}
        </span>
      </div>
      <div class="timer" v-if="session">
        <span class="time">{{ formatTime(timeElapsed) }}</span>
        <button v-if="session?.status === 'ongoing'" class="btn-pause"
                @click="togglePause">暂停</button>
        <button v-if="session?.status === 'paused'" class="btn-resume"
                @click="togglePause">继续</button>
      </div>
    </div>

    <!-- 登录弹窗 -->
    <Modal v-if="showLoginModal" @close="showLoginModal = false">
      <Login inline @login-success="onLoginSuccess" />
    </Modal>

    <!-- 材料区域 -->
    <div v-if="currentMaterial" class="material-panel">
      <h3>{{ currentMaterial.title }}</h3>
      <div class="material-content" v-html="currentMaterial.content"></div>
    </div>

    <!-- 题目区域 -->
    <div class="question-panel" v-if="currentQuestion">
      <div class="question-header">
        <span class="question-index">{{ currentIndex + 1 }} / {{ questions.length }}</span>
        <span class="question-module">{{ currentQuestion.module }}</span>
        <span class="question-type">{{ typeLabel(currentQuestion.type) }}</span>
      </div>

      <div class="question-content" v-html="currentQuestion.content"></div>

      <!-- 选项（选择题） -->
      <div v-if="hasOptions" class="options">
        <div v-for="opt in parsedOptions" :key="opt.label"
             class="option-item"
             :class="{ selected: isOptionSelected(opt.label) }"
             @click="selectAnswer(opt.label)">
          <span class="option-label">{{ opt.label }}.</span>
          <span class="option-text">{{ opt.text }}</span>
        </div>
      </div>

      <!-- 填空题 -->
      <div v-if="currentQuestion.type === 'fill_blank'" class="fill-blank">
        <input v-model="answers[currentQuestion.id]" placeholder="请输入答案" />
      </div>
    </div>

    <!-- 底部导航 -->
    <div class="bottom-bar">
      <button :disabled="currentIndex <= 0" @click="prevQuestion">上一题</button>
      <div class="progress">
        <div class="progress-bar" :style="{ width: progressPercent + '%' }"></div>
      </div>
      <span class="answered-count">{{ answeredCount }} / {{ questions.length }} 已答</span>
      <button v-if="currentIndex < questions.length - 1"
              @click="nextQuestion">下一题</button>
      <button v-else class="btn-submit" @click="confirmSubmit">提交试卷</button>
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

    <!-- 提交结果 -->
    <div v-if="result" class="result-panel">
      <h3>练习结果</h3>
      <div class="result-stats">
        <div class="stat-item">
          <span class="stat-value">{{ result.correctCount }}</span>
          <span class="stat-label">正确</span>
        </div>
        <div class="stat-item wrong">
          <span class="stat-value">{{ result.wrongCount }}</span>
          <span class="stat-label">错误</span>
        </div>
        <div class="stat-item">
          <span class="stat-value">{{ result.accuracy }}%</span>
          <span class="stat-label">正确率</span>
        </div>
      </div>

      <!-- 逐题解析 -->
      <div class="result-questions">
        <div v-for="(q, idx) in result.questions" :key="q.id"
             class="result-question"
             :class="{ correct: q.isCorrect, wrong: q.isCorrect === false }">
          <div class="rq-header">
            <span>第{{ idx + 1 }}题</span>
            <span v-if="q.isCorrect === true" class="badge-correct">正确</span>
            <span v-else-if="q.isCorrect === false" class="badge-wrong">错误</span>
            <span v-else class="badge-pending">待批改</span>
          </div>
          <div class="rq-content" v-html="q.content"></div>
          <div class="rq-answer">
            <p>你的答案：<strong>{{ q.userAnswer || '未作答' }}</strong></p>
            <p>正确答案：<strong>{{ q.answer }}</strong></p>
          </div>
          <div v-if="q.explanation" class="rq-explanation">
            <h4>解析</h4>
            <div v-html="q.explanation"></div>
          </div>
        </div>
      </div>
    </div>
  </div>
  <div v-else class="loading">加载中...</div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { paperApi, sessionApi } from '../utils/api.js'
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
  try {
    return JSON.parse(q.options)
  } catch { return [] }
})

const hasOptions = computed(() => {
  const q = currentQuestion.value
  return q && (q.type === 'single_choice' || q.type === 'multi_choice')
})

const answeredCount = computed(() => Object.keys(answers.value).length)

const progressPercent = computed(() =>
  questions.value.length > 0
    ? Math.round((answeredCount.value / questions.value.length) * 100)
    : 0
)

function typeLabel(type) {
  const map = {
    single_choice: '单选题',
    multi_choice: '多选题',
    fill_blank: '填空题',
    essay: '主观题'
  }
  return map[type] || type
}

function formatTime(seconds) {
  const h = Math.floor(seconds / 3600)
  const m = Math.floor((seconds % 3600) / 60)
  const s = seconds % 60
  return h > 0
    ? `${h}:${String(m).padStart(2,'0')}:${String(s).padStart(2,'0')}`
    : `${m}:${String(s).padStart(2,'0')}`
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

function prevQuestion() {
  if (currentIndex.value > 0) currentIndex.value--
}

function nextQuestion() {
  if (currentIndex.value < questions.value.length - 1) currentIndex.value++
}

async function togglePause() {
  if (!session.value) return
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
    showLoginModal.value = true
    return
  }
  showSubmitModal.value = true
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
      // 合并服务端已有进度
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
      if (sessionRes.data.status === 'ongoing') startTimer()
    }
  } catch (e) {
    console.warn('创建会话失败', e)
  }
}

async function submitExam() {
  showSubmitModal.value = false
  stopTimer()

  try {
    const answerItems = Object.entries(answers.value).map(([qId, ans]) => ({
      questionId: Number(qId), answer: ans
    }))

    // 题库模式：先提交会话再提交答案
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
    }
  } catch (e) {
    console.error('提交失败', e)
  }
}

function startTimer() {
  stopTimer()
  timer = setInterval(() => { timeElapsed.value++ }, 1000)
}

function stopTimer() {
  if (timer) { clearInterval(timer); timer = null }
  if (saveTimer) { clearTimeout(saveTimer); saveTimer = null }
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
      // 题库模式：通过 paperId 加载整张试卷
      const detailRes = await paperApi.getDetail(paperId)
      if (!detailRes.success) { loaded.value = true; return }
      paperDetail.value = detailRes.data
      questions.value = detailRes.data.questions || []
      materials.value = detailRes.data.materials || []
    } else {
      // 专项练习模式：题目数据由 PaperList 通过 localStorage 传递
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

    // 恢复之前保存的做题状态（从登录页返回时）
    const savedState = localStorage.getItem('practiceState')
    if (savedState) {
      try {
        const state = JSON.parse(savedState)
        if (state.answers) answers.value = state.answers
        if (state.currentIndex) currentIndex.value = state.currentIndex
        if (state.timeElapsed) timeElapsed.value = state.timeElapsed
        localStorage.removeItem('practiceState')
      } catch {}
    }

    // 只有题库模式 + 已登录 才创建会话和计时
    if (!paperId || !localStorage.getItem('token')) return

    try {
      const sessionRes = await sessionApi.create({ paperId: Number(paperId) })
      if (sessionRes.success) {
        session.value = sessionRes.data
        if (sessionRes.data.answers) {
          try {
            const savedAnswers = JSON.parse(sessionRes.data.answers)
            savedAnswers.forEach(a => { answers.value[a.questionId] = a.answer })
          } catch {}
        }
        currentIndex.value = sessionRes.data.currentIndex || 0
        timeElapsed.value = sessionRes.data.timeElapsed || 0

        if (sessionRes.data.status === 'ongoing') startTimer()
      }
    } catch (e) {
      console.warn('创建会话失败，切换到浏览模式', e)
    }
  } catch (e) {
    console.error('加载试卷失败', e)
    loaded.value = true
  }
}

onMounted(init)
onUnmounted(stopTimer)
</script>

<style scoped>
.online-practice { max-width: 960px; margin: 0 auto; padding: 20px; }
.top-bar {
  display: flex; justify-content: space-between; align-items: center;
  padding: 12px 16px; background: var(--vp-c-bg-soft); border-radius: 8px;
  margin-bottom: 16px;
}
.paper-info h2 { font-size: 18px; margin: 0 0 4px; }
.paper-info .meta { font-size: 13px; color: var(--vp-c-text-2); }
.timer { display: flex; align-items: center; gap: 8px; }
.time { font-size: 20px; font-weight: 600; font-variant-numeric: tabular-nums; }
.btn-pause, .btn-resume {
  padding: 4px 12px; border: 1px solid var(--vp-c-divider);
  border-radius: 4px; cursor: pointer; background: var(--vp-c-bg);
}
.material-panel {
  padding: 16px; background: var(--vp-c-bg-soft);
  border-radius: 8px; margin-bottom: 16px;
  max-height: 300px; overflow-y: auto;
}
.material-panel h3 { margin: 0 0 8px; font-size: 15px; }
.question-panel {
  padding: 20px; background: var(--vp-c-bg-soft);
  border-radius: 8px; margin-bottom: 16px;
}
.question-header {
  display: flex; gap: 12px; margin-bottom: 12px;
  font-size: 13px; color: var(--vp-c-text-2);
}
.question-content { font-size: 15px; line-height: 1.8; margin-bottom: 16px; }
.options { display: flex; flex-direction: column; gap: 8px; }
.option-item {
  display: flex; align-items: flex-start; gap: 8px;
  padding: 10px 12px; border: 1px solid var(--vp-c-divider);
  border-radius: 6px; cursor: pointer; transition: all 0.2s;
}
.option-item:hover { border-color: var(--vp-c-brand); }
.option-item.selected {
  border-color: var(--vp-c-brand); background: var(--vp-c-brand-dimm);
}
.option-label { font-weight: 600; min-width: 24px; }
.fill-blank input {
  width: 100%; padding: 8px 12px; border: 1px solid var(--vp-c-divider);
  border-radius: 4px; font-size: 14px;
}
.bottom-bar {
  display: flex; align-items: center; gap: 12px;
  padding: 12px 16px; background: var(--vp-c-bg-soft);
  border-radius: 8px;
}
.bottom-bar button {
  padding: 6px 16px; border: 1px solid var(--vp-c-divider);
  border-radius: 4px; cursor: pointer; background: var(--vp-c-bg);
}
.bottom-bar button:disabled { opacity: 0.5; cursor: not-allowed; }
.btn-submit { background: var(--vp-c-brand) !important; color: #fff !important; border: none !important; }
.btn-primary { background: var(--vp-c-brand); color: #fff; border: none; padding: 8px 24px; border-radius: 4px; cursor: pointer; }
.progress { flex: 1; height: 6px; background: var(--vp-c-divider); border-radius: 3px; overflow: hidden; }
.progress-bar { height: 100%; background: var(--vp-c-brand); transition: width 0.3s; }
.answered-count { font-size: 13px; color: var(--vp-c-text-2); white-space: nowrap; }
.submit-confirm { text-align: center; }
.submit-confirm h3 { margin: 0 0 12px; }
.submit-confirm .warn { color: #e6a23c; }
.submit-actions { display: flex; gap: 12px; justify-content: center; margin-top: 16px; }
.result-panel { margin-top: 24px; }
.result-panel h3 { font-size: 18px; margin-bottom: 16px; }
.result-stats { display: flex; gap: 24px; margin-bottom: 24px; }
.stat-item { text-align: center; padding: 16px; background: var(--vp-c-bg-soft); border-radius: 8px; min-width: 100px; }
.stat-item.wrong .stat-value { color: #f56c6c; }
.stat-value { display: block; font-size: 28px; font-weight: 700; }
.stat-label { font-size: 13px; color: var(--vp-c-text-2); }
.result-questions { display: flex; flex-direction: column; gap: 16px; }
.result-question {
  padding: 16px; border-radius: 8px; border-left: 4px solid var(--vp-c-divider);
}
.result-question.correct { border-left-color: #67c23a; }
.result-question.wrong { border-left-color: #f56c6c; }
.rq-header { display: flex; gap: 8px; align-items: center; margin-bottom: 8px; }
.badge-correct { color: #67c23a; font-size: 13px; font-weight: 600; }
.badge-wrong { color: #f56c6c; font-size: 13px; font-weight: 600; }
.badge-pending { color: #e6a23c; font-size: 13px; }
.rq-content { margin-bottom: 8px; }
.rq-answer p { margin: 4px 0; font-size: 14px; }
.rq-explanation { margin-top: 8px; padding-top: 8px; border-top: 1px solid var(--vp-c-divider); }
.rq-explanation h4 { margin: 0 0 4px; font-size: 14px; }
.loading { text-align: center; padding: 40px; color: var(--vp-c-text-2); }
</style>
