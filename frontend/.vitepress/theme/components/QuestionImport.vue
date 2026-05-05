<template>
  <div class="qi-container">
    <!-- Non-admin block -->
    <div v-if="!isAdmin" class="qi-blocked">
      <div class="qi-blocked-icon">🔒</div>
      <h3>仅管理员可操作</h3>
      <p>题库导入功能仅限管理员使用，如需权限请联系管理员</p>
      <button class="qi-btn" @click="$router?.push('/') || (window.location.href = '/')">返回首页</button>
    </div>

    <!-- Step indicator -->
    <template v-if="isAdmin">
    <div class="qi-steps">
      <div :class="['qi-step', step >= 1 && 'active']">
        <span class="qi-step-num">1</span> 上传文件
      </div>
      <div class="qi-step-line" />
      <div :class="['qi-step', step >= 2 && 'active']">
        <span class="qi-step-num">2</span> AI解析
      </div>
      <div class="qi-step-line" />
      <div :class="['qi-step', step >= 3 && 'active']">
        <span class="qi-step-num">3</span> 审核导入
      </div>
    </div>

    <!-- Step 1: Upload -->
    <div v-if="step === 1" class="qi-upload">
      <div class="qi-form-grid">
        <div class="qi-upload-zone" @dragover.prevent @drop.prevent="onDropQuestion">
          <input ref="qFileInput" type="file" accept=".pdf" hidden @change="onQuestionFileChange" />
          <div v-if="!questionFile" class="qi-upload-placeholder" @click="qFileInput.click()">
            <div class="qi-upload-icon">📄</div>
            <p>点击或拖拽上传题目文件</p>
            <span class="qi-upload-hint">支持 PDF 格式</span>
          </div>
          <div v-else class="qi-upload-selected" @click="qFileInput.click()">
            <p>{{ questionFile.name }}</p>
            <span>{{ formatSize(questionFile.size) }}</span>
          </div>
        </div>

        <div class="qi-upload-zone" @dragover.prevent @drop.prevent="onDropAnswer">
          <input ref="aFileInput" type="file" accept=".pdf,.docx,.doc" hidden @change="onAnswerFileChange" />
          <div v-if="!answerFile" class="qi-upload-placeholder" @click="aFileInput.click()">
            <div class="qi-upload-icon">📝</div>
            <p>点击或拖拽上传答案解析文件</p>
            <span class="qi-upload-hint">支持 PDF / DOCX 格式</span>
          </div>
          <div v-else class="qi-upload-selected" @click="aFileInput.click()">
            <p>{{ answerFile.name }}</p>
            <span>{{ formatSize(answerFile.size) }}</span>
          </div>
        </div>
      </div>

      <div class="qi-meta-form">
        <div class="qi-field">
          <label>试卷标题</label>
          <input v-model="paperTitle" placeholder="如：2025年安徽省公务员录用考试《行测》" />
        </div>
        <div class="qi-field-row">
          <div class="qi-field">
            <label>年份</label>
            <input v-model.number="paperYear" type="number" />
          </div>
          <div class="qi-field">
            <label>分类</label>
            <select v-model="paperCategory">
              <option value="行测">行测</option>
              <option value="申论">申论</option>
            </select>
          </div>
          <div class="qi-field">
            <label>地区</label>
            <input v-model="regionName" placeholder="如：安徽、国考" />
          </div>
        </div>
        <div class="qi-field">
          <label>解析模型</label>
          <select v-model="selectedModel">
            <option :value="null">默认模型</option>
            <option v-for="m in models" :key="m.name" :value="m.name">
              {{ m.name }}{{ m.supports_vision ? ' (支持图片)' : '' }}
            </option>
          </select>
        </div>
      </div>

      <button
        class="qi-btn qi-btn-primary"
        :disabled="!canStartParse"
        @click="startParse"
      >
        开始解析
      </button>
    </div>

    <!-- Step 2: Parsing progress -->
    <div v-if="step === 2" class="qi-progress">
      <h3>AI 正在解析中...</h3>
      <div class="qi-progress-modules">
        <div v-for="(status, mod) in moduleStatus" :key="mod" :class="['qi-progress-item', status]">
          <span class="qi-progress-icon">
            {{ status === 'done' ? '✓' : status === 'extracting' ? '⟳' : status === 'failed' ? '✗' : '○' }}
          </span>
          <span>{{ mod }}</span>
          <span v-if="status === 'done'" class="qi-progress-count">{{ moduleCounts[mod] || 0 }}题</span>
        </div>
      </div>
      <p class="qi-progress-hint">首次解析可能需要 2-5 分钟，请耐心等待</p>
    </div>

    <!-- Step 3: Review & Edit -->
    <div v-if="step === 3" class="qi-review">
      <div class="qi-review-body">
        <div class="qi-review-sidebar">
          <div class="qi-review-stats">
            共 {{ questions.length }} 题 · {{ materialGroups.length }} 个材料组
          </div>
          <div class="qi-review-filter">
            <select v-model="filterModule">
              <option value="">全部模块</option>
              <option v-for="mod in uniqueModules" :key="mod" :value="mod">{{ mod }}</option>
            </select>
          </div>
          <div class="qi-review-list">
            <div
              v-for="(q, idx) in filteredQuestions"
              :key="idx"
              :class="['qi-review-item', selectedIdx === questions.indexOf(q) && 'active']"
              @click="selectedIdx = questions.indexOf(q)"
            >
              <span class="qi-review-num">{{ q.sort_order }}</span>
              <span class="qi-review-module">[{{ q.module }}]</span>
              <span class="qi-review-preview">{{ stripHtml(q.content).slice(0, 40) }}</span>
            </div>
          </div>
        </div>

        <div v-if="selectedQuestion" class="qi-review-editor">
          <div class="qi-editor-section">
            <label>题目内容</label>
            <textarea v-model="selectedQuestion.content" rows="5" />
          </div>
          <div class="qi-editor-section">
            <label>选项</label>
            <div v-for="(opt, oi) in selectedQuestion.options" :key="oi" class="qi-option-row">
              <input v-model="opt.label" class="qi-option-label" />
              <input v-model="opt.text" class="qi-option-text" />
            </div>
          </div>
          <div class="qi-editor-row">
            <div class="qi-editor-section">
              <label>正确答案</label>
              <input v-model="selectedQuestion.answer" />
            </div>
            <div class="qi-editor-section">
              <label>题型</label>
              <select v-model="selectedQuestion.type">
                <option value="single_choice">单选</option>
                <option value="multi_choice">多选</option>
              </select>
            </div>
            <div class="qi-editor-section">
              <label>分值</label>
              <input v-model.number="selectedQuestion.score" type="number" step="0.1" />
            </div>
          </div>
          <div class="qi-editor-row">
            <div class="qi-editor-section">
              <label>模块</label>
              <select v-model="selectedQuestion.module">
                <option v-for="mod in allModules" :key="mod" :value="mod">{{ mod }}</option>
              </select>
            </div>
            <div class="qi-editor-section">
              <label>子模块</label>
              <input v-model="selectedQuestion.sub_module" placeholder="如：片段阅读" />
            </div>
          </div>
          <div class="qi-editor-section">
            <label>解析</label>
            <textarea v-model="selectedQuestion.explanation" rows="6" />
          </div>
          <div class="qi-editor-actions">
            <button @click="prevQuestion">上一题</button>
            <button @click="nextQuestion">下一题</button>
            <button class="qi-btn-danger" @click="deleteQuestion">删除此题</button>
          </div>
        </div>
        <div v-else class="qi-review-empty">
          选择左侧题目进行编辑
        </div>
      </div>

      <div class="qi-review-footer">
        <button class="qi-btn" @click="step = 1">返回上传</button>
        <button class="qi-btn qi-btn-primary" :disabled="questions.length === 0" @click="confirmImport">
          确认导入 ({{ questions.length }} 题)
        </button>
      </div>
    </div>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { importApi, isLoggedIn } from '../utils/api.js'

