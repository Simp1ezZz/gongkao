# BALA 公考网站 — 系统设计文档

> 日期：2026-05-04
> 目标：完整复刻 https://www.baijing1.top/ 的所有功能和样式

---

## 一、技术栈与版本

### 运行时环境

| 组件 | 版本 | 说明 |
|------|------|------|
| Java JDK | **21 (LTS)** | Spring Boot 3.x 推荐 |
| Node.js | **20 LTS** | VitePress 要求 |
| Python | **3.12** | FastAPI 推荐 |
| Maven | **3.9.x** | Java 构建工具 |

### 前端

| 组件 | 版本 | 说明 |
|------|------|------|
| VitePress | **2.0.0-alpha.15** | 与原站一致 |
| Vue 3 | VitePress 内置 | 无需单独指定 |

### Java 后端

| 组件 | 版本 | 说明 |
|------|------|------|
| Spring Boot | **3.5.12** | LTS 稳定版 |
| MyBatis-Plus | **3.5.12** | `mybatis-plus-spring-boot3-starter` |
| Spring Security | Spring Boot 内置 | JWT 认证 |
| JJWT | **0.12.6** | `io.jsonwebtoken:jjwt-api` |
| JavaMail | Spring Boot starter | QQ邮箱 SMTP |
| MinIO Java SDK | **8.5.14** | `io.minio:minio` |

### Python AI 微服务

| 组件 | 版本 | 说明 |
|------|------|------|
| FastAPI | **0.115.12** | 最新稳定 |
| uvicorn | **0.34.2** | ASGI 服务器 |
| httpx | **0.28.1** | 调用 LLM API |
| python-jose | **3.3.0** | JWT 验证(与 Java 同密钥) |
| pydantic | **2.10.6** | 数据模型 |
| sse-starlette | **2.2.1** | SSE 流式响应 |

### 数据与存储

| 组件 | 版本 | 说明 |
|------|------|------|
| MySQL | **9.7.0** | 最新稳定 |
| Redis | **8.6.2** | 最新稳定 |
| MinIO | **latest** | Docker 镜像，S3 兼容 |

### 部署

| 组件 | 版本 | 说明 |
|------|------|------|
| Docker Compose | **v2** | 一键启动所有服务 |

---

## 二、系统架构

```
┌─────────────┐
│  VitePress   │
│  (前端 SPA)  │
│  :5173       │
└──────┬───────┘
       │
       │  JWT Token (Java签发，Python验证，同密钥)
       │
       ├──────────────────────────────────┐
       │                                  │
       ▼                                  ▼
┌──────────────┐                  ┌──────────────┐
│ Spring Boot  │                  │   FastAPI    │
│ (主后端)      │                  │ (AI微服务)    │
│ :8080        │                  │ :8000        │
│              │                  │              │
│ 认证/CRUD    │                  │ AI流式接口    │
│ 业务逻辑     │                  │ LLM调用      │
└──┬───┬───┬───┘                  └──────┬───────┘
   │   │   │                             │
   │   │   │                      ┌──────▼───────┐
   │   │   │                      │  LLM Proxy   │
   │   │   │                      │ (OpenAI/     │
   │   │   │                      │  Anthropic)  │
   │   │   │                      └──────────────┘
   │   │   │
   ▼   ▼   ▼
┌─────┐┌─────┐┌───────┐
│MySQL││Redis││ MinIO │
│:3306││:6379││:9000  │
└─────┘└─────┘└───────┘
```

### API 路由分配

**Spring Boot (:8080) — 前缀 /api**

