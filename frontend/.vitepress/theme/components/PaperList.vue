<template>
  <div class="paper-list">
    <!-- 题库模式 -->
    <template v-if="mode === 'bank'">
      <!-- 地区筛选：pill 按钮 -->
      <div class="filter-tags">
        <button :class="['filter-tag', { active: !filters.regionName }]"
                @click="selectRegion(null)">全部</button>
        <button v-for="name in regions" :key="name"
                :class="['filter-tag', { active: filters.regionName === name }]"
                @click="selectRegion(name)">
          {{ name }}
        </button>
      </div>

      <!-- 加载中 -->
      <div v-if="loading" class="loading">加载中...</div>

      <!-- 试卷列表 -->
      <div v-else class="papers">
        <div v-for="paper in papers" :key="paper.id" class="paper-card"
             @click="startBankPractice(paper)">
          <div class="paper-main">
            <div class="paper-title">{{ paper.title }}</div>
            <span v-if="paper.regionName" class="region-tag">{{ paper.regionName }}</span>
          </div>
          <div class="paper-rating">{{ '★'.repeat(paper.rating || 0) }}</div>
        </div>
        <Empty v-if="papers.length === 0 && !loading" text="暂无试卷" />
      </div>

      <!-- 加载更多 -->
      <button v-if="total > papers.length" class="btn-load-more" @click="loadMore"
              :disabled="loadingMore">
        {{ loadingMore ? '加载中...' : `📥 加载更多 (已加载 ${papers.length}/${total})` }}
      </button>

      <div class="bank-footer">
        <a href="/practice/special/" class="link-special">📝 专项练习</a>
      </div>
    </template>

    <!-- 专项练习模式 -->
    <template v-else>
      <div class="special-header">
        <h1>💪 专项练习</h1>
        <p>动态随机出题，提升你的公考能力</p>
      </div>

      <!-- 加载中 -->
      <div v-if="loading" class="loading">加载中...</div>

      <div v-else class="module-grid">
        <div v-for="mod in modules" :key="mod.value" class="module-card">
          <div class="module-icon">{{ mod.emoji }}</div>
          <h3>{{ mod.name }}</h3>
          <p>{{ mod.desc }}</p>
          <div class="module-actions">
            <button v-for="count in [10, 20, 30]" :key="count"
                    class="btn-count"
                    @click="startSpecial(mod.value, count)">
              {{ count }} 题
            </button>
            <div class="custom-count">
              <input type="number" min="1" max="200"
                     v-model.number="customCounts[mod.value]"
                     @keyup.enter="startSpecial(mod.value, customCounts[mod.value])"
                     placeholder="自定义" />
              <button class="btn-count btn-go"
                      @click="startSpecial(mod.value, customCounts[mod.value])">题</button>
            </div>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { paperApi } from '../utils/api.js'
import Empty from './Empty.vue'

const props = defineProps({
  mode: { type: String, default: 'bank' },
  category: { type: String, default: '行测' }
})

const modules = [
  { name: '言语理解', emoji: '📖', desc: '词汇积累、语句表达、片段阅读', value: '言语理解' },
  { name: '数量关系', emoji: '🔢', desc: '数字推理、数学运算、应用题', value: '数量关系' },
  { name: '判断推理', emoji: '🧩', desc: '图形推理、定义判断、类比推理', value: '判断推理' },
  { name: '资料分析', emoji: '📊', desc: '文字资料、表格资料、图形资料', value: '资料分析' },
  { name: '常识判断', emoji: '🌍', desc: '政治、经济、文化、科技、法律', value: '常识判断' },
  { name: '政治理论', emoji: '🏛️', desc: '马克思主义、毛泽东思想、中国特色社会主义', value: '政治理论' },
]

const papers = ref([])
const loading = ref(false)
const loadingMore = ref(false)
const page = ref(1)
const pageSize = 25
const total = ref(0)
const customCounts = reactive({})

const filters = ref({
  regionName: null,
  module: ''
})

// 从已加载的试卷中聚合去重的地区列表
const regions = computed(() => {
  const set = new Set()
  papers.value.forEach(p => { if (p.regionName) set.add(p.regionName) })
  // 总是包含已知的常见地区
  return [...set]
})

// --- 题库模式 ---

function selectRegion(name) {
  if (name === null) {
    filters.value.regionName = null
  } else {
    filters.value.regionName = filters.value.regionName === name ? null : name
  }
  page.value = 1
  papers.value = []
  loadPapers()
}

async function loadPapers() {
  loading.value = true
  try {
    const params = {
      category: props.category,
      regionName: filters.value.regionName,
      page: page.value,
      pageSize
    }
    const res = await paperApi.list(params)
    if (res.success) {
      if (page.value === 1) {
        papers.value = res.data.list
      } else {
        papers.value.push(...res.data.list)
      }
      total.value = res.data.total
    }
  } catch (e) {
    console.error('加载试卷失败', e)
  } finally {
    loading.value = false
  }
}

async function loadMore() {
  page.value++
  loadingMore.value = true
  try {
    const params = {
      category: props.category,
      regionName: filters.value.regionName,
      page: page.value,
      pageSize
    }
    const res = await paperApi.list(params)
    if (res.success) {
      papers.value.push(...res.data.list)
      total.value = res.data.total
    }
  } catch (e) {
    console.error('加载更多失败', e)
    page.value--
  } finally {
    loadingMore.value = false
  }
}

function startBankPractice(paper) {
  window.location.href = `/practice/online/?paperId=${paper.id}`
}

// --- 专项练习模式 ---

