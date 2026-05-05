-- ============================================================
-- BALA 公考 数据库 Schema
-- 22 张表，按依赖顺序排列
-- ============================================================

-- 1. 用户
CREATE TABLE user (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    email           VARCHAR(255) NOT NULL UNIQUE,
    role            ENUM('user', 'admin') NOT NULL DEFAULT 'user',
    password_hash   VARCHAR(255) NOT NULL COMMENT 'BCrypt加密',
    nickname        VARCHAR(50) DEFAULT '',
    avatar          VARCHAR(500) DEFAULT '' COMMENT 'MinIO图片URL',
    deleted         TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0=正常, 1=已删除',
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 2. 地区
CREATE TABLE region (
    id              INT PRIMARY KEY AUTO_INCREMENT,
    name            VARCHAR(50) NOT NULL UNIQUE,
    category        ENUM('national', 'provincial') NOT NULL DEFAULT 'provincial',
    sort_order      INT NOT NULL DEFAULT 0,
    INDEX idx_category (category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 3. 试卷
CREATE TABLE paper (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    title           VARCHAR(500) NOT NULL,
    category        ENUM('行测', '申论') NOT NULL,
    region_name     VARCHAR(50) DEFAULT '' COMMENT '地区名称: 国考/北京/上海/...',
    rating          TINYINT DEFAULT 0 COMMENT '难度星级1-5',
    question_count  INT NOT NULL DEFAULT 0 COMMENT '冗余,加速查询',
    year            INT DEFAULT NULL,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_category (category),
    INDEX idx_region_name (region_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 4. 材料组
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

-- 5. 题目
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

-- 6. 做题会话
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

-- 7. 用户答案
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

-- 8. 行测分析
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

-- 9. 申论批改记录
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

-- 10. AI错题补练
CREATE TABLE ai_question (
    id                  BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id             BIGINT NOT NULL,
    question_id         BIGINT DEFAULT NULL,
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

-- 11. 题目AI解析
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

-- 12. 每日打卡
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

-- 13. 打卡任务
CREATE TABLE checkin_task (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id         BIGINT NOT NULL,
    text            VARCHAR(200) NOT NULL,
    completed       TINYINT(1) NOT NULL DEFAULT 0,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user (user_id),
    FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 14. 成语
CREATE TABLE idiom (
    id              INT PRIMARY KEY AUTO_INCREMENT,
    idiom           VARCHAR(20) NOT NULL UNIQUE,
    pinyin          VARCHAR(100) DEFAULT '',
    explanation     TEXT,
    provenance      TEXT,
    `usage`          TEXT,
    sentence        TEXT,
    analysis        TEXT,
    INDEX idx_idiom (idiom),
    FULLTEXT INDEX ft_idiom (idiom, explanation)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 15. 高频词语
CREATE TABLE high_freq_word (
    id              INT PRIMARY KEY AUTO_INCREMENT,
    word            VARCHAR(50) NOT NULL,
    category        VARCHAR(30) NOT NULL,
    explanation     TEXT NOT NULL,
    `usage`          TEXT,
    INDEX idx_category (category),
    INDEX idx_word (word)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 16. 高频成语
CREATE TABLE high_freq_idiom (
    id              INT PRIMARY KEY AUTO_INCREMENT,
    idiom           VARCHAR(20) NOT NULL,
    category        VARCHAR(30) NOT NULL,
    explanation     TEXT NOT NULL,
    `usage`          TEXT,
    example         TEXT,
    INDEX idx_category (category),
    INDEX idx_idiom (idiom)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 17. 学习计划
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

-- 18. 上岸秘籍
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

-- 19. 用户收藏
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

-- 20. 意见反馈
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

-- 21. 行为追踪
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

-- 22. 站点配置
CREATE TABLE site_config (
    id              INT PRIMARY KEY AUTO_INCREMENT,
    config_key      VARCHAR(100) NOT NULL UNIQUE,
    config_value    LONGTEXT NOT NULL,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE INDEX idx_key (config_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
