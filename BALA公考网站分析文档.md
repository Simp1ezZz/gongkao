# BALA 公考网站 — 完整功能与样式文档

> 网站地址：https://www.baijing1.top/
> 技术栈：VitePress v2.0.0-alpha.15 + Vue 3（SPA 单页应用）
> 所有API基础路径：`/api`
> 主题：支持亮色/暗色模式切换，默认跟随系统

---

## 一、全局布局结构

### 1.1 顶部导航栏 (VPNav)
- 固定在页面顶部（sticky header）
- 左侧：网站 Logo 文字「BALA 公考」，点击返回首页
- 中部导航菜单项：
  - **主页** → `/`
  - **行测** → `/题库/`
  - **申论** → `/essay-bank/`
  - **专项练习** → `/practice/special/`
  - **个人中心** → `/login/`
- 右侧功能区：
  - **搜索按钮**（VitePress 内置搜索）
  - **主题切换按钮**（太阳/月亮图标，亮色/暗色模式）
- 移动端：导航折叠为汉堡菜单，点击展开侧边栏式导航

### 1.2 侧边栏 (VPSidebar)
所有内页统一侧边栏，分三个分组：

**题库练习**
- 💪 专项练习 → `/practice/special/`
- 🖊️ 行测题库 → `/题库/`
- ✍️ 申论题库 → `/essay-bank/`
- ❎ 错题题库 → `/wrong-questions/`

**学习工具**
- 🔍 成语查询 → `/idiom/`
- 📝 高频词语 → `/high-freq-words/`
- 📖 高频成语 → `/high-freq-idiom/`
- 📺 每日新闻 → `/news/`
- 🔥 实时热榜 → `/今日热榜/`
- 📜 今日史事 → `/history-today/`
- 📅 每日打卡 → `/checkin/`

**个人中心**
- 👤 个人中心 → `/login/`
- 📚 资料合集 → 外部飞书链接
- 📊 行测智能分析 → `/practice/history/`
- 🤖 申论智能批改 → `/essay-review/`
- ❤️ 错题AI补练 → `/ai-recommended/`
- 💯 学习计划 → `/study-plan/`

### 1.3 返回顶部按钮 (BackToTop)
- 页面滚动超过 200px 后显示
- 点击平滑滚动回顶部
- 按钮文字「↑」

### 1.4 全局字体
- 主字体：Inter（Google Fonts，支持拉丁/西里尔/希腊/越南语等多语言子集）
- 字体文件格式：woff2

---

## 二、首页（主页） `/`

### 2.1 Hero 区域 (VPHero)
- 左侧文字区：
  - 大标题：「BALA 公考」+ 副标题「上岸没烦恼」
  - 标语：「不负每一次努力，只为助你稳稳上岸、一战成公。」
  - 三个行动按钮：
    - 「个人中心」→ `/login/` （品牌色主按钮样式）
    - 「进站必读」→ `/must-read/` （次要按钮样式）
    - 「上岸秘籍」→ `/secrets/` （次要按钮样式）
- 右侧图片区：
  - 显示 `/logo1.png` Logo 图片
  - 图片容器带背景渐变效果

### 2.2 三大特性卡片区域（Features）
水平排列三个特性卡片，可点击跳转：
1. **📊 行测智能分析** → `/practice/history/`
   - 描述：「AI智能诊断学习情况，分析各模块正确率，提供个性化提升建议，助您精准突破薄弱环节。」
2. **📚 资料合集** → 外部飞书链接（新窗口打开）
   - 描述：「汇集公考学习资料，包含行测、申论、面试等各类资源，助力高效备考，一站式学习。」
3. **🤖 申论智能批改** → `/essay-review/`
   - 描述：「AI智能批改申论作文，提供专业点评与改进建议，帮助您快速提升申论写作水平。」

### 2.3 快捷导航卡片区域
标题「📌 快捷导航」，采用网格布局展示18个功能卡片：

每个卡片包含：
- 图标 emoji
- 标题
- 描述文字
- 标签（如「题库」「专项」「免费」等）

