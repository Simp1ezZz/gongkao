# 做题历史功能设计

## 概述

为用户提供做题历史的查看功能，包括整卷练习和专项练习两种模式的记录。用户可以查看历史练习的成绩、用时、正确率等统计信息，并能回顾每道题的作答详情。

## 背景

当前已实现的做题流程（P3）：
1. 用户创建会话 → 做题 → 提交 → 批量提交答案 → 返回成绩
2. `practice_session` 表存储会话状态，`user_answer` 表存储逐题答案
3. 整卷练习通过 `paperId` 进入 OnlinePractice，专项练习通过 `questionIds` 进入

**现状问题：**
- 专项练习不创建 session（OnlinePractice 仅在 `paperId` 模式下创建 session），提交时 `sessionId` 为 null
- 因此 `practice_session` 表中没有专项练习记录，无法查询专项练习历史
- `practice_session.paper_id` 为 NOT NULL，专项练习没有对应的 paper

## 前置改动

### 1. 专项练习也创建 session

修改 `OnlinePractice.vue`：专项练习（`questionIds` 模式）也创建 session。

**前端改动：**
- 专项练习进入时，调用 `sessionApi.create({ module: '言语理解', questionCount: 20 })` 创建 session
- 已登录用户：先创建 session 再做题
- 未登录用户：不创建 session（保持现有 localStorage 逻辑）

**后端改动：**
- `SessionCreateRequest` 增加可选字段 `module` 和 `questionCount`
- `SessionService.createSession()` 支持 `paperId` 为 null 的情况
  - 有 `paperId` → 整卷练习（复用现有逻辑）
  - 无 `paperId`、有 `module` → 专项练习

### 2. 数据库变更

```sql
-- paper_id 改为可空，支持专项练习（无关联试卷）
ALTER TABLE practice_session MODIFY COLUMN paper_id BIGINT DEFAULT NULL;

-- 新增 module 列，专项练习存储模块名
ALTER TABLE practice_session ADD COLUMN module VARCHAR(50) DEFAULT NULL
    COMMENT '专项练习模块名，整卷练习为null' AFTER paper_id;
```

### 3. 区分整卷 vs 专项

- `paper_id IS NOT NULL` → `type = "paper"`（整卷练习）
- `paper_id IS NULL` → `type = "special"`（专项练习），module 存模块名

## 核心需求

1. **历史列表页**：展示用户所有已提交的练习记录，区分整卷练习和专项练习
2. **记录详情**：点击某条记录可查看逐题作答情况（对错、答案、解析）
3. **统计概览**：独立接口返回全局汇总（总练习次数、总做题量、平均正确率、总用时）
4. **分页加载**：支持分页查询历史记录
5. **只展示已提交记录**：ongoing/paused 状态的会话不在历史页展示

## 数据流

```
前端页面加载:
  GET /api/practice/history/summary → 统计概览
  GET /api/practice/history?page=1&pageSize=10&type=all → 历史列表
  Backend:
    1. 查询当前用户 status='submitted' 的 practice_session
    2. 关联 paper 表获取试卷标题（整卷），或使用 module 字段（专项）
    3. 关联 user_answer 按 session 聚合统计（正确/错误）
    4. 组装返回分页列表
  前端渲染历史列表

点击某条记录查看详情:
  GET /api/practice/history/{sessionId}
  Backend:
    1. 获取 session 信息（试卷/模块、用时、提交时间）
    2. 查询该 session 下所有 user_answer
    3. 关联 question 表获取题目内容、正确答案、解析
    4. 组装返回逐题详情
  前端渲染答题详情页
```

## API 设计

### GET /api/practice/history/summary

查询当前用户做题的全局统计概览。

**Response:**

```json
{
  "success": true,
  "data": {
    "totalSessions": 25,
    "totalQuestions": 1250,
    "correctQuestions": 900,
    "wrongQuestions": 300,
    "unansweredQuestions": 50,
    "avgAccuracy": 75.0,
    "totalTimeElapsed": 54000,
    "paperSessionCount": 10,
    "specialSessionCount": 15
  }
}
```

### GET /api/practice/history

查询当前用户的做题历史列表。

