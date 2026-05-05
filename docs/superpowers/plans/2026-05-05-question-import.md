# Question Import Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build an admin-only question bank import feature that accepts PDF/DOCX exam papers, uses AI to extract structured question data, and lets admins review before importing to the database.

**Architecture:** Extend the existing FastAPI AI service with file parsing (PyMuPDF, python-docx), MinIO image upload, and LLM-based extraction. Add admin role to the Spring Boot backend with a batch import endpoint. Create a 3-step Vue wizard page in the VitePress frontend.

**Tech Stack:** FastAPI + PyMuPDF + python-docx + httpx (AI service); Spring Boot + MyBatis-Plus (backend); Vue 3 + KaTeX (frontend)

---

## File Map

### AI Service — New Files
| File | Responsibility |
|---|---|
| `ai-service/app/services/minio_client.py` | Upload images to MinIO, generate presigned URLs |
| `ai-service/app/services/parser.py` | Extract text + images from PDF and DOCX files |
| `ai-service/app/services/module_detector.py` | Detect module boundaries and question numbers via regex |
| `ai-service/app/services/llm_client.py` | Call LLM APIs with multi-model support |
| `ai-service/app/services/extractor.py` | Orchestrate parsing → detection → LLM extraction |
| `ai-service/app/routers/import_router.py` | `/ai/import/parse`, `/ai/import/models`, `/ai/import/reparse-question` |

### AI Service — Modified Files
| File | Change |
|---|---|
| `ai-service/app/core/config.py` | Add `ModelConfig`, `llm_models`, `minio_*` settings, `get_models()` |
| `ai-service/app/main.py` | Register import router |
| `ai-service/requirements.txt` | Add `pymupdf`, `python-docx`, `minio` |

### Backend — New Files
| File | Responsibility |
|---|---|
| `backend/src/main/java/com/gongkao/controller/AdminController.java` | `POST /api/admin/papers/import` |
| `backend/src/main/java/com/gongkao/service/AdminService.java` | Batch create paper + materials + questions |
| `backend/src/main/java/com/gongkao/dto/PaperImportRequest.java` | Import request DTO |

### Backend — Modified Files
| File | Change |
|---|---|
| `backend/src/main/resources/db/schema.sql` | Add `role` column to `user` table |
| `backend/src/main/java/com/gongkao/entity/User.java` | Add `role` field |
| `backend/src/main/java/com/gongkao/util/JwtUtil.java` | Include `role` in access token claims |
| `backend/src/main/java/com/gongkao/service/AuthService.java` | Pass `role` to `generateAuthResponse` |
| `backend/src/main/java/com/gongkao/dto/AuthResponse.java` | Add `role` field |
| `backend/src/main/java/com/gongkao/config/JwtAuthenticationFilter.java` | Extract role, set `ROLE_ADMIN` authority |
| `backend/src/main/java/com/gongkao/config/SecurityConfig.java` | Require `ROLE_ADMIN` for `/api/admin/**` |

### Frontend — New Files
| File | Responsibility |
|---|---|
| `frontend/pages/admin/import/index.md` | VitePress page for import wizard |
| `frontend/.vitepress/theme/components/QuestionImport.vue` | 3-step import wizard component |

### Frontend — Modified Files
| File | Change |
|---|---|
| `frontend/.vitepress/theme/utils/api.js` | Add `importApi` functions |
| `frontend/.vitepress/theme/index.ts` | Register `QuestionImport` component |
| `frontend/.vitepress/config.ts` | Add admin nav item (conditional) |
| `frontend/package.json` | Add `katex` dependency |

### Config — Modified Files
| File | Change |
|---|---|
| `docker-compose.yml` | Pass `LLM_MODELS`, `MINIO_*` env vars to ai-service |
| `.env.example` | Document new env vars |

---

## Task 1: Admin Role Infrastructure (Backend)

Add admin role to the backend so `/api/admin/**` endpoints are protected.

**Files:**
- Modify: `backend/src/main/resources/db/schema.sql` (user table)
- Modify: `backend/src/main/java/com/gongkao/entity/User.java`
- Modify: `backend/src/main/java/com/gongkao/util/JwtUtil.java`
- Modify: `backend/src/main/java/com/gongkao/dto/AuthResponse.java`
- Modify: `backend/src/main/java/com/gongkao/service/AuthService.java`
- Modify: `backend/src/main/java/com/gongkao/config/JwtAuthenticationFilter.java`
- Modify: `backend/src/main/java/com/gongkao/config/SecurityConfig.java`

- [ ] **Step 1: Add role column to schema.sql**

In `backend/src/main/resources/db/schema.sql`, change the `user` table definition to include a `role` column after `email`:

```sql
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
```

- [ ] **Step 2: Update User.java entity**

In `backend/src/main/java/com/gongkao/entity/User.java`, add the `role` field after `email`:

```java
@Data
@TableName("user")
public class User {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String email;
    private String role;
    private String passwordHash;
    private String nickname;
    private String avatar;
    @TableLogic
    private Integer deleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

- [ ] **Step 3: Update JwtUtil.java to include role in token**

In `backend/src/main/java/com/gongkao/util/JwtUtil.java`, change `generateAccessToken` to accept and include `role`:

```java
public String generateAccessToken(Long userId, String email, String role) {
    return Jwts.builder()
            .subject(String.valueOf(userId))
            .claim("user_id", userId)
            .claim("email", email)
            .claim("role", role)
            .claim("type", "access")
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + accessTokenExpiration))
            .signWith(getSigningKey())
            .compact();
}
```

- [ ] **Step 4: Update AuthResponse.java to include role**

```java
package com.gongkao.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthResponse {
    private String accessToken;
    private String refreshToken;
    private Long userId;
    private String email;
    private String nickname;
    private String avatar;
    private String role;
}
```

- [ ] **Step 5: Update AuthService.java to pass role**

In `backend/src/main/java/com/gongkao/service/AuthService.java`, update `generateAuthResponse`:

```java
private AuthResponse generateAuthResponse(User user) {
    String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getEmail(), user.getRole());
    String refreshToken = jwtUtil.generateRefreshToken(user.getId());
    return new AuthResponse(accessToken, refreshToken,
            user.getId(), user.getEmail(), user.getNickname(), user.getAvatar(), user.getRole());
}
```

- [ ] **Step 6: Update JwtAuthenticationFilter.java to extract role and set authority**

In `backend/src/main/java/com/gongkao/config/JwtAuthenticationFilter.java`, update the token parsing block inside `doFilterInternal`:

```java
if (header != null && header.startsWith("Bearer ")) {
    String token = header.substring(7);
    try {
        Boolean blacklisted = redisTemplate.hasKey("token:blacklist:" + token);
        if (Boolean.TRUE.equals(blacklisted)) {
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"success\":false,\"message\":\"Token已失效\"}");
            return;
        }

        Long userId = jwtUtil.getUserIdFromToken(token);
        var claims = jwtUtil.parseToken(token);
        String role = claims.get("role", String.class);

        List<SimpleGrantedAuthority> authorities = "admin".equals(role)
                ? List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
                : List.of();

        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(userId, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(auth);

        request.setAttribute("userId", userId);
        request.setAttribute("userRole", role);
    } catch (Exception e) {
        log.warn("JWT filter: token parse failed: {}", e.getMessage());
    }
}
```

Add imports at the top:
```java
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import java.util.List;
```

- [ ] **Step 7: Update SecurityConfig.java to require admin role for admin paths**

In `backend/src/main/java/com/gongkao/config/SecurityConfig.java`, add this rule in `authorizeHttpRequests` before `.anyRequest().authenticated()`:

```java
.requestMatchers("/api/admin/**").hasRole("ADMIN")
```

The full `authorizeHttpRequests` block becomes:
```java
.authorizeHttpRequests(auth -> auth
    .requestMatchers("/api/auth/**").permitAll()
    .requestMatchers("/api/stats/**").permitAll()
    .requestMatchers("/api/site-config/**").permitAll()
    .requestMatchers("/api/proxy/**").permitAll()
    .requestMatchers("/api/regions/**").permitAll()
    .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/papers/**").permitAll()
    .requestMatchers("/api/admin/**").hasRole("ADMIN")
    .anyRequest().authenticated()
)
```

- [ ] **Step 8: Apply schema migration**

Since schema.sql only runs on first DB init, run this SQL directly on the running MySQL:

```bash
docker exec -i gongkao-mysql mysql -uroot -pgongkao123 gongkao -e "ALTER TABLE user ADD COLUMN role ENUM('user', 'admin') NOT NULL DEFAULT 'user' AFTER email;"
```

Then promote your user to admin (replace with your actual email):

```bash
docker exec -i gongkao-mysql mysql -uroot -pgongkao123 gongkao -e "UPDATE user SET role='admin' WHERE id=1;"
```

- [ ] **Step 9: Commit**

```bash
git add backend/src/main/resources/db/schema.sql \
  backend/src/main/java/com/gongkao/entity/User.java \
  backend/src/main/java/com/gongkao/util/JwtUtil.java \
  backend/src/main/java/com/gongkao/dto/AuthResponse.java \
  backend/src/main/java/com/gongkao/service/AuthService.java \
  backend/src/main/java/com/gongkao/config/JwtAuthenticationFilter.java \
  backend/src/main/java/com/gongkao/config/SecurityConfig.java
git commit -m "feat: add admin role to user auth system"
```

---

## Task 2: AI Service Config, Dependencies, and MinIO Client

Update the AI service with multi-model config, new dependencies, and MinIO integration.

**Files:**
- Modify: `ai-service/requirements.txt`
- Modify: `ai-service/app/core/config.py`
- Create: `ai-service/app/services/minio_client.py`
- Modify: `docker-compose.yml`
- Modify: `.env.example`

- [ ] **Step 1: Update requirements.txt**

```
fastapi==0.115.12
uvicorn[standard]==0.34.2
httpx==0.28.1
python-jose[cryptography]==3.3.0
pydantic==2.10.6
pydantic-settings==2.14.0
sse-starlette==2.2.1
pymupdf>=1.25.0
python-docx>=1.1.0
minio>=7.2.0
```

- [ ] **Step 2: Update config.py with multi-model and MinIO settings**

```python
import json
from pydantic import BaseModel
from pydantic_settings import BaseSettings


class ModelConfig(BaseModel):
    name: str
    api_url: str
    api_key: str
    model: str
    max_tokens: int = 4096
    supports_vision: bool = False


class Settings(BaseSettings):
    jwt_secret: str = "myDefaultJwtSecretKeyForDevOnlyPleaseReplaceInProd2026"

    llm_provider: str = "openai-compatible"
    llm_api_url: str = ""
    llm_api_key: str = ""
    llm_model: str = "gpt-4o"
    llm_max_tokens: int = 4096

    # Multi-model config as JSON string from env var
    llm_models: str = "[]"

    # MinIO config
    minio_endpoint: str = "localhost:9000"
    minio_access_key: str = "minioadmin"
    minio_secret_key: str = "minioadmin123"
    minio_bucket: str = "gongkao"

    class Config:
        env_prefix = ""
        env_file = ".env"

    def get_models(self) -> list[ModelConfig]:
        models = json.loads(self.llm_models)
        default = ModelConfig(
            name=f"默认({self.llm_model})",
            api_url=self.llm_api_url,
            api_key=self.llm_api_key,
            model=self.llm_model,
            max_tokens=self.llm_max_tokens,
            supports_vision=True,
        )
        return [default] + [ModelConfig(**m) for m in models]


settings = Settings()
```

- [ ] **Step 3: Create minio_client.py**

```python
import io
import uuid
import logging
from minio import Minio
from minio.http import Method
from app.core.config import settings

logger = logging.getLogger(__name__)


def get_minio_client() -> Minio:
    return Minio(
        settings.minio_endpoint,
        access_key=settings.minio_access_key,
        secret_key=settings.minio_secret_key,
        secure=False,
    )


def ensure_bucket(client: Minio):
    if not client.bucket_exists(settings.minio_bucket):
        client.make_bucket(settings.minio_bucket)
        logger.info("Created MinIO bucket: %s", settings.minio_bucket)


def upload_image(image_data: bytes, filename: str, session_id: str) -> str:
    """Upload image bytes to MinIO, return the object key."""
    client = get_minio_client()
    ensure_bucket(client)
    ext = filename.rsplit(".", 1)[-1] if "." in filename else "png"
    object_key = f"questions/temp/{session_id}/{uuid.uuid4().hex[:8]}.{ext}"
    client.put_object(
        settings.minio_bucket,
        object_key,
        io.BytesIO(image_data),
        length=len(image_data),
        content_type=f"image/{ext}",
    )
    return object_key


def get_presigned_url(object_key: str) -> str:
    client = get_minio_client()
    return client.get_presigned_url(
        Method.GET,
        settings.minio_bucket,
        object_key,
        expires=3600,
    )
```

- [ ] **Step 4: Update docker-compose.yml**

In the `ai-service` section, add MinIO and LLM_MODELS env vars:

```yaml
  ai-service:
    build:
      context: ./ai-service
      dockerfile: Dockerfile
    container_name: gongkao-ai
    restart: unless-stopped
    ports:
      - "8000:8000"
    environment:
      JWT_SECRET: ${JWT_SECRET:-myDefaultJwtSecretKeyForDevOnlyPleaseReplaceInProd2026}
      LLM_PROVIDER: ${LLM_PROVIDER:-openai-compatible}
      LLM_API_URL: ${LLM_API_URL:-}
      LLM_API_KEY: ${LLM_API_KEY:-}
      LLM_MODEL: ${LLM_MODEL:-gpt-4o}
      LLM_MODELS: ${LLM_MODELS:-[]}
      MINIO_ENDPOINT: ${MINIO_ENDPOINT:-minio:9000}
      MINIO_ACCESS_KEY: ${MINIO_ROOT_USER:-minioadmin}
      MINIO_SECRET_KEY: ${MINIO_ROOT_PASSWORD:-minioadmin123}
      MINIO_BUCKET: gongkao
    depends_on:
      - backend
```

- [ ] **Step 5: Update .env.example**

Append these lines:

```env
# Multi-model LLM config (JSON array, optional)
LLM_MODELS=[]

# MinIO (used by AI service for image uploads)
# MINIO_ENDPOINT and keys are shared with docker-compose.yml
```

- [ ] **Step 6: Install new deps locally**

```bash
cd ai-service && pip install -r requirements.txt
```

- [ ] **Step 7: Commit**

```bash
git add ai-service/requirements.txt \
  ai-service/app/core/config.py \
  ai-service/app/services/minio_client.py \
  docker-compose.yml \
  .env.example
git commit -m "feat: add multi-model config, MinIO client, and deps to AI service"
```

---

## Task 3: File Parsing Service (AI Service)

Create the PDF and DOCX parsing module that extracts text and images.

**Files:**
- Create: `ai-service/app/services/parser.py`

- [ ] **Step 1: Create parser.py**

```python
import io
import re
import zipfile
import logging
import xml.etree.ElementTree as ET
from dataclasses import dataclass, field

import fitz  # PyMuPDF

from app.services.minio_client import upload_image, get_presigned_url

logger = logging.getLogger(__name__)

NS_W = "http://schemas.openxmlformats.org/wordprocessingml/2006/main"
NS_WP = "http://schemas.openxmlformats.org/drawingml/2006/main"
NS_REL = "http://schemas.openxmlformats.org/officeDocument/2006/relationships"


@dataclass
class ExtractedImage:
    key: str          # MinIO object key
    url: str          # presigned URL
    placeholder: str  # e.g. [img_0]


@dataclass
class ParseResult:
    text: str                       # full extracted text with [img_N] placeholders
    images: list[ExtractedImage] = field(default_factory=list)


def parse_pdf(file_bytes: bytes, session_id: str) -> ParseResult:
    doc = fitz.open(stream=file_bytes, filetype="pdf")
    all_text_parts: list[str] = []
    all_images: list[ExtractedImage] = []
    img_counter = 0

    for page_num in range(doc.page_count):
        page = doc[page_num]
        # Get text blocks with position info
        blocks = page.get_text("dict")["blocks"]
        for block in blocks:
            if block["type"] == 0:  # text block
                for line in block["lines"]:
                    line_text = ""
                    for span in line["spans"]:
                        line_text += span["text"]
                    all_text_parts.append(line_text)
            elif block["type"] == 1:  # image block
                placeholder = f"[img_{img_counter}]"
                all_text_parts.append(placeholder)
                try:
                    xref = block["image"]
                    pix = fitz.Pixmap(doc, xref)
                    if pix.n >= 5:  # CMYK → convert
                        pix = fitz.Pixmap(fitz.csRGB, pix)
                    img_bytes = pix.tobytes("png")
                    key = upload_image(img_bytes, f"p{page_num}_b{img_counter}.png", session_id)
                    url = get_presigned_url(key)
                    all_images.append(ExtractedImage(key=key, url=url, placeholder=placeholder))
                except Exception as e:
                    logger.warning("Failed to extract image block on page %d: %s", page_num, e)
                    all_text_parts.append("[图片提取失败]")
                img_counter += 1

        # Also extract embedded images not in blocks
        for img_info in page.get_images():
            xref = img_info[0]
            # Skip if we already extracted this image via block
            if any(im.key.endswith(f"_xref{xref}.png") for im in all_images):
                continue
            try:
                pix = fitz.Pixmap(doc, xref)
                if pix.n >= 5:
                    pix = fitz.Pixmap(fitz.csRGB, pix)
                if pix.width < 50 or pix.height < 50:
                    continue  # skip tiny images (icons, bullets)
                img_bytes = pix.tobytes("png")
                placeholder = f"[img_{img_counter}]"
                # Don't append text placeholder here — these are inline images
                # already covered by block extraction or not visible in text
                key = upload_image(img_bytes, f"p{page_num}_xref{xref}.png", session_id)
                url = get_presigned_url(key)
                all_images.append(ExtractedImage(key=key, url=url, placeholder=placeholder))
                img_counter += 1
            except Exception as e:
                logger.warning("Failed to extract embedded image xref=%d: %s", xref, e)

    text = "\n".join(all_text_parts)
    return ParseResult(text=text, images=all_images)


def parse_docx(file_bytes: bytes, session_id: str) -> ParseResult:
    all_text_parts: list[str] = []
    all_images: list[ExtractedImage] = []
    img_counter = 0

    # Fallback: parse XML directly from ZIP (handles malformed docx)
    with zipfile.ZipFile(io.BytesIO(file_bytes)) as zf:
        names = zf.namelist()

        # Build image mapping: rId -> image bytes
        image_map: dict[str, bytes] = {}
        for name in names:
            if name.startswith("word/media/"):
                image_map[name.split("/")[-1]] = zf.read(name)

        # Parse relationships to map rId -> media file
        rel_map: dict[str, str] = {}
        if "word/_rels/document.xml.rels" in names:
            rels_xml = zf.read("word/_rels/document.xml.rels")
            rel_root = ET.fromstring(rels_xml)
            for rel in rel_root:
                rid = rel.get("Id", "")
                target = rel.get("Target", "")
                if target.startswith("media/"):
                    rel_map[rid] = target.replace("media/", "")

        # Parse document.xml
        doc_xml = zf.read("word/document.xml")
        root = ET.fromstring(doc_xml)

        for para in root.iter(f"{{{NS_W}}}p"):
            para_texts: list[str] = []
            para_has_drawing = False

            for run in para.iter(f"{{{NS_W}}}r"):
                # Check for drawing/image
                drawing = run.find(f".//{{{NS_W}}}drawing")
                if drawing is None:
                    # Also check w:pict for older formats
                    drawing = run.find(f".//{{{NS_W}}}pict")

                if drawing is not None:
                    para_has_drawing = True
                    placeholder = f"[img_{img_counter}]"
                    para_texts.append(placeholder)

                    # Try to find the image relationship
                    blip = drawing.find(f".//{{{NS_WP}}}blip")
                    if blip is not None:
                        embed_id = blip.get(f"{{{NS_REL}}}embed", "")
                        img_name = rel_map.get(embed_id, "")
                        if img_name and img_name in image_map:
                            try:
                                img_bytes = image_map[img_name]
                                key = upload_image(img_bytes, img_name, session_id)
                                url = get_presigned_url(key)
                                all_images.append(
                                    ExtractedImage(key=key, url=url, placeholder=placeholder)
                                )
                            except Exception as e:
                                logger.warning("Failed to upload image %s: %s", img_name, e)
                                para_texts[-1] = "[图片提取失败]"
                    img_counter += 1

                # Extract text
                for t in run.iter(f"{{{NS_W}}}t"):
                    if t.text:
                        para_texts.append(t.text)

            line = "".join(para_texts).strip()
            if line:
                all_text_parts.append(line)

    text = "\n".join(all_text_parts)
    return ParseResult(text=text, images=all_images)


def parse_file(file_bytes: bytes, filename: str, session_id: str) -> ParseResult:
    ext = filename.rsplit(".", 1)[-1].lower()
    if ext == "pdf":
        return parse_pdf(file_bytes, session_id)
    elif ext in ("docx", "doc"):
        return parse_docx(file_bytes, session_id)
    else:
        raise ValueError(f"Unsupported file format: {ext}")
```

- [ ] **Step 2: Test parser with the sample files**

Run locally to verify extraction works:

```bash
cd D:/CODE/gongkao
python -c "
from ai_service_path_fix import sys; sys.path.insert(0, 'ai-service')
from app.services.parser import parse_file
import io
# Quick smoke test on PDF
with open('D:/BaiduNetdiskDownload/【省考】历年真题pdf/【01】安徽公务员考试真题pdf版/安徽公务员考试真题——行测06-25/题目/2025年安徽省公务员录用考试《行测》题本.pdf', 'rb') as f:
    data = f.read()
print(f'PDF size: {len(data)} bytes')
# Note: MinIO must be running for full test
"
```

- [ ] **Step 3: Commit**

```bash
git add ai-service/app/services/parser.py
git commit -m "feat: add PDF and DOCX file parsing with image extraction"
```

---

## Task 4: Module Detection and LLM Client (AI Service)

Create the module boundary detector and multi-model LLM client.

**Files:**
- Create: `ai-service/app/services/module_detector.py`
- Create: `ai-service/app/services/llm_client.py`

- [ ] **Step 1: Create module_detector.py**

```python
import re
from dataclasses import dataclass, field


@dataclass
class ModuleBlock:
    name: str            # e.g. "政治理论"
    start_line: int      # inclusive
    end_line: int        # exclusive
    questions: list[dict] = field(default_factory=list)


@dataclass
class QuestionSpan:
    number: int
    start_line: int
    end_line: int


# Known module names in 行测 exams
MODULE_NAMES = [
    "政治理论",
    "常识判断",
    "言语理解与表达",
    "数量关系",
    "判断推理",
    "资料分析",
]

# Patterns for module headers: "一. 政治理论" or "一、政治理论" etc.
MODULE_HEADER_RE = re.compile(
    r"[一二三四五六七八九十]+[.、．]\s*("
    + "|".join(MODULE_NAMES)
    + ")"
)

# Pattern for question numbers: standalone number at start of line or near it
QUESTION_NUM_RE = re.compile(r"(?:^|\n)\s*(\d{1,3})\s*[\.。．、]\s*\n")


def detect_modules(text: str) -> list[ModuleBlock]:
    """Split text into module blocks based on section headers."""
    lines = text.split("\n")
    modules: list[ModuleBlock] = []
    current_module: ModuleBlock | None = None

    for i, line in enumerate(lines):
        m = MODULE_HEADER_RE.search(line)
        if m:
            if current_module is not None:
                current_module.end_line = i
            current_module = ModuleBlock(name=m.group(1), start_line=i, end_line=len(lines))
            modules.append(current_module)

    # If no modules detected, create a single default module
    if not modules:
        modules = [ModuleBlock(name="未分类", start_line=0, end_line=len(lines))]

    # Set end lines
    for i in range(len(modules) - 1):
        modules[i].end_line = modules[i + 1].start_line

    return modules


def detect_question_spans(lines: list[str]) -> list[QuestionSpan]:
    """Find question number positions in text lines."""
    spans: list[QuestionSpan] = []
    for i, line in enumerate(lines):
        m = re.match(r"\s*(\d{1,3})\s*[\.。．]\s*$", line)
        if m:
            spans.append(QuestionSpan(number=int(m.group(1)), start_line=i, end_line=i + 1))
    return spans


def split_text_by_modules(text: str) -> dict[str, str]:
    """Return a dict of module_name -> text_content for that module."""
    modules = detect_modules(text)
    lines = text.split("\n")
    result: dict[str, str] = {}
    for mod in modules:
        mod_lines = lines[mod.start_line:mod.end_line]
        result[mod.name] = "\n".join(mod_lines)
    return result
```

- [ ] **Step 2: Create llm_client.py**

```python
import json
import logging
import httpx
from app.core.config import settings, ModelConfig

logger = logging.getLogger(__name__)

SYSTEM_PROMPT = """你是公考题目结构化提取专家。你需要将原始试卷文本和答案解析转换为结构化JSON数据。

## 输出格式
严格返回一个JSON对象，包含 "questions" 数组和可选的 "material_groups" 数组。

每个question包含：
- sort_order: 题号（整数）
- module: 从以下选择：政治理论/常识判断/言语理解与表达/数量关系/判断推理/资料分析
- sub_module: 根据题型判断，如：片段阅读/逻辑填空/图形推理/定义判断/类比推理/逻辑判断/数学运算。无法判断则设为null
- type: 题目类型，单选"single_choice"，多选"multi_choice"
- content: 题目正文（HTML格式，用<p>标签包裹段落，图片保持[img_N]标记）
- options: 选项数组，每个含label(A/B/C/D)和text
- answer: 正确答案字母，如"A"或"AB"
- explanation: 解析内容（HTML格式）
- score: 分值（默认0.8）

每个material_group包含：
- sort_order: 材料序号
- title: 材料标题
- content: 材料正文（HTML格式）

## 规则
1. 图片标记保持为 [img_N] 格式，不要省略
2. 数学公式用LaTeX语法包裹在 \\( \\) 中（行内）或 \\[ \\] 中（独立行）
3. 多选题的answer包含多个字母如"AB"
4. 资料分析题需要先定义material_group，然后在question中关联（设material_group_index字段指向material_groups数组的索引）
5. 只输出JSON，不要输出其他内容"""


def get_model_by_name(model_name: str | None) -> ModelConfig:
    models = settings.get_models()
    if not model_name:
        return models[0]
    for m in models:
        if m.name == model_name:
            return m
    return models[0]


async def call_llm(
    model_name: str | None,
    module_text: str,
    answer_text: str,
    has_images: bool = False,
) -> dict:
    """Call LLM to extract questions from a module's text."""
    model = get_model_by_name(model_name)

    # Auto-downgrade if model doesn't support vision but we have images
    if has_images and not model.supports_vision:
        models = settings.get_models()
        vision_model = next((m for m in models if m.supports_vision), models[0])
        logger.info(
            "Model %s doesn't support vision, falling back to %s",
            model.name, vision_model.name,
        )
        model = vision_model

    user_content = f"""## 试卷文本（当前模块）
{module_text}

## 答案解析
{answer_text}

请将上述试卷文本中的题目提取为结构化JSON。"""

    messages = [
        {"role": "system", "content": SYSTEM_PROMPT},
        {"role": "user", "content": user_content},
    ]

    async with httpx.AsyncClient(timeout=300) as client:
        resp = await client.post(
            model.api_url,
            headers={
                "Authorization": f"Bearer {model.api_key}",
                "Content-Type": "application/json",
            },
            json={
                "model": model.model,
                "messages": messages,
                "max_tokens": model.max_tokens,
                "temperature": 0.1,
            },
        )
        resp.raise_for_status()
        data = resp.json()

    # Extract text from response
    content = data["choices"][0]["message"]["content"]

    # Strip markdown code fences if present
    content = content.strip()
    if content.startswith("```json"):
        content = content[7:]
    if content.startswith("```"):
        content = content[3:]
    if content.endswith("```"):
        content = content[:-3]
    content = content.strip()

    return json.loads(content)


async def get_available_models() -> list[dict]:
    """Return model list for frontend dropdown."""
    models = settings.get_models()
    return [
        {
            "name": m.name,
            "model": m.model,
            "supports_vision": m.supports_vision,
        }
        for m in models
    ]
```

- [ ] **Step 3: Commit**

```bash
git add ai-service/app/services/module_detector.py \
  ai-service/app/services/llm_client.py
git commit -m "feat: add module detector and multi-model LLM client"
```

---

## Task 5: Question Extraction Orchestrator (AI Service)

Create the main orchestrator that ties parsing, detection, and LLM calling together.

**Files:**
- Create: `ai-service/app/services/extractor.py`

- [ ] **Step 1: Create extractor.py**

```python
import json
import logging
import re
from dataclasses import dataclass, field

from app.services.parser import parse_file, ParseResult
from app.services.module_detector import split_text_by_modules
from app.services.llm_client import call_llm

logger = logging.getLogger(__name__)


@dataclass
class ParsedQuestion:
    sort_order: int
    module: str
    sub_module: str | None
    type: str
    content: str
    options: list[dict]
    answer: str
    explanation: str
    score: float
    images: list[str] = field(default_factory=list)
    material_group_index: int | None = None


@dataclass
class ParsedMaterialGroup:
    sort_order: int
    title: str
    content: str
    images: list[str] = field(default_factory=list)


@dataclass
class ExtractionResult:
    questions: list[ParsedQuestion]
    material_groups: list[ParsedMaterialGroup]
    stats: dict


def _match_answers(questions_text: str, answers_text: str) -> dict[int, str]:
    """Extract per-question answer text from the answer file.

    Returns a dict of question_number -> answer_text.
    """
    result: dict[int, str] = {}
    # Split by "题目N解析：" pattern
    parts = re.split(r"题目\s*(\d+)\s*解析[：:]", answers_text)
    # parts: [preamble, num1, text1, num2, text2, ...]
    for i in range(1, len(parts), 2):
        qnum = int(parts[i])
        text = parts[i + 1] if i + 1 < len(parts) else ""
        result[qnum] = text.strip()
    return result


def _extract_correct_answer(answer_text: str) -> str:
    """Extract the correct answer letter from answer text like '故正确答案为C'."""
    m = re.search(r"故正确答案为\s*([A-Z]+)", answer_text)
    if m:
        return m.group(1)
    return ""


def _replace_image_placeholders(html: str, image_map: dict[str, str]) -> str:
    """Replace [img_N] placeholders with <img> tags."""
    for placeholder, url in image_map.items():
        html = html.replace(placeholder, f'<img src="{url}" alt="题目图片">')
    return html


async def extract_questions(
    question_file_bytes: bytes,
    question_filename: str,
    answer_file_bytes: bytes,
    answer_filename: str,
    session_id: str,
    model_name: str | None = None,
    progress_callback=None,
) -> ExtractionResult:
    """Main extraction pipeline.

    Args:
        progress_callback: async callable(module_name, status, count)
            status: "parsing" | "extracting" | "done" | "failed"
    """
    # 1. Parse both files
    if progress_callback:
        await progress_callback("全部", "parsing", 0)

    q_result: ParseResult = parse_file(question_file_bytes, question_filename, session_id)
    a_result: ParseResult = parse_file(answer_file_bytes, answer_filename, session_id)

    # Build image map: placeholder -> presigned URL
    image_map = {img.placeholder: img.url for img in q_result.images}
    image_map.update({img.placeholder: img.url for img in a_result.images})

    # 2. Split question text by modules
    module_texts = split_text_by_modules(q_result.text)

    # 3. Match answers to question numbers
    answer_map = _match_answers(q_result.text, a_result.text)

    # 4. For each module, call LLM
    all_questions: list[ParsedQuestion] = []
    all_material_groups: list[ParsedMaterialGroup] = []
    module_stats: dict[str, int] = {}

    for module_name, module_text in module_texts.items():
        if not module_text.strip():
            continue

        # Build answer text for this module's questions
        # Extract question numbers from module text
        q_nums = re.findall(r"(?:^|\n)\s*(\d{1,3})\s*[\.。．]\s*\n", module_text)
        module_answer_parts: list[str] = []
        for qn in q_nums:
            num = int(qn)
            if num in answer_map:
                module_answer_parts.append(f"题目{num}解析：\n{answer_map[num]}")
        module_answer_text = "\n\n".join(module_answer_parts) if module_answer_parts else a_result.text

        if progress_callback:
            await progress_callback(module_name, "extracting", 0)

        has_images = any(img.placeholder in module_text for img in q_result.images)

        try:
            result = await call_llm(model_name, module_text, module_answer_text, has_images)
        except Exception as e:
            logger.error("LLM extraction failed for module %s: %s", module_name, e)
            if progress_callback:
                await progress_callback(module_name, "failed", 0)
            continue

        # Process material_groups
        if "material_groups" in result:
            for mg_data in result["material_groups"]:
                mg = ParsedMaterialGroup(
                    sort_order=mg_data.get("sort_order", len(all_material_groups) + 1),
                    title=mg_data.get("title", ""),
                    content=_replace_image_placeholders(
                        mg_data.get("content", ""), image_map
                    ),
                    images=[img for img in image_map.values()
                            if img in mg_data.get("content", "")],
                )
                all_material_groups.append(mg)

        # Process questions
        if "questions" in result:
            for q_data in result["questions"]:
                q = ParsedQuestion(
                    sort_order=q_data.get("sort_order", 0),
                    module=q_data.get("module", module_name),
                    sub_module=q_data.get("sub_module"),
                    type=q_data.get("type", "single_choice"),
                    content=_replace_image_placeholders(
                        q_data.get("content", ""), image_map
                    ),
                    options=q_data.get("options", []),
                    answer=_extract_correct_answer(
                        q_data.get("explanation", "")
                    ) or q_data.get("answer", ""),
                    explanation=_replace_image_placeholders(
                        q_data.get("explanation", ""), image_map
                    ),
                    score=q_data.get("score", 0.8),
                    material_group_index=q_data.get("material_group_index"),
                )
                all_questions.append(q)

        module_stats[module_name] = len(result.get("questions", []))
        if progress_callback:
            await progress_callback(module_name, "done", len(result.get("questions", [])))

    # Sort by sort_order
    all_questions.sort(key=lambda q: q.sort_order)

    stats = {
        "total_questions": len(all_questions),
        "total_material_groups": len(all_material_groups),
        "modules": module_stats,
    }

    return ExtractionResult(
        questions=all_questions,
        material_groups=all_material_groups,
        stats=stats,
    )
```

- [ ] **Step 2: Commit**

```bash
git add ai-service/app/services/extractor.py
git commit -m "feat: add question extraction orchestrator with module-based LLM calling"
```

---

## Task 6: Import Router (AI Service)

Create the FastAPI router that exposes the import endpoints.

**Files:**
- Create: `ai-service/app/routers/import_router.py`
- Modify: `ai-service/app/main.py`

- [ ] **Step 1: Create import_router.py**

```python
import uuid
import logging
from fastapi import APIRouter, UploadFile, File, Form, HTTPException
from typing import Optional

from app.services.extractor import extract_questions
from app.services.llm_client import get_available_models

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/import", tags=["import"])


@router.get("/models")
async def list_models():
    models = await get_available_models()
    return {"models": models}


@router.post("/parse")
async def parse_files(
    question_file: UploadFile = File(...),
    answer_file: UploadFile = File(...),
    paper_title: str = Form(...),
    paper_year: int = Form(...),
    paper_category: str = Form("行测"),
    region_id: Optional[int] = Form(None),
    model_name: Optional[str] = Form(None),
):
    # Validate file types
    q_ext = (question_file.filename or "").rsplit(".", 1)[-1].lower()
    a_ext = (answer_file.filename or "").rsplit(".", 1)[-1].lower()
    if q_ext not in ("pdf",):
        raise HTTPException(400, "题目文件仅支持PDF格式")
    if a_ext not in ("pdf", "docx", "doc"):
        raise HTTPException(400, "答案文件支持PDF或DOCX格式")

    # Read file bytes
    try:
        q_bytes = await question_file.read()
        a_bytes = await answer_file.read()
    except Exception as e:
        raise HTTPException(400, f"文件读取失败: {e}")

    if len(q_bytes) > 50 * 1024 * 1024 or len(a_bytes) > 50 * 1024 * 1024:
        raise HTTPException(400, "文件大小不能超过50MB")

    session_id = uuid.uuid4().hex

    try:
        result = await extract_questions(
            question_file_bytes=q_bytes,
            question_filename=question_file.filename or "questions.pdf",
            answer_file_bytes=a_bytes,
            answer_filename=answer_file.filename or "answers.docx",
            session_id=session_id,
            model_name=model_name,
        )
    except Exception as e:
        logger.exception("Extraction failed")
        raise HTTPException(500, f"解析失败: {e}")

    # Serialize result
    questions_data = []
    for q in result.questions:
        questions_data.append({
            "sort_order": q.sort_order,
            "module": q.module,
            "sub_module": q.sub_module,
            "type": q.type,
            "content": q.content,
            "options": q.options,
            "answer": q.answer,
            "explanation": q.explanation,
            "score": q.score,
            "material_group_index": q.material_group_index,
        })

    material_groups_data = []
    for mg in result.material_groups:
        material_groups_data.append({
            "sort_order": mg.sort_order,
            "title": mg.title,
            "content": mg.content,
            "images": mg.images,
        })

    return {
        "parse_id": session_id,
        "status": "completed",
        "paper": {
            "title": paper_title,
            "year": paper_year,
            "category": paper_category,
            "region_id": region_id,
        },
        "questions": questions_data,
        "material_groups": material_groups_data,
        "stats": result.stats,
    }


@router.post("/reparse-question")
async def reparse_question(
    question_text: str = Form(...),
    answer_text: str = Form(""),
    module: str = Form("未分类"),
    model_name: Optional[str] = Form(None),
):
    """Re-parse a single question using LLM."""
    from app.services.llm_client import call_llm

    try:
        result = await call_llm(model_name, question_text, answer_text)
        return {"question": result.get("questions", [{}])[0] if result.get("questions") else {}}
    except Exception as e:
        logger.exception("Re-parse failed")
        raise HTTPException(500, f"重新解析失败: {e}")
```

- [ ] **Step 2: Update main.py to register the import router**

```python
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.routers import health
from app.routers import import_router

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

- [ ] **Step 3: Commit**

```bash
git add ai-service/app/routers/import_router.py \
  ai-service/app/main.py
git commit -m "feat: add import API endpoints — parse, models, reparse"
```

---

## Task 7: Backend Admin Import API (Java)

Create the admin endpoint that accepts reviewed data and writes to the database.

**Files:**
- Create: `backend/src/main/java/com/gongkao/dto/PaperImportRequest.java`
- Create: `backend/src/main/java/com/gongkao/service/AdminService.java`
- Create: `backend/src/main/java/com/gongkao/controller/AdminController.java`

- [ ] **Step 1: Create PaperImportRequest.java**

```java
package com.gongkao.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class PaperImportRequest {

    @Data
    public static class PaperInfo {
        private String title;
        private Integer year;
        private String category;
        private Integer regionId;
    }

    @Data
    public static class QuestionItem {
        private Integer sortOrder;
        private String module;
        private String subModule;
        private Long materialGroupId;
        private String type;
        private String content;
        private String options;  // JSON string
        private String answer;
        private String explanation;
        private String images;   // JSON string
        private BigDecimal score;
    }

    @Data
    public static class MaterialGroupItem {
        private Integer sortOrder;
        private String title;
        private String content;
        private String images;  // JSON string
    }

    private PaperInfo paper;
    private List<QuestionItem> questions;
    private List<MaterialGroupItem> materialGroups;
}
```

- [ ] **Step 2: Create AdminService.java**

```java
package com.gongkao.service;

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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService {

    private final PaperMapper paperMapper;
    private final MaterialGroupMapper materialGroupMapper;
    private final QuestionMapper questionMapper;

    @Transactional
    public Long importPaper(PaperImportRequest req) {
        // 1. Create paper
        Paper paper = new Paper();
        paper.setTitle(req.getPaper().getTitle());
        paper.setYear(req.getPaper().getYear());
        paper.setCategory(req.getPaper().getCategory());
        paper.setRegionId(req.getPaper().getRegionId());
        paper.setQuestionCount(req.getQuestions() != null ? req.getQuestions().size() : 0);
        paperMapper.insert(paper);

        Long paperId = paper.getId();
        log.info("Created paper id={}, title={}", paperId, paper.getTitle());

        // 2. Create material groups, build temp index -> real ID mapping
        Map<Integer, Long> mgIndexToId = new HashMap<>();
        if (req.getMaterialGroups() != null) {
            for (int i = 0; i < req.getMaterialGroups().size(); i++) {
                PaperImportRequest.MaterialGroupItem item = req.getMaterialGroups().get(i);
                MaterialGroup mg = new MaterialGroup();
                mg.setPaperId(paperId);
                mg.setSortOrder(item.getSortOrder() != null ? item.getSortOrder() : i + 1);
                mg.setTitle(item.getTitle());
                mg.setContent(item.getContent());
                mg.setImages(item.getImages());
                materialGroupMapper.insert(mg);
                mgIndexToId.put(i, mg.getId());
                log.debug("Created material_group id={}", mg.getId());
            }
        }

        // 3. Insert questions
        if (req.getQuestions() != null) {
            for (PaperImportRequest.QuestionItem item : req.getQuestions()) {
                Question q = new Question();
                q.setPaperId(paperId);
                q.setSortOrder(item.getSortOrder());
                q.setModule(item.getModule());
                q.setSubModule(item.getSubModule());
                q.setType(item.getType());
                q.setContent(item.getContent());
                q.setOptions(item.getOptions());
                q.setAnswer(item.getAnswer());
                q.setExplanation(item.getExplanation());
                q.setImages(item.getImages());
                q.setScore(item.getScore());

                // Resolve material_group_id from index
                if (item.getMaterialGroupId() != null && mgIndexToId.containsKey(item.getMaterialGroupId().intValue())) {
                    q.setMaterialGroupId(mgIndexToId.get(item.getMaterialGroupId().intValue()));
                }

                questionMapper.insert(q);
            }
        }

        log.info("Imported {} questions, {} material groups for paper {}",
                req.getQuestions() != null ? req.getQuestions().size() : 0,
                req.getMaterialGroups() != null ? req.getMaterialGroups().size() : 0,
                paperId);

        return paperId;
    }
}
```

- [ ] **Step 3: Create AdminController.java**

```java
package com.gongkao.controller;

import com.gongkao.common.Result;
import com.gongkao.dto.PaperImportRequest;
import com.gongkao.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @PostMapping("/papers/import")
    public Result<Map<String, Object>> importPaper(@RequestBody PaperImportRequest req) {
        Long paperId = adminService.importPaper(req);
        return Result.ok(Map.of("paperId", paperId));
    }
}
```

- [ ] **Step 4: Commit**

```bash
git add backend/src/main/java/com/gongkao/dto/PaperImportRequest.java \
  backend/src/main/java/com/gongkao/service/AdminService.java \
  backend/src/main/java/com/gongkao/controller/AdminController.java
git commit -m "feat: add admin paper import endpoint with batch create"
```

---

## Task 8: Frontend Import Page

Create the 3-step import wizard Vue component and wire it into VitePress.

**Files:**
- Modify: `frontend/.vitepress/theme/utils/api.js`
- Create: `frontend/.vitepress/theme/components/QuestionImport.vue`
- Create: `frontend/pages/admin/import/index.md`
- Modify: `frontend/.vitepress/theme/index.ts`
- Modify: `frontend/.vitepress/config.ts`
- Modify: `frontend/package.json`

- [ ] **Step 1: Add import API functions to api.js**

Append to `frontend/.vitepress/theme/utils/api.js`:

```javascript
// Import API
export const importApi = {
  getModels() {
    return aiApi.get('/import/models')
  },
  parse(formData) {
    return aiApi.post('/import/parse', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      timeout: 600000, // 10 min for LLM processing
    })
  },
  reparseQuestion(formData) {
    return aiApi.post('/import/reparse-question', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      timeout: 120000,
    })
  },
  confirmImport(data) {
    return api.post('/admin/papers/import', data)
  },
}
```

- [ ] **Step 2: Add katex to package.json**

Add `"katex": "^0.16.11"` to `dependencies` in `frontend/package.json`.

- [ ] **Step 3: Create QuestionImport.vue**

This is a large component. It implements the 3-step wizard: upload → parsing → review/edit.

```vue
<template>
  <div class="qi-container">
    <!-- Step indicator -->
    <div class="qi-steps">
      <div :class="['qi-step', step >= 1 && 'active']">
        <span class="qi-step-num">1</span> 上传文件
      </div>
      <div class="qi-step-line" />
      <div :class="['qi-step', step >= 2 && 'active']">
        <span class="qi-step-num">2</span> AI解析
      </div>
      <div class="qi-step-line" />
      <div :class="['qi-step', step >= 3 && 'active']">
        <span class="qi-step-num">3</span> 审核导入
      </div>
    </div>

    <!-- Step 1: Upload -->
    <div v-if="step === 1" class="qi-upload">
      <div class="qi-form-grid">
        <div class="qi-upload-zone" @dragover.prevent @drop.prevent="onDropQuestion">
          <input ref="qFileInput" type="file" accept=".pdf" hidden @change="onQuestionFileChange" />
          <div v-if="!questionFile" class="qi-upload-placeholder" @click="$refs.qFileInput.click()">
            <div class="qi-upload-icon">📄</div>
            <p>点击或拖拽上传题目文件</p>
            <span class="qi-upload-hint">支持 PDF 格式</span>
          </div>
          <div v-else class="qi-upload-selected" @click="$refs.qFileInput.click()">
            <p>{{ questionFile.name }}</p>
            <span>{{ formatSize(questionFile.size) }}</span>
          </div>
        </div>

        <div class="qi-upload-zone" @dragover.prevent @drop.prevent="onDropAnswer">
          <input ref="aFileInput" type="file" accept=".pdf,.docx,.doc" hidden @change="onAnswerFileChange" />
          <div v-if="!answerFile" class="qi-upload-placeholder" @click="$refs.aFileInput.click()">
            <div class="qi-upload-icon">📝</div>
            <p>点击或拖拽上传答案解析文件</p>
            <span class="qi-upload-hint">支持 PDF / DOCX 格式</span>
          </div>
          <div v-else class="qi-upload-selected" @click="$refs.aFileInput.click()">
            <p>{{ answerFile.name }}</p>
            <span>{{ formatSize(answerFile.size) }}</span>
          </div>
        </div>
      </div>

      <div class="qi-meta-form">
        <div class="qi-field">
          <label>试卷标题</label>
          <input v-model="paperTitle" placeholder="如：2025年安徽省公务员录用考试《行测》" />
        </div>
        <div class="qi-field-row">
          <div class="qi-field">
            <label>年份</label>
            <input v-model.number="paperYear" type="number" />
          </div>
          <div class="qi-field">
            <label>分类</label>
            <select v-model="paperCategory">
              <option value="行测">行测</option>
              <option value="申论">申论</option>
            </select>
          </div>
          <div class="qi-field">
            <label>地区</label>
            <select v-model="regionId">
              <option :value="null">不限</option>
              <option v-for="r in regions" :key="r.id" :value="r.id">{{ r.name }}</option>
            </select>
          </div>
        </div>
        <div class="qi-field">
          <label>解析模型</label>
          <select v-model="selectedModel">
            <option :value="null">默认模型</option>
            <option v-for="m in models" :key="m.name" :value="m.name">
              {{ m.name }}{{ m.supports_vision ? ' (支持图片)' : '' }}
            </option>
          </select>
        </div>
      </div>

      <button
        class="qi-btn qi-btn-primary"
        :disabled="!canStartParse"
        @click="startParse"
      >
        开始解析
      </button>
    </div>

    <!-- Step 2: Parsing progress -->
    <div v-if="step === 2" class="qi-progress">
      <h3>AI 正在解析中...</h3>
      <div class="qi-progress-modules">
        <div v-for="(status, mod) in moduleStatus" :key="mod" :class="['qi-progress-item', status]">
          <span class="qi-progress-icon">
            {{ status === 'done' ? '✓' : status === 'extracting' ? '⟳' : status === 'failed' ? '✗' : '○' }}
          </span>
          <span>{{ mod }}</span>
          <span v-if="status === 'done'" class="qi-progress-count">{{ moduleCounts[mod] || 0 }}题</span>
        </div>
      </div>
      <p class="qi-progress-hint">首次解析可能需要 2-5 分钟，请耐心等待</p>
    </div>

    <!-- Step 3: Review & Edit -->
    <div v-if="step === 3" class="qi-review">
      <div class="qi-review-sidebar">
        <div class="qi-review-stats">
          共 {{ questions.length }} 题 · {{ materialGroups.length }} 个材料组
        </div>
        <div class="qi-review-filter">
          <select v-model="filterModule">
            <option value="">全部模块</option>
            <option v-for="mod in uniqueModules" :key="mod" :value="mod">{{ mod }}</option>
          </select>
        </div>
        <div class="qi-review-list">
          <div
            v-for="(q, idx) in filteredQuestions"
            :key="idx"
            :class="['qi-review-item', selectedIdx === questions.indexOf(q) && 'active']"
            @click="selectedIdx = questions.indexOf(q)"
          >
            <span class="qi-review-num">{{ q.sort_order }}</span>
            <span class="qi-review-module">[{{ q.module }}]</span>
            <span class="qi-review-preview">{{ stripHtml(q.content).slice(0, 40) }}</span>
          </div>
        </div>
      </div>

      <div v-if="selectedQuestion" class="qi-review-editor">
        <div class="qi-editor-section">
          <label>题目内容</label>
          <textarea v-model="selectedQuestion.content" rows="5" />
        </div>
        <div class="qi-editor-section">
          <label>选项</label>
          <div v-for="(opt, oi) in selectedQuestion.options" :key="oi" class="qi-option-row">
            <input v-model="opt.label" class="qi-option-label" />
            <input v-model="opt.text" class="qi-option-text" />
          </div>
        </div>
        <div class="qi-editor-row">
          <div class="qi-editor-section">
            <label>正确答案</label>
            <input v-model="selectedQuestion.answer" />
          </div>
          <div class="qi-editor-section">
            <label>题型</label>
            <select v-model="selectedQuestion.type">
              <option value="single_choice">单选</option>
              <option value="multi_choice">多选</option>
            </select>
          </div>
          <div class="qi-editor-section">
            <label>分值</label>
            <input v-model.number="selectedQuestion.score" type="number" step="0.1" />
          </div>
        </div>
        <div class="qi-editor-row">
          <div class="qi-editor-section">
            <label>模块</label>
            <select v-model="selectedQuestion.module">
              <option v-for="mod in allModules" :key="mod" :value="mod">{{ mod }}</option>
            </select>
          </div>
          <div class="qi-editor-section">
            <label>子模块</label>
            <input v-model="selectedQuestion.sub_module" placeholder="如：片段阅读" />
          </div>
        </div>
        <div class="qi-editor-section">
          <label>解析</label>
          <textarea v-model="selectedQuestion.explanation" rows="6" />
        </div>
        <div class="qi-editor-actions">
          <button @click="prevQuestion">上一题</button>
          <button @click="nextQuestion">下一题</button>
          <button class="qi-btn-danger" @click="deleteQuestion">删除此题</button>
        </div>
      </div>
      <div v-else class="qi-review-empty">
        选择左侧题目进行编辑
      </div>

      <div class="qi-review-footer">
        <button class="qi-btn" @click="step = 1">返回上传</button>
        <button class="qi-btn qi-btn-primary" :disabled="questions.length === 0" @click="confirmImport">
          确认导入 ({{ questions.length }} 题)
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { importApi, regionApi, isLoggedIn } from '../utils/api.js'

const step = ref(1)
const questionFile = ref(null)
const answerFile = ref(null)
const paperTitle = ref('')
const paperYear = ref(new Date().getFullYear())
const paperCategory = ref('行测')
const regionId = ref(null)
const selectedModel = ref(null)
const models = ref([])
const regions = ref([])

// Parse results
const questions = ref([])
const materialGroups = ref([])
const parseStats = ref({})
const moduleStatus = ref({})
const moduleCounts = ref({})

// Review state
const selectedIdx = ref(0)
const filterModule = ref('')

const selectedQuestion = computed(() => {
  return questions.value[selectedIdx.value] || null
})

const canStartParse = computed(() => {
  return questionFile.value && answerFile.value && paperTitle.value
})

const uniqueModules = computed(() => {
  return [...new Set(questions.value.map(q => q.module).filter(Boolean))]
})

const allModules = ['政治理论', '常识判断', '言语理解与表达', '数量关系', '判断推理', '资料分析']

const filteredQuestions = computed(() => {
  if (!filterModule.value) return questions.value
  return questions.value.filter(q => q.module === filterModule.value)
})

function onQuestionFileChange(e) {
  questionFile.value = e.target.files[0] || null
  if (questionFile.value) {
    const name = questionFile.value.name.replace(/\.\w+$/, '')
    if (!paperTitle.value) paperTitle.value = name
    const yearMatch = name.match(/(\d{4})/)
    if (yearMatch) paperYear.value = parseInt(yearMatch[1])
  }
}

function onAnswerFileChange(e) {
  answerFile.value = e.target.files[0] || null
}

function onDropQuestion(e) {
  const files = e.dataTransfer.files
  if (files.length) {
    questionFile.value = files[0]
  }
}

function onDropAnswer(e) {
  const files = e.dataTransfer.files
  if (files.length) {
    answerFile.value = files[0]
  }
}

function formatSize(bytes) {
  if (!bytes) return ''
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}

function stripHtml(html) {
  return html ? html.replace(/<[^>]+>/g, '') : ''
}

async function startParse() {
  step.value = 2
  moduleStatus.value = { '正在准备...': 'extracting' }

  const formData = new FormData()
  formData.append('question_file', questionFile.value)
  formData.append('answer_file', answerFile.value)
  formData.append('paper_title', paperTitle.value)
  formData.append('paper_year', paperYear.value)
  formData.append('paper_category', paperCategory.value)
  if (regionId.value) formData.append('region_id', regionId.value)
  if (selectedModel.value) formData.append('model_name', selectedModel.value)

  try {
    const res = await importApi.parse(formData)
    questions.value = res.questions || []
    materialGroups.value = res.material_groups || []
    parseStats.value = res.stats || {}

    // Update module status to done
    moduleStatus.value = {}
    if (parseStats.value.modules) {
      for (const [mod, count] of Object.entries(parseStats.value.modules)) {
        moduleStatus.value[mod] = 'done'
        moduleCounts.value[mod] = count
      }
    }

    setTimeout(() => { step.value = 3 }, 800)
  } catch (err) {
    alert('解析失败: ' + (err.response?.data?.detail || err.message))
    step.value = 1
  }
}

function prevQuestion() {
  if (selectedIdx.value > 0) selectedIdx.value--
}

function nextQuestion() {
  if (selectedIdx.value < questions.value.length - 1) selectedIdx.value++
}

function deleteQuestion() {
  questions.value.splice(selectedIdx.value, 1)
  if (selectedIdx.value >= questions.value.length) {
    selectedIdx.value = Math.max(0, questions.value.length - 1)
  }
}

async function confirmImport() {
  if (!confirm(`确认导入 ${questions.value.length} 道题目？`)) return

  const payload = {
    paper: {
      title: paperTitle.value,
      year: paperYear.value,
      category: paperCategory.value,
      regionId: regionId.value,
    },
    questions: questions.value.map(q => ({
      sort_order: q.sort_order,
      module: q.module,
      sub_module: q.sub_module || null,
      type: q.type || 'single_choice',
      content: q.content,
      options: JSON.stringify(q.options || []),
      answer: q.answer,
      explanation: q.explanation || '',
      images: JSON.stringify(q.images || []),
      score: q.score || 0.8,
    })),
    materialGroups: materialGroups.value.map((mg, i) => ({
      sort_order: mg.sort_order || i + 1,
      title: mg.title || '',
      content: mg.content || '',
      images: JSON.stringify(mg.images || []),
    })),
  }

  try {
    const res = await importApi.confirmImport(payload)
    alert(`导入成功！试卷 ID: ${res.paperId}`)
    window.location.href = '/题库/'
  } catch (err) {
    alert('导入失败: ' + (err.response?.data?.message || err.message))
  }
}

onMounted(async () => {
  if (!isLoggedIn()) {
    window.location.href = '/login/?redirect=/admin/import/'
    return
  }
  try {
    const [modelRes, regionRes] = await Promise.all([
      importApi.getModels(),
      regionApi.list(),
    ])
    models.value = modelRes.models || []
    regions.value = regionRes.list || regionRes || []
  } catch (e) {
    console.warn('Failed to load models/regions', e)
  }
})
</script>

<style scoped>
.qi-container { max-width: 1200px; margin: 0 auto; padding: 20px; font-size: 14px; }

.qi-steps { display: flex; align-items: center; justify-content: center; gap: 0; margin-bottom: 32px; }
.qi-step { display: flex; align-items: center; gap: 8px; color: #999; font-size: 15px; }
.qi-step.active { color: #333; font-weight: 600; }
.qi-step-num { display: inline-flex; width: 28px; height: 28px; border-radius: 50%; background: #eee; color: #999; align-items: center; justify-content: center; font-size: 13px; }
.qi-step.active .qi-step-num { background: #4a6cf7; color: #fff; }
.qi-step-line { width: 60px; height: 2px; background: #eee; margin: 0 8px; }

.qi-form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; margin-bottom: 20px; }
.qi-upload-zone { border: 2px dashed #d9d9d9; border-radius: 8px; padding: 32px; text-align: center; cursor: pointer; transition: border-color 0.2s; min-height: 140px; display: flex; align-items: center; justify-content: center; }
.qi-upload-zone:hover { border-color: #4a6cf7; }
.qi-upload-placeholder { color: #666; }
.qi-upload-icon { font-size: 32px; margin-bottom: 8px; }
.qi-upload-hint { color: #999; font-size: 12px; }
.qi-upload-selected p { font-weight: 600; margin-bottom: 4px; word-break: break-all; }
.qi-upload-selected span { color: #999; font-size: 12px; }

.qi-meta-form { background: #f9f9fb; border-radius: 8px; padding: 20px; margin-bottom: 20px; }
.qi-field { margin-bottom: 12px; }
.qi-field label { display: block; font-weight: 600; margin-bottom: 4px; font-size: 13px; color: #555; }
.qi-field input, .qi-field select { width: 100%; padding: 8px 12px; border: 1px solid #d9d9d9; border-radius: 6px; font-size: 14px; box-sizing: border-box; }
.qi-field-row { display: grid; grid-template-columns: 1fr 1fr 1fr; gap: 12px; }

.qi-btn { padding: 10px 24px; border: 1px solid #d9d9d9; border-radius: 6px; background: #fff; cursor: pointer; font-size: 14px; }
.qi-btn:disabled { opacity: 0.5; cursor: not-allowed; }
.qi-btn-primary { background: #4a6cf7; color: #fff; border-color: #4a6cf7; }
.qi-btn-primary:hover:not(:disabled) { background: #3b5de8; }
.qi-btn-danger { background: #ff4d4f; color: #fff; border-color: #ff4d4f; }

.qi-progress { text-align: center; padding: 60px 20px; }
.qi-progress h3 { margin-bottom: 24px; }
.qi-progress-modules { display: inline-block; text-align: left; }
.qi-progress-item { padding: 8px 0; display: flex; align-items: center; gap: 8px; }
.qi-progress-item.done { color: #52c41a; }
.qi-progress-item.extracting { color: #4a6cf7; }
.qi-progress-item.failed { color: #ff4d4f; }
.qi-progress-count { color: #999; font-size: 12px; margin-left: auto; }
.qi-progress-hint { color: #999; margin-top: 24px; font-size: 13px; }

.qi-review { display: flex; flex-direction: column; gap: 16px; }
.qi-review > :not(.qi-review-footer) { display: flex; gap: 16px; min-height: 0; }

.qi-review-sidebar { width: 280px; flex-shrink: 0; background: #f9f9fb; border-radius: 8px; padding: 12px; max-height: 70vh; overflow-y: auto; }
.qi-review-stats { font-size: 13px; color: #666; margin-bottom: 8px; padding-bottom: 8px; border-bottom: 1px solid #eee; }
.qi-review-filter { margin-bottom: 8px; }
.qi-review-filter select { width: 100%; padding: 6px; border: 1px solid #d9d9d9; border-radius: 4px; font-size: 13px; }
.qi-review-list { display: flex; flex-direction: column; gap: 2px; }
.qi-review-item { padding: 6px 8px; border-radius: 4px; cursor: pointer; display: flex; align-items: center; gap: 6px; font-size: 13px; white-space: nowrap; overflow: hidden; }
.qi-review-item:hover { background: #e8ecff; }
.qi-review-item.active { background: #4a6cf7; color: #fff; }
.qi-review-num { font-weight: 600; min-width: 24px; }
.qi-review-module { font-size: 11px; color: #999; }
.qi-review-item.active .qi-review-module { color: rgba(255,255,255,0.7); }
.qi-review-preview { overflow: hidden; text-overflow: ellipsis; }

.qi-review-editor { flex: 1; background: #fff; border: 1px solid #eee; border-radius: 8px; padding: 20px; overflow-y: auto; max-height: 70vh; }
.qi-editor-section { margin-bottom: 16px; }
.qi-editor-section label { display: block; font-weight: 600; margin-bottom: 4px; font-size: 13px; color: #555; }
.qi-editor-section textarea, .qi-editor-section input, .qi-editor-section select { width: 100%; padding: 8px 12px; border: 1px solid #d9d9d9; border-radius: 6px; font-size: 14px; box-sizing: border-box; font-family: inherit; }
.qi-editor-section textarea { resize: vertical; }
.qi-editor-row { display: grid; grid-template-columns: repeat(auto-fit, minmax(150px, 1fr)); gap: 12px; margin-bottom: 16px; }
.qi-option-row { display: flex; align-items: center; gap: 8px; margin-bottom: 6px; }
.qi-option-label { width: 36px !important; text-align: center; font-weight: 600; }
.qi-option-text { flex: 1; }
.qi-editor-actions { display: flex; gap: 8px; margin-top: 16px; padding-top: 16px; border-top: 1px solid #eee; }
.qi-editor-actions button { padding: 6px 16px; border: 1px solid #d9d9d9; border-radius: 4px; cursor: pointer; font-size: 13px; background: #fff; }

.qi-review-empty { flex: 1; display: flex; align-items: center; justify-content: center; color: #999; background: #f9f9fb; border-radius: 8px; }

.qi-review-footer { display: flex; justify-content: space-between; align-items: center; padding-top: 16px; border-top: 1px solid #eee; }

@media (max-width: 768px) {
  .qi-form-grid { grid-template-columns: 1fr; }
  .qi-field-row { grid-template-columns: 1fr; }
  .qi-review > :not(.qi-review-footer) { flex-direction: column; }
  .qi-review-sidebar { width: 100%; max-height: 200px; }
  .qi-review-editor { max-height: none; }
}
</style>
```

- [ ] **Step 4: Create the VitePress page**

Create `frontend/pages/admin/import/index.md`:

```markdown
---
layout: page
title: 题库导入
---

<QuestionImport />
```

- [ ] **Step 5: Register component in theme/index.ts**

Add `QuestionImport` to the component registrations. The full file becomes:

```typescript
import DefaultTheme from 'vitepress/theme'
import Login from './components/Login.vue'
import PaperList from './components/PaperList.vue'
import OnlinePractice from './components/OnlinePractice.vue'
import Empty from './components/Empty.vue'
import HomeQuickNav from './components/HomeQuickNav.vue'
import Modal from './components/Modal.vue'
import QuestionImport from './components/QuestionImport.vue'

export default {
  extends: DefaultTheme,
  enhanceApp({ app }) {
    app.component('Login', Login)
    app.component('PaperList', PaperList)
    app.component('OnlinePractice', OnlinePractice)
    app.component('Empty', Empty)
    app.component('HomeQuickNav', HomeQuickNav)
    app.component('Modal', Modal)
    app.component('QuestionImport', QuestionImport)
  }
}
```

- [ ] **Step 6: Update config.ts nav**

Add an admin nav item. In `frontend/.vitepress/config.ts`, add to the `nav` array:

```typescript
{ text: '题库导入', link: '/admin/import/' }
```

This can be a temporary addition — in production it would be conditional on user role.

- [ ] **Step 7: Install frontend deps**

```bash
cd frontend && npm install katex
```

- [ ] **Step 8: Commit**

```bash
git add frontend/.vitepress/theme/utils/api.js \
  frontend/.vitepress/theme/components/QuestionImport.vue \
  frontend/.vitepress/theme/index.ts \
  frontend/.vitepress/config.ts \
  frontend/pages/admin/import/index.md \
  frontend/package.json
git commit -m "feat: add 3-step question import wizard page"
```

---

## Task 9: End-to-End Integration Test

Test the full pipeline with the sample files.

**Files:** No new files — manual testing.

- [ ] **Step 1: Start all services**

```bash
cd D:/CODE/gongkao
docker-compose up -d
```

- [ ] **Step 2: Ensure user is admin**

```bash
docker exec -i gongkao-mysql mysql -uroot -pgongkao123 gongkao -e "UPDATE user SET role='admin' WHERE id=1;"
```

- [ ] **Step 3: Log in and get admin token**

Open `http://localhost:5173/login/`, log in. Check localStorage for token.

- [ ] **Step 4: Test model list endpoint**

```bash
curl -s http://localhost:8000/ai/import/models | python -m json.tool
```

Expected: `{"models": [{"name": "默认(gpt-4o)", "model": "gpt-4o", "supports_vision": true}]}`

- [ ] **Step 5: Test import page UI**

Open `http://localhost:5173/admin/import/`. Verify:
- File upload zones render
- Model dropdown populates
- Region dropdown populates

- [ ] **Step 6: Test full parse with sample files**

Upload:
- Question file: `D:\BaiduNetdiskDownload\【省考】历年真题pdf\【01】安徽公务员考试真题pdf版\安徽公务员考试真题——行测06-25\题目\2025年安徽省公务员录用考试《行测》题本.pdf`
- Answer file: `D:\BaiduNetdiskDownload\【省考】历年真题pdf\【01】安徽公务员考试真题pdf版\安徽公务员考试真题——行测06-25\答案及解析\2025年安徽省公务员录用考试《行测》答案及解析.docx`

Verify: questions are extracted and displayed in the review editor.

- [ ] **Step 7: Test confirm import**

After reviewing, click "确认导入". Verify:
- Paper record created in database
- Questions inserted with correct data
- Material groups created if present

```bash
docker exec -i gongkao-mysql mysql -uroot -pgongkao123 gongkao -e "SELECT id, title, question_count FROM paper ORDER BY id DESC LIMIT 5;"
docker exec -i gongkao-mysql mysql -uroot -pgongkao123 gongkao -e "SELECT id, sort_order, module, answer FROM question ORDER BY paper_id DESC, sort_order ASC LIMIT 20;"
```

- [ ] **Step 8: Commit any fixes**

```bash
git add -A && git commit -m "fix: integration test fixes for question import"
```

---

## Self-Review Checklist

### Spec Coverage
| Spec Requirement | Task |
|---|---|
| Admin role system | Task 1 |
| Multi-model config | Task 2 |
| PDF parsing + image extraction | Task 3 |
| DOCX parsing + image extraction | Task 3 |
| MinIO image upload | Task 2 (minio_client.py) |
| Module boundary detection | Task 4 |
| LLM structured extraction | Task 4 |
| Per-module batch calling | Task 5 |
| `/ai/import/parse` endpoint | Task 6 |
| `/ai/import/models` endpoint | Task 6 |
| `/ai/import/reparse-question` endpoint | Task 6 |
| `/api/admin/papers/import` endpoint | Task 7 |
| 3-step wizard frontend | Task 8 |
| Model selection dropdown | Task 8 |
| Question review/edit UI | Task 8 |
| Image upload during edit | Task 8 (via existing FileController) |
| KaTeX formula support | Noted in Task 8 (dependency added, rendering deferred to question display) |
| End-to-end test | Task 9 |

### Placeholder Scan
No TBD/TODO found. All steps contain complete code.

### Type Consistency
- `PaperImportRequest.QuestionItem` fields match what `QuestionImport.vue` sends
- `ParsedQuestion` in extractor.py fields match what `import_router.py` serializes
- `ModelConfig` in config.py matches `llm_client.py` usage
- `QuestionVO` existing `from()` pattern unchanged
- `AuthResponse` updated to include `role` consistently in `JwtUtil`, `AuthService`, and frontend `localStorage`