完整列表：
| 图标 | 标题 | 路由 | 描述 | 标签 |
|------|------|------|------|------|
| 💪 | 专项练习 | `/practice/special/` | 分模块刷题，哪里不会练哪里 | 题库、专项、免费 |
| 🖊️ | 行测题库 | `/题库/` | 收录近6年真题，支持在线练习 | 题库、试卷、免费 |
| ✍️ | 申论题库 | `/essay-bank/` | 规范作答训练，支持智能批改 | 申论、题库、免费 |
| 📝 | 高频词语 | `/high-freq-words/` | 申论常用词汇，助力申论写作 | 词汇、短语、免费 |
| 📖 | 高频成语 | `/high-freq-idiom/` | 常考成语解析，助力言语理解 | 成语、解析、免费 |
| ❎ | 错题题库 | `/wrong-questions/` | 自动归集错题，针对性复习 | 错题、复习、免费 |
| 🔍 | 成语查询 | `/idiom/` | 收录近3万四字成语，用法清晰易懂 | 成语、词语、免费 |
| 🔥 | 实时热榜 | `/今日热榜/` | 实时热门话题，紧跟考试动态 | 热点、推荐、免费 |
| 📅 | 每日打卡 | `/checkin/` | 记录学习进度，养成良好习惯 | 打卡、记录、免费 |
| 📜 | 今日史事 | `/history-today/` | 当天重要历史事件，增长知识储备 | 历史、知识、免费 |
| 👤 | 个人中心 | `/login/` | 个人信息管理，学习数据统计 | 个人、设置、免费 |
| 📺 | 每日新闻 | `/news/` | 关注时事热点，把握政策走向 | 新闻、时政、免费 |
| 📊 | 行测智能分析 | `/practice/history/` | AI智能诊断学习情况，提供个性化提升建议 | AI、分析、免费 |
| 🤖 | 申论智能批改 | `/essay-review/` | AI逐句批改，对标高分范文 | AI、批改、免费 |
| 📚 | 资料合集 | 飞书外链 | 汇集公考学习资料，助力高效备考 | 资料、文档、免费 |
| ❤️ | 错题AI补练 | `/ai-recommended/` | 基于错题智能分析，AI生成类似题目补练 | AI、推荐、免费 |
| 💯 | 学习计划 | `/study-plan/` | AI分析弱项，智能生成个性化学习计划 | AI、计划、免费 |
| 🎁 | 上岸秘籍 | `/secrets/` | 行测申论技巧合集，助你成功上岸 | 技巧、秘籍、免费 |

### 2.4 底部横幅
- 显示 `/footer-bg.png` 图片

---

## 三、各页面详细功能

### 3.1 行测题库 `/题库/`

**组件名**：`PaperList`

**功能描述**：
- 展示行测类试卷列表
- 支持按省份/地区筛选
- 点击试卷进入在线练习 `/practice/online/?paperId={id}`

**API端点**：
- `GET /api/papers?category=行测` — 获取行测试卷列表
- `GET /api/papers?{params}` — 筛选试卷
- `GET /api/papers/{id}` — 获取试卷详情及题目

**页面元素**：
- 页面标题「🖊️ 行测题库」
- 地区筛选标签/Tab
- 试卷卡片列表（每张卡片显示标题、地区、星级评分）
- 加载更多按钮

---

### 3.2 申论题库 `/essay-bank/`

**组件名**：`EssayBank`

**功能描述**：
- 展示申论类试卷列表，按地区筛选
- 每张试卷卡片显示标题、地区、评分（星级，1-5星）
- 点击试卷进入申论练习 `/shenlun-practice/?paperId={id}`
- 支持分页加载更多

**API端点**：
- `GET /api/papers?category=申论&page=1&pageSize={pageSize}` — 首次加载
- `GET /api/papers?category=申论&region={encodeURIComponent(region)}` — 按地区筛选
- `GET /api/papers?category=申论&page={page}&pageSize={pageSize}` — 翻页

**页面元素**：
- 页面标题「✍️ 申论题库」
- 地区Tab栏（动态生成）
- 加载状态（⏳ 加载中...）
- 错误状态（❌ + 错误信息）
- 试卷卡片网格
- 「加载更多」按钮（显示已加载/总数）
- 空状态：「📭 暂无该地区试卷」

**评分显示**：使用 ★★★★★ 星级

---

### 3.3 专项练习 `/practice/special/`

**组件名**：`PaperList`（与行测题库共用，但参数不同）

**功能描述**：
- 按知识点分类展示题目
- 支持按知识点筛选练习