**Query Parameters:**

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `page` | int | 1 | 页码 |
| `pageSize` | int | 10 | 每页条数 |
| `type` | string | "all" | 筛选类型: `all`/`paper`(整卷)/`special`(专项) |

**Response:**

```json
{
  "success": true,
  "data": {
    "total": 25,
    "list": [
      {
        "sessionId": 42,
        "paperId": 5,
        "paperTitle": "2026年国考行测（行政执法卷）",
        "type": "paper",
        "module": null,
        "totalQuestions": 130,
        "correctCount": 95,
        "wrongCount": 30,
        "unansweredCount": 5,
        "accuracy": 76.0,
        "timeElapsed": 5400,
        "submittedAt": "2026-05-06 14:30:00"
      },
      {
        "sessionId": 41,
        "paperId": null,
        "type": "special",
        "module": "言语理解",
        "questionCount": 20,
        "totalQuestions": 20,
        "correctCount": 15,
        "wrongCount": 4,
        "unansweredCount": 1,
        "accuracy": 78.9,
        "timeElapsed": 600,
        "submittedAt": "2026-05-06 10:00:00"
      }
    ]
  }
}
```

**字段说明：**

- `type`: `paper` 整卷练习（有 paperId），`special` 专项练习
- `module`: 专项练习的模块名，整卷为 null
- `accuracy`: `correctCount / (correctCount + wrongCount) * 100`，仅计已答题
- `unansweredCount`: 有 user_answer 记录但 `is_correct IS NULL` 的题目数

### GET /api/practice/history/{sessionId}

查询单次练习的逐题详情。

**Response:**

```json
{
  "success": true,
  "data": {
    "sessionId": 42,
    "paperId": 5,
    "paperTitle": "2026年国考行测（行政执法卷）",
    "type": "paper",
    "module": null,
    "timeElapsed": 5400,
    "submittedAt": "2026-05-06 14:30:00",
    "summary": {
      "totalQuestions": 130,
      "correctCount": 95,
      "wrongCount": 30,
      "unansweredCount": 5,
      "accuracy": 76.0
    },
    "questions": [
      {
        "questionId": 101,
        "sortOrder": 1,
        "module": "政治理论",
        "content": "<p>题干 HTML</p>",
        "options": [{"label": "A", "text": "..."}, {"label": "B", "text": "..."}],
        "correctAnswer": "D",
        "userAnswer": "B",
        "isCorrect": false,
        "explanation": "<p>解析 HTML</p>"
      }
    ]
  }
}
```

**专项练习的标题显示：** `2026-05-06 10:00 言语理解 · 20题`（格式：`{submittedAt} {module} · {totalQuestions}题`）

## 后端实现

### 新增文件

| 文件 | 说明 |
|------|------|
| `controller/PracticeHistoryController.java` | 做题历史 API 控制器（3 个接口） |
| `service/PracticeHistoryService.java` | 做题历史查询逻辑 |
| `dto/PracticeHistoryItemVO.java` | 历史列表项 |
| `dto/PracticeHistoryDetailVO.java` | 历史详情（含逐题） |
| `dto/PracticeHistorySummaryVO.java` | 统计概览 |
| `dto/PracticeHistoryQuery.java` | 查询参数 (page, pageSize, type) |

### 修改文件

| 文件 | 改动 |
|------|------|
| `dto/SessionCreateRequest.java` | 增加 `module`、`questionCount` 可选字段 |
| `service/SessionService.java` | `createSession` 支持 paperId 为 null |
| `entity/PracticeSession.java` | 增加 `module` 字段 |
| `mapper/PracticeSessionMapper.java` | 适配 paper_id 可空 |

### 查询逻辑

**统计概览：**

```sql
SELECT
    COUNT(*) AS totalSessions,
    SUM(ua_stats.total) AS totalQuestions,
    SUM(ua_stats.correct) AS correctQuestions,
    SUM(ua_stats.wrong) AS wrongQuestions,
    SUM(ps.time_elapsed) AS totalTimeElapsed
FROM practice_session ps
LEFT JOIN (
    SELECT session_id,
           COUNT(*) AS total,
           SUM(CASE WHEN is_correct = 1 THEN 1 ELSE 0 END) AS correct,
           SUM(CASE WHEN is_correct = 0 THEN 1 ELSE 0 END) AS wrong
    FROM user_answer
    WHERE user_id = #{userId}
    GROUP BY session_id
) ua_stats ON ua_stats.session_id = ps.id
WHERE ps.user_id = #{userId}
  AND ps.status = 'submitted'
```

