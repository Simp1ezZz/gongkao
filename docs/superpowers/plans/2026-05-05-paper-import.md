# Paper Import Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement a 3-step admin workflow (upload → preview → confirm) to import exam papers from 3 HTML files (questions, answers, explanations) parsed with BeautifulSoup, images stored in MinIO, data persisted to MySQL.

**Architecture:** AI Service (FastAPI/Python) handles file upload, HTML parsing, image downloading, and returns structured JSON for preview. Backend (Spring Boot) handles the final confirm step that writes paper/material_group/question records to MySQL. Frontend (Vue 3) provides the admin import wizard UI.

**Tech Stack:** FastAPI, BeautifulSoup4, httpx, minio (Python), Spring Boot, MyBatis-Plus, Vue 3, Axios

---

## File Structure

### AI Service (create/modify)

```
ai-service/
  requirements.txt                    -- MODIFY: add bs4, httpx, minio, python-multipart
  app/
    core/
      config.py                       -- MODIFY: add minio + backend URL settings
    routers/
      import_router.py                -- CREATE: upload + parse endpoints
    services/
      html_parser.py                  -- CREATE: parse 3 HTML files into structured data
      image_handler.py                -- CREATE: download images → upload MinIO → replace URLs
```

### Backend (create/modify)

```
backend/src/main/java/com/gongkao/
  controller/
    FileController.java               -- MODIFY: add GET /api/files/serve/** endpoint
    AdminImportController.java        -- CREATE: POST /api/admin/import/confirm
  dto/
    PaperImportRequest.java           -- CREATE: confirm request DTO (nested sections/questions/material_groups)
  service/
    AdminImportService.java           -- CREATE: create paper + material_groups + questions in transaction
```

### Frontend (create/modify)

```
frontend/
  .vitepress/
    config.ts                         -- MODIFY: add admin nav link
    theme/
      index.ts                        -- MODIFY: register PaperImport component
      components/
        PaperImport.vue               -- CREATE: 3-step import wizard
      utils/
        api.js                        -- MODIFY: add importApi methods
  pages/
    admin/
      import/
        index.md                      -- CREATE: admin import page
```

---

## Task 1: AI Service — Dependencies & Config

**Files:**
- Modify: `ai-service/requirements.txt`
- Modify: `ai-service/app/core/config.py`

- [ ] **Step 1: Add dependencies to requirements.txt**

Add these lines to `ai-service/requirements.txt`:

```
beautifulsoup4==4.13.4
httpx==0.28.1
minio==7.2.15
python-multipart==0.0.20
lxml==5.4.0
```

- [ ] **Step 2: Add MinIO and backend URL settings to config.py**

Replace `ai-service/app/core/config.py` with:

```python
from pydantic_settings import BaseSettings

class Settings(BaseSettings):
    jwt_secret: str = "myDefaultJwtSecretKeyForDevOnlyPleaseReplaceInProd2026"

    # MinIO
    minio_endpoint: str = "localhost:9000"
    minio_access_key: str = "minioadmin"
    minio_secret_key: str = "minioadmin123"
    minio_bucket: str = "gongkao"
    minio_secure: bool = False

    # Backend URL for proxy calls
    backend_url: str = "http://localhost:8080"

    class Config:
        env_prefix = ""
        env_file = ".env"

settings = Settings()
```

- [ ] **Step 3: Install dependencies**

Run: `cd ai-service && pip install -r requirements.txt`

Expected: All packages install successfully.

- [ ] **Step 4: Commit**

```bash
git add ai-service/requirements.txt ai-service/app/core/config.py
git commit -m "feat(ai): add dependencies and config for paper import"
```

---

## Task 2: AI Service — HTML Parser

**Files:**
- Create: `ai-service/app/services/html_parser.py`

This is the core parsing logic. It extracts structured data from the 3 HTML files using BeautifulSoup.

- [ ] **Step 1: Create the HTML parser service**

Create `ai-service/app/services/html_parser.py`:

```python
"""Parse 3 HTML files (questions, answers, explanations) into structured paper data."""

import re
from bs4 import BeautifulSoup, Tag
from pydantic import BaseModel, ConfigDict
from pydantic.alias_generators import to_camel


class OptionItem(BaseModel):
    model_config = ConfigDict(alias_generator=to_camel, populate_by_name=True)

    label: str
    text: str
    image: str = ""


class ParsedQuestion(BaseModel):
    model_config = ConfigDict(alias_generator=to_camel, populate_by_name=True)

    sort_order: int
    content: str
    options: list[OptionItem]
    answer: str = ""
    explanation: str = ""
    images: list[str] = []
    type: str = "single_choice"
    module: str = ""
    material_group_index: int | None = None


class ParsedMaterialGroup(BaseModel):
    model_config = ConfigDict(alias_generator=to_camel, populate_by_name=True)

    title: str
    content: str
    images: list[str] = []
    sort_order: int = 0
    question_numbers: list[int] = []


class ParsedSection(BaseModel):
    model_config = ConfigDict(alias_generator=to_camel, populate_by_name=True)

    module: str
    description: str
    questions: list[ParsedQuestion]


class ParsedMetadata(BaseModel):
    model_config = ConfigDict(alias_generator=to_camel, populate_by_name=True)

    title: str = ""
    year: int | None = None
    category: str = "行测"
    region_name: str = ""


class ParsedPaper(BaseModel):
    model_config = ConfigDict(alias_generator=to_camel, populate_by_name=True)

    metadata: ParsedMetadata
    sections: list[ParsedSection]
    material_groups: list[ParsedMaterialGroup]


# Module mapping: Chinese section prefix → database module value
MODULE_MAP = {
    "政治理论": "政治理论",
    "常识判断": "常识判断",
    "言语理解": "言语理解",
    "数量关系": "数量关系",
    "判断推理": "判断推理",
    "资料分析": "资料分析",
}


def parse_module(section_title: str) -> str:
    """Extract module name from section title like '一、政治理论。...'."""
    for key, value in MODULE_MAP.items():
        if key in section_title:
            return value
    return section_title.split("、", 1)[1].split("。")[0] if "、" in section_title else section_title


def extract_metadata(soup: BeautifulSoup) -> ParsedMetadata:
    """Extract paper metadata from the title and subtitle."""
    title_tag = soup.find("h3", align="center")
    title = title_tag.get_text(strip=True) if title_tag else ""

    year_match = re.search(r"(\d{4})年", title)
    year = int(year_match.group(1)) if year_match else None

    category = "行测" if "行测" in title else ("申论" if "申论" in title else "行测")

    if "国家" in title or "国考" in title:
        region_name = "国考"
    else:
        region_name = ""

    return ParsedMetadata(title=title, year=year, category=category, region_name=region_name)


def parse_answer_map(html_content: str) -> dict[int, str]:
    """Parse the answers HTML file into {question_number: answer_letter}."""
    matches = re.findall(r"(\d+)、([A-D])", html_content)
    return {int(num): letter for num, letter in matches}


def parse_option_div(div: Tag) -> OptionItem:
    """Parse an option div like 'A、some text' into OptionItem."""
    text = div.get_text(strip=True)
    match = re.match(r"^([A-F])[、.:：]\s*(.*)", text, re.DOTALL)
    if match:
        label = match.group(1)
        option_text = match.group(2).strip()
    else:
        label = ""
        option_text = text

    # Check for image inside the option div
    img = ""
    img_tag = div.find("img")
    if img_tag and img_tag.get("src"):
        img = img_tag["src"]

    return OptionItem(label=label, text=option_text, image=img)


def extract_images(element: Tag) -> list[str]:
    """Extract all image URLs from an HTML element."""
    imgs = []
    for img_tag in element.find_all("img"):
        src = img_tag.get("src", "")
        if src:
            imgs.append(src)
    return imgs


def parse_question_row(row: Tag) -> tuple[int, str, list[OptionItem], list[str]]:
    """Parse a single question row. Returns (number, content_html, options, images)."""
    left = row.find("div", class_="left")
    right = row.find("div", class_="right")

    number = int(left.get_text(strip=True)) if left else 0

    content_parts = []
    options = []
    images = []

    if right:
        # Extract images from the right div
        images = extract_images(right)

        # Find all direct children that are NOT option divs
        # Options are in div.col-xs-3 or div.col-xs-6
        for child in right.children:
            if not isinstance(child, Tag):
                continue
            classes = child.get("class", [])
            if "col-xs-3" in classes or "col-xs-6" in classes:
                opt = parse_option_div(child)
                if opt.label:
                    options.append(opt)

        # Build content HTML from <p> tags only (exclude option divs)
        content_tags = []
        for child in right.children:
            if not isinstance(child, Tag):
                continue
            if child.name == "p":
                content_tags.append(str(child))

        content = "\n".join(content_tags)

    return number, content, options, images


def parse_questions_file(html_content: str) -> tuple[ParsedMetadata, list[ParsedSection], list[ParsedMaterialGroup]]:
    """Parse the questions HTML file into sections, questions, and material groups."""
    soup = BeautifulSoup(html_content, "lxml")
    metadata = extract_metadata(soup)

    body = soup.find("body")
    if not body:
        return metadata, [], []

    rows = body.find_all("div", class_="row")

    sections = []
    material_groups = []
    current_section = None
    current_material_group = None
    material_group_sort = 0

    for row in rows:
        # Check for section title (div.subtitle inside div.col-xs-12)
        subtitle = row.find("div", class_="subtitle")
        if subtitle:
            section_text = subtitle.get_text(strip=True)
            module = parse_module(section_text)
            description = section_text.split("。", 1)[1].strip() if "。" in section_text else ""
            current_section = ParsedSection(module=module, description=description, questions=[])
            sections.append(current_section)
            current_material_group = None
            continue

        # Check for material group title (div.sub2title)
        sub2title = row.find("div", class_="sub2title")
        if sub2title:
            title = sub2title.get_text(strip=True)
            material_group_sort += 1
            # Material content is in a sibling div.col-xs-12 (not the sub2title one)
            content_div = row.find("div", class_="col-xs-12")
            if content_div:
                # Remove the sub2title div to get pure content
                for st in content_div.find_all("div", class_="sub2title"):
                    st.decompose()
                content_html = "".join(str(c) for c in content_div.children)
                content_images = extract_images(content_div)
            else:
                content_html = ""
                content_images = []

            current_material_group = ParsedMaterialGroup(
                title=title,
                content=content_html.strip(),
                images=content_images,
                sort_order=material_group_sort,
                question_numbers=[]
            )
            material_groups.append(current_material_group)
            continue

        # Check for question row (div.col-xs-1.left exists)
        left = row.find("div", class_="left")
        if left and current_section is not None:
            number, content, options, images = parse_question_row(row)
            q = ParsedQuestion(
                sort_order=number,
                content=content,
                options=options,
                images=images,
                module=current_section.module,
            )
            if current_material_group is not None:
                q.material_group_index = len(material_groups) - 1
                current_material_group.question_numbers.append(number)

            current_section.questions.append(q)

    return metadata, sections, material_groups


def parse_explanations_file(html_content: str) -> dict[int, str]:
    """Parse explanations file into {question_number: explanation_html}."""
    soup = BeautifulSoup(html_content, "lxml")
    body = soup.find("body")
    if not body:
        return {}

    rows = body.find_all("div", class_="row")
    explanations = {}

    for row in rows:
        left = row.find("div", class_="left")
        right = row.find("div", class_="right")
        if left and right:
            number = int(left.get_text(strip=True))
            # Build explanation HTML from all <p> tags in right div
            exp_parts = [str(p) for p in right.find_all("p")]
            explanations[number] = "\n".join(exp_parts)

    return explanations


def parse_all(questions_html: str, answers_html: str, explanations_html: str) -> ParsedPaper:
    """Parse all 3 files and merge into a single ParsedPaper."""
    metadata, sections, material_groups = parse_questions_file(questions_html)
    answer_map = parse_answer_map(answers_html)
    explanation_map = parse_explanations_file(explanations_html)

    for section in sections:
        for q in section.questions:
            q.answer = answer_map.get(q.sort_order, "")
            q.explanation = explanation_map.get(q.sort_order, "")

    return ParsedPaper(
        metadata=metadata,
        sections=sections,
        material_groups=material_groups,
    )
```