**API端点**：
- `GET /api/papers/questions/by-knowledge?knowledge_point={encodeURIComponent(point)}` — 按知识点获取题目

**页面元素**：
- 知识点分类导航
- 题目列表
- 点击进入在线练习 `/practice/special/?paperId={id}`

---

### 3.4 在线练习（做题页面） `/practice/online/`

**组件名**：`OnlinePractice`

**URL参数**：`?paperId={id}&review=true(可选)`

**功能描述**：
- 核心做题页面，展示题目、选项、提交答案
- 支持倒计时/暂停功能
- 答题完成后自动批改
- 支持AI智能分析错题（流式输出）
- 支持查看答题详情（review模式）

**API端点**：
- `GET /api/papers/{id}` — 获取试卷及题目
- `POST /api/papers/user-answers/batch` — 批量提交答案
- `POST /api/ai-question/analyze-stream` — AI分析流式接口
- `GET /api/papers/{id}/my-answers` — 获取我的答案（review模式）

**页面元素**：
- 加载状态卡片
- 题目卡片（显示试卷标题、题目序号）
- 暂停覆盖层（⏸️ 已暂停，点击继续答题）
- 题目导航栏
- 选项列表（A/B/C/D单选）
- 答题进度指示器
- 提交按钮
- 返回按钮（返回行测题库）
- AI分析结果弹窗

---

### 3.5 申论练习（做题页面） `/shenlun-practice/`

**组件名**：`ShenlunPractice`

**URL参数**：`?paperId={id}&questionIndex={index}(可选)`

**功能描述**：
- 申论在线作答页面
- 支持倒计时和暂停
- 提交后进行AI批改
- 支持OCR图片识别文字
- 显示材料、题目、作答区域
- 批改后显示评分、采分点分析

**API端点**：
- `GET /api/papers/{id}` — 获取试卷
- `GET /api/papers/{id}/materials` — 获取试卷材料
- `GET /api/essay-review/count/remaining` — 获取剩余批改次数
- `POST /api/essay-review/stream` — AI批改流式接口
- `POST /api/essay-review/ocr` — OCR图片识别
- `GET /api/essay-review/{id}` — 获取批改详情

**页面元素**：
- 暂停覆盖层（同在线练习）
- 顶部信息栏：试卷标题、控制按钮（返回、暂停、计时器）
- 材料展示区（可折叠）
- 题目列表/导航
- 作答文本框
- 提交/AI批改按钮
- 批改详情弹窗：
  - 总分 + 等级评定
  - 分项评分条（基础分、内容分、表达分，进度条可视化）
  - 你的答案展示
  - ✅ 得分要点列表
  - 📋 采分点分析表格（采分点、覆盖情况、分析）
  - ⚠️ 存在问题列表
  - 📝 详细评语
  - 💡 改进建议列表
  - 分享/删除按钮

**批改评分颜色**：
- ≥80：绿色（excellent）
- ≥60：橙色（good）
- <60：红色（need-improve）

---

### 3.6 成语查询 `/idiom/`

**组件名**：`IdiomSearch`

**功能描述**：
- 搜索框输入成语，实时查询
- 支持收藏和复制释义
- 收录近3万四字成语

**API端点**：
- 通过 fetch 获取成语数据（内部API，搜索成语）

**页面元素**：
- 页面标题「🔍 成语查询」
- 搜索框 + 搜索按钮（支持回车搜索）
- 加载状态：⏳ 搜索中...
- 错误状态：❌ + 错误信息
- 搜索结果卡片：
  - 成语名称 + 拼音
  - 操作按钮：收藏（❤️/🤍）、复制释义（📋/✓）
  - 📄 成语解释
  - 📚 成语出处
  - 💡 成语用法
  - ✏️ 成语造句
  - 🔍 成语辨析
- 未搜索状态：「📋 功能说明 — 收录近3万四字成语，用法清晰易懂，支持精准检索。」

---

### 3.7 高频词语 `/high-freq-words/`

**组件名**：`HighFreqWords`

**功能描述**：
- 展示申论高频词语列表
- 支持分类筛选和搜索
- 每个词语展示解释和用法

**API端点**：
- `GET /api/high-freq-words` — 获取高频词语列表

