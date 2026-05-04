# P5 — 其余功能模块 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现除核心做题和 AI 批改之外的所有功能模块——错题题库、AI补练、每日打卡、成语查询、高频词语/成语、每日新闻、实时热榜、今日史事、学习计划、上岸秘籍、个人中心、收藏、反馈、进站必读、行为追踪，使项目功能完整覆盖原站。

**Architecture:** Java 后端提供所有 CRUD 和第三方 API 代理（Redis 缓存），FastAPI 提供学习计划生成（SSE 流式）和错题 AI 补练生成。前端各组件独立，每个页面对应一个 Vue 组件。各模块之间松耦合，可独立开发和测试。

**Tech Stack:** Spring Boot 3.5.12, MyBatis-Plus 3.5.12, Redis 7.4, MinIO 8.5.14, FastAPI 0.115.12, Vue 3, VitePress 2.0.0-alpha.15

**Spec:** `docs/superpowers/specs/2026-05-04-bala-gongkao-design.md`

---

## File Structure

```
backend/src/main/java/com/gongkao/
├── entity/
│   ├── AiQuestion.java                 # 错题AI补练
│   ├── Checkin.java                    # 每日打卡
│   ├── CheckinTask.java                # 打卡任务
│   ├── Idiom.java                      # 成语
│   ├── HighFreqWord.java               # 高频词语
│   ├── HighFreqIdiom.java              # 高频成语
│   ├── StudyPlan.java                  # 学习计划
│   ├── Secret.java                     # 上岸秘籍
│   ├── UserFavorite.java               # 收藏
│   ├── Feedback.java                   # 意见反馈
│   ├── StatsTrack.java                 # 行为追踪
│   └── SiteConfig.java                 # 站点配置
├── mapper/
│   ├── AiQuestionMapper.java
│   ├── CheckinMapper.java
│   ├── CheckinTaskMapper.java
│   ├── IdiomMapper.java
│   ├── HighFreqWordMapper.java
│   ├── HighFreqIdiomMapper.java
│   ├── StudyPlanMapper.java
│   ├── SecretMapper.java
│   ├── UserFavoriteMapper.java
│   ├── FeedbackMapper.java
│   ├── StatsTrackMapper.java
│   └── SiteConfigMapper.java
├── dto/
│   ├── AiQuestionVO.java
│   ├── CheckinVO.java
│   ├── CheckinTaskRequest.java
│   ├── StudyPlanVO.java
│   ├── SecretVO.java
│   ├── UserFavoriteVO.java
│   ├── FeedbackRequest.java
│   └── ProfileUpdateRequest.java
├── service/
│   ├── AiQuestionService.java
│   ├── CheckinService.java
│   ├── StudyToolService.java           # 成语/高频词语/高频成语
│   ├── StudyPlanService.java
│   ├── SecretService.java
│   ├── FavoriteService.java
│   ├── FeedbackService.java
│   ├── ProxyService.java               # 第三方API代理(热榜/新闻/今日史事)
│   ├── StatsService.java
│   ├── SiteConfigService.java
│   └── UserProfileService.java
├── controller/
│   ├── AiQuestionController.java       # /api/ai-question/**
│   ├── CheckinController.java          # /api/checkin/**
│   ├── StudyToolController.java        # /api/idiom/** /api/high-freq-words/** /api/high-freq-idiom/**
│   ├── StudyPlanController.java        # /api/study-plan/**
│   ├── SecretController.java           # /api/secrets/**
│   ├── FavoriteController.java         # /api/favorites/**
│   ├── FeedbackController.java         # /api/feedback/**
│   ├── ProxyController.java            # /api/proxy/**
│   ├── StatsController.java            # /api/stats/**
│   ├── SiteConfigController.java       # /api/site-config
│   └── UserController.java             # /api/user/profile
└── config/
    └── RestTemplateConfig.java         # RestTemplate for proxy calls

ai-service/
├── app/
│   ├── routers/
│   │   ├── question.py                 # (已存在, 补充 /ai/question/generate)
│   │   └── study_plan.py              # POST /ai/study-plan/generate (SSE)
│   └── services/
│       └── plan_generator.py           # 学习计划 Prompt 模板
└── app/main.py                         # (已存在, 注册新路由)

frontend/.vitepress/theme/components/
├── WrongQuestions.vue                  # 错题题库
├── AIRecommendedQuestions.vue          # 错题AI补练
├── CheckIn.vue                         # 每日打卡
├── IdiomSearch.vue                     # 成语查询
├── HighFreqWords.vue                   # 高频词语
├── HighFreqIdiom.vue                   # 高频成语
├── NewsList.vue                        # 每日新闻
├── HotList.vue                         # 实时热榜
├── HistoryToday.vue                    # 今日史事
├── StudyPlan.vue                       # 学习计划
├── SecretsContent.vue                  # 上岸秘籍
├── SecretsSidebar.vue                  # 秘籍侧边栏
├── MustRead.vue                        # 进站必读
└── Profile.vue                         # 个人中心(扩展Login.vue)

frontend/pages/
├── wrong-questions/index.md
├── checkin/index.md
├── idiom/index.md
├── high-freq-words/index.md
├── high-freq-idiom/index.md
├── news/index.md
├── 今日热榜/index.md
├── history-today/index.md
├── study-plan/index.md
├── secrets/index.md
├── must-read/index.md
└── ai-recommended/index.md
```

---

## Task 1: 错题题库 — 实体 + Mapper + Service + Controller

**Files:**
- Create: `backend/src/main/java/com/gongkao/entity/AiQuestion.java`
- Create: `backend/src/main/java/com/gongkao/mapper/AiQuestionMapper.java`
- Create: `backend/src/main/java/com/gongkao/dto/AiQuestionVO.java`
- Create: `backend/src/main/java/com/gongkao/service/AiQuestionService.java`
- Create: `backend/src/main/java/com/gongkao/controller/AiQuestionController.java`

- [ ] **Step 1: 创建 AiQuestion 实体**

```java
// backend/src/main/java/com/gongkao/entity/AiQuestion.java
package com.gongkao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ai_question")
public class AiQuestion {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long questionId;
    private String knowledgePoint;
    private String originalQuestion;
    private String originalAnswer;
    private String originalOptions;    // JSON
    private String originalExplanation;
    private String originalMaterial;
    private Integer errorCount;
    private String wrongAnswers;       // JSON
    private String errorAnalysis;
    private String recommendedQuestions; // JSON
    private String allErrorRecords;    // JSON
    private LocalDateTime createdAt;
}
```

- [ ] **Step 2: 创建 AiQuestionMapper**

```java
// backend/src/main/java/com/gongkao/mapper/AiQuestionMapper.java
package com.gongkao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gongkao.entity.AiQuestion;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface AiQuestionMapper extends BaseMapper<AiQuestion> {

    @Select("SELECT * FROM ai_question WHERE user_id = #{userId} ORDER BY created_at DESC")
    List<AiQuestion> selectByUserId(@Param("userId") Long userId);

    @Select("SELECT * FROM ai_question WHERE user_id = #{userId} AND question_id = #{questionId}")
    AiQuestion selectByUserQuestion(@Param("userId") Long userId, @Param("questionId") Long questionId);
}
```

- [ ] **Step 3: 创建 AiQuestionVO**

```java
// backend/src/main/java/com/gongkao/dto/AiQuestionVO.java
package com.gongkao.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AiQuestionVO {
    private Long id;
    private Long questionId;
    private String knowledgePoint;
    private String originalQuestion;
    private String originalAnswer;
    private String originalOptions;
    private String originalExplanation;
    private Integer errorCount;
    private String wrongAnswers;
    private String errorAnalysis;
    private String recommendedQuestions;
    private LocalDateTime createdAt;
}
```

- [ ] **Step 4: 创建 AiQuestionService**

```java
// backend/src/main/java/com/gongkao/service/AiQuestionService.java
package com.gongkao.service;

import com.gongkao.dto.AiQuestionVO;
import com.gongkao.entity.AiQuestion;
import com.gongkao.entity.Question;
import com.gongkao.entity.UserAnswer;
import com.gongkao.mapper.AiQuestionMapper;
import com.gongkao.mapper.QuestionMapper;
import com.gongkao.mapper.UserAnswerMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AiQuestionService {

    private final AiQuestionMapper mapper;
    private final QuestionMapper questionMapper;
    private final UserAnswerMapper userAnswerMapper;

    public List<AiQuestionVO> listByUser(Long userId) {
        return mapper.selectByUserId(userId).stream()
                .map(this::toVO).collect(Collectors.toList());
    }

    public void deleteById(Long id, Long userId) {
        AiQuestion entity = mapper.selectById(id);
        if (entity == null || !entity.getUserId().equals(userId)) {
            throw new RuntimeException("记录不存在或无权访问");
        }
        mapper.deleteById(id);
    }

    /**
     * 从错题自动归集：当用户答案判为错误时调用
     */
    public void recordWrongAnswer(Long userId, Long questionId, String userAnswer) {
        AiQuestion existing = mapper.selectByUserQuestion(userId, questionId);
        Question question = questionMapper.selectById(questionId);

        if (existing != null) {
            // 已有记录，增加错误次数
            existing.setErrorCount(existing.getErrorCount() + 1);
            // 追加错误答案
            try {
                com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
                java.util.List<String> wrongs = om.readValue(
                        existing.getWrongAnswers() != null ? existing.getWrongAnswers() : "[]",
                        java.util.List.class);
                wrongs.add(userAnswer);
                existing.setWrongAnswers(om.writeValueAsString(wrongs));
            } catch (Exception e) { /* ignore */ }
            mapper.updateById(existing);
        } else {
            // 新建错题记录
            AiQuestion aq = new AiQuestion();
            aq.setUserId(userId);
            aq.setQuestionId(questionId);
            aq.setKnowledgePoint(question != null ? question.getKnowledgePoint() : null);
            aq.setOriginalQuestion(question != null ? question.getContent() : null);
            aq.setOriginalAnswer(question != null ? question.getAnswer() : null);
            aq.setOriginalOptions(question != null ? question.getOptions() : null);
            aq.setOriginalExplanation(question != null ? question.getExplanation() : null);
            aq.setErrorCount(1);
            try {
                com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
                aq.setWrongAnswers(om.writeValueAsString(java.util.List.of(userAnswer)));
            } catch (Exception e) { /* ignore */ }
            mapper.insert(aq);
        }
    }

    /**
     * 保存 AI 推荐题
     */
    public void updateRecommendedQuestions(Long id, Long userId, String recommendedJson) {
        AiQuestion entity = mapper.selectById(id);
        if (entity == null || !entity.getUserId().equals(userId)) {
            throw new RuntimeException("记录不存在或无权访问");
        }
        entity.setRecommendedQuestions(recommendedJson);
        mapper.updateById(entity);
    }

    private AiQuestionVO toVO(AiQuestion e) {
        AiQuestionVO vo = new AiQuestionVO();
        BeanUtils.copyProperties(e, vo);
        return vo;
    }
}
```

- [ ] **Step 5: 创建 AiQuestionController**

```java
// backend/src/main/java/com/gongkao/controller/AiQuestionController.java
package com.gongkao.controller;

import com.gongkao.common.Result;
import com.gongkao.dto.AiQuestionVO;
import com.gongkao.service.AiQuestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ai-question")
@RequiredArgsConstructor
public class AiQuestionController {

    private final AiQuestionService service;

    @GetMapping("/my-questions")
    public Result<List<AiQuestionVO>> listMyQuestions(@AuthenticationPrincipal Long userId) {
        return Result.ok(service.listByUser(userId));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
        service.deleteById(id, userId);
        return Result.ok(null);
    }
}
```

- [ ] **Step 6: 在 AnswerService 中集成错题归集**

在 `backend/src/main/java/com/gongkao/service/AnswerService.java` 的 `batchSubmit` 方法中，当 `isCorrect == false` 时调用 `aiQuestionService.recordWrongAnswer`：

在 `AnswerService` 类中注入 `AiQuestionService`：

```java
private final AiQuestionService aiQuestionService;
```

在 `batchSubmit` 方法的判分循环中，在 `userAnswerMapper.insert(ua)` 之后追加：

```java
if (Boolean.FALSE.equals(isCorrect)) {
    aiQuestionService.recordWrongAnswer(userId, question.getId(), userAns);
}
```

