<script setup>
import { ref, computed } from 'vue'
import { importApi, regionApi } from '../utils/api.js'

const step = ref(1)
const loading = ref(false)
const error = ref('')

const files = ref({ questions: null, answers: null, explanations: null })
const tempId = ref('')
const metadata = ref({})

const parsedData = ref(null)
const regions = ref([])
const loadingMsg = ref('')
const progressWidth = ref('0%')

const canUpload = computed(() =>
  files.value.questions && files.value.answers && files.value.explanations
)

const totalQuestions = computed(() => {
  if (!parsedData.value?.sections) return 0
  return parsedData.value.sections.reduce((sum, s) => sum + s.questions.length, 0)
})

function onFileChange(field, event) {
  files.value[field] = event.target.files[0]
}

async function handleUpload() {
  loading.value = true
  error.value = ''
  loadingMsg.value = '正在上传文件...'
  progressWidth.value = '10%'
  try {
    const res = await importApi.uploadFiles(files.value)
    tempId.value = res.temp_id
    metadata.value = res.metadata
    loadingMsg.value = '正在解析试卷结构...'
    progressWidth.value = '40%'
    step.value = 2
    const regionRes = await regionApi.list()
    regions.value = regionRes.data || regionRes
    loadingMsg.value = '正在下载图片并生成预览...'
    progressWidth.value = '60%'
    await handleParse()
    progressWidth.value = '100%'
  } catch (e) {
    error.value = e.message || '上传失败'
    step.value = 1
  } finally {
    loading.value = false
  }
}

async function handleParse() {
  loading.value = true
  error.value = ''
  try {
    const res = await importApi.parseFiles(tempId.value)
    parsedData.value = res
  } catch (e) {
    error.value = e.message || '解析失败'
  } finally {
    loading.value = false
  }
}

function getRegionId() {
  if (!metadata.value.region_name || !regions.value.length) return null
  const match = regions.value.find(r =>
    r.name.includes(metadata.value.region_name) ||
    metadata.value.region_name.includes(r.name) ||
    (metadata.value.region_name === '国考' && r.name === '国家')
  )
  return match ? match.id : null
}

async function handleConfirm() {
  loading.value = true
  error.value = ''
  loadingMsg.value = '正在导入试卷...'
  progressWidth.value = '80%'
  try {
    const payload = {
      metadata: {
        ...metadata.value,
        regionId: getRegionId(),
      },
      sections: parsedData.value.sections,
      materialGroups: parsedData.value.materialGroups || [],
    }
    await importApi.confirmImport(payload)
    step.value = 3
  } catch (e) {
    error.value = e.message || '导入失败'
  } finally {
    loading.value = false
  }
}

function reset() {
  step.value = 1
  files.value = { questions: null, answers: null, explanations: null }
  tempId.value = ''
  metadata.value = {}
  parsedData.value = null
  error.value = ''
}
</script>

<template>
  <div class="paper-import">
    <div class="steps">
      <div :class="['step-item', { active: step >= 1, done: step > 1 }]">
        <span class="step-num">1</span> 上传文件
      </div>
      <div class="step-line"></div>
      <div :class="['step-item', { active: step >= 2, done: step > 2 }]">
        <span class="step-num">2</span> 预览确认
      </div>
      <div class="step-line"></div>
      <div :class="['step-item', { active: step >= 3 }]">
        <span class="step-num">3</span> 完成
      </div>
    </div>

    <div v-if="error" class="error-msg">{{ error }}</div>

    <!-- Step 1: Upload -->
    <div v-if="step === 1 && !loading" class="upload-section">
      <div class="file-group">
        <label>试题文件</label>
        <input type="file" accept=".doc,.docx,.html" @change="onFileChange('questions', $event)" />
      </div>
      <div class="file-group">
        <label>答案文件</label>
        <input type="file" accept=".doc,.docx,.html" @change="onFileChange('answers', $event)" />
      </div>
      <div class="file-group">
        <label>解析文件</label>
        <input type="file" accept=".doc,.docx,.html" @change="onFileChange('explanations', $event)" />
      </div>
      <button class="btn-primary" :disabled="!canUpload" @click="handleUpload">
        上传并解析
      </button>
    </div>

    <!-- Loading -->
    <div v-if="loading" class="loading-section">
      <div class="spinner"></div>
      <p class="loading-text">{{ loadingMsg }}</p>
      <div class="progress-bar"><div class="progress-fill" :style="{ width: progressWidth }"></div></div>
    </div>

    <!-- Step 2: Preview -->
    <div v-if="step === 2 && parsedData && !loading" class="preview-section">
      <div class="meta-edit">
        <h3>试卷信息</h3>
        <div class="meta-grid">
          <div class="meta-field">
            <label>标题</label>
            <input v-model="metadata.title" />
          </div>
          <div class="meta-field">
            <label>年份</label>
            <input v-model.number="metadata.year" type="number" />
          </div>
          <div class="meta-field">
            <label>分类</label>
            <select v-model="metadata.category">
              <option value="行测">行测</option>
              <option value="申论">申论</option>
            </select>
          </div>
          <div class="meta-field">
            <label>地区</label>
            <input v-model="metadata.region_name" placeholder="国考/省名" />
          </div>
        </div>
      </div>

      <div class="summary">
        <h3>解析结果</h3>
        <p>共 {{ parsedData.sections?.length || 0 }} 个题型，{{ totalQuestions }} 道题目</p>
        <p v-if="parsedData.materialGroups?.length">
          含 {{ parsedData.materialGroups.length }} 个材料组
        </p>
      </div>

      <div class="sections-preview">
        <details v-for="section in parsedData.sections" :key="section.module" class="section-detail">
          <summary>
            <strong>{{ section.module }}</strong>（{{ section.questions.length }} 题）
          </summary>
          <div v-for="q in section.questions.slice(0, 3)" :key="q.sortOrder" class="question-preview">
            <p><strong>{{ q.sortOrder }}.</strong> <span v-html="q.content?.substring(0, 100)"></span>...</p>
            <p class="answer-preview">答案：{{ q.answer }}</p>
          </div>
          <p v-if="section.questions.length > 3" class="more-hint">
            ...等共 {{ section.questions.length }} 题
          </p>
        </details>
      </div>

      <div class="actions" v-if="!loading">
        <button class="btn-secondary" @click="reset">重新上传</button>
        <button class="btn-primary" @click="handleConfirm">确认导入</button>
      </div>
    </div>

    <!-- Step 3: Done -->
    <div v-if="step === 3" class="done-section">
      <div class="success-icon">&#10004;</div>
      <h3>导入成功！</h3>
      <p>共导入 {{ totalQuestions }} 道题目</p>
      <button class="btn-primary" @click="reset">继续导入</button>
    </div>
  </div>