**页面元素**：
- 页面标题区域
- 搜索框
- 分类筛选栏（Tab/标签）：全部、政策词汇、经济术语、社会热点、文化经典、科技发展、生态环保、民生保障
- 加载状态
- 错误状态
- 词语卡片网格：
  - 词语标题 + 分类标签
  - 解释
  - 用法示例框
  - 收藏按钮

---

### 3.8 高频成语 `/high-freq-idiom/`

**组件名**：`HighFreqIdiom`

**功能描述**：
- 展示公考常考高频成语
- 类似高频词语，但专注于成语

**API端点**：
- `GET /api/high-freq-idiom` — 获取高频成语列表

**页面元素**：与高频词语类似
- 分类筛选
- 成语卡片（成语、释义、用法、出处）

---

### 3.9 错题题库 `/wrong-questions/`

**功能描述**：
- 自动归集所有做错的题目
- 按知识点分类展示
- 支持重新练习

**页面元素**：
- 需要登录才能查看
- 错题列表/卡片
- 按知识点筛选

---

### 3.10 实时热榜 `/今日热榜/`

**组件名**：`HotList`

**功能描述**：
- 实时展示热门话题
- 紧跟考试动态

**API端点**：
- `GET https://uapis.cn/api/v1/misc/hotboard?type={type}` — 获取热榜数据（第三方API）

**页面元素**：
- 热榜条目列表
- 排名、标题、热度值

---

### 3.11 每日打卡 `/checkin/`

**组件名**：`CheckIn`

**功能描述**：
- 每日学习打卡系统
- 记录学习模块、刷题量、学习时长、学习心得
- 打卡日历视图
- 学习数据统计图表
- 打卡历史记录
- 生成打卡海报

**API端点**：
- `GET /api/checkin` — 获取打卡数据
- `POST /api/checkin` — 提交打卡
- `GET /api/checkin/tasks` — 获取任务列表
- `POST /api/checkin/tasks` — 添加任务
- `PUT /api/checkin/tasks/{id}` — 更新任务
- `DELETE /api/checkin/tasks/{id}` — 删除任务
- `DELETE /api/checkin/{timestamp}` — 删除打卡记录

**页面结构**：
- 页面标题「📅 每日学习打卡」，副标题「坚持打卡，稳步上岸」

**统计展示**：
- 🔥 当前连续打卡天数
- 累计打卡总天数
- 打卡按钮：「今日学习打卡」/「今日已打卡」
- 「生成打卡海报」按钮

**内容Tab切换**：
1. **打卡日历**
   - 月度日历视图（周日~周六）
   - 已打卡日期标记
   - 模考日标记
   - 今日标记
   - 月份前后翻页按钮
   - 悬停已打卡日期显示详情（模块、刷题量、时长）
   - 添加学习任务区域（输入框 + 添加按钮）
   - 任务列表（复选框勾选完成、删除按钮）

2. **学习数据**
   - 近7天学习时长柱状图
   - 各科目学习占比饼图（行测 #3b82f6、申论 #10b981、常识 #f59e0b、公基 #8b5cf6）
   - 统计数字：
     - 当前连续天数
     - 本月打卡率（百分比）
     - 累计刷题道数
     - 学习时长（小时）

3. **历史记录**
   - 月份筛选下拉框
   - 近期打卡记录列表
   - 每条记录显示：日期、学习模块标签、题量/时长、学习心得
   - 删除记录按钮

**打卡弹窗**：
- 今日学习模块（多选）：行测、申论、常识、公基
- 今日刷题量（数字输入，0~1000道）
- 学习时长（数字输入，1~1440分钟）
- 学习心得（选填，50字以内）
- 取消/提交按钮

---

### 3.12 今日史事 `/history-today/`

**组件名**：`HistoryToday`

**功能描述**：
- 展示当天发生的重要历史事件

**API端点**：
- `GET https://v2.xxapi.cn/api/history` — 获取历史事件（第三方API）

**页面元素**：
- 历史事件列表
- 日期、事件描述

---

### 3.13 每日新闻 `/news/`

**组件名**：`NewsList`

**功能描述**：
- 展示每日新闻/时政热点
- 关注时事热点，把握政策走向

**API端点**：
- `GET /api/news/xwlb` — 获取新闻联播数据

**页面元素**：
- 新闻列表
- 新闻标题、来源、时间

---

### 3.14 个人中心/登录注册 `/login/`

**组件名**：`Login`