| 模块 | 端点 | 方法 |
|------|------|------|
| 认证 | `/api/auth/login-password` | POST |
| | `/api/auth/register` | POST |
| | `/api/auth/send-code` | POST |
| | `/api/auth/reset-password` | POST |
| | `/api/auth/refresh` | POST |
| 题库 | `/api/papers?category=&region=&page=&pageSize=` | GET |
| | `/api/papers/{id}` | GET |
| | `/api/papers/{id}/materials` | GET |
| | `/api/papers/questions/by-knowledge?module=&sub_module=&knowledge_point=` | GET |
| | `/api/papers/user-answers/batch` | POST |
| | `/api/papers/{id}/my-answers` | GET |
| 会话 | `/api/sessions` (创建/查询/更新/删除) | POST/GET/PUT/DELETE |
| 分析 | `/api/analysis/history` | GET |
| | `/api/analysis/{id}` | GET/DELETE |
| 申论批改记录 | `/api/essay-review` | GET |
| | `/api/essay-review/{id}` | GET/DELETE |
| 错题补练 | `/api/ai-question/my-questions` | GET |
| | `/api/ai-question/{id}` | DELETE |
| 题目AI解析 | `/api/questions/{id}/ai-analysis` | GET |
| 收藏 | `/api/favorites?type=&page=` | GET |
| | `/api/favorites` | POST |
| | `/api/favorites/{id}` | DELETE |
| 打卡 | `/api/checkin` | GET/POST |
| | `/api/checkin/tasks` | GET/POST |
| | `/api/checkin/tasks/{id}` | PUT/DELETE |
| | `/api/checkin/{timestamp}` | DELETE |
| 学习工具 | `/api/high-freq-words?category=&keyword=` | GET |
| | `/api/high-freq-idiom?category=&keyword=` | GET |
| | `/api/idiom/search?keyword=` | GET |
| | `/api/news/xwlb` | GET |
| | `/api/secrets/tree?type=xingce\|shenlun` | GET |
| | `/api/secrets/article/{id}` | GET |
| 学习计划 | `/api/study-plan/archive` | GET |
| | `/api/study-plan/archived` | GET |
| | `/api/study-plan/update-daily-task` | POST |
| 用户 | `/api/user/profile` | GET/PUT |
| | `/api/feedback/submit` | POST |
| | `/api/feedback/my` | GET |
| 文件 | `/api/files/upload` | POST |
| 统计 | `/api/stats/track` | POST |
| | `/api/stats/today-online` | GET |
| 第三方代理 | `/api/proxy/hotboard?type=` | GET |
| | `/api/proxy/history-today` | GET |
| 站点配置 | `/api/site-config?key=must-read` | GET |

**FastAPI (:8000) — 前缀 /ai**

| 端点 | 方法 | 说明 |
|------|------|------|
| `/ai/essay-review/stream` | POST (SSE) | 申论AI批改 |
| `/ai/analysis/stream` | POST (SSE) | 行测AI智能分析 |
| `/ai/question/generate` | POST | 错题AI补练生成推荐题 |
| `/ai/question/analysis` | POST (SSE) | 题目AI解析+参考答案 |
| `/ai/study-plan/generate` | POST (SSE) | 学习计划生成 |
| `/ai/essay-review/count/remaining` | GET | 剩余批改次数 |

---

## 三、认证方案

```
JWT 无状态认证 + Redis 黑名单

登录流程:
  POST /api/auth/login-password {email, password}
  → Java 验证密码 (BCrypt)
  → 签发 Access Token (2h) + Refresh Token (7d)
  → 前端存 localStorage

请求验证:
  Authorization: Bearer {access_token}
  → Java/FastAPI 验证 JWT 签名
  → 检查 Redis 黑名单 (token:blacklist:{token})
  → 提取 user_id

登出:
  → 将当前 Token 写入 Redis 黑名单, TTL = Token 剩余过期时间

验证码:
  POST /api/auth/send-code {email, type}
  → 生成6位随机码
  → 存入 Redis: verify:{email}:{type}, TTL=5min
  → 通过 QQ邮箱 SMTP 发送

刷新:
  POST /api/auth/refresh {refresh_token}
  → 验证 refresh_token 有效性
  → 签发新 access_token + refresh_token
  → 旧 refresh_token 加入黑名单
```

---

## 四、数据库设计

### 4.1 用户体系