const step = ref(1)
const questionFile = ref(null)
const answerFile = ref(null)
const qFileInput = ref(null)
const aFileInput = ref(null)
const paperTitle = ref('')
const paperYear = ref(new Date().getFullYear())
const paperCategory = ref('行测')
const regionName = ref('')
const selectedModel = ref(null)
const models = ref([])

// Parse results
const questions = ref([])
const materialGroups = ref([])
const parseStats = ref({})
const moduleStatus = ref({})
const moduleCounts = ref({})

// Review state
const selectedIdx = ref(0)
const filterModule = ref('')

const selectedQuestion = computed(() => {
  return questions.value[selectedIdx.value] || null
})

const canStartParse = computed(() => {
  return questionFile.value && answerFile.value && paperTitle.value
})

const uniqueModules = computed(() => {
  return [...new Set(questions.value.map(q => q.module).filter(Boolean))]
})

const allModules = ['政治理论', '常识判断', '言语理解与表达', '数量关系', '判断推理', '资料分析']

const filteredQuestions = computed(() => {
  if (!filterModule.value) return questions.value
  return questions.value.filter(q => q.module === filterModule.value)
})

function onQuestionFileChange(e) {
  questionFile.value = e.target.files[0] || null
  if (questionFile.value) {
    const name = questionFile.value.name.replace(/\.\w+$/, '')
    if (!paperTitle.value) paperTitle.value = name
    const yearMatch = name.match(/(\d{4})/)
    if (yearMatch) paperYear.value = parseInt(yearMatch[1])
  }
}