**功能描述**：
- 用户认证系统（登录/注册/忘记密码）
- 登录后显示个人中心和各项数据
- 支持修改昵称、头像
- 显示学习数据概览

**API端点**：
- `POST /api/auth/login-password` — 邮箱密码登录
- `POST /api/auth/register` — 注册（邮箱+密码+验证码）
- `POST /api/auth/send-code` — 发送验证码（注册/重置密码）
- `POST /api/auth/reset-password` — 重置密码
- `POST /api/auth/refresh` — 刷新Token
- `GET /api/feedback/my` — 获取我的反馈
- `POST /api/feedback/submit` — 提交反馈

**页面元素**：

**登录/注册表单**：
- 邮箱输入框
- 密码输入框
- 验证码输入框 + 发送验证码按钮
- 登录/注册切换
- 忘记密码链接

**登录后个人中心**：
- 头像 + 昵称
- 快捷入口：
  - ❎ 错题题库 → `/wrong-questions/`
  - 📅 每日打卡 → `/checkin/`
  - 📊 行测智能分析 → `/practice/history/`
  - 🤖 申论智能批改 → `/essay-review/`
  - ❤️ 错题AI补练 → `/ai-recommended/`
  - 💯 学习计划 → `/study-plan/`
- 退出登录按钮
- 意见反馈功能

---

### 3.15 行测智能分析 `/practice/history/`

**组件名**：`PracticeHistory`

**功能描述**：
- 查看AI诊断报告历史
- 按试卷分组展示分析记录
- 支持搜索试卷名
- 查看详细的AI分析结果
- 删除分析记录

**API端点**：
- `GET /api/analysis/history` — 获取分析历史
- `GET /api/analysis/{id}` — 获取分析详情
- `DELETE /api/analysis/{id}` — 删除分析记录
- `POST /api/ai-question/analyze-stream` — 生成AI分析（流式）

**页面元素**：
- 页面标题「📊 行测智能分析」
- 副标题「提交试卷即可生成 AI 诊断报告，查看你的学习趋势与提升建议。」
- 搜索框（搜索试卷名）
- 加载状态（旋转图标 + 加载中）
- 错误状态（⚠️ + 错误信息 + 登录链接）
- 空状态：「📭 暂无分析记录」→「开始练习」链接
- 试卷分组列表（按试卷名分组，默认展开前8个）：
  - 分组标题：📄 图标 + 试卷名 + 分析次数
  - 展开箭头
  - 记录列表：正确率（颜色：≥80绿 ≥60橙 <60红）、总题数、正确/错误数、时间、删除按钮
- 「展开全部/收起」按钮
- AI分析详情弹窗：
  - 试卷标题（可链接到答题详情）
  - 统计信息：总题数、正确、错误、正确率
  - 「📋 查看答题详情」链接
  - AI分析结果（Markdown渲染，支持表格、列表、标题等）
- 确认删除弹窗

---

### 3.16 申论智能批改 `/essay-review/`

**组件名**：`EssayReview`

**功能描述**：
- 查看AI批改历史记录
- 按试卷分组展示
- 查看详细批改报告
- 删除批改记录
- 分享批改结果

**API端点**：
- `GET /api/essay-review` — 获取批改列表
- `GET /api/essay-review/{id}` — 获取批改详情
- `DELETE /api/essay-review/{id}` — 删除批改记录

**页面元素**：
- 页面标题「🤖 申论智能批改」
- 副标题「查看您的AI批改历史记录」
- 搜索框（带🔍图标 + 清除按钮）
- 空状态：「📭 暂无批改记录」→「开始练习」链接
- 搜索无结果状态
- 试卷分组列表：
  - 每组显示：📜 图标、试卷标题、题目数量、总分/满分、展开箭头
  - 展开后显示题目列表
  - 每题显示：题目名称、批改次数（如>1次）
  - 评分显示：分数/满分 + 等级文字（优秀/良好/改进）
  - 多次批改：展开历史记录
- 批改详情弹窗：
  - 📄 试卷信息（可点击跳转到申论练习）
  - 总分区域（分数/满分 + 等级评定）
  - 分项评分条（基础分、内容分、表达分，带进度条）
  - 📝 你的答案
  - ✅ 得分要点列表
  - 📋 采分点分析表格（采分点、覆盖情况、分析，覆盖状态：已覆盖/部分/未覆盖）
  - ⚠️ 存在问题列表
  - 📝 详细评语
  - 💡 改进建议列表
  - 📤 分享按钮 + 🗑️ 删除按钮
