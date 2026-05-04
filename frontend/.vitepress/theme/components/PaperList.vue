<template>
  <div class="paper-list">
    <!-- 筛选栏 -->
    <div class="filter-bar">
      <div class="filter-group">
        <label>地区：</label>
        <select v-model="filters.regionId" @change="loadPapers">
          <option :value="null">全部</option>
          <option v-for="r in regions" :key="r.id" :value="r.id">
            {{ r.name }}
          </option>
        </select>
      </div>
      <div class="filter-group" v-if="mode === 'special'">
        <label>模块：</label>
        <select v-model="filters.module" @change="loadPapers">
          <option value="">全部</option>
          <option value="言语理解">言语理解</option>
          <option value="数量关系">数量关系</option>
          <option value="判断推理">判断推理</option>
          <option value="资料分析">资料分析</option>
          <option value="常识判断">常识判断</option>
        </select>
      </div>
    </div>

    <!-- 加载中 -->
    <div v-if="loading" class="loading">加载中...</div>

    <!-- 试卷列表（题库模式） -->
    <div v-else-if="mode === 'bank'" class="papers">
      <div v-for="paper in papers" :key="paper.id" class="paper-card"
           @click="startPractice(paper)">
        <div class="paper-title">{{ paper.title }}</div>
        <div class="paper-meta">
          <span v-if="paper.regionName" class="region">{{ paper.regionName }}</span>
          <span class="year">{{ paper.year }}年</span>
          <span class="count">{{ paper.questionCount }}题</span>
          <span class="rating">{{ '★'.repeat(paper.rating || 0) }}</span>
        </div>
      </div>
      <Empty v-if="papers.length === 0" text="暂无试卷" />
    </div>

    <!-- 题目列表（专项练习模式） -->
    <div v-else class="papers">
      <div v-if="!filters.module" class="hint">请先选择模块</div>
      <div v-else-if="questions.length > 0" class="special-start">
        <p>已筛选 {{ questions.length }} 道{{ filters.module }}题目</p>
        <button class="btn-primary" @click="startPractice()">开始练习</button>
      </div>
      <Empty v-else text="该模块暂无题目" />
    </div>

    <!-- 分页 -->
    <div class="pagination" v-if="total > pageSize">
      <button :disabled="page <= 1" @click="page--; loadPapers()">上一页</button>
      <span>{{ page }} / {{ totalPages }}</span>
      <button :disabled="page >= totalPages" @click="page++; loadPapers()">下一页</button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { paperApi, regionApi } from '../utils/api.js'
import Empty from './Empty.vue'

const props = defineProps({
  mode: { type: String, default: 'bank' },
  category: { type: String, default: '行测' }
})

const regions = ref([])
const papers = ref([])
const questions = ref([])
const loading = ref(false)
const page = ref(1)
const pageSize = 20
const total = ref(0)
const totalPages = computed(() => Math.ceil(total.value / pageSize))

const filters = ref({
  regionId: null,
  module: ''
})

async function loadPapers() {
  loading.value = true
  try {
    if (props.mode === 'special') {
      if (!filters.value.module) {
        papers.value = []
        total.value = 0
        return
      }
      const params = { module: filters.value.module, limit: 50 }
      if (filters.value.subModule) params.sub_module = filters.value.subModule
      const res = await paperApi.getQuestionsByKnowledge(params)
      if (res.success) {
        questions.value = res.data || []
        total.value = questions.value.length
      }
    } else {
      const params = {
        category: props.category,
        regionId: filters.value.regionId,
        page: page.value,
        pageSize
      }
      const res = await paperApi.list(params)
      if (res.success) {
        papers.value = res.data.list
        total.value = res.data.total
      }
    }
  } catch (e) {
    console.error('加载数据失败', e)
  } finally {
    loading.value = false
  }
}

async function loadRegions() {
  try {
    const res = await regionApi.list()
    if (res.success) {
      regions.value = res.data
    }
  } catch (e) {
    console.error('加载地区失败', e)
  }
}

function startPractice(paper) {
  if (props.mode === 'special') {
    localStorage.setItem('specialQuestions', JSON.stringify(questions.value))
    const qIds = questions.value.map(q => q.id).join(',')
    window.location.href = `/practice/online/?questionIds=${qIds}`
  } else {
    window.location.href = `/practice/online/?paperId=${paper.id}`
  }
}

onMounted(() => {
  loadRegions()
  loadPapers()
})
</script>

<style scoped>
.paper-list { max-width: 960px; margin: 0 auto; padding: 20px; }
.filter-bar {
  display: flex; gap: 16px; margin-bottom: 20px;
  padding: 12px 16px; background: var(--vp-c-bg-soft);
  border-radius: 8px;
}
.filter-group { display: flex; align-items: center; gap: 6px; }
.filter-group label { font-size: 14px; color: var(--vp-c-text-2); }
.filter-group select {
  padding: 4px 8px; border: 1px solid var(--vp-c-divider);
  border-radius: 4px; background: var(--vp-c-bg);
  color: var(--vp-c-text-1); font-size: 14px;
}
.papers { display: flex; flex-direction: column; gap: 12px; }
.paper-card {
  padding: 16px; background: var(--vp-c-bg-soft);
  border-radius: 8px; cursor: pointer;
  transition: box-shadow 0.2s;
}
.paper-card:hover { box-shadow: 0 2px 12px rgba(0,0,0,0.1); }
.paper-title { font-size: 16px; font-weight: 600; margin-bottom: 8px; }
.paper-meta { display: flex; gap: 12px; font-size: 13px; color: var(--vp-c-text-2); }
.rating { color: #f5a623; }
.pagination {
  display: flex; align-items: center; justify-content: center;
  gap: 16px; margin-top: 20px;
}
.pagination button {
  padding: 6px 16px; border: 1px solid var(--vp-c-divider);
  border-radius: 4px; background: var(--vp-c-bg); cursor: pointer;
}
.pagination button:disabled { opacity: 0.5; cursor: not-allowed; }
.loading { text-align: center; padding: 40px; color: var(--vp-c-text-2); }
.hint { text-align: center; padding: 40px; color: var(--vp-c-text-2); font-size: 15px; }
.special-start { text-align: center; padding: 20px; }
.special-start p { margin-bottom: 16px; color: var(--vp-c-text-2); }
.btn-primary {
  padding: 8px 24px; background: var(--vp-c-brand);
  color: #fff; border: none; border-radius: 6px; cursor: pointer; font-size: 15px;
}
</style>