async function startSpecial(moduleName, count) {
  if (!count || count < 1) return
  loading.value = true
  try {
    // Check if there's a saved state for the same module+count — resume it
    const stateKey = `specialState_${moduleName}_${count}`
    const saved = localStorage.getItem(stateKey)
    if (saved) {
      try {
        const state = JSON.parse(saved)
        if (state.questionIds && state.questionIds.length === count) {
          // Re-store questions for OnlinePractice to read, and navigate
          localStorage.setItem('specialQuestions', JSON.stringify(state.questions))
          window.location.href = `/practice/online/?questionIds=${state.questionIds.join(',')}&module=${encodeURIComponent(moduleName)}&count=${count}`
          return
        }
      } catch {}
      // Saved state is invalid or count mismatch — remove it
      localStorage.removeItem(stateKey)
    }

    // No saved state or different count — fetch new questions
    const res = await paperApi.getQuestionsByKnowledge({ module: moduleName, limit: count })
    if (res.success && res.data?.length > 0) {
      const questions = res.data
      localStorage.setItem('specialQuestions', JSON.stringify(questions))
      const qIds = questions.map(q => q.id).join(',')
      window.location.href = `/practice/online/?questionIds=${qIds}&module=${encodeURIComponent(moduleName)}&count=${count}`
    } else {
      alert('该模块暂无题目')
    }
  } catch (e) {
    console.error('加载题目失败', e)
    alert('加载题目失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

// --- 公共 ---

onMounted(() => {
  if (props.mode === 'bank') {
    loadPapers()
  }
})
</script>

<style scoped>
.paper-list { max-width: 960px; margin: 0 auto; padding: 20px; }

/* --- 地区筛选 pill 按钮 --- */
.filter-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 20px;
}
.filter-tag {
  padding: 6px 14px;
  border: 1px solid var(--vp-c-divider);
  border-radius: 20px;
  background: var(--vp-c-bg);
  color: var(--vp-c-text-1);
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
}
.filter-tag:hover {
  border-color: var(--vp-c-brand);
  color: var(--vp-c-brand);
}
.filter-tag.active {
  background: var(--vp-c-brand);
  color: #fff;
  border-color: var(--vp-c-brand);
}

/* --- 试卷卡片 --- */
.papers { display: flex; flex-direction: column; gap: 12px; }
.paper-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  background: var(--vp-c-bg-soft);
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
}
.paper-card:hover {
  box-shadow: 0 2px 12px rgba(0,0,0,0.1);
  transform: translateX(2px);
}
.paper-main { flex: 1; min-width: 0; }
.paper-title {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 8px;
  line-height: 1.5;
}
.region-tag {
  display: inline-block;
  padding: 2px 8px;
  font-size: 12px;
  border-radius: 4px;
  background: var(--vp-c-brand-dimm);
  color: var(--vp-c-brand);
}
.paper-rating {
  color: #f5a623;
  font-size: 14px;
  white-space: nowrap;
  margin-left: 12px;
}

/* --- 加载更多 --- */
.btn-load-more {
  display: block;
  width: 100%;
  padding: 14px;
  margin-top: 16px;
  border: 1px solid var(--vp-c-divider);
  border-radius: 8px;
  background: var(--vp-c-bg-soft);
  color: var(--vp-c-text-1);
  font-size: 15px;
  cursor: pointer;
  transition: all 0.2s;
}
.btn-load-more:hover {
  border-color: var(--vp-c-brand);
  color: var(--vp-c-brand);
}
.btn-load-more:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.bank-footer {
  text-align: center;
  margin-top: 20px;
}
.link-special {
  color: var(--vp-c-brand);
  text-decoration: none;
  font-size: 15px;
}
.link-special:hover { text-decoration: underline; }

.loading { text-align: center; padding: 40px; color: var(--vp-c-text-2); }

/* --- 专项练习 --- */
.special-header {
  text-align: center;
  margin-bottom: 32px;
}
.special-header h1 {
  font-size: 24px;
  margin: 0 0 8px;
}
.special-header p {
  color: var(--vp-c-text-2);
  font-size: 15px;
  margin: 0;
}

.module-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}
@media (max-width: 640px) {
  .module-grid { grid-template-columns: 1fr; }
}
.module-card {
  padding: 24px;
  background: var(--vp-c-bg-soft);
  border: 1px solid var(--vp-c-divider);
  border-radius: 12px;
  transition: all 0.25s;
}
.module-card:hover {
  border-color: var(--vp-c-brand);
  box-shadow: 0 4px 16px rgba(0,0,0,0.06);
}
.module-icon {
  font-size: 36px;
  margin-bottom: 8px;
}
.module-card h3 {
  font-size: 18px;
  font-weight: 600;
  margin: 0 0 6px;
}
.module-card p {
  font-size: 13px;
  color: var(--vp-c-text-2);
  margin: 0 0 16px;
}
.module-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}
.btn-count {
  padding: 6px 14px;
  border: 1px solid var(--vp-c-divider);
  border-radius: 6px;
  background: var(--vp-c-bg);
  color: var(--vp-c-text-1);
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}
.btn-count:hover {
  border-color: var(--vp-c-brand);
  color: var(--vp-c-brand);
  background: var(--vp-c-brand-dimm);
}
.btn-go {
  border-radius: 0 6px 6px 0;
}
.custom-count {
  display: flex;
  align-items: center;
}
.custom-count input {
  width: 64px;
  padding: 6px 8px;
  border: 1px solid var(--vp-c-divider);
  border-right: none;
  border-radius: 6px 0 0 6px;
  background: var(--vp-c-bg);
  color: var(--vp-c-text-1);
  font-size: 13px;
  outline: none;
}
.custom-count input:focus {
  border-color: var(--vp-c-brand);
}
.custom-count input::-webkit-inner-spin-button { -webkit-appearance: none; }
.custom-count input[type=number] { -moz-appearance: textfield; }
</style>