- 确认删除弹窗

---

### 3.17 错题AI补练 `/ai-recommended/`

**组件名**：`AIRecommendedQuestions`

**功能描述**：
- 基于用户错题，AI智能分析并生成类似题目
- 按知识点筛选推荐题目
- 查看题目详情（原题、选项、错误分析、推荐题目）
- 删除推荐记录

**API端点**：
- `GET /api/ai-question/my-questions` — 获取AI推荐题目列表
- `DELETE /api/ai-question/{id}` — 删除推荐记录

**页面元素**：
- 页面标题「❤️ 错题AI补练」
- 副标题「基于你的错题，AI 智能分析并生成类似题目补练」
- 未登录状态：🔒 请先登录
- 加载状态：旋转图标 + 加载中
- 空状态：「📭 暂无 AI 推荐题目」→「去练习」按钮
- 知识点筛选区域：「按知识点筛选」+ 标签按钮（显示各知识点及数量）
- 题目卡片列表：
  - 知识点标签 + 错误次数徽章
  - 时间
  - 题目文本（HTML渲染）
  - 你的答案（红色标签）+ 正确答案
  - 推荐题目数量
  - 删除按钮
- 题目详情弹窗：
  - 📊 材料（如有）
  - 📝 原题内容 + 选项（标记正确/错误）+ 解析
  - ❌ 错误记录列表（第N次、选了什么、时间、分析）
  - 💡 错误原因分析
  - 📚 AI 推荐类似题目（多道）
    - 题目文本 + 选项
    - 答案（模糊处理，点击显示）
    - 解析

---

### 3.18 学习计划 `/study-plan/`

**组件名**：`StudyPlan`

**功能描述**：
- AI分析学习弱项，智能生成个性化学习计划
- 展示弱项分析、学习计划、历史计划
- 支持流式生成计划
- 任务完成打卡
- 计划归档

**API端点**：
- `GET /api/study-plan/archive` — 获取归档计划列表
- `GET /api/study-plan/archived` — 获取已归档计划详情
- `POST /api/study-plan/update-daily-task` — 更新每日任务完成状态

**页面元素**：
- 加载状态
- 空状态
- Tab导航：
  1. **弱项分析 (weakness)**
     - 分析时间
     - 统计网格卡片（练习数据）
     - 各模块正确率柱状图
     - 薄弱知识点列表（排名、名称、错误次数）

  2. **学习计划 (plan)**
     - 生成设置：学习时长（小时）、考试目标（如省考）
     - 生成按钮
     - 流式输出区域（实时显示AI生成内容）
     - 结构化计划展示：
       - 本周学习重点（重点卡片）
       - 每日任务列表（带完成打卡按钮）
       - 里程碑节点
       - 学习建议

  3. **历史计划 (history)**
     - 历史计划列表
     - 每条显示：日期范围、进度条、学习重点
     - 归档详情弹窗：
       - 日期、进度
       - 学习重点卡片
       - 任务列表
       - 里程碑
       - 建议

---

### 3.19 上岸秘籍 `/secrets/`

**组件名**：`SecretsContent` + `SecretsSidebar`

**功能描述**：
- 行测申论技巧合集
- 树形目录结构浏览

**API端点**：
- `GET /api/secrets/tree?type=xingce` — 获取行测秘籍目录树
- `GET /api/secrets/tree?type=shenlun` — 获取申论秘籍目录树
- `GET /api/secrets/article/{id}` — 获取具体文章内容

**页面元素**：
- 树形侧边栏导航（TreeNodeItem 递归组件）
- 文章内容展示区
- Markdown渲染

---

### 3.20 进站必读 `/must-read/`

**组件名**：`MustRead`

**功能描述**：
- 网站使用说明/指南

**API端点**：
- `GET /api/must-read` — 获取必读内容

---

### 3.21 资料合集
- 外部链接：飞书知识库
- URL：`https://my.feishu.cn/wiki/UuZWwzr6bivixHkPQ07c6QO7nGe?from=from_copylink`
- 新窗口打开

---

## 四、全局统计/追踪 API

