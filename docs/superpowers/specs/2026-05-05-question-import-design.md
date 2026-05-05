# 题库导入功能设计文档

## 概述

管理员上传PDF/Word格式的真题试卷和答案解析文件，系统通过AI辅助半自动解析，提取题目、选项、答案、解析、图片等内容，经人工审核编辑后导入题库。

## 技术决策

| 决策项 | 选择 | 理由 |
|---|---|---|
| 自动化程度 | AI辅助半自动 | 准确率优先，人工兜底 |
| 使用角色 | 管理员专用 | 保证题库质量 |
| 解析服务 | FastAPI AI服务扩展 | Python PDF/DOCX生态最成熟 |
| 图片处理 | 提取并上传MinIO | 完整保留原始内容 |
| LLM调用策略 | 按模块分批 | 控制token消耗，单次导入约 ¥0.5-2 |
| 公式处理 | LaTeX + KaTeX渲染 | 支持数学公式、化学式等 |

## 整体流程

```
管理员前端 /admin/import/
    │
    ├─ Step 1: 上传题目PDF + 答案文件 + 填写试卷元信息
    │
    ▼
FastAPI /ai/import/parse
    │
    ├─ 1. PyMuPDF提取PDF文本+图片 / python-docx提取DOCX文本+图片
    ├─ 2. 图片上传MinIO，文本中标记 [img_N] 占位符
    ├─ 3. 规则引擎识别模块边界和题号位置
    ├─ 4. 按模块分批发给LLM做结构化提取
    │    - 纯文本题目 → 文本模型（Haiku/Sonnet）
    │    - 含图片题目 → 多模态模型（Sonnet）
    │
    ▼
前端 Step 2: 解析进度（实时显示各模块状态）
    │
    ▼
前端 Step 3: 审核编辑
    │
    ├─ 逐题审核：编辑题目/选项/答案/解析/图片
    ├─ 图片操作：上传新图片、替换已有图片、插入到内容中
    ├─ 模块/子模块调整、分值设置
    ├─ 删除错误题目、重新解析单题
    │
    ▼
Spring Boot POST /api/admin/papers/import
    │
    ├─ 创建 paper 记录
    ├─ 批量插入 question 记录
    ├─ 创建 material_group 记录（资料分析材料）
    │
    ▼
导入完成
```

## API设计

### 1. `POST /ai/import/parse` — 解析文件（FastAPI AI服务）

**输入**：`multipart/form-data`
- `question_file`: 题目文件（PDF）
- `answer_file`: 答案文件（PDF/DOCX）
- `paper_title`: 试卷标题
- `paper_year`: 年份
- `paper_category`: 分类（行测/申论）
- `region_id`: 地区ID

**处理流程**：

1. **文本和图片提取**
   - PDF: PyMuPDF逐页提取文本，`page.get_images()`提取图片，`page.get_text("dict")`获取文本位置信息用于图文关联
   - DOCX: python-docx提取段落文本，从`word/media/`提取嵌入图片，通过`document.xml.rels`关联图片到段落。对python-docx无法解析的异常DOCX（如测试文件），回退到直接解压ZIP读取`word/document.xml`

2. **图片上传MinIO**
   - 路径：`questions/temp/{uuid}/{img_N}.png`
   - 返回presigned URL，占位符`[img_N]`映射到URL

3. **模块边界识别**（规则引擎）
   - 匹配模块标题关键词："一. 政治理论"、"二. 常识判断"、"三. 言语理解与表达"等
   - 匹配题号模式：`\d+\.` 或 `(\d+)\s*$`
   - 确定每道题的文本范围

4. **LLM结构化提取**（按模块分批）
   - 输入：模块内所有题目文本 + 对应答案解析
   - 输出格式：
     ```json
     {
       "questions": [
         {
           "sort_order": 1,
           "module": "政治理论",
           "sub_module": null,
           "type": "single_choice",
           "content": "<p>习近平总书记指出...</p>",
           "options": [
             {"label": "A", "text": "1项", "image": ""},
             {"label": "B", "text": "2项", "image": ""},
             {"label": "C", "text": "3项", "image": ""},
             {"label": "D", "text": "4项", "image": ""}
           ],
           "answer": "C",
           "explanation": "<p>本题考查政治常识。②③④正确...</p>",
           "score": 0.8
         }
       ],
       "material_groups": [
         {
           "sort_order": 1,
           "title": "资料分析材料一",
           "content": "<p>2023年全国著作权...</p>",
           "images": ["http://minio.../chart1.png"]
         }
       ]
     }
     ```

