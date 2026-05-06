# 做题历史功能设计

## 概述

为用户提供做题历史的查看功能，包括整卷练习和专项练习两种模式的记录。用户可以查看历史练习的成绩、用时、正确率等统计信息，并能回顾每道题的作答详情。

## 背景

当前已实现的做题流程（P3）：
1. 用户创建会话 → 做题 → 提交 → 批量提交答案 → 返回成绩
2. `practice_session` 表存储会话状态，`user_answer` 表存储逐题答案
3. 整卷练习和专项练习均通过 `OnlinePractice` 组件完成，共用同一个提交流程

但提交后没有历史记录页面，用户无法回顾之前的练习。

## 核心需求

1. **历史列表页**：展示用户所有已提交的练习记录，区分整卷练习和专项练习
2. **记录详情**：点击某条记录可查看逐题作答情况（对错、答案、解析）
3. **统计概览**：展示总练习次数、总做题量、平均正确率等汇总数据
4. **分页加载**：支持分页查询历史记录

## 数据流

```
前端页面加载:
  GET /api/practice/history?page=1&pageSize=10&type=all
  Backend:
    1. 查询当前用户 status='submitted' 的 practice_session
    2. 关联 paper 表获取试卷标题
    3. 关联 user_answer 按 session 聚合统计（正确/错误/未答）
    4. 组装返回分页列表
  前端渲染历史列表

点击某条记录查看详情:
  GET /api/practice/history/{sessionId}
  Backend:
    1. 获取 session 信息（试卷、用时、提交时间）
    2. 查询该 session 下所有 user_answer
    3. 关联 question 表获取题目内容、正确答案、解析
    4. 组装返回逐题详情
  前端渲染答题详情页（复用 OnlinePractice 的分析展示逻辑）
```

## API 设计

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

- `type`: `paper` 表示整卷练习（关联了 paperId），`special` 表示专项练习
- `module`: 专项练习时的模块名称（如"言语理解"），整卷练习为 null
- `unansweredCount`: 未作答题目数 = totalQuestions - correctCount - wrongCount

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

## 后端实现

### 新增文件

| 文件 | 说明 |
|------|------|
| `controller/PracticeHistoryController.java` | 做题历史 API 控制器 |
| `service/PracticeHistoryService.java` | 做题历史查询逻辑 |
| `dto/PracticeHistoryItemVO.java` | 历史列表项 |
| `dto/PracticeHistoryDetailVO.java` | 历史详情 |
| `dto/PracticeHistoryQuery.java` | 查询参数 (page, pageSize, type) |

### 查询逻辑

**历史列表查询（核心 SQL）：**

```sql
SELECT
    ps.id AS sessionId,
    ps.paper_id,
    p.title AS paperTitle,
    ps.time_elapsed,
    ps.updated_at AS submittedAt,
    -- 专项练习的 module 从 session 的 answers JSON 或 URL 参数推断
FROM practice_session ps
LEFT JOIN paper p ON ps.paper_id = p.id
WHERE ps.user_id = #{userId}
  AND ps.status = 'submitted'
ORDER BY ps.updated_at DESC
LIMIT #{offset}, #{pageSize}
```

**单条记录的统计查询：**

```sql
SELECT
    COUNT(*) AS totalQuestions,
    SUM(CASE WHEN ua.is_correct = 1 THEN 1 ELSE 0 END) AS correctCount,
    SUM(CASE WHEN ua.is_correct = 0 THEN 1 ELSE 0 END) AS wrongCount,
    SUM(CASE WHEN ua.is_correct IS NULL THEN 1 ELSE 0 END) AS unansweredCount
FROM user_answer ua
WHERE ua.session_id = #{sessionId}
  AND ua.user_id = #{userId}
```

### 整卷 vs 专项的区分

整卷练习通过 `PaperList.startBankPractice()` 进入，URL 带有 `paperId`，session 关联了 `paper_id`。

专项练习通过 `PaperList.startSpecial()` 进入，URL 带有 `questionIds` 和 `module`，session 的 `paper_id` 为 null 或指向一个虚拟的"专项练习"记录。

区分策略：
- `paper_id IS NOT NULL` → `type = "paper"`（整卷练习）
- `paper_id IS NULL` → `type = "special"`（专项练习），module 从 session 的 `answers` JSON 或前端传入的参数获取

### 专项练习的 module 存储

当前专项练习创建 session 时未记录 module 信息。需要扩展：

**方案**: 在 `SessionCreateRequest` 中增加可选字段 `module`，前端专项练习创建 session 时传入 module 名称。session 表无需加列，复用现有的 `answers` JSON 字段或在 `practice_session` 表新增 `module` 列。

**推荐**: 在 `practice_session` 表新增 `module VARCHAR(50) DEFAULT NULL` 列，整卷练习为 null，专项练习存储模块名。

### 数据库变更

```sql
ALTER TABLE practice_session ADD COLUMN module VARCHAR(50) DEFAULT NULL
    COMMENT '专项练习模块名，整卷练习为null' AFTER paper_id;
```

## 前端实现

### 新增文件

| 文件 | 说明 |
|------|------|
| `theme/components/PracticeHistory.vue` | 做题历史页面组件 |
| `pages/practice/history/index.md` | 路由页面 |

### 页面结构

```
┌─────────────────────────────────────────┐
│  做题历史                                │
│                                          │
│  ┌─ 统计概览 ─────────────────────────┐  │
│  │ 总练习 25次 │ 总做题 1250题 │      │  │
│  │ 平均正确率 72.5% │ 总用时 15h     │  │
│  └────────────────────────────────────┘  │
│                                          │
│  [全部] [整卷练习] [专项练习]    筛选Tab  │
│                                          │
│  ┌─ 记录卡片 ────────────────────────┐  │
│  │ 📄 2026年国考行测（行政执法卷）    │  │
│  │ 正确率 76% │ 用时 1h30m │ 130题    │  │
│  │ 2026-05-06 14:30           → 详情  │  │
│  └────────────────────────────────────┘  │
│  ┌─ 记录卡片 ────────────────────────┐  │
│  │ 📝 专项练习 · 言语理解              │  │
│  │ 正确率 78.9% │ 用时 10m │ 20题      │  │
│  │ 2026-05-06 10:00           → 详情  │  │
│  └────────────────────────────────────┘  │
│  ...                                     │
│                                          │
│  [加载更多]                              │
└─────────────────────────────────────────┘
```

**详情页（在列表页内展开或跳转）：**

复用 `OnlinePractice` 的分析结果展示样式，显示每道题的作答情况、正确答案和解析。

### 前端改动

1. **`api.js`**: 新增 `historyApi`，包含 `list(params)` 和 `getDetail(sessionId)` 方法
2. **`config.ts`**: 导航栏或侧边栏添加"做题历史"入口
3. **`PaperList.vue`**: 专项练习创建 session 时传入 `module` 参数
4. **`theme/index.ts`**: 注册 `PracticeHistory` 组件

### 登录要求

做题历史需要登录才能查看。未登录用户进入该页面时，提示"请先登录后查看做题历史"并跳转登录页。

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