### 4.1 用户行为追踪
- `POST /api/stats/track` — 访客追踪（POST，body含 visitor_id）
- `POST /api/stats/record` — 行为记录
- `POST /api/stats/heartbeat` — 心跳检测
- `GET /api/stats/today-online` — 今日在线人数

### 4.2 成语查询API
- 第三方：`GET https://api.yuafeng.cn/API/cy/api.php?msg={成语}&b=1&hh=[换行]`

---

## 五、完整API端点汇总

| 方法 | 端点 | 说明 |
|------|------|------|
| POST | `/api/auth/login-password` | 邮箱密码登录 |
| POST | `/api/auth/register` | 注册 |
| POST | `/api/auth/send-code` | 发送验证码 |
| POST | `/api/auth/reset-password` | 重置密码 |
| POST | `/api/auth/refresh` | 刷新Token |
| GET | `/api/papers?category=行测` | 获取行测试卷列表 |
| GET | `/api/papers?category=申论` | 获取申论试卷列表 |
| GET | `/api/papers?{params}` | 筛选试卷 |
| GET | `/api/papers/{id}` | 获取试卷详情 |
| GET | `/api/papers/{id}/materials` | 获取试卷材料 |
| GET | `/api/papers/{id}/my-answers` | 获取我的答案 |
| GET | `/api/papers/questions/by-knowledge?knowledge_point={}` | 按知识点获取题目 |
| POST | `/api/papers/user-answers/batch` | 批量提交答案 |
| POST | `/api/ai-question/analyze-stream` | AI分析（流式） |
| GET | `/api/ai-question/my-questions` | 获取AI推荐题目 |
| DELETE | `/api/ai-question/{id}` | 删除推荐题目 |
| GET | `/api/essay-review` | 获取批改列表 |
| GET | `/api/essay-review/{id}` | 获取批改详情 |
| DELETE | `/api/essay-review/{id}` | 删除批改记录 |
| POST | `/api/essay-review/stream` | AI批改（流式） |
| POST | `/api/essay-review/ocr` | OCR图片识别 |
| GET | `/api/essay-review/count/remaining` | 剩余批改次数 |
| GET | `/api/analysis/history` | 获取分析历史 |
| GET | `/api/analysis/{id}` | 分析详情 |
| DELETE | `/api/analysis/{id}` | 删除分析 |
| GET | `/api/checkin` | 获取打卡数据 |
| POST | `/api/checkin` | 提交打卡 |
| GET | `/api/checkin/tasks` | 获取任务 |
| POST | `/api/checkin/tasks` | 添加任务 |
| PUT | `/api/checkin/tasks/{id}` | 更新任务 |
| DELETE | `/api/checkin/tasks/{id}` | 删除任务 |
| DELETE | `/api/checkin/{timestamp}` | 删除打卡记录 |
| GET | `/api/high-freq-words` | 高频词语 |
| GET | `/api/high-freq-idiom` | 高频成语 |
| GET | `/api/news/xwlb` | 新闻联播 |
| GET | `/api/must-read` | 进站必读内容 |
| GET | `/api/secrets/tree?type=xingce` | 行测秘籍目录 |
| GET | `/api/secrets/tree?type=shenlun` | 申论秘籍目录 |
| GET | `/api/secrets/article/{id}` | 秘籍文章 |
| GET | `/api/study-plan/archive` | 学习计划归档 |
| GET | `/api/study-plan/archived` | 已归档计划 |
| POST | `/api/study-plan/update-daily-task` | 更新任务状态 |
| POST | `/api/stats/track` | 访客追踪 |
| POST | `/api/stats/record` | 行为记录 |
| POST | `/api/stats/heartbeat` | 心跳 |
| GET | `/api/stats/today-online` | 今日在线 |
| GET | `/api/feedback/my` | 我的反馈 |
| POST | `/api/feedback/submit` | 提交反馈 |

---

## 六、认证机制

- 使用 **Bearer Token** 认证
- Token 存储在 `localStorage` 的 `token` 字段
- 用户信息存储在 `localStorage` 的 `user` 字段（JSON格式，含 id、email 等）
- 登录后所有API请求在 headers 中携带 `Authorization: Bearer {token}`
- 支持 Token 自动刷新（`/api/auth/refresh`）

---

## 七、样式设计规范