- [ ] **Step 7: 验证编译**

Run: `cd backend && mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 8: Commit**

```bash
git add backend/src/main/java/com/gongkao/entity/AiQuestion.java \
        backend/src/main/java/com/gongkao/mapper/AiQuestionMapper.java \
        backend/src/main/java/com/gongkao/dto/AiQuestionVO.java \
        backend/src/main/java/com/gongkao/service/AiQuestionService.java \
        backend/src/main/java/com/gongkao/controller/AiQuestionController.java \
        backend/src/main/java/com/gongkao/service/AnswerService.java
git commit -m "feat(P5): add wrong question recording with auto-aggregation on submit"
```

---

## Task 2: 前端 — WrongQuestions.vue + AIRecommendedQuestions.vue

**Files:**
- Create: `frontend/.vitepress/theme/components/WrongQuestions.vue`
- Create: `frontend/.vitepress/theme/components/AIRecommendedQuestions.vue`
- Modify: `frontend/pages/wrong-questions/index.md`
- Modify: `frontend/pages/ai-recommended/index.md`

- [ ] **Step 1: 创建 WrongQuestions.vue**

```vue
<!-- frontend/.vitepress/theme/components/WrongQuestions.vue -->
<template>
  <div class="wrong-questions">
    <h2>错题题库</h2>
    <p class="desc">自动归集你做错的题目，针对性复习</p>

    <div v-if="loading" class="loading">加载中...</div>
    <div v-else>
      <Empty v-if="questions.length === 0" text="暂无错题，继续保持！" />
      <div v-else class="question-list">
        <div v-for="q in questions" :key="q.id" class="wrong-card">
          <div class="wrong-header">
            <span class="knowledge">{{ q.knowledgePoint || '未知知识点' }}</span>
            <span class="count">错 {{ q.errorCount }} 次</span>
            <button class="btn-delete" @click="deleteQuestion(q.id)">删除</button>
          </div>
          <div class="wrong-content" v-html="q.originalQuestion"></div>
          <div class="wrong-answers">
            <span>错误答案：{{ q.wrongAnswers }}</span>
            <span>正确答案：{{ q.originalAnswer }}</span>
          </div>
          <div v-if="q.originalExplanation" class="explanation" v-html="q.originalExplanation"></div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { get, del } from '../utils/api.js'
import Empty from './Empty.vue'

const questions = ref([])
const loading = ref(false)

async function loadQuestions() {
  loading.value = true
  try {
    const res = await get('/ai-question/my-questions')
    if (res.code === 0) questions.value = res.data
  } finally { loading.value = false }
}

async function deleteQuestion(id) {
  if (!confirm('确认删除？')) return
  await del(`/ai-question/${id}`)
  questions.value = questions.value.filter(q => q.id !== id)
}

onMounted(loadQuestions)
</script>