**输出**：
```json
{
  "parse_id": "uuid-for-this-parse-session（用于关联本次解析的临时图片和进度状态）",
  "status": "completed",
  "paper": {
    "title": "2025年安徽省公务员录用考试《行测》",
    "year": 2025,
    "category": "行测",
    "region_id": 1
  },
  "questions": [...],
  "material_groups": [...],
  "stats": {
    "total_questions": 120,
    "parsed_successfully": 118,
    "parsed_with_warnings": 2,
    "modules": {
      "政治理论": 15,
      "常识判断": 15,
      "言语理解与表达": 30,
      "数量关系": 15,
      "判断推理": 30,
      "资料分析": 15
    }
  }
}
```

### 2. `POST /api/admin/papers/import` — 确认导入（Spring Boot）

**输入**：
```json
{
  "paper": {
    "title": "2025年安徽省公务员录用考试《行测》",
    "year": 2025,
    "category": "行测",
    "region_id": 1
  },
  "questions": [
    {
      "sort_order": 1,
      "module": "政治理论",
      "sub_module": null,
      "material_group_id": null,
      "type": "single_choice",
      "content": "<p>...</p>",
      "options": [{"label": "A", "text": "...", "image": ""}],
      "answer": "C",
      "explanation": "<p>...</p>",
      "images": [],
      "score": 0.8
    }
  ],
  "material_groups": [
    {
      "sort_order": 1,
      "title": "材料一",
      "content": "<p>...</p>",
      "images": ["http://minio.../chart.png"]
    }
  ]
}
```

**处理**：
1. 创建paper记录，设置question_count
2. 先批量创建material_group记录（获取ID）
3. 关联material_group_id到对应questions
4. 批量插入question记录
5. 返回paper ID

### 3. `POST /ai/import/reparse-question` — 重新解析单题（可选）

用于审核时对某道题重新调用LLM解析。

## 数据格式

### 模块映射

| 数据库module | 数据库sub_module | 识别关键词 |
|---|---|---|
| 政治理论 | - | "政治理论" |
| 常识判断 | - | "常识判断" |
| 言语理解与表达 | 片段阅读 | 阅读理解、主旨概括 |
| 言语理解与表达 | 逻辑填空 | 选词填空、填入画横线 |
| 数量关系 | 数学运算 | 数学计算、概率、排列组合 |
| 判断推理 | 图形推理 | 图形类题目 |
| 判断推理 | 定义判断 | "根据上述定义" |
| 判断推理 | 类比推理 | 类比、对应关系 |
| 判断推理 | 逻辑判断 | 加强、削弱、前提假设 |
| 资料分析 | - | 含图表材料的综合题 |

### content字段格式

HTML富文本，支持：
- `<p>` 段落
- `<img src="minio-url">` 图片
- `<span class="math-inline">\\(公式\\)</span>` 行内公式（KaTeX）
- `<div class="math-display">\\[公式\\]</div>` 块级公式（KaTeX）

### 图片存储路径

- 解析阶段：`questions/temp/{session_uuid}/{img_N}.png`
- 导入后重命名为：`questions/{paper_id}/{question_id}/{img_N}.png`

## 前端页面设计

### 路由：`/admin/import/`

**Step 1 — 上传文件**
- 两个拖拽上传区域（题目文件 + 答案文件）
- 试卷元信息表单：标题（自动从文件名提取）、年份、地区（下拉）、分类（行测/申论）
- 「开始解析」按钮

**Step 2 — 解析进度**
- 各模块进度列表：✓已完成 / ⟳解析中 / ○等待中
- 当前模块的解析结果实时流入显示
- 可取消解析

**Step 3 — 审核编辑**
- 左侧面板：题目列表（按模块分组，显示题号+内容摘要）
- 右侧面板：选中题目的编辑表单
  - 题目内容：富文本编辑器（支持图片上传插入、LaTeX公式插入）
  - 选项列表：每项可编辑文本和图片
  - 正确答案：下拉选择
  - 解析内容：富文本编辑器
  - 模块/子模块：下拉选择
  - 分值：数字输入
  - 图片管理：查看/替换/删除/新增
- 底部操作栏：上一题/下一题、删除、重新解析此题
- 顶部操作：按模块筛选、统计信息
- 「确认导入」按钮 → 批量提交

### 富文本编辑器需求
- 基础格式：加粗、斜体、列表
- 图片上传：调用MinIO上传API，插入`<img>`标签
- LaTeX公式：弹出输入框，预览后插入KaTeX标记
- 纯HTML存储到数据库

## LLM Prompt设计

### 系统提示词骨架