function onAnswerFileChange(e) {
  answerFile.value = e.target.files[0] || null
}

function onDropQuestion(e) {
  const files = e.dataTransfer.files
  if (files.length) {
    questionFile.value = files[0]
  }
}

function onDropAnswer(e) {
  const files = e.dataTransfer.files
  if (files.length) {
    answerFile.value = files[0]
  }
}

function formatSize(bytes) {
  if (!bytes) return ''
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}

function stripHtml(html) {
  return html ? html.replace(/<[^>]+>/g, '') : ''
}

async function startParse() {
  step.value = 2
  moduleStatus.value = {}
  moduleCounts.value = {}

  const formData = new FormData()
  formData.append('question_file', questionFile.value)
  formData.append('answer_file', answerFile.value)
  formData.append('paper_title', paperTitle.value)
  formData.append('paper_year', paperYear.value)
  formData.append('paper_category', paperCategory.value)
  if (regionName.value) formData.append('region_name', regionName.value)
  if (selectedModel.value) formData.append('model_name', selectedModel.value)

  try {
    const { stream, getResult } = importApi.parseStream(formData)
    for await (const event of stream) {
      if (event.type === 'progress') {
        moduleStatus.value[event.module] = event.status
        if (event.count > 0) moduleCounts.value[event.module] = event.count
      } else if (event.type === 'error') {
        throw new Error(event.message)
      }
      // 'complete' event ends the stream
    }
    const res = getResult()
    questions.value = res.questions || []
    materialGroups.value = res.material_groups || []
    parseStats.value = res.stats || {}
    setTimeout(() => { step.value = 3 }, 800)
  } catch (err) {
    alert('解析失败: ' + (err.response?.data?.detail || err.message))
    step.value = 1
  }
}

function prevQuestion() {
  if (selectedIdx.value > 0) selectedIdx.value--
}

function nextQuestion() {
  if (selectedIdx.value < questions.value.length - 1) selectedIdx.value++
}

function deleteQuestion() {
  questions.value.splice(selectedIdx.value, 1)
  if (selectedIdx.value >= questions.value.length) {
    selectedIdx.value = Math.max(0, questions.value.length - 1)
  }
}