<style scoped>
.wrong-questions { max-width: 960px; margin: 0 auto; padding: 20px; }
.wrong-questions h2 { margin: 0 0 4px; }
.desc { color: var(--vp-c-text-2); font-size: 14px; margin-bottom: 20px; }
.question-list { display: flex; flex-direction: column; gap: 12px; }
.wrong-card { padding: 16px; background: var(--vp-c-bg-soft); border-radius: 8px; border-left: 4px solid #f56c6c; }
.wrong-header { display: flex; gap: 12px; align-items: center; margin-bottom: 8px; }
.knowledge { font-size: 13px; color: var(--vp-c-brand); font-weight: 600; }
.count { font-size: 13px; color: #f56c6c; }
.btn-delete { margin-left: auto; padding: 2px 8px; border: none; color: var(--vp-c-text-3); cursor: pointer; font-size: 12px; background: none; }
.wrong-content { margin-bottom: 8px; line-height: 1.6; }
.wrong-answers { display: flex; gap: 16px; font-size: 13px; color: var(--vp-c-text-2); margin-bottom: 8px; }
.explanation { padding-top: 8px; border-top: 1px solid var(--vp-c-divider); font-size: 13px; color: var(--vp-c-text-2); }
.loading { text-align: center; padding: 40px; color: var(--vp-c-text-2); }
</style>
```

- [ ] **Step 2: 创建 AIRecommendedQuestions.vue（AI 补练占位）**

```vue
<!-- frontend/.vitepress/theme/components/AIRecommendedQuestions.vue -->
<template>
  <div class="ai-recommended">
    <h2>错题 AI 补练</h2>
    <p class="desc">基于你的错题记录，AI 生成针对性练习题</p>

    <div v-if="loading" class="loading">加载中...</div>
    <div v-else>
      <Empty v-if="wrongQuestions.length === 0" text="暂无错题记录，无需补练" />
      <div v-else>
        <div class="wrong-list">
          <div v-for="q in wrongQuestions" :key="q.id" class="wrong-item">
            <span class="knowledge">{{ q.knowledgePoint }}</span>
            <span class="count">错{{ q.errorCount }}次</span>
            <button v-if="!q.recommendedQuestions"
                    class="btn-generate" @click="generateRecommendation(q)">
              生成练习题
            </button>
            <span v-else class="generated">已生成</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { get } from '../utils/api.js'
import Empty from './Empty.vue'

const wrongQuestions = ref([])
const loading = ref(false)

async function loadQuestions() {
  loading.value = true
  try {
    const res = await get('/ai-question/my-questions')
    if (res.code === 0) wrongQuestions.value = res.data
  } finally { loading.value = false }
}

async function generateRecommendation(q) {
  // TODO: 调用 /ai/question/generate 生成推荐题
  alert('AI 补练生成功能开发中')
}

onMounted(loadQuestions)
</script>

<style scoped>
.ai-recommended { max-width: 960px; margin: 0 auto; padding: 20px; }
.ai-recommended h2 { margin: 0 0 4px; }
.desc { color: var(--vp-c-text-2); font-size: 14px; margin-bottom: 20px; }
.wrong-list { display: flex; flex-direction: column; gap: 8px; }
.wrong-item { display: flex; align-items: center; gap: 12px; padding: 12px 16px; background: var(--vp-c-bg-soft); border-radius: 8px; }
.knowledge { font-size: 14px; }
.count { font-size: 13px; color: #f56c6c; }
.btn-generate { margin-left: auto; padding: 4px 12px; background: var(--vp-c-brand); color: #fff; border: none; border-radius: 4px; cursor: pointer; font-size: 13px; }
.generated { margin-left: auto; font-size: 13px; color: #67c23a; }
.loading { text-align: center; padding: 40px; color: var(--vp-c-text-2); }
</style>
```

- [ ] **Step 3: 更新页面入口**

```markdown
<!-- frontend/pages/wrong-questions/index.md -->
---
layout: page
title: 错题题库
---

<WrongQuestions />
```

```markdown
<!-- frontend/pages/ai-recommended/index.md -->
---
layout: page
title: 错题AI补练
---

<AIRecommendedQuestions />
```

- [ ] **Step 4: Commit**

```bash
git add frontend/.vitepress/theme/components/WrongQuestions.vue \
        frontend/.vitepress/theme/components/AIRecommendedQuestions.vue \
        frontend/pages/wrong-questions/index.md \
        frontend/pages/ai-recommended/index.md
git commit -m "feat(P5): add WrongQuestions and AIRecommendedQuestions components"
```

---

## Task 3: 每日打卡 — 实体 + Mapper + Service + Controller + 前端

**Files:**
- Create: `backend/src/main/java/com/gongkao/entity/Checkin.java`
- Create: `backend/src/main/java/com/gongkao/entity/CheckinTask.java`
- Create: `backend/src/main/java/com/gongkao/mapper/CheckinMapper.java`
- Create: `backend/src/main/java/com/gongkao/mapper/CheckinTaskMapper.java`
- Create: `backend/src/main/java/com/gongkao/dto/CheckinVO.java`
- Create: `backend/src/main/java/com/gongkao/dto/CheckinTaskRequest.java`
- Create: `backend/src/main/java/com/gongkao/service/CheckinService.java`
- Create: `backend/src/main/java/com/gongkao/controller/CheckinController.java`
- Create: `frontend/.vitepress/theme/components/CheckIn.vue`
- Modify: `frontend/pages/checkin/index.md`

- [ ] **Step 1: 创建 Checkin 实体**

```java
// backend/src/main/java/com/gongkao/entity/Checkin.java
package com.gongkao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("checkin")
public class Checkin {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private LocalDate date;
    private String studyModules;  // JSON
    private Integer questionCount;
    private Integer studyMinutes;
    private String note;
    private Long timestamp;
    private LocalDateTime createdAt;
}
```

- [ ] **Step 2: 创建 CheckinTask 实体**

```java
// backend/src/main/java/com/gongkao/entity/CheckinTask.java
package com.gongkao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("checkin_task")
public class CheckinTask {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String text;
    private Boolean completed;
    private LocalDateTime createdAt;
}
```

- [ ] **Step 3: 创建 Mapper**

```java
// backend/src/main/java/com/gongkao/mapper/CheckinMapper.java
package com.gongkao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gongkao.entity.Checkin;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CheckinMapper extends BaseMapper<Checkin> {

    @Select("SELECT * FROM checkin WHERE user_id = #{userId} ORDER BY date DESC")
    List<Checkin> selectByUserId(@Param("userId") Long userId);

    @Select("SELECT * FROM checkin WHERE user_id = #{userId} AND date = #{date}")
    Checkin selectByUserDate(@Param("userId") Long userId, @Param("date") java.time.LocalDate date);
}

// backend/src/main/java/com/gongkao/mapper/CheckinTaskMapper.java
package com.gongkao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gongkao.entity.CheckinTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CheckinTaskMapper extends BaseMapper<CheckinTask> {

    @Select("SELECT * FROM checkin_task WHERE user_id = #{userId} ORDER BY created_at ASC")
    List<CheckinTask> selectByUserId(@Param("userId") Long userId);
}
```

- [ ] **Step 4: 创建 DTOs**

```java
// backend/src/main/java/com/gongkao/dto/CheckinVO.java
package com.gongkao.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class CheckinVO {
    private Long id;
    private LocalDate date;
    private String studyModules;
    private Integer questionCount;
    private Integer studyMinutes;
    private String note;
}

// backend/src/main/java/com/gongkao/dto/CheckinTaskRequest.java
package com.gongkao.dto;

import lombok.Data;

@Data
public class CheckinTaskRequest {
    private String text;
}
```

- [ ] **Step 5: 创建 CheckinService**

```java
// backend/src/main/java/com/gongkao/service/CheckinService.java
package com.gongkao.service;

import com.gongkao.dto.CheckinVO;
import com.gongkao.entity.Checkin;
import com.gongkao.entity.CheckinTask;
import com.gongkao.mapper.CheckinMapper;
import com.gongkao.mapper.CheckinTaskMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CheckinService {

    private final CheckinMapper checkinMapper;
    private final CheckinTaskMapper taskMapper;

    public List<CheckinVO> getCheckinHistory(Long userId) {
        return checkinMapper.selectByUserId(userId).stream().map(c -> {
            CheckinVO vo = new CheckinVO();
            BeanUtils.copyProperties(c, vo);
            return vo;
        }).collect(Collectors.toList());
    }

    public CheckinVO checkin(Long userId, String note, List<String> modules) {
        LocalDate today = LocalDate.now();
        Checkin existing = checkinMapper.selectByUserDate(userId, today);
        if (existing != null) {
            // 已打卡，更新
            if (note != null) existing.setNote(note);
            if (modules != null) {
                try {
                    existing.setStudyModules(new com.fasterxml.jackson.databind.ObjectMapper()
                            .writeValueAsString(modules));
                } catch (Exception e) { /* ignore */ }
            }
            checkinMapper.updateById(existing);
            CheckinVO vo = new CheckinVO();
            BeanUtils.copyProperties(existing, vo);
            return vo;
        }

        Checkin c = new Checkin();
        c.setUserId(userId);
        c.setDate(today);
        c.setNote(note != null ? note : "");
        c.setQuestionCount(0);
        c.setStudyMinutes(0);
        c.setTimestamp(System.currentTimeMillis());
        if (modules != null) {
            try {
                c.setStudyModules(new com.fasterxml.jackson.databind.ObjectMapper()
                        .writeValueAsString(modules));
            } catch (Exception e) { /* ignore */ }
        }
        checkinMapper.insert(c);

        CheckinVO vo = new CheckinVO();
        BeanUtils.copyProperties(c, vo);
        return vo;
    }

    public void deleteCheckin(Long userId, Long timestamp) {
        // 按 timestamp 删除打卡记录
        LocalDate date = LocalDate.now(); // 简化
        Checkin c = checkinMapper.selectByUserDate(userId, date);
        if (c != null && c.getUserId().equals(userId)) {
            checkinMapper.deleteById(c.getId());
        }
    }

    // ===== 打卡任务 =====

    public List<CheckinTask> getTasks(Long userId) {
        return taskMapper.selectByUserId(userId);
    }

    public CheckinTask createTask(Long userId, String text) {
        CheckinTask task = new CheckinTask();
        task.setUserId(userId);
        task.setText(text);
        task.setCompleted(false);
        taskMapper.insert(task);
        return task;
    }

    public CheckinTask updateTask(Long taskId, Long userId, Boolean completed) {
        CheckinTask task = taskMapper.selectById(taskId);
        if (task == null || !task.getUserId().equals(userId)) {
            throw new RuntimeException("任务不存在");
        }
        task.setCompleted(completed);
        taskMapper.updateById(task);
        return task;
    }

    public void deleteTask(Long taskId, Long userId) {
        CheckinTask task = taskMapper.selectById(taskId);
        if (task == null || !task.getUserId().equals(userId)) {
            throw new RuntimeException("任务不存在");
        }
        taskMapper.deleteById(taskId);
    }
}
```

- [ ] **Step 6: 创建 CheckinController**

```java
// backend/src/main/java/com/gongkao/controller/CheckinController.java
package com.gongkao.controller;

import com.gongkao.common.Result;
import com.gongkao.dto.CheckinTaskRequest;
import com.gongkao.dto.CheckinVO;
import com.gongkao.entity.CheckinTask;
import com.gongkao.service.CheckinService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/checkin")
@RequiredArgsConstructor
public class CheckinController {

    private final CheckinService service;

    @GetMapping
    public Result<List<CheckinVO>> history(@AuthenticationPrincipal Long userId) {
        return Result.ok(service.getCheckinHistory(userId));
    }

    @PostMapping
    public Result<CheckinVO> checkin(
            @AuthenticationPrincipal Long userId,
            @RequestBody Map<String, Object> body) {
        String note = (String) body.getOrDefault("note", "");
        @SuppressWarnings("unchecked")
        List<String> modules = (List<String>) body.get("modules");
        return Result.ok(service.checkin(userId, note, modules));
    }

    @DeleteMapping("/{timestamp}")
    public Result<Void> delete(@AuthenticationPrincipal Long userId, @PathVariable Long timestamp) {
        service.deleteCheckin(userId, timestamp);
        return Result.ok(null);
    }

    // 打卡任务
    @GetMapping("/tasks")
    public Result<List<CheckinTask>> listTasks(@AuthenticationPrincipal Long userId) {
        return Result.ok(service.getTasks(userId));
    }

    @PostMapping("/tasks")
    public Result<CheckinTask> createTask(
            @AuthenticationPrincipal Long userId,
            @RequestBody CheckinTaskRequest req) {
        return Result.ok(service.createTask(userId, req.getText()));
    }

    @PutMapping("/tasks/{id}")
    public Result<CheckinTask> updateTask(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long id,
            @RequestBody Map<String, Boolean> body) {
        return Result.ok(service.updateTask(id, userId, body.get("completed")));
    }

    @DeleteMapping("/tasks/{id}")
    public Result<Void> deleteTask(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long id) {
        service.deleteTask(id, userId);
        return Result.ok(null);
    }
}
```

- [ ] **Step 7: 创建 CheckIn.vue**

```vue
<!-- frontend/.vitepress/theme/components/CheckIn.vue -->
<template>
  <div class="checkin-page">
    <h2>每日打卡</h2>

    <div class="checkin-section">
      <button class="btn-checkin" :class="{ done: todayChecked }"
              @click="doCheckin" :disabled="todayChecked">
        {{ todayChecked ? '已打卡' : '打卡' }}
      </button>
      <p v-if="todayChecked" class="checked-info">今日已打卡 ✓</p>
    </div>

    <!-- 打卡任务 -->
    <div class="tasks-section">
      <h3>今日任务</h3>
      <div class="task-input">
        <input v-model="newTaskText" placeholder="添加学习任务..." @keyup.enter="addTask" />
        <button @click="addTask">添加</button>
      </div>
      <div class="task-list">
        <div v-for="t in tasks" :key="t.id" class="task-item">
          <input type="checkbox" :checked="t.completed" @change="toggleTask(t)" />
          <span :class="{ completed: t.completed }">{{ t.text }}</span>
          <button class="btn-del" @click="deleteTask(t.id)">×</button>
        </div>
      </div>
    </div>

    <!-- 打卡日历（简化：显示最近7天） -->
    <div class="calendar-section">
      <h3>最近打卡</h3>
      <div class="week-row">
        <div v-for="d in recentDays" :key="d.date" class="day-cell" :class="{ active: d.checked }">
          <span class="day-label">{{ d.label }}</span>
          <span class="day-date">{{ d.date }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { get, post, del, put } from '../utils/api.js'

const todayChecked = ref(false)
const tasks = ref([])
const checkinHistory = ref([])
const newTaskText = ref('')

const recentDays = computed(() => {
  const days = []
  const labels = ['日', '一', '二', '三', '四', '五', '六']
  for (let i = 6; i >= 0; i--) {
    const d = new Date()
    d.setDate(d.getDate() - i)
    const dateStr = d.toISOString().slice(0, 10)
    days.push({
      date: dateStr.slice(5),
      label: labels[d.getDay()],
      checked: checkinHistory.value.some(c => c.date === dateStr),
    })
  }
  return days
})

async function doCheckin() {
  const res = await post('/checkin', { note: '', modules: [] })
  if (res.code === 0) { todayChecked.value = true; loadHistory() }
}

async function loadHistory() {
  const res = await get('/checkin')
  if (res.code === 0) {
    checkinHistory.value = res.data
    const today = new Date().toISOString().slice(0, 10)
    todayChecked.value = res.data.some(c => c.date === today)
  }
}

async function loadTasks() {
  const res = await get('/checkin/tasks')
  if (res.code === 0) tasks.value = res.data
}

async function addTask() {
  if (!newTaskText.value.trim()) return
  const res = await post('/checkin/tasks', { text: newTaskText.value })
  if (res.code === 0) { tasks.value.push(res.data); newTaskText.value = '' }
}

async function toggleTask(t) {
  const res = await put(`/checkin/tasks/${t.id}`, { completed: !t.completed })
  if (res.code === 0) t.completed = !t.completed
}

async function deleteTask(id) {
  await del(`/checkin/tasks/${id}`)
  tasks.value = tasks.value.filter(t => t.id !== id)
}

onMounted(() => { loadHistory(); loadTasks() })
</script>

<style scoped>
.checkin-page { max-width: 960px; margin: 0 auto; padding: 20px; }
.checkin-section { text-align: center; margin: 24px 0; }
.btn-checkin {
  width: 100px; height: 100px; border-radius: 50%;
  background: var(--vp-c-brand); color: #fff; border: none;
  font-size: 18px; cursor: pointer;
  transition: all 0.3s;
}
.btn-checkin.done { background: #67c23a; cursor: default; }
.checked-info { color: #67c23a; margin-top: 8px; }
.tasks-section { margin-bottom: 24px; }
.tasks-section h3 { margin: 0 0 12px; }
.task-input { display: flex; gap: 8px; margin-bottom: 12px; }
.task-input input { flex: 1; padding: 8px 12px; border: 1px solid var(--vp-c-divider); border-radius: 4px; background: var(--vp-c-bg); }
.task-input button { padding: 8px 16px; background: var(--vp-c-brand); color: #fff; border: none; border-radius: 4px; cursor: pointer; }
.task-list { display: flex; flex-direction: column; gap: 6px; }
.task-item { display: flex; align-items: center; gap: 8px; padding: 8px; background: var(--vp-c-bg-soft); border-radius: 4px; }
.completed { text-decoration: line-through; color: var(--vp-c-text-3); }
.btn-del { margin-left: auto; background: none; border: none; color: var(--vp-c-text-3); cursor: pointer; font-size: 16px; }
.calendar-section h3 { margin: 0 0 12px; }
.week-row { display: flex; gap: 8px; }
.day-cell {
  flex: 1; text-align: center; padding: 8px; border-radius: 6px;
  background: var(--vp-c-bg-soft);
}
.day-cell.active { background: var(--vp-c-brand); color: #fff; }
.day-label { display: block; font-size: 12px; }
.day-date { display: block; font-size: 14px; font-weight: 600; margin-top: 2px; }
</style>
```

- [ ] **Step 8: 更新 checkin/index.md**

```markdown
---
layout: page
title: 每日打卡
---

<CheckIn />
```

- [ ] **Step 9: 验证编译**

Run: `cd backend && mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 10: Commit**

```bash
git add backend/src/main/java/com/gongkao/entity/Checkin.java \
        backend/src/main/java/com/gongkao/entity/CheckinTask.java \
        backend/src/main/java/com/gongkao/mapper/CheckinMapper.java \
        backend/src/main/java/com/gongkao/mapper/CheckinTaskMapper.java \
        backend/src/main/java/com/gongkao/dto/CheckinVO.java \
        backend/src/main/java/com/gongkao/dto/CheckinTaskRequest.java \
        backend/src/main/java/com/gongkao/service/CheckinService.java \
        backend/src/main/java/com/gongkao/controller/CheckinController.java \
        frontend/.vitepress/theme/components/CheckIn.vue \
        frontend/pages/checkin/index.md
git commit -m "feat(P5): add daily checkin with tasks and calendar view"
```

---

## Task 4: 学习工具 — 成语查询/高频词语/高频成语

**Files:**
- Create: `backend/src/main/java/com/gongkao/entity/Idiom.java`
- Create: `backend/src/main/java/com/gongkao/entity/HighFreqWord.java`
- Create: `backend/src/main/java/com/gongkao/entity/HighFreqIdiom.java`
- Create: `backend/src/main/java/com/gongkao/mapper/IdiomMapper.java`
- Create: `backend/src/main/java/com/gongkao/mapper/HighFreqWordMapper.java`
- Create: `backend/src/main/java/com/gongkao/mapper/HighFreqIdiomMapper.java`
- Create: `backend/src/main/java/com/gongkao/service/StudyToolService.java`
- Create: `backend/src/main/java/com/gongkao/controller/StudyToolController.java`
- Create: `frontend/.vitepress/theme/components/IdiomSearch.vue`
- Create: `frontend/.vitepress/theme/components/HighFreqWords.vue`
- Create: `frontend/.vitepress/theme/components/HighFreqIdiom.vue`
- Modify: `frontend/pages/idiom/index.md`
- Modify: `frontend/pages/high-freq-words/index.md`
- Modify: `frontend/pages/high-freq-idiom/index.md`

- [ ] **Step 1: 创建实体**

```java
// backend/src/main/java/com/gongkao/entity/Idiom.java
package com.gongkao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("idiom")
public class Idiom {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String idiom;
    private String pinyin;
    private String explanation;
    private String provenance;
    private String usage;
    private String sentence;
    private String analysis;
}

// backend/src/main/java/com/gongkao/entity/HighFreqWord.java
package com.gongkao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("high_freq_word")
public class HighFreqWord {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String word;
    private String category;
    private String explanation;
    private String usage;
}

// backend/src/main/java/com/gongkao/entity/HighFreqIdiom.java
package com.gongkao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("high_freq_idiom")
public class HighFreqIdiom {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String idiom;
    private String category;
    private String explanation;
    private String usage;
    private String example;
}
```

- [ ] **Step 2: 创建 Mapper**

```java
// backend/src/main/java/com/gongkao/mapper/IdiomMapper.java
package com.gongkao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gongkao.entity.Idiom;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface IdiomMapper extends BaseMapper<Idiom> {

    @Select("SELECT * FROM idiom WHERE idiom LIKE CONCAT('%',#{keyword},'%') LIMIT 20")
    List<Idiom> searchByKeyword(@Param("keyword") String keyword);
}

// backend/src/main/java/com/gongkao/mapper/HighFreqWordMapper.java
package com.gongkao.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gongkao.entity.HighFreqWord;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface HighFreqWordMapper extends BaseMapper<HighFreqWord> {
}

// backend/src/main/java/com/gongkao/mapper/HighFreqIdiomMapper.java
package com.gongkao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gongkao.entity.HighFreqIdiom;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface HighFreqIdiomMapper extends BaseMapper<HighFreqIdiom> {
}
```

- [ ] **Step 3: 创建 StudyToolService**

```java
// backend/src/main/java/com/gongkao/service/StudyToolService.java
package com.gongkao.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gongkao.entity.HighFreqIdiom;
import com.gongkao.entity.HighFreqWord;
import com.gongkao.entity.Idiom;
import com.gongkao.mapper.HighFreqIdiomMapper;
import com.gongkao.mapper.HighFreqWordMapper;
import com.gongkao.mapper.IdiomMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StudyToolService {

    private final IdiomMapper idiomMapper;
    private final HighFreqWordMapper wordMapper;
    private final HighFreqIdiomMapper idiomHighMapper;

    public List<Idiom> searchIdioms(String keyword) {
        if (keyword == null || keyword.isBlank()) return List.of();
        return idiomMapper.searchByKeyword(keyword);
    }

    public List<HighFreqWord> listWords(String category, String keyword) {
        LambdaQueryWrapper<HighFreqWord> w = new LambdaQueryWrapper<>();
        if (category != null && !category.isBlank()) w.eq(HighFreqWord::getCategory, category);
        if (keyword != null && !keyword.isBlank()) w.like(HighFreqWord::getWord, keyword);
        return wordMapper.selectList(w);
    }

    public List<HighFreqIdiom> listIdioms(String category, String keyword) {
        LambdaQueryWrapper<HighFreqIdiom> w = new LambdaQueryWrapper<>();
        if (category != null && !category.isBlank()) w.eq(HighFreqIdiom::getCategory, category);
        if (keyword != null && !keyword.isBlank()) w.like(HighFreqIdiom::getIdiom, keyword);
        return idiomHighMapper.selectList(w);
    }
}
```

- [ ] **Step 4: 创建 StudyToolController**

```java
// backend/src/main/java/com/gongkao/controller/StudyToolController.java
package com.gongkao.controller;

import com.gongkao.common.Result;
import com.gongkao.entity.HighFreqIdiom;
import com.gongkao.entity.HighFreqWord;
import com.gongkao.entity.Idiom;
import com.gongkao.service.StudyToolService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class StudyToolController {

    private final StudyToolService service;

    @GetMapping("/api/idiom/search")
    public Result<List<Idiom>> searchIdiom(@RequestParam String keyword) {
        return Result.ok(service.searchIdioms(keyword));
    }

    @GetMapping("/api/high-freq-words")
    public Result<List<HighFreqWord>> listWords(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword) {
        return Result.ok(service.listWords(category, keyword));
    }

    @GetMapping("/api/high-freq-idiom")
    public Result<List<HighFreqIdiom>> listIdioms(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword) {
        return Result.ok(service.listIdioms(category, keyword));
    }
}
```

- [ ] **Step 5: 创建 IdiomSearch.vue**

```vue
<!-- frontend/.vitepress/theme/components/IdiomSearch.vue -->
<template>
  <div class="idiom-search">
    <h2>成语查询</h2>
    <div class="search-bar">
      <input v-model="keyword" placeholder="输入成语关键字..." @keyup.enter="search" />
      <button @click="search">搜索</button>
    </div>
    <div v-if="results.length" class="results">
      <div v-for="item in results" :key="item.id" class="idiom-card">
        <h3>{{ item.idiom }} <small>{{ item.pinyin }}</small></h3>
        <p><strong>释义：</strong>{{ item.explanation }}</p>
        <p v-if="item.provenance"><strong>出处：</strong>{{ item.provenance }}</p>
        <p v-if="item.usage"><strong>用法：</strong>{{ item.usage }}</p>
        <p v-if="item.sentence"><strong>例句：</strong>{{ item.sentence }}</p>
      </div>
    </div>
    <Empty v-else-if="searched" text="未找到相关成语" />
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { get } from '../utils/api.js'
import Empty from './Empty.vue'

const keyword = ref('')
const results = ref([])
const searched = ref(false)

async function search() {
  if (!keyword.value.trim()) return
  const res = await get('/idiom/search', { keyword: keyword.value })
  if (res.code === 0) results.value = res.data
  searched.value = true
}
</script>

<style scoped>
.idiom-search { max-width: 960px; margin: 0 auto; padding: 20px; }
.idiom-search h2 { margin: 0 0 16px; }
.search-bar { display: flex; gap: 8px; margin-bottom: 20px; }
.search-bar input { flex: 1; padding: 8px 12px; border: 1px solid var(--vp-c-divider); border-radius: 4px; background: var(--vp-c-bg); }
.search-bar button { padding: 8px 20px; background: var(--vp-c-brand); color: #fff; border: none; border-radius: 4px; cursor: pointer; }
.results { display: flex; flex-direction: column; gap: 12px; }
.idiom-card { padding: 16px; background: var(--vp-c-bg-soft); border-radius: 8px; }
.idiom-card h3 { margin: 0 0 8px; }
.idiom-card h3 small { font-weight: 400; color: var(--vp-c-text-2); }
.idiom-card p { margin: 4px 0; font-size: 14px; line-height: 1.6; }
</style>
```

- [ ] **Step 6: 创建 HighFreqWords.vue**

```vue
<!-- frontend/.vitepress/theme/components/HighFreqWords.vue -->
<template>
  <div class="high-freq-words">
    <h2>高频词语</h2>
    <div class="filter-bar">
      <select v-model="category" @change="loadWords">
        <option value="">全部分类</option>
        <option value="实词">实词</option>
        <option value="虚词">虚词</option>
      </select>
      <input v-model="keyword" placeholder="搜索..." @input="loadWords" />
    </div>
    <div class="word-list">
      <div v-for="w in words" :key="w.id" class="word-card">
        <div class="word-title">{{ w.word }} <small>{{ w.category }}</small></div>
        <p>{{ w.explanation }}</p>
        <p v-if="w.usage" class="usage">{{ w.usage }}</p>
      </div>
      <Empty v-if="words.length === 0 && loaded" text="暂无数据" />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { get } from '../utils/api.js'
import Empty from './Empty.vue'

const words = ref([])
const category = ref('')
const keyword = ref('')
const loaded = ref(false)

async function loadWords() {
  const res = await get('/high-freq-words', { category: category.value || undefined, keyword: keyword.value || undefined })
  if (res.code === 0) words.value = res.data
  loaded.value = true
}

onMounted(loadWords)
</script>

<style scoped>
.high-freq-words { max-width: 960px; margin: 0 auto; padding: 20px; }
.high-freq-words h2 { margin: 0 0 16px; }
.filter-bar { display: flex; gap: 8px; margin-bottom: 20px; }
.filter-bar select, .filter-bar input { padding: 6px 10px; border: 1px solid var(--vp-c-divider); border-radius: 4px; background: var(--vp-c-bg); }
.filter-bar input { flex: 1; }
.word-list { display: flex; flex-direction: column; gap: 8px; }
.word-card { padding: 12px; background: var(--vp-c-bg-soft); border-radius: 6px; }
.word-title { font-weight: 600; margin-bottom: 4px; }
.word-title small { font-weight: 400; color: var(--vp-c-text-2); font-size: 12px; }
.word-card p { margin: 2px 0; font-size: 14px; }
.usage { color: var(--vp-c-text-2); font-size: 13px; }
</style>
```

- [ ] **Step 7: 创建 HighFreqIdiom.vue（结构同 HighFreqWords，替换字段名）**

```vue
<!-- frontend/.vitepress/theme/components/HighFreqIdiom.vue -->
<template>
  <div class="high-freq-idiom">
    <h2>高频成语</h2>
    <div class="filter-bar">
      <select v-model="category" @change="loadIdioms">
        <option value="">全部分类</option>
        <option value="典故类">典故类</option>
        <option value="描写类">描写类</option>
      </select>
      <input v-model="keyword" placeholder="搜索..." @input="loadIdioms" />
    </div>
    <div class="idiom-list">
      <div v-for="item in idioms" :key="item.id" class="idiom-card">
        <div class="idiom-title">{{ item.idiom }} <small>{{ item.category }}</small></div>
        <p>{{ item.explanation }}</p>
        <p v-if="item.usage" class="usage">用法：{{ item.usage }}</p>
        <p v-if="item.example" class="example">例句：{{ item.example }}</p>
      </div>
      <Empty v-if="idioms.length === 0 && loaded" text="暂无数据" />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { get } from '../utils/api.js'
import Empty from './Empty.vue'

const idioms = ref([])
const category = ref('')
const keyword = ref('')
const loaded = ref(false)

async function loadIdioms() {
  const res = await get('/high-freq-idiom', { category: category.value || undefined, keyword: keyword.value || undefined })
  if (res.code === 0) idioms.value = res.data
  loaded.value = true
}

onMounted(loadIdioms)
</script>

<style scoped>
.high-freq-idiom { max-width: 960px; margin: 0 auto; padding: 20px; }
.high-freq-idiom h2 { margin: 0 0 16px; }
.filter-bar { display: flex; gap: 8px; margin-bottom: 20px; }
.filter-bar select, .filter-bar input { padding: 6px 10px; border: 1px solid var(--vp-c-divider); border-radius: 4px; background: var(--vp-c-bg); }
.filter-bar input { flex: 1; }
.idiom-list { display: flex; flex-direction: column; gap: 8px; }
.idiom-card { padding: 12px; background: var(--vp-c-bg-soft); border-radius: 6px; }
.idiom-title { font-weight: 600; margin-bottom: 4px; }
.idiom-title small { font-weight: 400; color: var(--vp-c-text-2); font-size: 12px; }
.idiom-card p { margin: 2px 0; font-size: 14px; }
.usage, .example { color: var(--vp-c-text-2); font-size: 13px; }
</style>
```

- [ ] **Step 8: 更新页面入口文件**

```markdown
<!-- frontend/pages/idiom/index.md -->
---
layout: page
title: 成语查询
---

<IdiomSearch />
```

```markdown
<!-- frontend/pages/high-freq-words/index.md -->
---
layout: page
title: 高频词语
---

<HighFreqWords />
```

```markdown
<!-- frontend/pages/high-freq-idiom/index.md -->
---
layout: page
title: 高频成语
---

<HighFreqIdiom />
```

- [ ] **Step 9: 验证编译 + Commit**

Run: `cd backend && mvn compile -q`
Expected: BUILD SUCCESS

```bash
git add backend/src/main/java/com/gongkao/entity/Idiom.java \
        backend/src/main/java/com/gongkao/entity/HighFreqWord.java \
        backend/src/main/java/com/gongkao/entity/HighFreqIdiom.java \
        backend/src/main/java/com/gongkao/mapper/IdiomMapper.java \
        backend/src/main/java/com/gongkao/mapper/HighFreqWordMapper.java \
        backend/src/main/java/com/gongkao/mapper/HighFreqIdiomMapper.java \
        backend/src/main/java/com/gongkao/service/StudyToolService.java \
        backend/src/main/java/com/gongkao/controller/StudyToolController.java \
        frontend/.vitepress/theme/components/IdiomSearch.vue \
        frontend/.vitepress/theme/components/HighFreqWords.vue \
        frontend/.vitepress/theme/components/HighFreqIdiom.vue \
        frontend/pages/idiom/index.md \
        frontend/pages/high-freq-words/index.md \
        frontend/pages/high-freq-idiom/index.md
git commit -m "feat(P5): add idiom search, high-freq words and idioms"
```

---

## Task 5: 第三方 API 代理 — 新闻/热榜/今日史事（Java + Redis 缓存）

**Files:**
- Create: `backend/src/main/java/com/gongkao/config/RestTemplateConfig.java`
- Create: `backend/src/main/java/com/gongkao/service/ProxyService.java`
- Create: `backend/src/main/java/com/gongkao/controller/ProxyController.java`
- Create: `frontend/.vitepress/theme/components/NewsList.vue`
- Create: `frontend/.vitepress/theme/components/HotList.vue`
- Create: `frontend/.vitepress/theme/components/HistoryToday.vue`
- Modify: `frontend/pages/news/index.md`
- Modify: `frontend/pages/今日热榜/index.md`
- Modify: `frontend/pages/history-today/index.md`

- [ ] **Step 1: 创建 RestTemplateConfig**

```java
// backend/src/main/java/com/gongkao/config/RestTemplateConfig.java
package com.gongkao.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(10000);
        return new RestTemplate(factory);
    }
}
```

- [ ] **Step 2: 创建 ProxyService**

```java
// backend/src/main/java/com/gongkao/service/ProxyService.java
package com.gongkao.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProxyService {

    private final RestTemplate restTemplate;
    private final StringRedisTemplate redisTemplate;

    private static final long CACHE_TTL_MINUTES = 30;

    /**
     * 获取热榜数据（带 Redis 缓存）
     * type: 知乎/微博/百度 等
     */
    public String getHotboard(String type) {
        String cacheKey = "cache:hotboard:" + type;
        String cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) return cached;

        // 调用第三方热榜 API（示例用天行数据或类似接口）
        try {
            String url = "https://api.tianapi.com/hotlist/index?key=YOUR_KEY&type=" + type;
            String result = restTemplate.getForObject(url, String.class);
            if (result != null) {
                redisTemplate.opsForValue().set(cacheKey, result, CACHE_TTL_MINUTES, TimeUnit.MINUTES);
            }
            return result;
        } catch (Exception e) {
            log.error("获取热榜失败: type={}", type, e);
            return "{\"code\":500,\"msg\":\"获取热榜数据失败\"}";
        }
    }

    /**
     * 获取今日史事（带 Redis 缓存）
     */
    public String getHistoryToday() {
        String cacheKey = "cache:history_today";
        String cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) return cached;

        try {
            String url = "https://api.tianapi.com/lishi/index?key=YOUR_KEY";
            String result = restTemplate.getForObject(url, String.class);
            if (result != null) {
                redisTemplate.opsForValue().set(cacheKey, result, CACHE_TTL_MINUTES, TimeUnit.MINUTES);
            }
            return result;
        } catch (Exception e) {
            log.error("获取今日史事失败", e);
            return "{\"code\":500,\"msg\":\"获取数据失败\"}";
        }
    }

    /**
     * 获取新闻联播摘要（带 Redis 缓存）
     */
    public String getNewsXwlb() {
        String cacheKey = "cache:news:xwlb";
        String cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) return cached;

        try {
            String url = "https://api.tianapi.com/xwlb/index?key=YOUR_KEY";
            String result = restTemplate.getForObject(url, String.class);
            if (result != null) {
                redisTemplate.opsForValue().set(cacheKey, result, CACHE_TTL_MINUTES, TimeUnit.MINUTES);
            }
            return result;
        } catch (Exception e) {
            log.error("获取新闻失败", e);
            return "{\"code\":500,\"msg\":\"获取数据失败\"}";
        }
    }
}
```

- [ ] **Step 3: 创建 ProxyController**

```java
// backend/src/main/java/com/gongkao/controller/ProxyController.java
package com.gongkao.controller;

import com.gongkao.service.ProxyService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/proxy")
@RequiredArgsConstructor
public class ProxyController {

    private final ProxyService service;

    @GetMapping("/hotboard")
    public String getHotboard(@RequestParam(defaultValue = "zhihu") String type) {
        return service.getHotboard(type);
    }

    @GetMapping("/history-today")
    public String getHistoryToday() {
        return service.getHistoryToday();
    }
}
```

同时在 `StudyToolController`（Task 4 已创建）中追加新闻接口：

```java
@GetMapping("/api/news/xwlb")
public String getNewsXwlb() {
    return proxyService.getNewsXwlb();
}
```

需在 `StudyToolController` 中注入 `ProxyService proxyService`。

- [ ] **Step 4: 创建前端组件（3个）**

```vue
<!-- frontend/.vitepress/theme/components/HotList.vue -->
<template>
  <div class="hot-list">
    <h2>实时热榜</h2>
    <div class="tabs">
      <button v-for="t in types" :key="t.key" :class="{ active: type === t.key }"
              @click="type = t.key; loadData()">{{ t.label }}</button>
    </div>
    <div v-if="loading" class="loading">加载中...</div>
    <div v-else class="list">
      <a v-for="(item, idx) in items" :key="idx" :href="item.url" target="_blank"
         class="hot-item">
        <span class="rank" :class="{ top: idx < 3 }">{{ idx + 1 }}</span>
        <span class="title">{{ item.title || item.name || item.word }}</span>
        <span v-if="item.hot" class="hot">{{ item.hot }}</span>
      </a>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { get } from '../utils/api.js'

const types = [
  { key: 'zhihu', label: '知乎' },
  { key: 'weibo', label: '微博' },
  { key: 'baidu', label: '百度' },
]
const type = ref('zhihu')
const items = ref([])
const loading = ref(false)

async function loadData() {
  loading.value = true
  try {
    const res = await get('/proxy/hotboard', { type: type.value })
    try {
      const data = JSON.parse(res)
      items.value = data.result?.data || data.data || []
    } catch { items.value = [] }
  } finally { loading.value = false }
}

onMounted(loadData)
</script>

<style scoped>
.hot-list { max-width: 960px; margin: 0 auto; padding: 20px; }
.hot-list h2 { margin: 0 0 16px; }
.tabs { display: flex; gap: 8px; margin-bottom: 16px; }
.tabs button { padding: 6px 14px; border: 1px solid var(--vp-c-divider); border-radius: 4px; background: var(--vp-c-bg); cursor: pointer; }
.tabs button.active { background: var(--vp-c-brand); color: #fff; border-color: var(--vp-c-brand); }
.list { display: flex; flex-direction: column; gap: 6px; }
.hot-item { display: flex; align-items: center; gap: 10px; padding: 10px 12px; background: var(--vp-c-bg-soft); border-radius: 6px; text-decoration: none; color: var(--vp-c-text-1); }
.hot-item:hover { background: var(--vp-c-bg); box-shadow: 0 1px 4px rgba(0,0,0,0.08); }
.rank { width: 24px; height: 24px; display: flex; align-items: center; justify-content: center; border-radius: 4px; font-size: 12px; font-weight: 700; background: var(--vp-c-bg); color: var(--vp-c-text-2); }
.rank.top { background: var(--vp-c-brand); color: #fff; }
.title { flex: 1; font-size: 14px; }
.hot { font-size: 12px; color: var(--vp-c-text-3); }
.loading { text-align: center; padding: 40px; color: var(--vp-c-text-2); }
</style>
```

```vue
<!-- frontend/.vitepress/theme/components/NewsList.vue -->
<template>
  <div class="news-list">
    <h2>每日新闻</h2>
    <div v-if="loading" class="loading">加载中...</div>
    <div v-else class="list">
      <div v-for="(item, idx) in items" :key="idx" class="news-item">
        <div class="news-title">{{ item.title }}</div>
        <div class="news-meta">{{ item.date }} {{ item.author || '' }}</div>
        <p v-if="item.description" class="news-desc">{{ item.description }}</p>
      </div>
      <Empty v-if="items.length === 0" text="暂无新闻数据" />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { get } from '../utils/api.js'
import Empty from './Empty.vue'

const items = ref([])
const loading = ref(false)

async function loadData() {
  loading.value = true
  try {
    const res = await get('/news/xwlb')
    try {
      const data = JSON.parse(res)
      items.value = data.result?.data || data.data || []
    } catch { items.value = [] }
  } finally { loading.value = false }
}

onMounted(loadData)
</script>

<style scoped>
.news-list { max-width: 960px; margin: 0 auto; padding: 20px; }
.news-list h2 { margin: 0 0 16px; }
.list { display: flex; flex-direction: column; gap: 12px; }
.news-item { padding: 12px; background: var(--vp-c-bg-soft); border-radius: 6px; }
.news-title { font-weight: 600; margin-bottom: 4px; }
.news-meta { font-size: 12px; color: var(--vp-c-text-2); margin-bottom: 4px; }
.news-desc { font-size: 14px; color: var(--vp-c-text-2); margin: 0; }
.loading { text-align: center; padding: 40px; color: var(--vp-c-text-2); }
</style>
```

```vue
<!-- frontend/.vitepress/theme/components/HistoryToday.vue -->
<template>
  <div class="history-today">
    <h2>今日史事</h2>
    <div v-if="loading" class="loading">加载中...</div>
    <div v-else class="list">
      <div v-for="(item, idx) in items" :key="idx" class="history-item">
        <span class="date">{{ item.date || item.lsid }}</span>
        <span class="title">{{ item.title || item.content }}</span>
      </div>
      <Empty v-if="items.length === 0" text="暂无数据" />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { get } from '../utils/api.js'
import Empty from './Empty.vue'

const items = ref([])
const loading = ref(false)

async function loadData() {
  loading.value = true
  try {
    const res = await get('/proxy/history-today')
    try {
      const data = JSON.parse(res)
      items.value = data.result?.data || data.data || []
    } catch { items.value = [] }
  } finally { loading.value = false }
}

onMounted(loadData)
</script>

<style scoped>
.history-today { max-width: 960px; margin: 0 auto; padding: 20px; }
.history-today h2 { margin: 0 0 16px; }
.list { display: flex; flex-direction: column; gap: 8px; }
.history-item { display: flex; gap: 12px; padding: 10px 12px; background: var(--vp-c-bg-soft); border-radius: 6px; }
.date { font-size: 13px; color: var(--vp-c-brand); font-weight: 600; white-space: nowrap; }
.title { font-size: 14px; }
.loading { text-align: center; padding: 40px; color: var(--vp-c-text-2); }
</style>
```

- [ ] **Step 5: 更新页面入口**

```markdown
<!-- frontend/pages/news/index.md -->
---
layout: page
title: 每日新闻
---

<NewsList />
```

```markdown
<!-- frontend/pages/今日热榜/index.md -->
---
layout: page
title: 实时热榜
---

<HotList />
```

```markdown
<!-- frontend/pages/history-today/index.md -->
---
layout: page
title: 今日史事
---

<HistoryToday />
```

- [ ] **Step 6: 验证编译 + Commit**

Run: `cd backend && mvn compile -q`
Expected: BUILD SUCCESS

```bash
git add backend/src/main/java/com/gongkao/config/RestTemplateConfig.java \
        backend/src/main/java/com/gongkao/service/ProxyService.java \
        backend/src/main/java/com/gongkao/controller/ProxyController.java \
        backend/src/main/java/com/gongkao/controller/StudyToolController.java \
        frontend/.vitepress/theme/components/HotList.vue \
        frontend/.vitepress/theme/components/NewsList.vue \
        frontend/.vitepress/theme/components/HistoryToday.vue \
        frontend/pages/news/index.md \
        frontend/pages/今日热榜/index.md \
        frontend/pages/history-today/index.md
git commit -m "feat(P5): add third-party API proxy with Redis cache for news/hotboard/history"
```

---

## Task 6: 学习计划 — FastAPI 生成 + Java 存储 + 前端

**Files:**
- Create: `ai-service/app/services/plan_generator.py`
- Create: `ai-service/app/routers/study_plan.py`
- Modify: `ai-service/app/main.py` — 注册路由
- Create: `backend/src/main/java/com/gongkao/entity/StudyPlan.java`
- Create: `backend/src/main/java/com/gongkao/mapper/StudyPlanMapper.java`
- Create: `backend/src/main/java/com/gongkao/dto/StudyPlanVO.java`
- Create: `backend/src/main/java/com/gongkao/service/StudyPlanService.java`
- Create: `backend/src/main/java/com/gongkao/controller/StudyPlanController.java`
- Create: `frontend/.vitepress/theme/components/StudyPlan.vue`
- Modify: `frontend/pages/study-plan/index.md`

- [ ] **Step 1: 创建 plan_generator.py**

```python
# ai-service/app/services/plan_generator.py
import json


def build_study_plan_messages(
    weak_modules: list = None,
    module_accuracy: dict = None,
    days: int = 30,
) -> list[dict]:
    """构建学习计划生成的 messages"""
    weak_info = ""
    if weak_modules:
        weak_info = f"\n薄弱模块：{', '.join(weak_modules)}"
    accuracy_info = ""
    if module_accuracy:
        accuracy_info = "\n各模块正确率：" + ", ".join(
            f"{k}: {v}%" for k, v in module_accuracy.items()
        )

    system_prompt = """你是一位资深的公务员考试学习规划师。请根据学生的实际情况，生成一份详细的学习计划。

请按以下 JSON 格式输出：
{
  "date_range": "YYYY-MM-DD ~ YYYY-MM-DD",
  "daily_tasks": [
    {
      "day": "周一",
      "tasks": [{"text": "具体任务描述", "completed": false}]
    }
  ],
  "milestones": ["阶段性目标1", "阶段性目标2"],
  "recommendations": "总体建议",
  "focus_areas": ["重点突破方向1", "重点突破方向2"]
}"""

    user_prompt = f"""请为我制定一份 {days} 天的公考学习计划。{weak_info}{accuracy_info}

要求：
1. 每天的任务具体、可执行
2. 薄弱模块分配更多时间
3. 兼顾行测和申论
4. 包含模拟考试安排"""

    return [
        {"role": "system", "content": system_prompt},
        {"role": "user", "content": user_prompt},
    ]
```

- [ ] **Step 2: 创建 study_plan.py 路由**

```python
# ai-service/app/routers/study_plan.py
from fastapi import APIRouter, Request
from sse_starlette.sse import EventSourceResponse
from ..core.auth import get_current_user_id
from ..services.llm_service import llm_service
from ..services.plan_generator import build_study_plan_messages
import json

router = APIRouter(prefix="/ai", tags=["study-plan"])


@router.post("/study-plan/generate")
async def generate_study_plan(request: Request):
    """学习计划生成（SSE 流式）"""
    user_id = get_current_user_id(request)
    body = await request.json()

    days = body.get("days", 30)
    weak_modules = body.get("weak_modules", [])
    module_accuracy = body.get("module_accuracy", {})

    messages = build_study_plan_messages(weak_modules, module_accuracy, days)

    async def event_generator():
        full_text = ""
        try:
            async for chunk_text in llm_service.chat_stream(messages, max_tokens=4096):
                full_text += chunk_text
                yield {"event": "chunk", "data": chunk_text}

            yield {"event": "result", "data": full_text}
            yield {"event": "done", "data": ""}
        except Exception as e:
            yield {"event": "error", "data": str(e)}

    return EventSourceResponse(event_generator())
```

- [ ] **Step 3: 更新 main.py 注册路由**

```python
from app.routers import essay_review, analysis, question, study_plan

app.include_router(essay_review.router)
app.include_router(analysis.router)
app.include_router(question.router)
app.include_router(study_plan.router)
```

- [ ] **Step 4: 创建 Java 实体 + Mapper**

```java
// backend/src/main/java/com/gongkao/entity/StudyPlan.java
package com.gongkao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("study_plan")
public class StudyPlan {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String dateRange;
    private BigDecimal progress;
    private String focusAreas;       // JSON
    private String dailyTasks;       // JSON
    private String milestones;       // JSON
    private String recommendations;
    private String moduleAccuracy;   // JSON
    private String weakModules;      // JSON
    private String topWrongPoints;   // JSON
    private Boolean isArchived;
    private LocalDate analysisDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

// backend/src/main/java/com/gongkao/mapper/StudyPlanMapper.java
package com.gongkao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gongkao.entity.StudyPlan;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface StudyPlanMapper extends BaseMapper<StudyPlan> {

    @Select("SELECT * FROM study_plan WHERE user_id = #{userId} AND is_archived = false ORDER BY created_at DESC")
    List<StudyPlan> selectActive(@Param("userId") Long userId);

    @Select("SELECT * FROM study_plan WHERE user_id = #{userId} AND is_archived = true ORDER BY created_at DESC")
    List<StudyPlan> selectArchived(@Param("userId") Long userId);
}
```

- [ ] **Step 5: 创建 StudyPlanVO + Service + Controller**

```java
// backend/src/main/java/com/gongkao/dto/StudyPlanVO.java
package com.gongkao.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class StudyPlanVO {
    private Long id;
    private String dateRange;
    private BigDecimal progress;
    private String focusAreas;
    private String dailyTasks;
    private String milestones;
    private String recommendations;
    private String moduleAccuracy;
    private String weakModules;
    private Boolean isArchived;
    private LocalDate analysisDate;
}
```

```java
// backend/src/main/java/com/gongkao/service/StudyPlanService.java
package com.gongkao.service;

import com.gongkao.dto.StudyPlanVO;
import com.gongkao.entity.StudyPlan;
import com.gongkao.mapper.StudyPlanMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StudyPlanService {

    private final StudyPlanMapper mapper;

    public List<StudyPlanVO> getActivePlans(Long userId) {
        return mapper.selectActive(userId).stream().map(this::toVO).collect(Collectors.toList());
    }

    public List<StudyPlanVO> getArchivedPlans(Long userId) {
        return mapper.selectArchived(userId).stream().map(this::toVO).collect(Collectors.toList());
    }

    public StudyPlanVO savePlan(Long userId, String dateRange, String dailyTasks,
                                 String milestones, String recommendations, String focusAreas) {
        StudyPlan plan = new StudyPlan();
        plan.setUserId(userId);
        plan.setDateRange(dateRange);
        plan.setDailyTasks(dailyTasks);
        plan.setMilestones(milestones);
        plan.setRecommendations(recommendations);
        plan.setFocusAreas(focusAreas);
        plan.setProgress(java.math.BigDecimal.ZERO);
        plan.setIsArchived(false);
        mapper.insert(plan);
        return toVO(plan);
    }

    public void archivePlan(Long planId, Long userId) {
        StudyPlan plan = mapper.selectById(planId);
        if (plan != null && plan.getUserId().equals(userId)) {
            plan.setIsArchived(true);
            mapper.updateById(plan);
        }
    }

    public void updateDailyTask(Long planId, Long userId, String dailyTasks) {
        StudyPlan plan = mapper.selectById(planId);
        if (plan != null && plan.getUserId().equals(userId)) {
            plan.setDailyTasks(dailyTasks);
            mapper.updateById(plan);
        }
    }

    private StudyPlanVO toVO(StudyPlan e) {
        StudyPlanVO vo = new StudyPlanVO();
        BeanUtils.copyProperties(e, vo);
        return vo;
    }
}
```

```java
// backend/src/main/java/com/gongkao/controller/StudyPlanController.java
package com.gongkao.controller;

import com.gongkao.common.Result;
import com.gongkao.dto.StudyPlanVO;
import com.gongkao.service.StudyPlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/study-plan")
@RequiredArgsConstructor
public class StudyPlanController {

    private final StudyPlanService service;

    @GetMapping("/archive")
    public Result<List<StudyPlanVO>> getActive(@AuthenticationPrincipal Long userId) {
        return Result.ok(service.getActivePlans(userId));
    }

    @GetMapping("/archived")
    public Result<List<StudyPlanVO>> getArchived(@AuthenticationPrincipal Long userId) {
        return Result.ok(service.getArchivedPlans(userId));
    }

    @PostMapping("/update-daily-task")
    public Result<Void> updateDailyTask(
            @AuthenticationPrincipal Long userId,
            @RequestBody Map<String, Object> body) {
        Long planId = Long.valueOf(body.get("planId").toString());
        String dailyTasks = body.get("dailyTasks").toString();
        service.updateDailyTask(planId, userId, dailyTasks);
        return Result.ok(null);
    }
}
```

- [ ] **Step 6: 创建 StudyPlan.vue**

```vue
<!-- frontend/.vitepress/theme/components/StudyPlan.vue -->
<template>
  <div class="study-plan-page">
    <h2>学习计划</h2>

    <div class="generate-section" v-if="!hasActivePlan">
      <button class="btn-generate" @click="generatePlan" :disabled="generating">
        {{ generating ? '生成中...' : '生成学习计划' }}
      </button>
      <div v-if="generating" class="stream-output">{{ streamText }}</div>
    </div>

    <!-- 当前计划 -->
    <div v-if="activePlans.length" class="plan-list">
      <div v-for="plan in activePlans" :key="plan.id" class="plan-card">
        <div class="plan-header">
          <h3>{{ plan.dateRange }}</h3>
          <button @click="archivePlan(plan.id)">归档</button>
        </div>
        <div v-if="plan.focusAreas" class="section">
          <h4>重点方向</h4>
          <p>{{ plan.focusAreas }}</p>
        </div>
        <div v-if="plan.dailyTasks" class="section">
          <h4>每日任务</h4>
          <div class="tasks-preview">{{ plan.dailyTasks }}</div>
        </div>
        <div v-if="plan.recommendations" class="section">
          <h4>建议</h4>
          <p>{{ plan.recommendations }}</p>
        </div>
      </div>
    </div>

    <!-- 归档计划 -->
    <div v-if="archivedPlans.length">
      <h3>历史计划</h3>
      <div v-for="plan in archivedPlans" :key="plan.id" class="plan-card archived">
        <span>{{ plan.dateRange }}</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { get, post, aiApi } from '../utils/api.js'

const activePlans = ref([])
const archivedPlans = ref([])
const generating = ref(false)
const streamText = ref('')

const hasActivePlan = computed(() => activePlans.value.length > 0)

async function loadPlans() {
  const [activeRes, archivedRes] = await Promise.all([
    get('/study-plan/archive'),
    get('/study-plan/archived'),
  ])
  if (activeRes.code === 0) activePlans.value = activeRes.data
  if (archivedRes.code === 0) archivedPlans.value = archivedRes.data
}

async function generatePlan() {
  generating.value = true
  streamText.value = ''
  let fullText = ''

  await aiApi.analysisStream(
    { days: 30 },
    (chunk) => { streamText.value += chunk; fullText += chunk },
    async (result) => {
      // 保存计划到后端
      await post('/study-plan/update-daily-task', {
        planId: 0, // 新建
        dateRange: new Date().toISOString().slice(0, 10) + ' ~ ' +
                   new Date(Date.now() + 30*86400000).toISOString().slice(0, 10),
        dailyTasks: fullText,
      })
    },
    (err) => { console.error(err) },
    () => { generating.value = false; loadPlans() }
  )
}

async function archivePlan(id) {
  await post('/study-plan/update-daily-task', { planId: id, archive: true })
  loadPlans()
}

onMounted(loadPlans)
</script>

<style scoped>
.study-plan-page { max-width: 960px; margin: 0 auto; padding: 20px; }
.study-plan-page h2 { margin: 0 0 16px; }
.generate-section { text-align: center; margin: 24px 0; }
.btn-generate { padding: 12px 32px; background: var(--vp-c-brand); color: #fff; border: none; border-radius: 6px; font-size: 16px; cursor: pointer; }
.btn-generate:disabled { opacity: 0.6; cursor: not-allowed; }
.stream-output { margin-top: 12px; padding: 12px; background: var(--vp-c-bg-soft); border-radius: 6px; white-space: pre-wrap; font-size: 13px; max-height: 300px; overflow-y: auto; text-align: left; }
.plan-list { display: flex; flex-direction: column; gap: 12px; margin-bottom: 24px; }
.plan-card { padding: 16px; background: var(--vp-c-bg-soft); border-radius: 8px; }
.plan-card.archived { opacity: 0.6; }
.plan-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.plan-header h3 { margin: 0; font-size: 16px; }
.plan-header button { padding: 4px 12px; border: 1px solid var(--vp-c-divider); border-radius: 4px; background: var(--vp-c-bg); cursor: pointer; }
.section { margin-top: 12px; }
.section h4 { margin: 0 0 4px; font-size: 14px; color: var(--vp-c-text-2); }
.tasks-preview { font-size: 14px; max-height: 200px; overflow-y: auto; }
</style>
```

- [ ] **Step 7: 更新页面入口**

```markdown
<!-- frontend/pages/study-plan/index.md -->
---
layout: page
title: 学习计划
---

<StudyPlan />
```

- [ ] **Step 8: 验证编译 + Commit**

Run: `cd backend && mvn compile -q`
Expected: BUILD SUCCESS

```bash
git add ai-service/app/services/plan_generator.py \
        ai-service/app/routers/study_plan.py \
        ai-service/app/main.py \
        backend/src/main/java/com/gongkao/entity/StudyPlan.java \
        backend/src/main/java/com/gongkao/mapper/StudyPlanMapper.java \
        backend/src/main/java/com/gongkao/dto/StudyPlanVO.java \
        backend/src/main/java/com/gongkao/service/StudyPlanService.java \
        backend/src/main/java/com/gongkao/controller/StudyPlanController.java \
        frontend/.vitepress/theme/components/StudyPlan.vue \
        frontend/pages/study-plan/index.md
git commit -m "feat(P5): add study plan generation with AI streaming and archival"
```

---

## Task 7: 上岸秘籍 — 树形目录 + 前端

**Files:**
- Create: `backend/src/main/java/com/gongkao/entity/Secret.java`
- Create: `backend/src/main/java/com/gongkao/mapper/SecretMapper.java`
- Create: `backend/src/main/java/com/gongkao/service/SecretService.java`
- Create: `backend/src/main/java/com/gongkao/controller/SecretController.java`
- Create: `frontend/.vitepress/theme/components/SecretsContent.vue`
- Create: `frontend/.vitepress/theme/components/SecretsSidebar.vue`
- Modify: `frontend/pages/secrets/index.md`

- [ ] **Step 1: 创建 Secret 实体 + Mapper**

```java
// backend/src/main/java/com/gongkao/entity/Secret.java
package com.gongkao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("secret")
public class Secret {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long parentId;
    private String title;
    private String type; // xingce, shenlun
    private String content;
    private Integer sortOrder;
}
```

```java
// backend/src/main/java/com/gongkao/mapper/SecretMapper.java
package com.gongkao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gongkao.entity.Secret;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SecretMapper extends BaseMapper<Secret> {

    @Select("SELECT * FROM secret WHERE type = #{type} ORDER BY sort_order ASC, id ASC")
    List<Secret> selectByType(@Param("type") String type);
}
```

- [ ] **Step 2: 创建 SecretService + Controller**

```java
// backend/src/main/java/com/gongkao/service/SecretService.java
package com.gongkao.service;

import com.gongkao.entity.Secret;
import com.gongkao.mapper.SecretMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SecretService {

    private final SecretMapper mapper;

    /**
     * 获取树形结构
     */
    public List<Map<String, Object>> getTree(String type) {
        List<Secret> all = mapper.selectByType(type);
        Map<Long, List<Secret>> childrenMap = all.stream()
                .filter(s -> s.getParentId() != null)
                .collect(Collectors.groupingBy(Secret::getParentId));

        return all.stream()
                .filter(s -> s.getParentId() == null)
                .map(s -> toTreeNode(s, childrenMap))
                .collect(Collectors.toList());
    }

    public Secret getArticle(Long id) {
        return mapper.selectById(id);
    }

    private Map<String, Object> toTreeNode(Secret s, Map<Long, List<Secret>> childrenMap) {
        Map<String, Object> node = new java.util.LinkedHashMap<>();
        node.put("id", s.getId());
        node.put("title", s.getTitle());
        node.put("type", s.getType());
        List<Secret> children = childrenMap.getOrDefault(s.getId(), List.of());
        if (!children.isEmpty()) {
            node.put("children", children.stream()
                    .map(c -> toTreeNode(c, childrenMap))
                    .collect(Collectors.toList()));
        }
        if (s.getContent() != null) {
            node.put("content", s.getContent());
        }
        return node;
    }
}
```

```java
// backend/src/main/java/com/gongkao/controller/SecretController.java
package com.gongkao.controller;

import com.gongkao.common.Result;
import com.gongkao.entity.Secret;
import com.gongkao.service.SecretService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/secrets")
@RequiredArgsConstructor
public class SecretController {

    private final SecretService service;

    @GetMapping("/tree")
    public Result<List<Map<String, Object>>> getTree(
            @RequestParam(defaultValue = "xingce") String type) {
        return Result.ok(service.getTree(type));
    }

    @GetMapping("/article/{id}")
    public Result<Secret> getArticle(@PathVariable Long id) {
        return Result.ok(service.getArticle(id));
    }
}
```

- [ ] **Step 3: 创建 SecretsSidebar.vue**

```vue
<!-- frontend/.vitepress/theme/components/SecretsSidebar.vue -->
<template>
  <div class="secrets-sidebar">
    <div class="tabs">
      <button :class="{ active: type === 'xingce' }" @click="$emit('change-type', 'xingce')">行测</button>
      <button :class="{ active: type === 'shenlun' }" @click="$emit('change-type', 'shenlun')">申论</button>
    </div>
    <div class="tree">
      <div v-for="node in tree" :key="node.id" class="tree-node">
        <div class="node-title" @click="toggleOrSelect(node)">
          <span class="arrow" v-if="node.children">{{ expanded[node.id] ? '▼' : '▶' }}</span>
          {{ node.title }}
        </div>
        <div v-if="node.children && expanded[node.id]" class="children">
          <div v-for="child in node.children" :key="child.id"
               class="node-title child" @click="$emit('select', child)">
            {{ child.title }}
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'

const props = defineProps({
  tree: { type: Array, default: () => [] },
  type: { type: String, default: 'xingce' },
})
defineEmits(['change-type', 'select'])

const expanded = ref({})

function toggleOrSelect(node) {
  if (node.children) {
    expanded.value[node.id] = !expanded.value[node.id]
  }
}
</script>

<style scoped>
.secrets-sidebar { width: 240px; flex-shrink: 0; }
.tabs { display: flex; margin-bottom: 12px; }
.tabs button { flex: 1; padding: 6px; border: 1px solid var(--vp-c-divider); background: var(--vp-c-bg); cursor: pointer; font-size: 13px; }
.tabs button.active { background: var(--vp-c-brand); color: #fff; border-color: var(--vp-c-brand); }
.tree-node { margin-bottom: 2px; }
.node-title { padding: 6px 8px; cursor: pointer; border-radius: 4px; font-size: 14px; }
.node-title:hover { background: var(--vp-c-bg-soft); }
.node-title.child { padding-left: 24px; font-size: 13px; }
.arrow { margin-right: 4px; font-size: 10px; }
.children { margin-left: 8px; }
</style>
```

- [ ] **Step 4: 创建 SecretsContent.vue**

```vue
<!-- frontend/.vitepress/theme/components/SecretsContent.vue -->
<template>
  <div class="secrets-page">
    <SecretsSidebar :tree="tree" :type="type"
                     @change-type="changeType" @select="selectArticle" />
    <div class="content-area">
      <div v-if="article" class="article">
        <h2>{{ article.title }}</h2>
        <div v-html="article.content"></div>
      </div>
      <div v-else class="empty">请从左侧选择文章</div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { get } from '../utils/api.js'
import SecretsSidebar from './SecretsSidebar.vue'

const tree = ref([])
const article = ref(null)
const type = ref('xingce')

async function loadTree() {
  const res = await get('/secrets/tree', { type: type.value })
  if (res.code === 0) tree.value = res.data
}

function changeType(t) { type.value = t; article.value = null; loadTree() }

async function selectArticle(node) {
  if (node.content) { article.value = node; return }
  if (!node.children) {
    const res = await get(`/secrets/article/${node.id}`)
    if (res.code === 0) article.value = res.data
  }
}

onMounted(loadTree)
</script>

<style scoped>
.secrets-page { display: flex; gap: 24px; max-width: 1100px; margin: 0 auto; padding: 20px; }
.content-area { flex: 1; min-width: 0; }
.article h2 { margin: 0 0 16px; }
.article div { line-height: 1.8; font-size: 15px; }
.empty { text-align: center; padding: 60px 0; color: var(--vp-c-text-2); }
</style>
```

- [ ] **Step 5: 更新页面入口**

```markdown
<!-- frontend/pages/secrets/index.md -->
---
layout: page
title: 上岸秘籍
---

<SecretsContent />
```

- [ ] **Step 6: 验证编译 + Commit**

Run: `cd backend && mvn compile -q`
Expected: BUILD SUCCESS

```bash
git add backend/src/main/java/com/gongkao/entity/Secret.java \
        backend/src/main/java/com/gongkao/mapper/SecretMapper.java \
        backend/src/main/java/com/gongkao/service/SecretService.java \
        backend/src/main/java/com/gongkao/controller/SecretController.java \
        frontend/.vitepress/theme/components/SecretsContent.vue \
        frontend/.vitepress/theme/components/SecretsSidebar.vue \
        frontend/pages/secrets/index.md
git commit -m "feat(P5): add secrets tree structure with sidebar navigation"
```

---

## Task 8: 收藏/反馈/进站必读/行为追踪/个人中心

**Files:**
- Create: `backend/src/main/java/com/gongkao/entity/UserFavorite.java`
- Create: `backend/src/main/java/com/gongkao/entity/Feedback.java`
- Create: `backend/src/main/java/com/gongkao/entity/StatsTrack.java`
- Create: `backend/src/main/java/com/gongkao/entity/SiteConfig.java`
- Create: `backend/src/main/java/com/gongkao/mapper/UserFavoriteMapper.java`
- Create: `backend/src/main/java/com/gongkao/mapper/FeedbackMapper.java`
- Create: `backend/src/main/java/com/gongkao/mapper/StatsTrackMapper.java`
- Create: `backend/src/main/java/com/gongkao/mapper/SiteConfigMapper.java`
- Create: `backend/src/main/java/com/gongkao/service/FavoriteService.java`
- Create: `backend/src/main/java/com/gongkao/service/FeedbackService.java`
- Create: `backend/src/main/java/com/gongkao/service/StatsService.java`
- Create: `backend/src/main/java/com/gongkao/service/SiteConfigService.java`
- Create: `backend/src/main/java/com/gongkao/service/UserProfileService.java`
- Create: `backend/src/main/java/com/gongkao/controller/FavoriteController.java`
- Create: `backend/src/main/java/com/gongkao/controller/FeedbackController.java`
- Create: `backend/src/main/java/com/gongkao/controller/StatsController.java`
- Create: `backend/src/main/java/com/gongkao/controller/UserController.java`
- Create: `frontend/.vitepress/theme/components/MustRead.vue`
- Modify: `frontend/pages/must-read/index.md`

- [ ] **Step 1: 创建实体**

```java
// backend/src/main/java/com/gongkao/entity/UserFavorite.java
package com.gongkao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_favorite")
public class UserFavorite {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String targetType; // idiom, high_freq_word, high_freq_idiom, secret, question
    private Long targetId;
    private LocalDateTime createdAt;
}

// backend/src/main/java/com/gongkao/entity/Feedback.java
package com.gongkao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("feedback")
public class Feedback {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String content;
    private String reply;
    private String status; // pending, replied
    private LocalDateTime createdAt;
}

// backend/src/main/java/com/gongkao/entity/StatsTrack.java
package com.gongkao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("stats_track")
public class StatsTrack {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String visitorId;
    private String event;
    private String metadata; // JSON
    private LocalDateTime createdAt;
}

// backend/src/main/java/com/gongkao/entity/SiteConfig.java
package com.gongkao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("site_config")
public class SiteConfig {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String configKey;
    private String configValue;
    private LocalDateTime updatedAt;
}
```

- [ ] **Step 2: 创建 Mapper（4个）**

```java
// backend/src/main/java/com/gongkao/mapper/UserFavoriteMapper.java
package com.gongkao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gongkao.entity.UserFavorite;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface UserFavoriteMapper extends BaseMapper<UserFavorite> {

    @Select("SELECT * FROM user_favorite WHERE user_id = #{userId} " +
            "AND (#{type} IS NULL OR target_type = #{type}) ORDER BY created_at DESC")
    List<UserFavorite> selectByUser(@Param("userId") Long userId, @Param("type") String type);
}

// backend/src/main/java/com/gongkao/mapper/FeedbackMapper.java
package com.gongkao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gongkao.entity.Feedback;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface FeedbackMapper extends BaseMapper<Feedback> {

    @Select("SELECT * FROM feedback WHERE user_id = #{userId} ORDER BY created_at DESC")
    List<Feedback> selectByUserId(@Param("userId") Long userId);
}

// backend/src/main/java/com/gongkao/mapper/StatsTrackMapper.java
package com.gongkao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gongkao.entity.StatsTrack;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface StatsTrackMapper extends BaseMapper<StatsTrack> {
}

// backend/src/main/java/com/gongkao/mapper/SiteConfigMapper.java
package com.gongkao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gongkao.entity.SiteConfig;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SiteConfigMapper extends BaseMapper<SiteConfig> {
}
```

- [ ] **Step 3: 创建 Service（5个）**

```java
// backend/src/main/java/com/gongkao/service/FavoriteService.java
package com.gongkao.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gongkao.entity.UserFavorite;
import com.gongkao.mapper.UserFavoriteMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FavoriteService {

    private final UserFavoriteMapper mapper;

    public List<UserFavorite> listByUser(Long userId, String type) {
        return mapper.selectByUser(userId, type);
    }

    public void add(Long userId, String targetType, Long targetId) {
        LambdaQueryWrapper<UserFavorite> w = new LambdaQueryWrapper<>();
        w.eq(UserFavorite::getUserId, userId)
         .eq(UserFavorite::getTargetType, targetType)
         .eq(UserFavorite::getTargetId, targetId);
        if (mapper.selectCount(w) > 0) return; // 已收藏
        UserFavorite f = new UserFavorite();
        f.setUserId(userId);
        f.setTargetType(targetType);
        f.setTargetId(targetId);
        mapper.insert(f);
    }

    public void remove(Long id, Long userId) {
        UserFavorite f = mapper.selectById(id);
        if (f != null && f.getUserId().equals(userId)) mapper.deleteById(id);
    }
}

// backend/src/main/java/com/gongkao/service/FeedbackService.java
package com.gongkao.service;

import com.gongkao.entity.Feedback;
import com.gongkao.mapper.FeedbackMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FeedbackService {

    private final FeedbackMapper mapper;

    public List<Feedback> listByUser(Long userId) { return mapper.selectByUserId(userId); }

    public void submit(Long userId, String content) {
        Feedback f = new Feedback();
        f.setUserId(userId);
        f.setContent(content);
        f.setStatus("pending");
        mapper.insert(f);
    }
}

// backend/src/main/java/com/gongkao/service/StatsService.java
package com.gongkao.service;

import com.gongkao.entity.StatsTrack;
import com.gongkao.mapper.StatsTrackMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class StatsService {

    private final StatsTrackMapper mapper;

    public void track(String visitorId, String event, String metadata) {
        StatsTrack s = new StatsTrack();
        s.setVisitorId(visitorId);
        s.setEvent(event);
        s.setMetadata(metadata);
        mapper.insert(s);
    }

    public long countTodayOnline() {
        // 简化：统计今日有 event 的不同 visitorId
        return mapper.selectCount(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<StatsTrack>()
                .ge(StatsTrack::getCreatedAt, LocalDateTime.now().toLocalDate().atStartOfDay()));
    }
}

// backend/src/main/java/com/gongkao/service/SiteConfigService.java
package com.gongkao.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gongkao.entity.SiteConfig;
import com.gongkao.mapper.SiteConfigMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SiteConfigService {

    private final SiteConfigMapper mapper;

    public String getConfig(String key) {
        LambdaQueryWrapper<SiteConfig> w = new LambdaQueryWrapper<>();
        w.eq(SiteConfig::getConfigKey, key);
        SiteConfig config = mapper.selectOne(w);
        return config != null ? config.getConfigValue() : null;
    }
}

// backend/src/main/java/com/gongkao/service/UserProfileService.java
package com.gongkao.service;

import com.gongkao.entity.User;
import com.gongkao.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final UserMapper userMapper;
    private final FileService fileService;

    public User getProfile(Long userId) { return userMapper.selectById(userId); }

    public User updateProfile(Long userId, String nickname, String avatar) {
        User user = userMapper.selectById(userId);
        if (nickname != null) user.setNickname(nickname);
        if (avatar != null) user.setAvatar(avatar);
        userMapper.updateById(user);
        return user;
    }
}
```

- [ ] **Step 4: 创建 Controller（4个）**

```java
// backend/src/main/java/com/gongkao/controller/FavoriteController.java
package com.gongkao.controller;

import com.gongkao.common.Result;
import com.gongkao.entity.UserFavorite;
import com.gongkao.service.FavoriteService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/favorites")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService service;

    @GetMapping
    public Result<List<UserFavorite>> list(
            @AuthenticationPrincipal Long userId,
            @RequestParam(required = false) String type) {
        return Result.ok(service.listByUser(userId, type));
    }

    @PostMapping
    public Result<Void> add(@AuthenticationPrincipal Long userId,
                             @RequestBody Map<String, Object> body) {
        service.add(userId, body.get("type").toString(), Long.valueOf(body.get("targetId").toString()));
        return Result.ok(null);
    }

    @DeleteMapping("/{id}")
    public Result<Void> remove(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
        service.remove(id, userId);
        return Result.ok(null);
    }
}

// backend/src/main/java/com/gongkao/controller/FeedbackController.java
package com.gongkao.controller;

import com.gongkao.common.Result;
import com.gongkao.entity.Feedback;
import com.gongkao.service.FeedbackService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/feedback")
@RequiredArgsConstructor
public class FeedbackController {

    private final FeedbackService service;

    @PostMapping("/submit")
    public Result<Void> submit(@AuthenticationPrincipal Long userId,
                                @RequestBody Map<String, String> body) {
        service.submit(userId, body.get("content"));
        return Result.ok(null);
    }

    @GetMapping("/my")
    public Result<List<Feedback>> listMy(@AuthenticationPrincipal Long userId) {
        return Result.ok(service.listByUser(userId));
    }
}

// backend/src/main/java/com/gongkao/controller/StatsController.java
package com.gongkao.controller;

import com.gongkao.common.Result;
import com.gongkao.service.StatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class StatsController {

    private final StatsService service;

    @PostMapping("/track")
    public Result<Void> track(@RequestBody Map<String, String> body) {
        service.track(body.getOrDefault("visitorId", ""),
                      body.getOrDefault("event", ""),
                      body.getOrDefault("metadata", null));
        return Result.ok(null);
    }

    @GetMapping("/today-online")
    public Result<Long> todayOnline() {
        return Result.ok(service.countTodayOnline());
    }
}

// backend/src/main/java/com/gongkao/controller/UserController.java
package com.gongkao.controller;

import com.gongkao.common.Result;
import com.gongkao.entity.User;
import com.gongkao.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserProfileService service;

    @GetMapping("/profile")
    public Result<User> getProfile(@AuthenticationPrincipal Long userId) {
        User user = service.getProfile(userId);
        user.setPasswordHash(null); // 不返回密码
        return Result.ok(user);
    }

    @PutMapping("/profile")
    public Result<User> updateProfile(
            @AuthenticationPrincipal Long userId,
            @RequestBody Map<String, String> body) {
        return Result.ok(service.updateProfile(
                userId, body.get("nickname"), body.get("avatar")));
    }
}
```

同时在 P1 已创建的 `AuthController` 所在包中追加一个 SiteConfigController：

```java
// backend/src/main/java/com/gongkao/controller/SiteConfigController.java
package com.gongkao.controller;

import com.gongkao.common.Result;
import com.gongkao.service.SiteConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/site-config")
@RequiredArgsConstructor
public class SiteConfigController {

    private final SiteConfigService service;

    @GetMapping
    public Result<String> getConfig(@RequestParam String key) {
        return Result.ok(service.getConfig(key));
    }
}
```

- [ ] **Step 5: 创建 MustRead.vue**

```vue
<!-- frontend/.vitepress/theme/components/MustRead.vue -->
<template>
  <div class="must-read">
    <h2>进站必读</h2>
    <div v-if="loading" class="loading">加载中...</div>
    <div v-else class="content" v-html="content"></div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { get } from '../utils/api.js'

const content = ref('')
const loading = ref(false)

async function loadContent() {
  loading.value = true
  try {
    const res = await get('/site-config', { key: 'must-read' })
    if (res.code === 0 && res.data) content.value = res.data
    else content.value = '<p>欢迎来到 BALA 公考！请先阅读以下内容：</p><ul><li>本站为学习工具，请合理使用</li><li>题目数据来源于公开真题</li><li>AI 批改仅供参考</li></ul>'
  } finally { loading.value = false }
}

onMounted(loadContent)
</script>

<style scoped>
.must-read { max-width: 960px; margin: 0 auto; padding: 20px; }
.must-read h2 { margin: 0 0 16px; }
.content { line-height: 1.8; font-size: 15px; }
.loading { text-align: center; padding: 40px; color: var(--vp-c-text-2); }
</style>
```

- [ ] **Step 6: 更新页面入口**

```markdown
<!-- frontend/pages/must-read/index.md -->
---
layout: page
title: 进站必读
---

<MustRead />
```

- [ ] **Step 7: 验证编译 + Commit**

Run: `cd backend && mvn compile -q`
Expected: BUILD SUCCESS

```bash
git add backend/src/main/java/com/gongkao/entity/UserFavorite.java \
        backend/src/main/java/com/gongkao/entity/Feedback.java \
        backend/src/main/java/com/gongkao/entity/StatsTrack.java \
        backend/src/main/java/com/gongkao/entity/SiteConfig.java \
        backend/src/main/java/com/gongkao/mapper/UserFavoriteMapper.java \
        backend/src/main/java/com/gongkao/mapper/FeedbackMapper.java \
        backend/src/main/java/com/gongkao/mapper/StatsTrackMapper.java \
        backend/src/main/java/com/gongkao/mapper/SiteConfigMapper.java \
        backend/src/main/java/com/gongkao/service/FavoriteService.java \
        backend/src/main/java/com/gongkao/service/FeedbackService.java \
        backend/src/main/java/com/gongkao/service/StatsService.java \
        backend/src/main/java/com/gongkao/service/SiteConfigService.java \
        backend/src/main/java/com/gongkao/service/UserProfileService.java \
        backend/src/main/java/com/gongkao/controller/FavoriteController.java \
        backend/src/main/java/com/gongkao/controller/FeedbackController.java \
        backend/src/main/java/com/gongkao/controller/StatsController.java \
        backend/src/main/java/com/gongkao/controller/UserController.java \
        backend/src/main/java/com/gongkao/controller/SiteConfigController.java \
        frontend/.vitepress/theme/components/MustRead.vue \
        frontend/pages/must-read/index.md
git commit -m "feat(P5): add favorites, feedback, stats tracking, site config, user profile, must-read"
```

---

## P5 验收清单

| # | 验收项 | 验证方式 |
|---|--------|---------|
| 1 | 错题自动归集 | 做题提交后，错题出现在 `/wrong-questions/` |
| 2 | 错题查看/删除 | 错题列表展示，可删除 |
| 3 | AI 补练（占位） | `/ai-recommended/` 页面可访问 |
| 4 | 每日打卡 | `/checkin/` 可打卡，不可重复打卡 |
| 5 | 打卡任务 CRUD | 创建/完成/删除任务 |
| 6 | 打卡日历 | 显示最近 7 天打卡状态 |
| 7 | 成语查询 | 输入关键字返回匹配成语 |
| 8 | 高频词语 | 按分类筛选，关键字搜索 |
| 9 | 高频成语 | 按分类筛选，关键字搜索 |
| 10 | 热榜代理 | `/今日热榜/` 显示知乎/微博/百度热榜 |
| 11 | 新闻代理 | `/news/` 显示新闻联播摘要 |
| 12 | 今日史事 | `/history-today/` 显示历史事件 |
| 13 | Redis 缓存 | 第三方 API 30 分钟内不重复请求 |
| 14 | 学习计划生成 | 点击生成 → SSE 流式输出 → 保存 |
| 15 | 学习计划归档 | 可归档当前计划，查看历史 |
| 16 | 上岸秘籍树形目录 | 左侧导航树 + 右侧文章内容 |
| 17 | 收藏功能 | 可收藏成语/词语/题目，可查看收藏列表 |
| 18 | 意见反馈 | 可提交反馈，可查看历史 |
| 19 | 进站必读 | `/must-read/` 显示站点说明 |
| 20 | 行为追踪 | `POST /api/stats/track` 记录访问事件 |
| 21 | 今日在线 | `GET /api/stats/today-online` 返回统计 |
| 22 | 个人中心 | 查看和修改昵称/头像 |
| 23 | 所有页面路由 | 21 个页面全部可访问，无空白页 |

**P5 完成标志：** 以上 23 项全部通过 → 项目功能完整，可进行整体联调测试