- [ ] **Step 2: Verify parsing with the sample files**

Run a quick smoke test:

```bash
cd ai-service && python -c "
import sys
sys.stdout.reconfigure(encoding='utf-8')
from app.services.html_parser import parse_all

with open(r'C:\Users\24667\Downloads\2026年国家公务员录用考试_行测_题_行政执法卷网友回忆版_.doc', 'r', encoding='utf-8-sig') as f:
    q_html = f.read()
with open(r'C:\Users\24667\Downloads\2026年国家公务员录用考试_行测_题_行政执法卷网友回忆版__答案_.doc', 'r', encoding='utf-8-sig') as f:
    a_html = f.read()
with open(r'C:\Users\24667\Downloads\2026年国家公务员录用考试_行测_题_行政执法卷网友回忆版__解析_.doc', 'r', encoding='utf-8-sig') as f:
    e_html = f.read()

paper = parse_all(q_html, a_html, e_html)
print(f'Title: {paper.metadata.title}')
print(f'Year: {paper.metadata.year}, Category: {paper.metadata.category}')
print(f'Sections: {len(paper.sections)}')
for s in paper.sections:
    print(f'  {s.module}: {len(s.questions)} questions')
print(f'Material groups: {len(paper.material_groups)}')
for mg in paper.material_groups:
    print(f'  {mg.title}: questions {mg.question_numbers}')
total = sum(len(s.questions) for s in paper.sections)
print(f'Total questions: {total}')
# Check first question
q1 = paper.sections[0].questions[0]
print(f'Q1 answer: {q1.answer}')
print(f'Q1 options: {[o.label for o in q1.options]}')
print(f'Q1 has explanation: {bool(q1.explanation)}')
"
```

Expected: 6 sections, 130 questions, answer/explanation matched for each question.

- [ ] **Step 3: Commit**

```bash
git add ai-service/app/services/html_parser.py
git commit -m "feat(ai): add HTML parser for paper import"
```

---

## Task 3: AI Service — Image Handler

**Files:**
- Create: `ai-service/app/services/image_handler.py`

Downloads external images, uploads to MinIO, replaces URLs in HTML content.

- [ ] **Step 1: Create the image handler service**

Create `ai-service/app/services/image_handler.py`:

```python
"""Download external images and upload to MinIO."""

import hashlib
import httpx
import asyncio
from io import BytesIO
from minio import Minio
from urllib.parse import urlparse
from app.core.config import settings


def get_minio_client() -> Minio:
    return Minio(
        settings.minio_endpoint,
        access_key=settings.minio_access_key,
        secret_key=settings.minio_secret_key,
        secure=settings.minio_secure,
    )


def ensure_bucket(client: Minio):
    """Ensure the bucket exists."""
    if not client.bucket_exists(settings.minio_bucket):
        client.make_bucket(settings.minio_bucket)


def image_url_to_key(url: str) -> str:
    """Convert an external image URL to a MinIO object key."""
    ext = ".png"
    parsed = urlparse(url)
    path = parsed.path
    if "." in path.split("/")[-1]:
        ext = "." + path.split("/")[-1].rsplit(".", 1)[1]
        # Normalize uncommon extensions
        if ext not in (".png", ".jpg", ".jpeg", ".gif", ".bmp", ".webp", ".svg"):
            ext = ".png"
    hash_str = hashlib.md5(url.encode()).hexdigest()[:16]
    return f"questions/{hash_str}{ext}"


async def download_image(url: str) -> tuple[str, bytes, str] | None:
    """Download a single image. Returns (url, bytes, content_type) or None on failure."""
    try:
        async with httpx.AsyncClient(timeout=30, follow_redirects=True) as client:
            resp = await client.get(url)
            if resp.status_code == 200:
                content_type = resp.headers.get("content-type", "image/png")
                return url, resp.content, content_type
    except Exception:
        pass
    return None


async def download_and_upload_images(
    image_urls: list[str],
    minio_client: Minio,
) -> dict[str, str]:
    """Download images and upload to MinIO. Returns {original_url: minio_key}."""
    ensure_bucket(minio_client)
    url_to_key = {}

    # Download concurrently
    tasks = [download_image(url) for url in image_urls]
    results = await asyncio.gather(*tasks)

    for result in results:
        if result is None:
            continue
        url, data, content_type = result
        key = image_url_to_key(url)

        # Check if already uploaded
        try:
            minio_client.stat_object(settings.minio_bucket, key)
            url_to_key[url] = key
            continue
        except Exception:
            pass

        minio_client.put_object(
            settings.minio_bucket,
            key,
            BytesIO(data),
            length=len(data),
            content_type=content_type,
        )
        url_to_key[url] = key

    return url_to_key


def replace_urls_in_html(html: str, url_map: dict[str, str]) -> str:
    """Replace external URLs with MinIO serve URLs in HTML content."""
    for original_url, key in url_map.items():
        serve_url = f"/api/files/serve/{key}"
        html = html.replace(original_url, serve_url)
    return html
```

- [ ] **Step 2: Test image download with a sample URL**

```bash
cd ai-service && python -c "
import asyncio, sys
sys.stdout.reconfigure(encoding='utf-8')
from app.services.image_handler import download_image, image_url_to_key

url = 'https://upload.gkzhenti.cn/1767342831658/c0d8b20d4a05fd092d411fb3d18a37e1'
key = image_url_to_key(url)
print(f'Key: {key}')

result = asyncio.run(download_image(url))
if result:
    _, data, ct = result
    print(f'Downloaded {len(data)} bytes, type: {ct}')
else:
    print('Download failed')
"
```

Expected: Downloads successfully, key like `questions/xxxxxxxxxxxxxxxx.png`.

- [ ] **Step 3: Commit**