**历史列表查询：**

```sql
SELECT
    ps.id AS sessionId,
    ps.paper_id,
    ps.module,
    p.title AS paperTitle,
    ps.time_elapsed,
    ps.updated_at AS submittedAt
FROM practice_session ps
LEFT JOIN paper p ON ps.paper_id = p.id
WHERE ps.user_id = #{userId}
  AND ps.status = 'submitted'
ORDER BY ps.updated_at DESC
LIMIT #{offset}, #{pageSize}
```

每条记录的统计通过子查询或二次查询 `user_answer` 获取（按 session_id 聚合）。

**单条记录的逐题详情：**

```sql
SELECT
    ua.user_answer,
    ua.is_correct,
    q.id AS questionId,
    q.sort_order,
    q.module,
    q.content,
    q.options,
    q.answer AS correctAnswer,
    q.explanation
FROM user_answer ua
JOIN question q ON ua.question_id = q.id
WHERE ua.session_id = #{sessionId}
  AND ua.user_id = #{userId}
ORDER BY q.sort_order
```

## 前端实现

### 新增文件

| 文件 | 说明 |
|------|------|
| `theme/components/PracticeHistory.vue` | 做题历史页面组件 |
| `pages/practice/history/index.md` | 路由页面 |

### 修改文件

| 文件 | 改动 |
|------|------|
| `OnlinePractice.vue` | 专项练习也创建 session（传入 module） |
| `theme/utils/api.js` | 新增 `historyApi`（summary、list、getDetail） |
| `.vitepress/config.ts` | 导航栏/侧边栏添加"做题历史"入口 |
| `theme/index.ts` | 注册 `PracticeHistory` 组件 |

### 页面结构

```
┌─────────────────────────────────────────┐
│  做题历史                                │
│                                          │
│  ┌─ 统计概览 ─────────────────────────┐  │
│  │ 总练习 25次 │ 总做题 1250题 │      │  │
│  │ 平均正确率 75% │ 总用时 15h       │  │
│  └────────────────────────────────────┘  │
│                                          │
│  [全部] [整卷练习] [专项练习]    筛选Tab  │
│                                          │
│  ┌─ 记录卡片 ────────────────────────┐  │
│  │ 2026年国考行测（行政执法卷）        │  │
│  │ 正确率 76% │ 用时 1h30m │ 130题    │  │
│  │ 2026-05-06 14:30           → 详情  │  │
│  └────────────────────────────────────┘  │
│  ┌─ 记录卡片 ────────────────────────┐  │
│  │ 2026-05-06 10:00 言语理解 · 20题    │  │
│  │ 正确率 78.9% │ 用时 10m │ 20题      │  │
│  │                               → 详情  │  │
│  └────────────────────────────────────┘  │
│  ...                                     │
│                                          │
│  [加载更多]                              │
└─────────────────────────────────────────┘
```

**详情页：** 复用 OnlinePractice 的分析结果展示样式，显示每道题的作答情况、正确答案和解析。

### 登录要求

做题历史需要登录才能查看。未登录用户进入该页面时，显示登录引导提示。

## 配置变更

### 导航栏

在 `config.ts` 的 `nav` 中添加：

```js
{ text: '做题历史', link: '/practice/history/' }
```

### 侧边栏

在题库练习分组中添加：

```js
{ text: '做题历史', link: '/practice/history/' }
```

## 实现顺序

1. 数据库变更（ALTER TABLE）
2. 后端：SessionCreateRequest + SessionService 改造（支持专项 session）
3. 前端：OnlinePractice 专项练习创建 session
4. 后端：PracticeHistoryController + Service + DTO（3 个接口）
5. 前端：api.js 新增 historyApi
6. 前端：PracticeHistory.vue 组件 + 页面路由
7. 前端：config.ts 导航/侧边栏入口