async function confirmImport() {
  if (!confirm(`确认导入 ${questions.value.length} 道题目？`)) return

  const payload = {
    paper: {
      title: paperTitle.value,
      year: paperYear.value,
      category: paperCategory.value,
      regionName: regionName.value || null,
    },
    questions: questions.value.map(q => ({
      sort_order: q.sort_order,
      module: q.module,
      sub_module: q.sub_module || null,
      type: q.type || 'single_choice',
      content: q.content,
      options: JSON.stringify(q.options || []),
      answer: q.answer,
      explanation: q.explanation || '',
      images: JSON.stringify(q.images || []),
      score: q.score || 0.8,
    })),
    materialGroups: materialGroups.value.map((mg, i) => ({
      sort_order: mg.sort_order || i + 1,
      title: mg.title || '',
      content: mg.content || '',
      images: JSON.stringify(mg.images || []),
    })),
  }

  try {
    const res = await importApi.confirmImport(payload)
    alert(`导入成功！试卷 ID: ${res.data?.paperId || res.paperId}`)
    window.location.href = '/题库/'
  } catch (err) {
    alert('导入失败: ' + (err.response?.data?.message || err.message))
  }
}

const isAdmin = ref(true)

onMounted(async () => {
  if (!isLoggedIn()) {
    window.location.href = '/login/?redirect=/admin/import/'
    return
  }
  const user = JSON.parse(localStorage.getItem('user') || '{}')
  if (user.role !== 'admin') {
    isAdmin.value = false
    return
  }
  try {
    const modelRes = await importApi.getModels()
    models.value = modelRes.models || []
  } catch (e) {
    console.warn('Failed to load models', e)
  }
})
</script>