```bash
git add ai-service/app/services/image_handler.py
git commit -m "feat(ai): add image handler for downloading and uploading to MinIO"
```

---

## Task 4: AI Service — Import Router

**Files:**
- Create: `ai-service/app/routers/import_router.py`
- Modify: `ai-service/app/main.py`

Provides `/ai/import/upload` and `/ai/import/parse` endpoints.

- [ ] **Step 1: Create the import router**

Create `ai-service/app/routers/import_router.py`:

```python
"""Paper import endpoints: upload files and parse them."""

import os
import uuid
import asyncio
from fastapi import APIRouter, UploadFile, File, HTTPException
from pydantic import BaseModel

from app.services.html_parser import parse_all
from app.services.image_handler import (
    download_and_upload_images,
    replace_urls_in_html,
    get_minio_client,
)

router = APIRouter()

# In-memory temp storage (fine for single-instance admin tool)
TEMP_DIR = "/tmp/gongkao_import"
os.makedirs(TEMP_DIR, exist_ok=True)


class ParseRequest(BaseModel):
    temp_id: str


class ParseResponse(BaseModel):
    metadata: dict
    sections: list[dict]
    material_groups: list[dict]


def read_file(path: str) -> str:
    """Read a file, trying utf-8-sig first, then gbk."""
    for enc in ("utf-8-sig", "utf-8", "gbk"):
        try:
            with open(path, "r", encoding=enc) as f:
                return f.read()
        except (UnicodeDecodeError, ValueError):
            continue
    raise ValueError(f"Cannot decode file: {path}")


@router.post("/import/upload")
async def upload_files(
    questions: UploadFile = File(...),
    answers: UploadFile = File(...),
    explanations: UploadFile = File(...),
):
    """Upload 3 paper files and return auto-extracted metadata."""
    temp_id = str(uuid.uuid4())
    dir_path = os.path.join(TEMP_DIR, temp_id)
    os.makedirs(dir_path, exist_ok=True)

    # Save files
    for name, uploaded in [("questions", questions), ("answers", answers), ("explanations", explanations)]:
        file_path = os.path.join(dir_path, name)
        content = await uploaded.read()
        with open(file_path, "wb") as f:
            f.write(content)

    # Quick parse to extract metadata
    try:
        q_html = read_file(os.path.join(dir_path, "questions"))
        from app.services.html_parser import parse_questions_file
        metadata, _, _ = parse_questions_file(q_html)
    except Exception as e:
        raise HTTPException(status_code=400, detail=f"Failed to parse files: {e}")

    return {
        "temp_id": temp_id,
        "metadata": {
            "title": metadata.title,
            "year": metadata.year,
            "category": metadata.category,
            "region_name": metadata.region_name,
        },
    }


@router.post("/import/parse")
async def parse_files(req: ParseRequest):
    """Parse previously uploaded files and return structured paper data."""
    dir_path = os.path.join(TEMP_DIR, req.temp_id)
    if not os.path.isdir(dir_path):
        raise HTTPException(status_code=404, detail="Temp files not found. Please upload again.")

    try:
        q_html = read_file(os.path.join(dir_path, "questions"))
        a_html = read_file(os.path.join(dir_path, "answers"))
        e_html = read_file(os.path.join(dir_path, "explanations"))
    except Exception as e:
        raise HTTPException(status_code=400, detail=f"Failed to read files: {e}")

    # Parse all 3 files
    try:
        paper = parse_all(q_html, a_html, e_html)
    except Exception as e:
        raise HTTPException(status_code=400, detail=f"Failed to parse paper: {e}")

    # Collect all image URLs
    all_image_urls = []
    for section in paper.sections:
        for q in section.questions:
            all_image_urls.extend(q.images)
            for opt in q.options:
                if opt.image:
                    all_image_urls.append(opt.image)
    for mg in paper.material_groups:
        all_image_urls.extend(mg.images)

    # Deduplicate
    unique_urls = list(set(url for url in all_image_urls if url.startswith("http")))

    # Download and upload images to MinIO
    url_map = {}
    if unique_urls:
        try:
            minio_client = get_minio_client()
            url_map = await download_and_upload_images(unique_urls, minio_client)
        except Exception:
            pass  # Continue without images if MinIO is unavailable

    # Replace URLs in content
    for section in paper.sections:
        for q in section.questions:
            q.content = replace_urls_in_html(q.content, url_map)
            q.explanation = replace_urls_in_html(q.explanation, url_map)
            for opt in q.options:
                if opt.image and opt.image in url_map:
                    opt.image = f"/api/files/serve/{url_map[opt.image]}"
            q.images = [f"/api/files/serve/{url_map[u]}" for u in q.images if u in url_map]

    for mg in paper.material_groups:
        mg.content = replace_urls_in_html(mg.content, url_map)
        mg.images = [f"/api/files/serve/{url_map[u]}" for u in mg.images if u in url_map]

    return paper.model_dump(by_alias=True)
```

- [ ] **Step 2: Register the import router in main.py**

Replace `ai-service/app/main.py` with:

```python
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from app.routers import health, import_router

app = FastAPI(title="BALA 公考 AI 服务")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
    allow_credentials=True,
)

app.include_router(health.router, prefix="/ai")
app.include_router(import_router.router, prefix="/ai")
```

- [ ] **Step 3: Start AI service and test upload endpoint**

```bash
cd ai-service && uvicorn app.main:app --reload --port 8000
```

Then in another terminal, test with the sample files:

```bash
curl -X POST http://localhost:8000/ai/import/upload \
  -F "questions=@C:\Users\24667\Downloads\2026年国家公务员录用考试_行测_题_行政执法卷网友回忆版_.doc" \
  -F "answers=@C:\Users\24667\Downloads\2026年国家公务员录用考试_行测_题_行政执法卷网友回忆版__答案_.doc" \
  -F "explanations=@C:\Users\24667\Downloads\2026年国家公务员录用考试_行测_题_行政执法卷网友回忆版__解析_.doc"
```

Expected: JSON with `temp_id` and `metadata`.

- [ ] **Step 4: Test the parse endpoint**

Use the `temp_id` from step 3:

```bash
curl -X POST http://localhost:8000/ai/import/parse \
  -H "Content-Type: application/json" \
  -d '{"temp_id": "<TEMP_ID_FROM_STEP_3>"}'
```

Expected: Full paper JSON with 6 sections, 130 questions, matched answers and explanations.

- [ ] **Step 5: Commit**

```bash
git add ai-service/app/routers/import_router.py ai-service/app/main.py
git commit -m "feat(ai): add import upload and parse endpoints"
```

---

## Task 5: Backend — File Serve Endpoint

**Files:**
- Modify: `backend/src/main/java/com/gongkao/controller/FileController.java`

Add a GET endpoint to serve MinIO files by object key. This is needed because question images reference `/api/files/serve/{key}`.

- [ ] **Step 1: Add serve endpoint to FileController**

Add this method to `FileController.java` (inside the class, after the existing `upload` method):

```java
@GetMapping("/serve/**")
public void serveFile(jakarta.servlet.http.HttpServletRequest request,
                      jakarta.servlet.http.HttpServletResponse response) {
    String path = request.getRequestURI().substring("/api/files/serve/".length());
    try (InputStream stream = fileService.getObject(path)) {
        response.setContentType("application/octet-stream");
        response.setHeader("Cache-Control", "public, max-age=86400");
        byte[] buffer = new byte[8192];
        int bytesRead;
        var out = response.getOutputStream();
        while ((bytesRead = stream.read(buffer)) != -1) {
            out.write(buffer, 0, bytesRead);
        }
        out.flush();
    } catch (Exception e) {
        response.setStatus(404);
    }
}
```

- [ ] **Step 2: Add getObject method to FileService**

Add this method to `FileService.java` (after the existing `delete` method):

```java
public InputStream getObject(String objectKey) {
    try {
        return minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(minioConfig.getBucket())
                        .object(objectKey)
                        .build());
    } catch (Exception e) {
        throw new RuntimeException("文件获取失败", e);
    }
}
```

Add the import at the top if not present:

```java
import io.minio.GetObjectArgs;
```

- [ ] **Step 3: Update SecurityConfig to allow public file serving**

In `SecurityConfig.java`, add this line before the `.anyRequest().authenticated()` line:

```java
.requestMatchers("/api/files/serve/**").permitAll()
```

- [ ] **Step 4: Commit**

```bash
git add backend/src/main/java/com/gongkao/controller/FileController.java \
        backend/src/main/java/com/gongkao/service/FileService.java \
        backend/src/main/java/com/gongkao/config/SecurityConfig.java
git commit -m "feat(backend): add file serve endpoint for question images"
```

---

## Task 6: Backend — Import Confirm DTO

**Files:**
- Create: `backend/src/main/java/com/gongkao/dto/PaperImportRequest.java`

- [ ] **Step 1: Create the import confirm request DTO**

Create `backend/src/main/java/com/gongkao/dto/PaperImportRequest.java`:

```java
package com.gongkao.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class PaperImportRequest {

    @Data
    public static class Metadata {
        private String title;
        private String category;
        private Integer year;
        private Integer regionId;
        private Integer rating;
    }

    @Data
    public static class OptionItem {
        private String label;
        private String text;
        private String image;
    }

    @Data
    public static class QuestionItem {
        private Integer sortOrder;
        private String content;
        private List<OptionItem> options;
        private String answer;
        private String explanation;
        private List<String> images;
        private String type;
        private String module;
        private BigDecimal score;
        private Integer materialGroupIndex;
    }

    @Data
    public static class SectionItem {
        private String module;
        private String description;
        private List<QuestionItem> questions;
    }

    @Data
    public static class MaterialGroupItem {
        private String title;
        private String content;
        private List<String> images;
        private Integer sortOrder;
        private List<Integer> questionNumbers;
    }

    private Metadata metadata;
    private List<SectionItem> sections;
    private List<MaterialGroupItem> materialGroups;
}
```

- [ ] **Step 2: Commit**

```bash
git add backend/src/main/java/com/gongkao/dto/PaperImportRequest.java
git commit -m "feat(backend): add PaperImportRequest DTO"
```

---

## Task 7: Backend — Admin Import Controller & Service

**Files:**
- Create: `backend/src/main/java/com/gongkao/service/AdminImportService.java`
- Create: `backend/src/main/java/com/gongkao/controller/AdminImportController.java`

- [ ] **Step 1: Create AdminImportService**

Create `backend/src/main/java/com/gongkao/service/AdminImportService.java`:

```java
package com.gongkao.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gongkao.dto.PaperImportRequest;
import com.gongkao.entity.MaterialGroup;
import com.gongkao.entity.Paper;
import com.gongkao.entity.Question;
import com.gongkao.mapper.MaterialGroupMapper;
import com.gongkao.mapper.PaperMapper;
import com.gongkao.mapper.QuestionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminImportService {

    private final PaperMapper paperMapper;
    private final MaterialGroupMapper materialGroupMapper;
    private final QuestionMapper questionMapper;
    private final ObjectMapper objectMapper;

    @Transactional
    public Map<String, Object> confirmImport(PaperImportRequest req) {
        // 1. Create Paper
        Paper paper = new Paper();
        PaperImportRequest.Metadata meta = req.getMetadata();
        paper.setTitle(meta.getTitle());
        paper.setCategory(meta.getCategory());
        paper.setRegionId(meta.getRegionId());
        paper.setRating(meta.getRating() != null ? meta.getRating() : 0);
        paper.setYear(meta.getYear());
        paperMapper.insert(paper);

        // 2. Create Material Groups
        Map<Integer, Long> materialGroupIndexToId = new HashMap<>();
        if (req.getMaterialGroups() != null) {
            for (int i = 0; i < req.getMaterialGroups().size(); i++) {
                PaperImportRequest.MaterialGroupItem mgItem = req.getMaterialGroups().get(i);
                MaterialGroup mg = new MaterialGroup();
                mg.setPaperId(paper.getId());
                mg.setTitle(mgItem.getTitle());
                mg.setContent(mgItem.getContent());
                mg.setSortOrder(mgItem.getSortOrder() != null ? mgItem.getSortOrder() : i + 1);
                if (mgItem.getImages() != null) {
                    mg.setImages(toJson(mgItem.getImages()));
                }
                materialGroupMapper.insert(mg);
                materialGroupIndexToId.put(i, mg.getId());
            }
        }

        // 3. Create Questions
        int totalCount = 0;
        // Build a map from question number to material group id
        Map<Integer, Long> questionNumberToMgId = new HashMap<>();
        if (req.getMaterialGroups() != null) {
            for (int i = 0; i < req.getMaterialGroups().size(); i++) {
                PaperImportRequest.MaterialGroupItem mgItem = req.getMaterialGroups().get(i);
                Long mgId = materialGroupIndexToId.get(i);
                if (mgItem.getQuestionNumbers() != null) {
                    for (Integer qn : mgItem.getQuestionNumbers()) {
                        questionNumberToMgId.put(qn, mgId);
                    }
                }
            }
        }

        List<Question> allQuestions = new ArrayList<>();
        for (PaperImportRequest.SectionItem section : req.getSections()) {
            for (PaperImportRequest.QuestionItem qItem : section.getQuestions()) {
                Question q = new Question();
                q.setPaperId(paper.getId());
                q.setSortOrder(qItem.getSortOrder());
                q.setModule(qItem.getModule() != null ? qItem.getModule() : section.getModule());
                q.setType(qItem.getType() != null ? qItem.getType() : "single_choice");
                q.setContent(qItem.getContent());
                q.setAnswer(qItem.getAnswer());
                q.setExplanation(qItem.getExplanation());
                q.setScore(qItem.getScore() != null ? qItem.getScore() : BigDecimal.ONE);

                if (qItem.getOptions() != null) {
                    q.setOptions(toJson(qItem.getOptions()));
                }
                if (qItem.getImages() != null) {
                    q.setImages(toJson(qItem.getImages()));
                }

                // Set material group from either the index or the question number mapping
                if (qItem.getMaterialGroupIndex() != null && materialGroupIndexToId.containsKey(qItem.getMaterialGroupIndex())) {
                    q.setMaterialGroupId(materialGroupIndexToId.get(qItem.getMaterialGroupIndex()));
                } else if (questionNumberToMgId.containsKey(qItem.getSortOrder())) {
                    q.setMaterialGroupId(questionNumberToMgId.get(qItem.getSortOrder()));
                }

                allQuestions.add(q);
                totalCount++;
            }
        }

        // Batch insert questions
        for (Question q : allQuestions) {
            questionMapper.insert(q);
        }

        // 4. Update paper question count
        paper.setQuestionCount(totalCount);
        paperMapper.updateById(paper);

        log.info("Imported paper: id={}, title={}, questions={}", paper.getId(), paper.getTitle(), totalCount);

        Map<String, Object> result = new HashMap<>();
        result.put("paper_id", paper.getId());
        result.put("question_count", totalCount);
        return result;
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return null;
        }
    }
}
```

- [ ] **Step 2: Create AdminImportController**

Create `backend/src/main/java/com/gongkao/controller/AdminImportController.java`:

```java
package com.gongkao.controller;

import com.gongkao.common.Result;
import com.gongkao.dto.PaperImportRequest;
import com.gongkao.service.AdminImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminImportController {

    private final AdminImportService adminImportService;

    @PostMapping("/import/confirm")
    public Result<Map<String, Object>> confirmImport(@RequestBody PaperImportRequest req) {
        if (req.getMetadata() == null || req.getSections() == null) {
            return Result.fail("Missing metadata or sections");
        }
        try {
            Map<String, Object> result = adminImportService.confirmImport(req);
            return Result.ok(result);
        } catch (Exception e) {
            return Result.fail("导入失败: " + e.getMessage());
        }
    }
}
```

- [ ] **Step 3: Verify compilation**

```bash
cd backend && mvn compile -q
```

Expected: BUILD SUCCESS.

- [ ] **Step 4: Commit**

```bash
git add backend/src/main/java/com/gongkao/service/AdminImportService.java \
        backend/src/main/java/com/gongkao/controller/AdminImportController.java
git commit -m "feat(backend): add admin import confirm endpoint"
```

---

## Task 8: Frontend — API Methods

**Files:**
- Modify: `frontend/.vitepress/theme/utils/api.js`

- [ ] **Step 1: Add importApi to api.js**

Add a new AI API instance and import methods at the end of `api.js` (before the final `export` line).

First, add the AI service API instance after the existing `api` creation block (after line 10, before the interceptors):

```javascript
// AI 服务 API（不走 /api 代理，走 /ai 代理）
const aiApi = axios.create({
  baseURL: import.meta.env.VITE_AI_BASE_URL || '/ai',
  timeout: 120000,
  headers: { 'Content-Type': 'application/json' }
})
```

Add token interceptor for aiApi (right after `addTokenInterceptor(api)`):

```javascript
addTokenInterceptor(aiApi)
```

Add response interceptor for aiApi (right after `addRefreshInterceptor(api)`):

```javascript
addRefreshInterceptor(aiApi)
```

Add the import API object before the final `export` line:

```javascript
// 管理员导入 API
export const importApi = {
  uploadFiles(files) {
    const formData = new FormData()
    formData.append('questions', files.questions)
    formData.append('answers', files.answers)
    formData.append('explanations', files.explanations)
    return aiApi.post('/import/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  },
  parseFiles(tempId) {
    return aiApi.post('/import/parse', { temp_id: tempId })
  },
  confirmImport(data) {
    return api.post('/admin/import/confirm', data)
  }
}
```

Update the final export line to include `aiApi`:

```javascript
export { api, aiApi, API_BASE }
```

- [ ] **Step 2: Commit**

```bash
git add frontend/.vitepress/theme/utils/api.js
git commit -m "feat(frontend): add import API methods"
```

---

## Task 9: Frontend — PaperImport Component

**Files:**
- Create: `frontend/.vitepress/theme/components/PaperImport.vue`