```sql
CREATE TABLE user (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    email           VARCHAR(255) NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL COMMENT 'BCrypt加密',
    nickname        VARCHAR(50) DEFAULT '',
    avatar          VARCHAR(500) DEFAULT '' COMMENT 'MinIO图片URL',
    deleted         TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0=正常, 1=已删除',
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

Redis 负责: 验证码 (`verify:{email}:{type}`, TTL=5min)、Token黑名单 (`token:blacklist:{token}`, TTL=Token剩余时间)

### 4.2 题库体系

```sql
CREATE TABLE region (
    id              INT PRIMARY KEY AUTO_INCREMENT,
    name            VARCHAR(50) NOT NULL UNIQUE,
    category        ENUM('national', 'provincial') NOT NULL DEFAULT 'provincial',
    sort_order      INT NOT NULL DEFAULT 0,
    INDEX idx_category (category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE paper (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    title           VARCHAR(500) NOT NULL,
    category        ENUM('行测', '申论') NOT NULL,
    region_id       INT DEFAULT NULL COMMENT 'NULL=不限地区',
    rating          TINYINT DEFAULT 0 COMMENT '难度星级1-5',
    question_count  INT NOT NULL DEFAULT 0 COMMENT '冗余,加速查询',
    year            INT DEFAULT NULL,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_category (category),
    INDEX idx_region (region_id),
    INDEX idx_category_region (category, region_id),
    FOREIGN KEY (region_id) REFERENCES region(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE material_group (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    paper_id        BIGINT NOT NULL,
    title           VARCHAR(500) DEFAULT '',
    content         LONGTEXT COMMENT 'HTML富文本',
    images          JSON DEFAULT NULL COMMENT 'MinIO key列表',
    sort_order      INT NOT NULL DEFAULT 0,
    INDEX idx_paper (paper_id),
    FOREIGN KEY (paper_id) REFERENCES paper(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE question (
    id                  BIGINT PRIMARY KEY AUTO_INCREMENT,
    paper_id            BIGINT NOT NULL,
    material_group_id   BIGINT DEFAULT NULL COMMENT 'NULL=独立题',
    sort_order          INT NOT NULL DEFAULT 0,
    module              VARCHAR(50) DEFAULT NULL COMMENT '言语理解/数量关系/判断推理/资料分析/常识判断',
    sub_module          VARCHAR(50) DEFAULT NULL COMMENT '片段阅读/图形推理/数学运算...',
    knowledge_point     VARCHAR(100) DEFAULT NULL,
    type                ENUM('single_choice', 'multi_choice', 'fill_blank', 'essay')
                        NOT NULL DEFAULT 'single_choice',
    content             LONGTEXT NOT NULL COMMENT 'HTML富文本',
    options             JSON DEFAULT NULL COMMENT '[{"label":"A","text":"...","image":""}]',
    answer              LONGTEXT NOT NULL COMMENT '选择题存"A",申论存完整参考范文',
    explanation         LONGTEXT COMMENT 'HTML富文本',
    images              JSON DEFAULT NULL,
    score               DECIMAL(5,1) DEFAULT 1.0,
    created_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_paper (paper_id),
    INDEX idx_material (material_group_id),
    INDEX idx_module (module),
    INDEX idx_sub_module (sub_module),
    INDEX idx_knowledge (knowledge_point),
    INDEX idx_module_filter (module, sub_module, knowledge_point),
    FOREIGN KEY (paper_id) REFERENCES paper(id) ON DELETE CASCADE,
    FOREIGN KEY (material_group_id) REFERENCES material_group(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### 4.3 做题记录体系

```sql
CREATE TABLE practice_session (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id         BIGINT NOT NULL,
    paper_id        BIGINT NOT NULL,
    status          ENUM('ongoing', 'paused', 'submitted', 'abandoned')
                    NOT NULL DEFAULT 'ongoing',
    time_elapsed    INT NOT NULL DEFAULT 0 COMMENT '已用秒数',
    current_index   INT NOT NULL DEFAULT 0,
    answers         JSON DEFAULT NULL COMMENT '[{"question_id":1,"answer":"A"}]',
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user (user_id),
    INDEX idx_user_paper (user_id, paper_id),
    INDEX idx_status (status),
    FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE,
    FOREIGN KEY (paper_id) REFERENCES paper(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE user_answer (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id         BIGINT NOT NULL,
    paper_id        BIGINT NOT NULL,
    question_id     BIGINT NOT NULL,
    session_id      BIGINT DEFAULT NULL,
    user_answer     LONGTEXT COMMENT '选择题"A",申论完整作答',
    is_correct      TINYINT(1) DEFAULT NULL COMMENT 'NULL=未批改,0=错,1=对',
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE INDEX idx_user_question (user_id, question_id, created_at),
    INDEX idx_user_paper (user_id, paper_id),
    INDEX idx_correct (user_id, is_correct),
    INDEX idx_session (session_id),
    FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE,
    FOREIGN KEY (paper_id) REFERENCES paper(id) ON DELETE CASCADE,
    FOREIGN KEY (question_id) REFERENCES question(id) ON DELETE CASCADE,
    FOREIGN KEY (session_id) REFERENCES practice_session(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE analysis (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id         BIGINT NOT NULL,
    paper_id        BIGINT NOT NULL,
    session_id      BIGINT DEFAULT NULL,
    total_questions INT NOT NULL DEFAULT 0,
    correct_count   INT NOT NULL DEFAULT 0,
    wrong_count     INT NOT NULL DEFAULT 0,
    accuracy        DECIMAL(5,2) NOT NULL DEFAULT 0,
    ai_result       LONGTEXT COMMENT 'Markdown格式',
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user (user_id),
    INDEX idx_user_paper (user_id, paper_id),
    FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE,
    FOREIGN KEY (paper_id) REFERENCES paper(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE essay_review (
    id                  BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id             BIGINT NOT NULL,
    paper_id            BIGINT NOT NULL,
    question_id         BIGINT NOT NULL,
    sort_order          INT NOT NULL DEFAULT 1,
    user_answer         LONGTEXT NOT NULL,
    total_score         DECIMAL(5,1) DEFAULT NULL,
    max_score           DECIMAL(5,1) DEFAULT NULL,
    base_score          DECIMAL(5,1) DEFAULT NULL,
    base_max_score      DECIMAL(5,1) DEFAULT NULL,
    content_score       DECIMAL(5,1) DEFAULT NULL,
    content_max_score   DECIMAL(5,1) DEFAULT NULL,
    expression_score    DECIMAL(5,1) DEFAULT NULL,
    expression_max_score DECIMAL(5,1) DEFAULT NULL,
    pros                JSON DEFAULT NULL,
    problems            JSON DEFAULT NULL,
    suggestions         JSON DEFAULT NULL,
    comment             LONGTEXT,
    score_points        JSON DEFAULT NULL,
    created_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user (user_id),
    INDEX idx_user_paper (user_id, paper_id),
    INDEX idx_question (question_id),
    FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE,
    FOREIGN KEY (paper_id) REFERENCES paper(id) ON DELETE CASCADE,
    FOREIGN KEY (question_id) REFERENCES question(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE ai_question (
    id                  BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id             BIGINT NOT NULL,
    question_id         BIGINT NOT NULL,
    knowledge_point     VARCHAR(100) DEFAULT NULL,
    original_question   LONGTEXT NOT NULL,
    original_answer     LONGTEXT,
    original_options    JSON DEFAULT NULL,
    original_explanation LONGTEXT,
    original_material   LONGTEXT DEFAULT NULL,
    error_count         INT NOT NULL DEFAULT 1,
    wrong_answers       JSON DEFAULT NULL,
    error_analysis      LONGTEXT,
    recommended_questions JSON DEFAULT NULL,
    all_error_records   JSON DEFAULT NULL,
    created_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user (user_id),
    INDEX idx_user_question (user_id, question_id),
    INDEX idx_knowledge (knowledge_point),
    FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE,
    FOREIGN KEY (question_id) REFERENCES question(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE question_ai_analysis (
    id                  BIGINT PRIMARY KEY AUTO_INCREMENT,
    question_id         BIGINT NOT NULL,
    analysis            LONGTEXT COMMENT 'AI详细解析,Markdown',
    reference_answer    LONGTEXT COMMENT 'AI参考答案(申论等)',
    difficulty          VARCHAR(20) DEFAULT NULL COMMENT '简单/中等/较难/困难',
    key_points          JSON DEFAULT NULL,
    common_mistakes     JSON DEFAULT NULL,
    solving_steps       JSON DEFAULT NULL,
    created_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE INDEX idx_question (question_id),
    FOREIGN KEY (question_id) REFERENCES question(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### 4.4 学习工具体系

```sql
CREATE TABLE checkin (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id         BIGINT NOT NULL,
    date            DATE NOT NULL,
    study_modules   JSON DEFAULT NULL COMMENT '["行测","申论"]',
    question_count  INT NOT NULL DEFAULT 0,
    study_minutes   INT NOT NULL DEFAULT 0,
    note            VARCHAR(100) DEFAULT '',
    timestamp       BIGINT NOT NULL,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE INDEX idx_user_date (user_id, date),
    INDEX idx_user (user_id),
    INDEX idx_date (date),
    FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE checkin_task (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id         BIGINT NOT NULL,
    text            VARCHAR(200) NOT NULL,
    completed       TINYINT(1) NOT NULL DEFAULT 0,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user (user_id),
    FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE idiom (
    id              INT PRIMARY KEY AUTO_INCREMENT,
    idiom           VARCHAR(20) NOT NULL UNIQUE,
    pinyin          VARCHAR(100) DEFAULT '',
    explanation     TEXT,
    provenance      TEXT,
    usage           TEXT,
    sentence        TEXT,
    analysis        TEXT,
    INDEX idx_idiom (idiom),
    FULLTEXT INDEX ft_idiom (idiom, explanation)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE high_freq_word (
    id              INT PRIMARY KEY AUTO_INCREMENT,
    word            VARCHAR(50) NOT NULL,
    category        VARCHAR(30) NOT NULL,
    explanation     TEXT NOT NULL,
    usage           TEXT,
    INDEX idx_category (category),
    INDEX idx_word (word)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE high_freq_idiom (
    id              INT PRIMARY KEY AUTO_INCREMENT,
    idiom           VARCHAR(20) NOT NULL,
    category        VARCHAR(30) NOT NULL,
    explanation     TEXT NOT NULL,
    usage           TEXT,
    example         TEXT,
    INDEX idx_category (category),
    INDEX idx_idiom (idiom)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE study_plan (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id         BIGINT NOT NULL,
    date_range      VARCHAR(50) NOT NULL,
    progress        DECIMAL(5,1) DEFAULT 0,
    focus_areas     JSON DEFAULT NULL,
    daily_tasks     JSON DEFAULT NULL COMMENT '[{"day":"周一","tasks":[{"text":"...","completed":false}]}]',
    milestones      JSON DEFAULT NULL,
    recommendations TEXT,
    module_accuracy JSON DEFAULT NULL,
    weak_modules    JSON DEFAULT NULL,
    top_wrong_points JSON DEFAULT NULL,
    is_archived     TINYINT(1) NOT NULL DEFAULT 0,
    analysis_date   DATE,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user (user_id),
    INDEX idx_archived (user_id, is_archived),
    FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE secret (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    parent_id       BIGINT DEFAULT NULL,
    title           VARCHAR(200) NOT NULL,
    type            ENUM('xingce', 'shenlun') NOT NULL,
    content         LONGTEXT,
    sort_order      INT NOT NULL DEFAULT 0,
    INDEX idx_parent (parent_id),
    INDEX idx_type (type),
    FOREIGN KEY (parent_id) REFERENCES secret(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### 4.5 系统表

```sql
CREATE TABLE user_favorite (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id         BIGINT NOT NULL,
    target_type     ENUM('idiom', 'high_freq_word', 'high_freq_idiom', 'secret', 'question') NOT NULL,
    target_id       BIGINT NOT NULL,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE INDEX idx_user_target (user_id, target_type, target_id),
    INDEX idx_user (user_id),
    FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE feedback (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id         BIGINT NOT NULL,
    content         TEXT NOT NULL,
    reply           TEXT DEFAULT NULL,
    status          ENUM('pending', 'replied') NOT NULL DEFAULT 'pending',
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user (user_id),
    INDEX idx_status (status),
    FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE stats_track (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    visitor_id      VARCHAR(100) NOT NULL,
    event           VARCHAR(100) NOT NULL,
    metadata        JSON DEFAULT NULL,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_visitor (visitor_id),
    INDEX idx_event (event),
    INDEX idx_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE site_config (
    id              INT PRIMARY KEY AUTO_INCREMENT,
    config_key      VARCHAR(100) NOT NULL UNIQUE,
    config_value    LONGTEXT NOT NULL,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE INDEX idx_key (config_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### 4.6 表清单（22张）

| # | 表 | 上级依赖 | Redis互补 |
|---|---|---------|----------|
| 1 | user | — | — |
| 2 | region | — | — |
| 3 | paper | region | — |
| 4 | material_group | paper | — |
| 5 | question | paper, material_group | — |
| 6 | practice_session | user, paper | — |
| 7 | user_answer | user, paper, question, session | — |
| 8 | analysis | user, paper | — |
| 9 | essay_review | user, paper, question | — |
| 10 | ai_question | user, question | — |
| 11 | question_ai_analysis | question | — |
| 12 | checkin | user | — |
| 13 | checkin_task | user | — |
| 14 | idiom | — | — |
| 15 | high_freq_word | — | — |
| 16 | high_freq_idiom | — | — |
| 17 | study_plan | user | — |
| 18 | secret | secret(自引用) | — |
| 19 | user_favorite | user | — |
| 20 | feedback | user | — |
| 21 | stats_track | — | — |
| 22 | site_config | — | — |

Redis 用途:
- `verify:{email}:{type}` — 验证码 (TTL=5min)
- `token:blacklist:{token}` — Token黑名单 (TTL=Token剩余时间)
- `cache:hotboard` / `cache:history_today` / `cache:news` — 第三方API缓存 (TTL=30min)

---

## 五、前端页面路由与组件映射

| 路由 | 组件 | 页面 |
|------|------|------|
| `/` | VPHero + HomeQuickNav | 主页 |
| `/题库/` | PaperList | 行测题库 |
| `/essay-bank/` | EssayBank | 申论题库 |
| `/practice/special/` | PaperList | 专项练习 |
| `/practice/online/` | OnlinePractice | 在线练习 |
| `/practice/history/` | PracticeHistory | 行测智能分析 |
| `/shenlun-practice/` | ShenlunPractice | 申论练习 |
| `/idiom/` | IdiomSearch | 成语查询 |
| `/high-freq-words/` | HighFreqWords | 高频词语 |
| `/high-freq-idiom/` | HighFreqIdiom | 高频成语 |
| `/wrong-questions/` | WrongQuestions | 错题题库 |
| `/今日热榜/` | HotList | 实时热榜 |
| `/checkin/` | CheckIn | 每日打卡 |
| `/history-today/` | HistoryToday | 今日史事 |
| `/news/` | NewsList | 每日新闻 |
| `/login/` | Login | 个人中心 |
| `/essay-review/` | EssayReview | 申论智能批改 |
| `/ai-recommended/` | AIRecommendedQuestions | 错题AI补练 |
| `/study-plan/` | StudyPlan | 学习计划 |
| `/secrets/` | SecretsContent + SecretsSidebar | 上岸秘籍 |
| `/must-read/` | MustRead | 进站必读 |

侧边栏分组（按实现阶段逐步添加，当前仅包含已实现功能）:
- 题库练习: 专项练习、行测题库
- 学习工具: （待 P4-P5 实现）
- 个人中心: （待 P4-P5 实现）

---

## 六、开发阶段划分

每个阶段可独立测试验收，不依赖后续功能。

### P0 — 项目骨架 + Docker + 建表

**内容:**
- Docker Compose 配置 (VitePress + Spring Boot + FastAPI + MySQL + Redis + MinIO)
- Spring Boot 空项目 + MyBatis-Plus + 基础配置
- FastAPI 空项目 + 健康检查
- 全部 22 张表建表 SQL + region 种子数据
- VitePress 空项目 + 基础 config.ts

**验收:** `docker-compose up` 所有服务启动成功，MySQL 表已创建，各服务健康检查通过

### P1 — 用户认证

**内容:**
- Java: 注册(邮箱+密码+验证码)、登录、Token签发/刷新、登出黑名单
- Java: QQ邮箱SMTP发送验证码
- Redis: 验证码存取、Token黑名单
- 前端: 登录/注册/忘记密码页面 (Login组件)

**验收:** 注册→收到验证码邮件→登录→获取Token→Postman调用受保护API→登出→Token失效

### P2 — 前端骨架 + 首页

**内容:**
- VitePress 主题定制: 导航栏、侧边栏、暗色模式
- 首页: Hero区 + 特性卡片 + 18个快捷导航卡片
- 所有页面占位路由 (点进去显示"开发中")
- 全局组件: BackToTop、Toast、Loading、Empty、Modal
- 前端API请求封装 (axios + JWT拦截器)

**验收:** 首页完整渲染，导航跳转正常，暗色模式切换正常，移动端响应式正常

### P3 — 行测题库 + 在线练习（核心做题流程）

**内容:**
- Java: 试卷CRUD、题目查询(含筛选)、做题会话、批量提交答案
- 前端: PaperList(行测题库)、OnlinePractice(做题页面)、PaperList(专项练习)
- 数据: Python爬取脚本，导入少量测试数据
- MinIO: 题目图片上传/展示

**验收:** 选试卷→做题→暂停/恢复→提交→查看正确率→错题自动归集

### P4 — 申论题库 + AI批改

**内容:**
- Java: 申论试卷查询、材料获取、批改记录CRUD
- FastAPI: 申论AI批改(流式)、行测AI分析(流式)、题目AI解析(流式)
- FastAPI: LLM调用封装(支持OpenAI/Anthropic双格式，可配置api_url/api_key/model)
- Java回调FastAPI结果持久化
- 前端: EssayBank、ShenlunPractice、EssayReview、PracticeHistory

**验收:** 写申论→AI批改→看评分+采分点+建议; 做行测→AI分析→看诊断报告; 任何题目可看AI解析

### P5 — 其余功能模块

**内容:**
- 错题题库 + AI补练 (前端: WrongQuestions, AIRecommendedQuestions)
- 每日打卡 (前端: CheckIn, 含日历/统计/任务)
- 成语查询/高频词语/高频成语 (前端: IdiomSearch, HighFreqWords, HighFreqIdiom)
- 每日新闻/实时热榜/今日史事 (Java代理第三方API + Redis缓存)
- 学习计划 (FastAPI流式生成 + Java存储)
- 上岸秘籍 (树形目录)
- 个人中心完善 (头像上传、昵称修改、学习数据概览)
- 收藏功能
- 意见反馈
- 进站必读
- 行为追踪

**验收:** 各模块独立可用，全部功能端到端测试通过

---

## 七、项目目录结构

```
gongkao/
├── frontend/                          # VitePress 前端
│   ├── .vitepress/
│   │   ├── theme/
│   │   │   ├── components/            # 21个自定义组件
│   │   │   │   ├── Login.vue
│   │   │   │   ├── PaperList.vue
│   │   │   │   ├── EssayBank.vue
│   │   │   │   ├── OnlinePractice.vue
│   │   │   │   ├── ShenlunPractice.vue
│   │   │   │   ├── PracticeHistory.vue
│   │   │   │   ├── EssayReview.vue
│   │   │   │   ├── AIRecommendedQuestions.vue
│   │   │   │   ├── StudyPlan.vue
│   │   │   │   ├── CheckIn.vue
│   │   │   │   ├── IdiomSearch.vue
│   │   │   │   ├── HighFreqWords.vue
│   │   │   │   ├── HighFreqIdiom.vue
│   │   │   │   ├── WrongQuestions.vue
│   │   │   │   ├── HotList.vue
│   │   │   │   ├── NewsList.vue
│   │   │   │   ├── HistoryToday.vue
│   │   │   │   ├── SecretsContent.vue
│   │   │   │   ├── SecretsSidebar.vue
│   │   │   │   ├── TreeNodeItem.vue
│   │   │   │   ├── MustRead.vue
│   │   │   │   └── BackToTop.vue
│   │   │   ├── styles/               # 自定义样式
│   │   │   └── index.ts               # 主题入口
│   │   └── config.ts                  # VitePress配置
│   ├── pages/                         # .md页面文件(路由)
│   ├── public/                        # 静态资源(logo等)
│   └── package.json
│
├── backend/                           # Spring Boot 后端
│   ├── src/main/java/com/gongkao/
│   │   ├── controller/               # REST控制器
│   │   │   ├── AuthController.java
│   │   │   ├── PaperController.java
│   │   │   ├── SessionController.java
│   │   │   ├── AnalysisController.java
│   │   │   ├── EssayReviewController.java
│   │   │   ├── AiQuestionController.java
│   │   │   ├── QuestionAnalysisController.java
│   │   │   ├── CheckinController.java
│   │   │   ├── FavoriteController.java
│   │   │   ├── StudyToolController.java
│   │   │   ├── StudyPlanController.java
│   │   │   ├── SecretController.java
│   │   │   ├── FeedbackController.java
│   │   │   ├── FileController.java
│   │   │   ├── ProxyController.java
│   │   │   └── StatsController.java
│   │   ├── service/                   # 业务逻辑
│   │   ├── mapper/                    # MyBatis-Plus Mapper
│   │   ├── entity/                    # 数据实体(对应22张表)
│   │   ├── dto/                       # 请求/响应DTO
│   │   ├── config/                    # Security, JWT, CORS, Redis, MinIO
│   │   ├── common/                    # 统一响应、异常处理、工具类
│   │   └── GongkaoApplication.java
│   ├── src/main/resources/
│   │   ├── application.yml
│   │   ├── mapper/                    # MyBatis XML(复杂查询)
│   │   └── db/
│   │       ├── schema.sql             # 建表SQL
│   │       └── data.sql               # 种子数据
│   └── pom.xml
│
├── ai-service/                        # FastAPI AI微服务
│   ├── app/
│   │   ├── main.py
│   │   ├── routers/
│   │   │   ├── essay_review.py        # 申论批改
│   │   │   ├── analysis.py            # 行测分析
│   │   │   ├── question.py            # 题目解析+错题补练
│   │   │   └── study_plan.py          # 学习计划
│   │   ├── services/
│   │   │   ├── llm_service.py         # LLM统一调用(OpenAI/Anthropic)
│   │   │   ├── essay_grader.py        # 申论批改Prompt
│   │   │   ├── question_analyzer.py   # 题目分析Prompt
│   │   │   └── plan_generator.py      # 计划生成Prompt
│   │   ├── core/
│   │   │   ├── config.py              # 配置(api_url, api_key, model)
│   │   │   └── auth.py                # JWT验证(与Java同密钥)
│   │   └── schemas.py                 # Pydantic模型
│   ├── requirements.txt
│   └── Dockerfile
│
├── scripts/                           # 数据爬取/导入工具
│   ├── crawl_papers.py
│   ├── crawl_questions.py
│   ├── import_idioms.py
│   ├── import_words.py
│   └── seed.sql
│
├── docker-compose.yml
├── docs/
│   └── BALA公考网站分析文档.md          # 原站分析(已有)
└── README.md
```

---

## 八、LLM 配置设计

FastAPI 的 `config.yaml`:
```yaml
llm:
  default_provider: openai-compatible

  providers:
    openai-compatible:
      api_url: "https://your-proxy.com/v1/chat/completions"
      api_key: "sk-xxx"
      model: "gpt-4o"
      max_tokens: 4096

    anthropic-compatible:
      api_url: "https://your-proxy.com/v1/messages"
      api_key: "sk-xxx"
      model: "claude-sonnet-4-6"
      max_tokens: 4096
```

通过环境变量覆盖:
```
LLM_PROVIDER=openai-compatible
LLM_API_URL=https://...
LLM_API_KEY=sk-...
LLM_MODEL=gpt-4o
```

---

## 九、Docker Compose 服务清单

```yaml
services:
  frontend:    # VitePress :5173
  backend:     # Spring Boot :8080
  ai-service:  # FastAPI :8000
  mysql:       # MySQL 9.x :3306
  redis:       # Redis :6379
  minio:       # MinIO :9000(API) :9001(Console)
```

---

## 十、Redis 键设计

| Key模式 | 用途 | TTL |
|---------|------|-----|
| `verify:{email}:{type}` | 验证码 | 5min |
| `token:blacklist:{token}` | Token黑名单 | =Token剩余时间 |
| `cache:hotboard:{type}` | 热榜缓存 | 30min |
| `cache:history_today` | 今日史事缓存 | 30min |
| `cache:news:xwlb` | 新闻缓存 | 30min |