```
你是公考题目结构化提取专家。你需要将原始试卷文本转换为结构化JSON数据。

## 输出格式
返回JSON数组，每个元素包含：
- sort_order: 题号（整数）
- module: 从以下选择：政治理论/常识判断/言语理解与表达/数量关系/判断推理/资料分析
- sub_module: 根据题型判断，如：片段阅读/逻辑填空/图形推理/定义判断/类比推理/逻辑判断/数学运算
- type: 题目类型（single_choice/multi_choice）
- content: 题目正文（HTML格式，图片用[img_N]标记）
- options: 选项数组，每个含label和text
- answer: 正确答案（如"A"或"AB"）
- explanation: 解析内容（HTML格式）
- score: 分值（默认0.8）

## 规则
1. 图片标记保持为 [img_N] 格式，不要省略
2. 数学公式用LaTeX语法包裹在 \\( \\) 中
3. 多选题的answer包含多个字母
4. 资料分析题需关联到material_group
```

### 按模块分批策略

- 每个模块的题目文本 + 对应答案解析合为一次LLM调用
- 预估单模块token消耗：5K-15K
- 全卷总计：约60K-70K tokens
- 模型选择：纯文本用Haiku（便宜），含图片用Sonnet（多模态）

## 文件解析技术细节

### PDF解析（PyMuPDF）

```python
import fitz

def parse_pdf(file_path):
    doc = fitz.open(file_path)
    pages = []
    images = []

    for page_num in range(doc.page_count):
        page = doc[page_num]

        # 提取文本（保留位置信息）
        blocks = page.get_text("dict")["blocks"]
        text_blocks = []
        for block in blocks:
            if block["type"] == 0:  # text block
                for line in block["lines"]:
                    for span in line["spans"]:
                        text_blocks.append({
                            "text": span["text"],
                            "bbox": span["bbox"],
                            "font": span["font"],
                            "size": span["size"]
                        })

        # 提取图片
        for img_index, img in enumerate(page.get_images()):
            xref = img[0]
            pix = fitz.Pixmap(doc, xref)
            img_key = f"img_p{page_num}_{img_index}"
            # 保存图片并上传MinIO
            images.append({"key": img_key, "page": page_num, "bbox": None})

        pages.append({"text_blocks": text_blocks, "images": images})

    return pages, images
```

### DOCX解析（python-docx + zipfile）

```python
from docx import Document
import zipfile

def parse_docx(file_path):
    doc = Document(file_path)
    paragraphs = []
    images = {}

    # 提取段落文本和图片关系
    for i, para in enumerate(doc.paragraphs):
        text = para.text.strip()
        # 检查段落中是否有图片（通过runs）
        has_image = any(
            run._element.findall('.//{http://schemas.openxmlformats.org/wordprocessingml/2006/main}drawing')
            for run in para.runs
        )
        paragraphs.append({"index": i, "text": text, "has_image": has_image})

    # 提取图片文件
    with zipfile.ZipFile(file_path) as z:
        for name in z.namelist():
            if name.startswith("word/media/"):
                image_data = z.read(name)
                image_name = name.split("/")[-1]
                # 上传到MinIO
                images[image_name] = upload_to_minio(image_data, image_name)

    return paragraphs, images
```

### 题目-答案匹配规则

- 答案文件中的`题目N解析：`标记可直接与题目编号匹配
- 解析结尾的`故正确答案为X`可直接提取正确答案
- 匹配不上的题目标记为warning，等待人工审核

## 管理员权限

当前user表无role字段，两种方案：
1. 在user表增加role字段（ENUM 'user', 'admin'），默认'user'
2. 硬编码管理员邮箱白名单（更简单，后续可迁移）

推荐方案1，新增migration：
```sql
ALTER TABLE user ADD COLUMN role ENUM('user', 'admin') NOT NULL DEFAULT 'user' AFTER email;
```

SecurityConfig中对`/api/admin/**`路径增加角色校验。

## 依赖变更

### FastAPI AI服务（ai-service/requirements.txt）
```
pymupdf>=1.25.0
python-docx>=1.1.0
minio>=7.2.0
```

### 前端（frontend/package.json）
```
katex>=0.16.0
```

### 后端（backend/pom.xml）
无新增依赖。

## 错误处理

| 场景 | 处理方式 |
|---|---|
| 文件格式不支持 | 返回错误提示，前端显示支持的格式 |
| 文件过大（>50MB） | 前端限制上传大小 |
| LLM解析失败（某模块） | 标记该模块为failed，其余模块继续，支持重试 |
| 题目-答案匹配不上 | 标记warning，人工补充 |
| 图片提取失败 | 跳过该图片，标记为[图片提取失败]，人工上传 |
| 部分题目解析不完整 | 标记warning，人工编辑补全 |
| MinIO上传失败 | 重试3次，仍失败则返回错误 |

## 测试策略

1. 用提供的安徽2025行测PDF+答案DOCX作为端到端测试文件
2. 单元测试：PDF文本提取、DOCX文本提取、模块边界识别、题号匹配
3. 集成测试：完整导入流程（上传→解析→审核→导入→验证数据库记录）
4. 边界测试：图片密集的资料分析题、含数学公式的数量关系题