This is a 3-step wizard: Upload → Preview → Confirm.

- [ ] **Step 1: Create the PaperImport.vue component**

Create `frontend/.vitepress/theme/components/PaperImport.vue`:

```vue
<script setup>
import { ref, computed } from 'vue'
import { importApi, regionApi } from '../utils/api.js'

const step = ref(1) // 1=upload, 2=preview, 3=done
const loading = ref(false)
const error = ref('')

// Step 1: Upload
const files = ref({ questions: null, answers: null, explanations: null })
const tempId = ref('')
const metadata = ref({})

// Step 2: Preview
const parsedData = ref(null)
const regions = ref([])

const canUpload = computed(() =>
  files.value.questions && files.value.answers && files.value.explanations
)

const totalQuestions = computed(() => {
  if (!parsedData.value?.sections) return 0
  return parsedData.value.sections.reduce((sum, s) => sum + s.questions.length, 0)
})

function onFileChange(field, event) {
  files.value[field] = event.target.files[0]
}

async function handleUpload() {
  loading.value = true
  error.value = ''
  try {
    const res = await importApi.uploadFiles(files.value)
    tempId.value = res.data.temp_id
    metadata.value = res.data.metadata
    step.value = 2
    // Load regions for metadata editing
    const regionRes = await regionApi.list()
    regions.value = regionRes.data || regionRes
    // Auto-trigger parse
    await handleParse()
  } catch (e) {
    error.value = e.message || '上传失败'
  } finally {
    loading.value = false
  }
}

async function handleParse() {
  loading.value = true
  error.value = ''
  try {
    const res = await importApi.parseFiles(tempId.value)
    parsedData.value = res.data || res
  } catch (e) {
    error.value = e.message || '解析失败'
  } finally {
    loading.value = false
  }
}

async function handleConfirm() {
  loading.value = true
  error.value = ''
  try {
    const payload = {
      metadata: {
        ...metadata.value,
        regionId: getRegionId(),
      },
      sections: parsedData.value.sections,
      materialGroups: parsedData.value.materialGroups || [],
    }
    const res = await importApi.confirmImport(payload)
    step.value = 3
  } catch (e) {
    error.value = e.message || '导入失败'
  } finally {
    loading.value = false
  }
}

function reset() {
  step.value = 1
  files.value = { questions: null, answers: null, explanations: null }
  tempId.value = ''
  metadata.value = {}
  parsedData.value = null
  error.value = ''
}

function getRegionId() {
  if (!metadata.value.region_name || !regions.value.length) return null
  const match = regions.value.find(r =>
    r.name.includes(metadata.value.region_name) ||
    metadata.value.region_name.includes(r.name) ||
    (metadata.value.region_name === '国考' && r.name === '国家')
  )
  return match ? match.id : null
}
</script>

<template>
  <div class="paper-import">
    <!-- Progress indicator -->
    <div class="steps">
      <div :class="['step-item', { active: step >= 1, done: step > 1 }]">
        <span class="step-num">1</span> 上传文件
      </div>
      <div class="step-line"></div>
      <div :class="['step-item', { active: step >= 2, done: step > 2 }]">
        <span class="step-num">2</span> 预览确认
      </div>
      <div class="step-line"></div>
      <div :class="['step-item', { active: step >= 3 }]">
        <span class="step-num">3</span> 完成
      </div>
    </div>

    <!-- Error message -->
    <div v-if="error" class="error-msg">{{ error }}</div>

    <!-- Step 1: Upload -->
    <div v-if="step === 1" class="upload-section">
      <div class="file-group">
        <label>试题文件</label>
        <input type="file" accept=".doc,.docx,.html" @change="onFileChange('questions', $event)" />
      </div>
      <div class="file-group">
        <label>答案文件</label>
        <input type="file" accept=".doc,.docx,.html" @change="onFileChange('answers', $event)" />
      </div>
      <div class="file-group">
        <label>解析文件</label>
        <input type="file" accept=".doc,.docx,.html" @change="onFileChange('explanations', $event)" />
      </div>
      <button class="btn-primary" :disabled="!canUpload || loading" @click="handleUpload">
        {{ loading ? '上传解析中...' : '上传并解析' }}
      </button>
    </div>

    <!-- Step 2: Preview -->
    <div v-if="step === 2 && parsedData" class="preview-section">
      <!-- Metadata editing -->
      <div class="meta-edit">
        <h3>试卷信息</h3>
        <div class="meta-grid">
          <div class="meta-field">
            <label>标题</label>
            <input v-model="metadata.title" />
          </div>
          <div class="meta-field">
            <label>年份</label>
            <input v-model.number="metadata.year" type="number" />
          </div>
          <div class="meta-field">
            <label>分类</label>
            <select v-model="metadata.category">
              <option value="行测">行测</option>
              <option value="申论">申论</option>
            </select>
          </div>
          <div class="meta-field">
            <label>地区</label>
            <input v-model="metadata.region_name" placeholder="国考/省名" />
          </div>
        </div>
      </div>

      <!-- Summary -->
      <div class="summary">
        <h3>解析结果</h3>
        <p>共 {{ parsedData.sections?.length || 0 }} 个题型，{{ totalQuestions }} 道题目</p>
        <p v-if="parsedData.materialGroups?.length">
          含 {{ parsedData.materialGroups.length }} 个材料组
        </p>
      </div>

      <!-- Section preview -->
      <div class="sections-preview">
        <details v-for="section in parsedData.sections" :key="section.module" class="section-detail">
          <summary>
            <strong>{{ section.module }}</strong>（{{ section.questions.length }} 题）
          </summary>
          <div v-for="q in section.questions.slice(0, 3)" :key="q.sort_order" class="question-preview">
            <p><strong>{{ q.sort_order }}.</strong> <span v-html="q.content.substring(0, 100)"></span>...</p>
            <p class="answer-preview">答案：{{ q.answer }}</p>
          </div>
          <p v-if="section.questions.length > 3" class="more-hint">
            ...等共 {{ section.questions.length }} 题
          </p>
        </details>
      </div>

      <div class="actions">
        <button class="btn-secondary" @click="reset">重新上传</button>
        <button class="btn-primary" :disabled="loading" @click="handleConfirm">
          {{ loading ? '导入中...' : '确认导入' }}
        </button>
      </div>
    </div>

    <!-- Step 3: Done -->
    <div v-if="step === 3" class="done-section">
      <div class="success-icon">&#10004;</div>
      <h3>导入成功！</h3>
      <p>共导入 {{ totalQuestions }} 道题目</p>
      <button class="btn-primary" @click="reset">继续导入</button>
    </div>
  </div>
</template>

<style scoped>
.paper-import {
  max-width: 900px;
  margin: 0 auto;
  padding: 20px;
}

.steps {
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 30px;
}

.step-item {
  display: flex;
  align-items: center;
  gap: 6px;
  color: var(--vp-c-text-3);
  font-size: 14px;
}

.step-item.active { color: var(--vp-c-brand); font-weight: 600; }
.step-item.done { color: var(--vp-c-brand); }

.step-num {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: var(--vp-c-default-soft);
  font-size: 12px;
}

.step-item.active .step-num,
.step-item.done .step-num {
  background: var(--vp-c-brand);
  color: white;
}

.step-line {
  width: 60px;
  height: 2px;
  background: var(--vp-c-divider);
  margin: 0 10px;
}

.error-msg {
  background: #fef2f2;
  color: #dc2626;
  padding: 10px 16px;
  border-radius: 6px;
  margin-bottom: 16px;
  font-size: 14px;
}

.upload-section,
.preview-section,
.done-section {
  background: var(--vp-c-bg-soft);
  border-radius: 12px;
  padding: 24px;
}

.file-group {
  margin-bottom: 16px;
}

.file-group label {
  display: block;
  font-weight: 600;
  margin-bottom: 6px;
  color: var(--vp-c-text-1);
}

.file-group input[type="file"] {
  width: 100%;
  padding: 8px;
  border: 1px dashed var(--vp-c-divider);
  border-radius: 6px;
  background: var(--vp-c-bg);
}

.btn-primary,
.btn-secondary {
  padding: 10px 24px;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  border: none;
  margin-top: 16px;
}

.btn-primary {
  background: var(--vp-c-brand);
  color: white;
}

.btn-primary:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn-secondary {
  background: var(--vp-c-default-soft);
  color: var(--vp-c-text-1);
}

.meta-edit {
  margin-bottom: 24px;
}

.meta-edit h3 {
  margin-bottom: 12px;
}

.meta-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.meta-field label {
  display: block;
  font-size: 13px;
  color: var(--vp-c-text-2);
  margin-bottom: 4px;
}

.meta-field input,
.meta-field select {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid var(--vp-c-divider);
  border-radius: 6px;
  background: var(--vp-c-bg);
  color: var(--vp-c-text-1);
}

.summary {
  margin-bottom: 20px;
}

.section-detail {
  margin-bottom: 8px;
  border: 1px solid var(--vp-c-divider);
  border-radius: 6px;
  padding: 12px;
}

.section-detail summary {
  cursor: pointer;
  font-size: 15px;
}

.question-preview {
  padding: 8px 0;
  border-bottom: 1px solid var(--vp-c-divider);
  font-size: 14px;
}

.answer-preview {
  color: var(--vp-c-brand);
  font-size: 13px;
}

.more-hint {
  color: var(--vp-c-text-3);
  font-size: 13px;
  margin-top: 8px;
}

.actions {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
}

.done-section {
  text-align: center;
  padding: 40px;
}

.success-icon {
  font-size: 48px;
  color: var(--vp-c-brand);
  margin-bottom: 16px;
}
</style>
```

