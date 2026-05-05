# 试卷导入功能设计

## 概述

管理员通过上传三个文件（试题、答案、答案解析）来批量导入试卷。系统自动解析 HTML 文件内容，提取题型、题目、选项、答案、解析和图片，经预览确认后写入数据库。

## 背景

公考试卷通常以三个独立文件提供：
- **试题文件**：包含题目内容、选项，按题型（一~六）分组
- **答案文件**：仅包含 `序号→字母` 的答案列表
- **解析文件**：按题目序号提供详细解析

文件格式实际上是 HTML（以 `.doc` 扩展名保存），结构化程度高，适合纯规则解析。

## 核心需求

1. 管理员专用，通过后台页面上传
2. 纯规则解析（BeautifulSoup），不依赖 LLM
3. 支持图片下载到 MinIO 存储
4. 自动从文件名/标题提取元信息（年份、分类等），支持手动修正
5. 三步流程：上传文件 → 预览解析结果 → 确认入库
6. 支持资料分析部分的材料组（material_group）

## 数据流

```
步骤1: 上传文件
  前端 → POST /ai/import/upload (3 files)
  AI Service 暂存文件到临时目录
  返回: { temp_id, metadata: { title, year, category, region } }

步骤2: 解析预览
  前端 → POST /ai/import/parse { temp_id }
  AI Service:
    1. BeautifulSoup 解析 3 个 HTML 文件
    2. 提取题型、题目、选项、答案、解析
    3. 下载图片 → 上传 MinIO → 替换 URL
    4. 关联材料组
  返回: { metadata, sections: [{ module, questions: [...] }], material_groups: [...] }

步骤3: 确认入库
  前端 → POST /api/admin/import/confirm { metadata, sections, material_groups }
  Backend:
    1. 创建 Paper 记录
    2. 创建 MaterialGroup 记录
    3. 批量创建 Question 记录（设置 sort_order）
    4. 更新 Paper.question_count
  返回: { paper_id }
```

## HTML 解析规则

### 试题文件

| 元素 | CSS 选择器 / 模式 | 提取内容 |
|------|-------------------|----------|
| 标题 | `h3[align="center"]` | 试卷标题 |
| 分类信息 | `p[align="center"]` | 分类/来源（提取年份、分类） |
| 题型标题 | `div.subtitle` | 题型名 + 说明（如 "一、政治理论..."） |
| 材料组标题 | `div.sub2title` | 材料组编号（如 "（二）"） |
| 材料组内容 | sub2title 后的 `div.col-xs-12` | 材料正文 + 图片 |
| 题目序号 | `div.col-xs-1.left` | 题号 |
| 题目内容 | `div.col-xs-11.right` | 题干 + 选项 |
| 选项 | `div.col-xs-3` 或 `div.col-xs-6` | 选项文本，格式 "A、xxx" |
| 图片 | `img[src]` | 外部图片 URL |

### 答案文件

正则提取：`(\d+)、([A-D])`，构建 `dict[int, str]` 映射。

### 解析文件

与试题文件同结构（`div.col-xs-1.left` 序号 + `div.col-xs-11.right` 解析内容），按序号匹配。

### 题型映射

从 section title 提取 module：

| Section 前缀 | module 值 |
|-------------|-----------|
| 一、政治理论 | 政治理论 |
| 二、常识判断 | 常识判断 |
| 三、言语理解与表达 | 言语理解 |
| 四、数量关系 | 数量关系 |
| 五、判断推理 | 判断推理 |
| 六、资料分析 | 资料分析 |

### 元信息自动提取

从标题 `<h3>` 提取：
- 年份：正则 `(\d{4})年` → `2026`
- 分类：包含"行测"→ `行测`，包含"申论"→ `申论`
- 地区：包含"国家"→ 国考，否则从省名匹配
- 标题：完整 h3 文本

## 数据模型

导入最终写入的表：
- `paper`：试卷记录（title, category, region_id, year, question_count）
- `material_group`：材料组（资料分析部分的共享材料）
- `question`：题目（content HTML, options JSON, answer, explanation HTML, module, images JSON）

### Question.options JSON 格式

```json
[{"label": "A", "text": "选项文本", "image": "minio://..."}]
```

### Question.images JSON 格式

```json
["minio://bucket/path/image1.png"]
```

## API 设计

### AI Service (FastAPI)

#### POST /ai/import/upload

上传三个文件，暂存到临时目录。

Request: `multipart/form-data`
- `questions`: 试题文件
- `answers`: 答案文件
- `explanations`: 解析文件

Response:
```json
{
  "temp_id": "uuid",
  "metadata": {
    "title": "2026年国家公务员录用考试《行测》题（行政执法卷网友回忆版）",
    "year": 2026,
    "category": "行测",
    "region_name": "国考"
  }
}
```

#### POST /ai/import/parse

解析暂存文件，返回结构化数据。

Request:
```json
{ "temp_id": "uuid" }
```

Response:
```json
{
  "metadata": { ... },
  "sections": [
    {
      "module": "政治理论",
      "description": "根据题目要求，在四个选项中选出一个最恰当的答案。",
      "questions": [
        {
          "sort_order": 1,
          "content": "<p>题干 HTML</p>",
          "options": [
            {"label": "A", "text": "选项A", "image": ""},
            {"label": "B", "text": "选项B", "image": ""}
          ],
          "answer": "D",
          "explanation": "<p>解析 HTML</p>",
          "images": [],
          "type": "single_choice",
          "score": 0.8
        }
      ]
    }
  ],
  "material_groups": [
    {
      "title": "（二）",
      "content": "<p>材料内容</p><img src='minio://...'>",
      "images": ["minio://..."],
      "sort_order": 1,
      "question_numbers": [111, 112, 113, 114, 115]
    }
  ]
}
```

### Backend (Spring Boot)

#### POST /api/admin/import/confirm

确认导入，将数据写入数据库。

Request:
```json
{
  "metadata": {
    "title": "...",
    "year": 2026,
    "category": "行测",
    "region_id": 1,
    "rating": 0
  },
  "sections": [ ... ],
  "material_groups": [ ... ]
}
```

Response:
```json
{ "paper_id": 123, "question_count": 130 }
```

## 图片处理

1. 解析 HTML 时收集所有 `<img src="...">` URL
2. 批量下载图片到临时目录
3. 上传到 MinIO（`questions/` 前缀）
4. 将 HTML 中的外部 URL 替换为 MinIO URL
5. 图片 URL 格式：`/api/files/{minio_object_name}`

## 前端页面

管理后台新增试卷导入页面（`/admin/import/`），包含：

1. **上传区域**：拖拽或点击上传三个文件
2. **元信息编辑**：自动填充的标题、年份、分类、地区，可手动修改
3. **预览区域**：以题目列表形式展示解析结果，可展开查看每道题的完整内容
4. **确认按钮**：确认无误后点击入库

## 错误处理

- 文件格式校验：确认是 HTML 格式
- 序号匹配校验：三文件题目数量必须一致
- 图片下载失败：记录失败图片 URL，不阻塞导入流程
- 重复导入检测：根据标题+年份+地区检查是否已存在