<style scoped>
.qi-blocked { text-align: center; padding: 80px 20px; }
.qi-blocked-icon { font-size: 48px; margin-bottom: 16px; }
.qi-blocked h3 { margin: 0 0 8px; color: #333; }
.qi-blocked p { color: #666; margin: 0 0 20px; }

.qi-container { max-width: 1200px; margin: 0 auto; padding: 20px; font-size: 14px; }

.qi-steps { display: flex; align-items: center; justify-content: center; gap: 0; margin-bottom: 32px; }
.qi-step { display: flex; align-items: center; gap: 8px; color: #999; font-size: 15px; }
.qi-step.active { color: #333; font-weight: 600; }
.qi-step-num { display: inline-flex; width: 28px; height: 28px; border-radius: 50%; background: #eee; color: #999; align-items: center; justify-content: center; font-size: 13px; }
.qi-step.active .qi-step-num { background: #4a6cf7; color: #fff; }
.qi-step-line { width: 60px; height: 2px; background: #eee; margin: 0 8px; }

.qi-form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; margin-bottom: 20px; }
.qi-upload-zone { border: 2px dashed #d9d9d9; border-radius: 8px; padding: 32px; text-align: center; cursor: pointer; transition: border-color 0.2s; min-height: 140px; display: flex; align-items: center; justify-content: center; }
.qi-upload-zone:hover { border-color: #4a6cf7; }
.qi-upload-placeholder { color: #666; }
.qi-upload-icon { font-size: 32px; margin-bottom: 8px; }
.qi-upload-hint { color: #999; font-size: 12px; }
.qi-upload-selected p { font-weight: 600; margin-bottom: 4px; word-break: break-all; }
.qi-upload-selected span { color: #999; font-size: 12px; }

.qi-meta-form { background: #f9f9fb; border-radius: 8px; padding: 20px; margin-bottom: 20px; }
.qi-field { margin-bottom: 12px; }
.qi-field label { display: block; font-weight: 600; margin-bottom: 4px; font-size: 13px; color: #555; }
.qi-field input, .qi-field select { width: 100%; padding: 8px 12px; border: 1px solid #d9d9d9; border-radius: 6px; font-size: 14px; box-sizing: border-box; }
.qi-field-row { display: grid; grid-template-columns: 1fr 1fr 1fr; gap: 12px; }

.qi-btn { padding: 10px 24px; border: 1px solid #d9d9d9; border-radius: 6px; background: #fff; cursor: pointer; font-size: 14px; }
.qi-btn:disabled { opacity: 0.5; cursor: not-allowed; }
.qi-btn-primary { background: #4a6cf7; color: #fff; border-color: #4a6cf7; }
.qi-btn-primary:hover:not(:disabled) { background: #3b5de8; }
.qi-btn-danger { background: #ff4d4f; color: #fff; border-color: #ff4d4f; }

.qi-progress { text-align: center; padding: 60px 20px; }
.qi-progress h3 { margin-bottom: 24px; }
.qi-progress-modules { display: inline-block; text-align: left; }
.qi-progress-item { padding: 8px 0; display: flex; align-items: center; gap: 8px; }
.qi-progress-item.done { color: #52c41a; }
.qi-progress-item.extracting { color: #4a6cf7; }
.qi-progress-item.failed { color: #ff4d4f; }
.qi-progress-count { color: #999; font-size: 12px; margin-left: auto; }
.qi-progress-hint { color: #999; margin-top: 24px; font-size: 13px; }

.qi-review-body { display: flex; gap: 16px; min-height: 0; }

.qi-review-sidebar { width: 280px; flex-shrink: 0; background: #f9f9fb; border-radius: 8px; padding: 12px; max-height: 70vh; overflow-y: auto; }
.qi-review-stats { font-size: 13px; color: #666; margin-bottom: 8px; padding-bottom: 8px; border-bottom: 1px solid #eee; }
.qi-review-filter { margin-bottom: 8px; }
.qi-review-filter select { width: 100%; padding: 6px; border: 1px solid #d9d9d9; border-radius: 4px; font-size: 13px; }
.qi-review-list { display: flex; flex-direction: column; gap: 2px; }
.qi-review-item { padding: 6px 8px; border-radius: 4px; cursor: pointer; display: flex; align-items: center; gap: 6px; font-size: 13px; white-space: nowrap; overflow: hidden; }
.qi-review-item:hover { background: #e8ecff; }
.qi-review-item.active { background: #4a6cf7; color: #fff; }
.qi-review-num { font-weight: 600; min-width: 24px; }
.qi-review-module { font-size: 11px; color: #999; }
.qi-review-item.active .qi-review-module { color: rgba(255,255,255,0.7); }
.qi-review-preview { overflow: hidden; text-overflow: ellipsis; }

.qi-review-editor { flex: 1; background: #fff; border: 1px solid #eee; border-radius: 8px; padding: 20px; overflow-y: auto; max-height: 70vh; }
.qi-editor-section { margin-bottom: 16px; }
.qi-editor-section label { display: block; font-weight: 600; margin-bottom: 4px; font-size: 13px; color: #555; }
.qi-editor-section textarea, .qi-editor-section input, .qi-editor-section select { width: 100%; padding: 8px 12px; border: 1px solid #d9d9d9; border-radius: 6px; font-size: 14px; box-sizing: border-box; font-family: inherit; }
.qi-editor-section textarea { resize: vertical; }
.qi-editor-row { display: grid; grid-template-columns: repeat(auto-fit, minmax(150px, 1fr)); gap: 12px; margin-bottom: 16px; }
.qi-option-row { display: flex; align-items: center; gap: 8px; margin-bottom: 6px; }
.qi-option-label { width: 36px !important; text-align: center; font-weight: 600; }
.qi-option-text { flex: 1; }
.qi-editor-actions { display: flex; gap: 8px; margin-top: 16px; padding-top: 16px; border-top: 1px solid #eee; }
.qi-editor-actions button { padding: 6px 16px; border: 1px solid #d9d9d9; border-radius: 4px; cursor: pointer; font-size: 13px; background: #fff; }

.qi-review-empty { flex: 1; display: flex; align-items: center; justify-content: center; color: #999; background: #f9f9fb; border-radius: 8px; min-height: 300px; }

.qi-review-footer { display: flex; justify-content: space-between; align-items: center; padding-top: 16px; border-top: 1px solid #eee; margin-top: 16px; }

@media (max-width: 768px) {
  .qi-form-grid { grid-template-columns: 1fr; }
  .qi-field-row { grid-template-columns: 1fr; }
  .qi-review-body { flex-direction: column; }
  .qi-review-sidebar { width: 100%; max-height: 200px; }
  .qi-review-editor { max-height: none; }
}
</style>