- [ ] **Step 2: Commit**

```bash
git add frontend/.vitepress/theme/components/PaperImport.vue
git commit -m "feat(frontend): add PaperImport wizard component"
```

---

## Task 10: Frontend — Register Component, Create Page, Add Nav

**Files:**
- Modify: `frontend/.vitepress/theme/index.ts`
- Create: `frontend/pages/admin/import/index.md`
- Modify: `frontend/.vitepress/config.ts`

- [ ] **Step 1: Register PaperImport component in theme**

Add to `frontend/.vitepress/theme/index.ts`:

Add import:
```javascript
import PaperImport from './components/PaperImport.vue'
```

Add inside `enhanceApp`:
```javascript
app.component('PaperImport', PaperImport)
```

- [ ] **Step 2: Create admin import page**

Create directory and file `frontend/pages/admin/import/index.md`:

```markdown
---
layout: page
title: 试卷导入
---

<PaperImport />
```

- [ ] **Step 3: Add admin nav link to config.ts**

In `frontend/.vitepress/config.ts`, add to the `nav` array in `themeConfig`:

```javascript
{ text: '管理', link: '/admin/import/' },
```

- [ ] **Step 4: Commit**

```bash
git add frontend/.vitepress/theme/index.ts \
        frontend/pages/admin/import/index.md \
        frontend/.vitepress/config.ts
git commit -m "feat(frontend): add admin import page and navigation"
```

---

## Task 11: End-to-End Integration Test

**Files:** None (manual testing)

- [ ] **Step 1: Start all services**

```bash
cd D:/CODE/gongkao && docker compose up -d
```

Wait for all services to be healthy. Start the AI service separately for development:

```bash
cd ai-service && pip install -r requirements.txt && uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
```

- [ ] **Step 2: Test the full flow**

1. Open `http://localhost:5173/admin/import/`
2. Upload the 3 sample files from `C:\Users\24667\Downloads\`
3. Verify metadata is auto-filled correctly
4. Verify preview shows 6 sections, 130 questions
5. Click "确认导入"
6. Verify success message
7. Check the paper appears at `http://localhost:5173/题库/`
8. Click into the paper and verify questions render correctly

- [ ] **Step 3: Verify image serving**

Open a question with images in the browser. Verify images load from `/api/files/serve/questions/...`.

- [ ] **Step 4: Final commit if any fixes needed**

```bash
git add -A
git commit -m "fix: integration test fixes for paper import"
```