</template>

<style scoped>
.paper-import {
  max-width: 900px;
  margin: 0 auto;
  padding: 20px;
}

.steps {
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 30px;
}

.step-item {
  display: flex;
  align-items: center;
  gap: 6px;
  color: var(--vp-c-text-3);
  font-size: 14px;
}

.step-item.active { color: var(--vp-c-brand); font-weight: 600; }
.step-item.done { color: var(--vp-c-brand); }

.step-num {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: var(--vp-c-default-soft);
  font-size: 12px;
}

.step-item.active .step-num,
.step-item.done .step-num {
  background: var(--vp-c-brand);
  color: white;
}

.step-line {
  width: 60px;
  height: 2px;
  background: var(--vp-c-divider);
  margin: 0 10px;
}

.error-msg {
  background: #fef2f2;
  color: #dc2626;
  padding: 10px 16px;
  border-radius: 6px;
  margin-bottom: 16px;
  font-size: 14px;
}

.upload-section,
.preview-section,
.done-section {
  background: var(--vp-c-bg-soft);
  border-radius: 12px;
  padding: 24px;
}

.file-group {
  margin-bottom: 16px;
}

.file-group label {
  display: block;
  font-weight: 600;
  margin-bottom: 6px;
  color: var(--vp-c-text-1);
}

.file-group input[type="file"] {
  width: 100%;
  padding: 8px;
  border: 1px dashed var(--vp-c-divider);
  border-radius: 6px;
  background: var(--vp-c-bg);
}

.btn-primary,
.btn-secondary {
  padding: 10px 24px;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  border: none;
  margin-top: 16px;
}

.btn-primary {
  background: var(--vp-c-brand);
  color: white;
}

.btn-primary:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn-secondary {
  background: var(--vp-c-default-soft);
  color: var(--vp-c-text-1);
}

.meta-edit {
  margin-bottom: 24px;
}

.meta-edit h3 {
  margin-bottom: 12px;
}

.meta-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.meta-field label {
  display: block;
  font-size: 13px;
  color: var(--vp-c-text-2);
  margin-bottom: 4px;
}

.meta-field input,
.meta-field select {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid var(--vp-c-divider);
  border-radius: 6px;
  background: var(--vp-c-bg);
  color: var(--vp-c-text-1);
}

.summary {
  margin-bottom: 20px;
}

.section-detail {
  margin-bottom: 8px;
  border: 1px solid var(--vp-c-divider);
  border-radius: 6px;
  padding: 12px;
}

.section-detail summary {
  cursor: pointer;
  font-size: 15px;
}

.question-preview {
  padding: 8px 0;
  border-bottom: 1px solid var(--vp-c-divider);
  font-size: 14px;
}

.answer-preview {
  color: var(--vp-c-brand);
  font-size: 13px;
}

.more-hint {
  color: var(--vp-c-text-3);
  font-size: 13px;
  margin-top: 8px;
}

.actions {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
}

.done-section {
  text-align: center;
  padding: 40px;
}

.success-icon {
  font-size: 48px;
  color: var(--vp-c-brand);
  margin-bottom: 16px;
}

.loading-section {
  text-align: center;
  padding: 60px 24px;
  background: var(--vp-c-bg-soft);
  border-radius: 12px;
}

.spinner {
  width: 40px;
  height: 40px;
  border: 3px solid var(--vp-c-divider);
  border-top-color: var(--vp-c-brand);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
  margin: 0 auto 20px;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.loading-text {
  font-size: 15px;
  color: var(--vp-c-text-2);
  margin-bottom: 20px;
}

.progress-bar {
  max-width: 320px;
  height: 6px;
  background: var(--vp-c-divider);
  border-radius: 3px;
  margin: 0 auto;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: var(--vp-c-brand);
  border-radius: 3px;
  transition: width 0.6s ease;
}
</style>