### 7.1 配色方案
- 品牌主色：蓝色系（VitePress默认品牌色）
- 功能色：
  - 行测模块：`#3b82f6`（蓝色）
  - 申论模块：`#10b981`（绿色）
  - 常识模块：`#f59e0b`（琥珀色）
  - 公基模块：`#8b5cf6`（紫色）
- 评分等级色：
  - ≥80分：`#48bb78`（绿色）
  - ≥60分：`#ed8936`（橙色）
  - <60分：`#f56565`（红色）
- 通用灰色：`#6b7280`

### 7.2 组件样式

**卡片通用样式**：
- 圆角边框
- 阴影效果
- hover 时轻微上浮/加深阴影
- 内间距 padding

**按钮样式**：
- 品牌色主按钮（.btn-brand）：填充背景色
- 次要按钮（.btn-alt）：边框样式
- 功能按钮：圆角、内间距、hover变色

**表单元素**：
- 输入框：圆角、内间距、聚焦边框色变化
- 下拉框：统一圆角风格
- 复选框：自定义勾选样式

**加载状态**：
- 旋转加载图标 + "加载中..." 文字
- ⏳ emoji 加载

**空状态**：
- 居中显示
- 大emoji图标 + 提示文字
- 引导操作按钮/链接

**弹窗/模态框**：
- 遮罩层点击关闭
- 居中内容框
- 右上角关闭按钮 ×
- 底部操作按钮

**Toast 提示**：
- 底部弹出
- 自动消失
- 带动画过渡

**进度条**：
- 圆角条形
- 百分比填充
- 分色显示

### 7.3 响应式设计
- 移动端：导航栏折叠为汉堡菜单
- 侧边栏在移动端变为抽屉式
- 卡片网格自适应列数
- 字体/间距适配

### 7.4 主题切换
- 亮色模式：白色背景、深色文字
- 暗色模式：深色背景、浅色文字
- 通过 CSS 变量实现主题切换
- 存储在 `localStorage` 的 `vitepress-theme-appearance`
- 支持 `auto`（跟随系统）、`dark`、`light` 三种设置

---

## 八、完整页面路由映射

| 路由 | 组件 | 页面名称 |
|------|------|----------|
| `/` | VPHero + 首页内容 | 主页 |
| `/题库/` | PaperList | 行测题库 |
| `/essay-bank/` | EssayBank | 申论题库 |
| `/practice/special/` | PaperList | 专项练习 |
| `/practice/online/` | OnlinePractice | 在线练习 |
| `/practice/history/` | PracticeHistory | 行测智能分析 |
| `/shenlun-practice/` | ShenlunPractice | 申论练习 |
| `/idiom/` | IdiomSearch | 成语查询 |
| `/high-freq-words/` | HighFreqWords | 高频词语 |
| `/high-freq-idiom/` | HighFreqIdiom | 高频成语 |
| `/wrong-questions/` | — | 错题题库 |
| `/今日热榜/` | HotList | 实时热榜 |
| `/checkin/` | CheckIn | 每日打卡 |
| `/history-today/` | HistoryToday | 今日史事 |
| `/news/` | NewsList | 每日新闻 |
| `/login/` | Login | 个人中心/登录 |
| `/essay-review/` | EssayReview | 申论智能批改 |
| `/ai-recommended/` | AIRecommendedQuestions | 错题AI补练 |
| `/study-plan/` | StudyPlan | 学习计划 |
| `/secrets/` | SecretsContent + SecretsSidebar | 上岸秘籍 |
| `/must-read/` | MustRead | 进站必读 |

---

## 九、静态资源

| 文件 | 用途 |
|------|------|
| `/favicon.png` | 网站图标 |
| `/logo1.png` | 首页 Hero 区域 Logo |
| `/footer-bg.png` | 首页底部横幅图片 |
| `/vp-icons.css` | VitePress 图标样式 |
| `/assets/style.lL6xNAFk.css` | 主样式表（含所有组件样式） |
| `/assets/inter-roman-latin.Di8DUHzh.woff2` | Inter 字体文件 |

---

## 十、第三方服务依赖

| 服务 | 用途 |
|------|------|
| `uapis.cn` | 热榜数据API |
| `v2.xxapi.cn` | 历史上的今天API |
| `api.yuafeng.cn` | 成语查询API |
| `api.iconify.design` | SVG 图标库 |
| 飞书知识库 | 资料合集托管 |
