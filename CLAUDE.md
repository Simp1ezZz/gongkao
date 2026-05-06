# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

BALA公考 (BALA Gongkao) — a Chinese civil service exam online learning platform. Monorepo with three services orchestrated via Docker Compose.

## Common Commands

```bash
# Full stack
docker-compose up                    # Start all services (MySQL, Redis, MinIO, backend, ai-service, frontend)
docker-compose down -v               # Stop and wipe volumes (required to re-seed DB)

# Frontend (from frontend/)
npm run dev                          # VitePress dev server on localhost:5173
npm run build                        # Production build
npm run preview                      # Preview production build

# Backend (from backend/)
mvn spring-boot:run                  # Run Spring Boot locally
mvn clean package                    # Build JAR

# AI service (from ai-service/)
uvicorn app.main:app --host 0.0.0.0 --port 8000

# Utility
docker-compose logs -f <service>     # Follow logs for a service
docker-compose restart backend       # Restart a single service
```

No test or lint scripts are configured.

## Architecture

### Monorepo Structure

- **`frontend/`** — VitePress 2.0 (Vue 3 SPA). Pages in `pages/` as `.md` files embedding Vue components via frontmatter. Components in `.vitepress/theme/components/`.
- **`backend/`** — Spring Boot 3.5 (Java 21). Layered: Controller → Service → Mapper (MyBatis-Plus) → Entity. Unified `Result<T>` response wrapper.
- **`ai-service/`** — FastAPI (Python). BeautifulSoup HTML parsing, MinIO image handling. Shares JWT secret with backend.
- **`scripts/`** — SQL seeds and integration test scripts.
- **`docs/`** — Design specs and implementation plans.

### Service Ports

| Service | Port |
|---------|------|
| Frontend (VitePress dev) | 5173 |
| Backend (Spring Boot) | 8080 |
| AI Service (FastAPI) | 8000 |
| MinIO API | 9000 |
| MinIO Console | 9002 |
| MySQL | 3306 |
| Redis | 6379 |

### Frontend Routing

File-based routing via VitePress with `srcDir: 'pages'` and `cleanUrls: true`. Key routes:

- `/` → HomeQuickNav (homepage)
- `/题库/` → PaperList (mode="bank")
- `/practice/special/` → PaperList (mode="special")
- `/practice/online/` → OnlinePractice
- `/login/` → Login
- `/admin/import/` → PaperImport

API proxy: `/api/*` → backend, `/ai/*` → ai-service.

### Backend API Prefixes

- `/api/auth` — Authentication (login, register, verify, reset)
- `/api/papers` — Paper listing and details (public)
- `/api/papers/user-answers` — Answer submission
- `/api/sessions` — Practice session CRUD
- `/api/admin/import` — Paper import (admin only)
- `/api/files` — MinIO file upload/serve
- `/api/regions` — Region listing

### Key Frontend Files

- `.vitepress/config.ts` — VitePress config, nav, sidebar, proxy settings
- `.vitepress/theme/index.ts` — Theme entry, global component registration
- `.vitepress/theme/utils/api.js` — Axios instances with JWT injection and auto-refresh
- `.vitepress/theme/utils/latex.js` — KaTeX rendering utility for math content

### State Management

No Vuex/Pinia. State is component-local (Vue 3 `ref()`/`reactive()`) plus `localStorage` for JWT tokens, user info, and practice state.

### Database

MySQL with 22 tables. Schema at `backend/src/main/resources/db/schema.sql`. Seed data auto-loaded on first volume creation. To re-seed: `docker-compose down -v && docker-compose up`.

### Authentication

JWT stateless. Access token 2h, refresh token 7d. Redis blacklist for logged-out tokens. `GET /api/papers/**` public; `/api/admin/**` requires ADMIN role.

### Paper Import Flow

Admin uploads 3 HTML files (questions, answers, explanations) → AI service parses with BeautifulSoup → images downloaded and stored in MinIO → structured data returned → admin confirms import via backend API.

## Language and Conventions

- UI text and comments are in Chinese (中文)
- Code identifiers are in English
- Vue components use Composition API with `<script setup>`
- Backend uses Lombok annotations for boilerplate reduction
- CSS uses VitePress CSS variables (`--vp-c-*`)

## Environment Variables

Configured via `.env` file (see `.env.example`). Key variables: `MYSQL_ROOT_PASSWORD`, `MINIO_ROOT_USER`, `MINIO_ROOT_PASSWORD`, `JWT_SECRET`, `MAIL_USERNAME`, `MAIL_PASSWORD`.

## Design Documents

Full system design: `docs/superpowers/specs/2026-05-04-bala-gongkao-design.md`
Implementation plans: `docs/superpowers/plans/` (P0–P5 phases)
