# P0 — 项目骨架 + Docker + 建表 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 搭建项目基础骨架，所有服务可通过 Docker Compose 一键启动，数据库表结构就绪。

**Architecture:** 前后端分离 + AI 微服务。VitePress 前端、Spring Boot 主后端、FastAPI AI 服务各自独立容器，共享 MySQL/Redis/MinIO 基础设施。

**Tech Stack:** VitePress 2.0.0-alpha.15, Spring Boot 3.5.12, MyBatis-Plus 3.5.12, FastAPI 0.115.12, MySQL 9.2, Redis 7.4, MinIO latest, Docker Compose v2

**Spec:** `docs/superpowers/specs/2026-05-04-bala-gongkao-design.md`

---

## File Structure (本阶段涉及文件)

```
gongkao/
├── docker-compose.yml
├── frontend/
│   ├── package.json
│   ├── .vitepress/
│   │   └── config.ts
│   └── public/
├── backend/
│   ├── pom.xml
│   ├── Dockerfile
│   └── src/main/
│       ├── java/com/gongkao/
│       │   ├── GongkaoApplication.java
│       │   ├── config/
│       │   │   ├── SecurityConfig.java
│       │   │   ├── MyBatisPlusConfig.java
│       │   │   └── RedisConfig.java
│       │   └── common/
│       │       └── Result.java
│       └── resources/
│           ├── application.yml
│           └── db/
│               ├── schema.sql
│               └── data.sql
├── ai-service/
│   ├── Dockerfile
│   ├── requirements.txt
│   └── app/
│       ├── main.py
│       ├── core/
│       │   ├── config.py
│       │   └── auth.py
│       └── routers/
│           └── health.py
```

---

## Task 1: 初始化 Git 仓库 + 项目目录结构

**Files:**
- Create: `.gitignore`
- Create: `README.md`

- [ ] **Step 1: 初始化 Git 仓库并创建目录结构**

```bash
cd D:/CODE/gongkao
git init
mkdir -p frontend/.vitepress/theme/components
mkdir -p frontend/.vitepress/theme/styles
mkdir -p frontend/public
mkdir -p frontend/pages
mkdir -p backend/src/main/java/com/gongkao/config
mkdir -p backend/src/main/java/com/gongkao/common
mkdir -p backend/src/main/resources/db
mkdir -p backend/src/main/resources/mapper
mkdir -p ai-service/app/core
mkdir -p ai-service/app/routers
mkdir -p ai-service/app/services
mkdir -p scripts
```

- [ ] **Step 2: 创建 .gitignore**

```gitignore
# Java
backend/target/
*.class
*.jar
*.war
.idea/
*.iml

# Node
frontend/node_modules/
frontend/.vitepress/cache/
frontend/.vitepress/dist/

# Python
ai-service/__pycache__/
ai-service/.venv/
ai-service/venv/
*.pyc

# Environment
.env
.env.local

# IDE
.vscode/
.settings/

# OS
.DS_Store
Thumbs.db

# Data
data/
```

- [ ] **Step 3: 创建初始 README.md**

```markdown
# BALA 公考

公务员考试在线学习平台。

## 开发环境启动

```bash
docker-compose up
```

## 技术栈

详见 [设计文档](docs/superpowers/specs/2026-05-04-bala-gongkao-design.md)
```

- [ ] **Step 4: 提交**

```bash
git add .
git commit -m "chore: init project structure"
```

---

## Task 2: Docker Compose 配置

**Files:**
- Create: `docker-compose.yml`

- [ ] **Step 1: 创建 docker-compose.yml**

```yaml
services:
  mysql:
    image: mysql:9.2
    container_name: gongkao-mysql
    restart: unless-stopped
    environment:
      MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD:-gongkao123}
      MYSQL_DATABASE: gongkao
      MYSQL_CHARACTER_SET_SERVER: utf8mb4
      MYSQL_COLLATION_SERVER: utf8mb4_unicode_ci
    ports:
      - "3306:3306"
    volumes:
      - mysql_data:/var/lib/mysql
      - ./backend/src/main/resources/db/schema.sql:/docker-entrypoint-initdb.d/01-schema.sql
      - ./backend/src/main/resources/db/data.sql:/docker-entrypoint-initdb.d/02-data.sql
    # 注意: docker-entrypoint-initdb.d 中的脚本仅在 volume 首次创建时执行
    # 若需重建表结构，需先 docker-compose down -v 删除 volume 再重启
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]
      interval: 10s
      timeout: 5s
      retries: 5

  redis:
    image: redis:7.4-alpine
    container_name: gongkao-redis
    restart: unless-stopped
    ports:
      - "6379:6379"
    volumes:
      - redis_data:/data
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s
      timeout: 5s
      retries: 5

  minio:
    image: minio/minio:latest
    container_name: gongkao-minio
    restart: unless-stopped
    command: server /data --console-address ":9001"
    environment:
      MINIO_ROOT_USER: ${MINIO_ROOT_USER:-minioadmin}
      MINIO_ROOT_PASSWORD: ${MINIO_ROOT_PASSWORD:-minioadmin123}
    ports:
      - "9000:9000"
      - "9001:9001"
    volumes:
      - minio_data:/data

  backend:
    build:
      context: ./backend
      dockerfile: Dockerfile
    container_name: gongkao-backend
    restart: unless-stopped
    ports:
      - "8080:8080"
    environment:
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/gongkao?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai&characterEncoding=utf-8
      SPRING_DATASOURCE_USERNAME: root
      SPRING_DATASOURCE_PASSWORD: ${MYSQL_ROOT_PASSWORD:-gongkao123}
      SPRING_DATA_REDIS_HOST: redis
      SPRING_DATA_REDIS_PORT: 6379
      MINIO_ENDPOINT: http://minio:9000
      MINIO_ACCESS_KEY: ${MINIO_ROOT_USER:-minioadmin}
      MINIO_SECRET_KEY: ${MINIO_ROOT_PASSWORD:-minioadmin123}
      JWT_SECRET: ${JWT_SECRET:-myDefaultJwtSecretKeyForDevOnlyPleaseReplaceInProd2026}
      MAIL_HOST: smtp.qq.com
      MAIL_PORT: 587
      MAIL_USERNAME: ${MAIL_USERNAME:-}
      MAIL_PASSWORD: ${MAIL_PASSWORD:-}
    depends_on:
      mysql:
        condition: service_healthy
      redis:
        condition: service_healthy

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
    depends_on:
      - backend

  frontend:
    build:
      context: ./frontend
      dockerfile: Dockerfile
    container_name: gongkao-frontend
    restart: unless-stopped
    ports:
      - "5173:5173"
    environment:
      VITE_API_BASE_URL: http://localhost:8080/api
      VITE_AI_BASE_URL: http://localhost:8000/ai
    depends_on:
      - backend
      - ai-service

volumes:
  mysql_data:
  redis_data:
  minio_data:
```

- [ ] **Step 2: 创建 .env 模板文件**

```bash
cat > D:/CODE/gongkao/.env.example << 'EOF'
# MySQL
MYSQL_ROOT_PASSWORD=gongkao123

# MinIO
MINIO_ROOT_USER=minioadmin
MINIO_ROOT_PASSWORD=minioadmin123

# JWT (生产环境务必更换)
JWT_SECRET=myDefaultJwtSecretKeyForDevOnlyPleaseReplaceInProd2026

# QQ邮箱 SMTP (注册验证码)
MAIL_USERNAME=your_qq@qq.com
MAIL_PASSWORD=your_smtp_auth_code

# LLM配置
LLM_PROVIDER=openai-compatible
LLM_API_URL=https://your-proxy.com/v1/chat/completions
LLM_API_KEY=sk-xxx
LLM_MODEL=gpt-4o
EOF
```

- [ ] **Step 3: 复制 .env 并提交**

```bash
cp D:/CODE/gongkao/.env.example D:/CODE/gongkao/.env
git add docker-compose.yml .env.example .gitignore
git commit -m "chore: add docker-compose with all services"
```

---

## Task 3: Spring Boot 项目初始化

**Files:**
- Create: `backend/pom.xml`
- Create: `backend/Dockerfile`
- Create: `backend/src/main/java/com/gongkao/GongkaoApplication.java`
- Create: `backend/src/main/resources/application.yml`

- [ ] **Step 1: 创建 Maven pom.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.5.12</version>
        <relativePath/>
    </parent>

    <groupId>com.gongkao</groupId>
    <artifactId>gongkao-backend</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <name>gongkao-backend</name>
    <description>BALA 公考后端服务</description>

    <properties>
        <java.version>21</java.version>
        <mybatis-plus.version>3.5.12</mybatis-plus.version>
        <jjwt.version>0.12.6</jjwt.version>
        <minio.version>8.5.14</minio.version>
    </properties>

    <dependencies>
        <!-- Web -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- Security -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security</artifactId>
        </dependency>

        <!-- Validation -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>

        <!-- Mail -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-mail</artifactId>
        </dependency>

        <!-- MyBatis-Plus -->
        <dependency>
            <groupId>com.baomidou</groupId>
            <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
            <version>${mybatis-plus.version}</version>
        </dependency>

        <!-- MySQL -->
        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
            <scope>runtime</scope>
        </dependency>

        <!-- Redis -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-redis</artifactId>
        </dependency>

        <!-- JWT -->
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-api</artifactId>
            <version>${jjwt.version}</version>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-impl</artifactId>
            <version>${jjwt.version}</version>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-jackson</artifactId>
            <version>${jjwt.version}</version>
            <scope>runtime</scope>
        </dependency>

        <!-- MinIO -->
        <dependency>
            <groupId>io.minio</groupId>
            <artifactId>minio</artifactId>
            <version>${minio.version}</version>
        </dependency>

        <!-- Lombok -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- Test -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <excludes>
                        <exclude>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </exclude>
                    </excludes>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 2: 创建应用主类**

```java
// backend/src/main/java/com/gongkao/GongkaoApplication.java
package com.gongkao;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class GongkaoApplication {
    public static void main(String[] args) {
        SpringApplication.run(GongkaoApplication.class, args);
    }
}
```

- [ ] **Step 3: 创建 application.yml**

```yaml
server:
  port: 8080

spring:
  datasource:
    url: ${SPRING_DATASOURCE_URL:jdbc:mysql://localhost:3306/gongkao?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai&characterEncoding=utf-8}
    username: ${SPRING_DATASOURCE_USERNAME:root}
    password: ${SPRING_DATASOURCE_PASSWORD:gongkao123}
    driver-class-name: com.mysql.cj.jdbc.Driver

  data:
    redis:
      host: ${SPRING_DATA_REDIS_HOST:localhost}
      port: ${SPRING_DATA_REDIS_PORT:6379}

  mail:
    host: ${MAIL_HOST:smtp.qq.com}
    port: ${MAIL_PORT:587}
    username: ${MAIL_USERNAME:}
    password: ${MAIL_PASSWORD:}
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true

  servlet:
    multipart:
      max-file-size: 10MB
      max-request-size: 10MB

mybatis-plus:
  configuration:
    map-underscore-to-camel-case: true
  global-config:
    db-config:
      id-type: auto

jwt:
  secret: ${JWT_SECRET:myDefaultJwtSecretKeyForDevOnlyPleaseReplaceInProd2026}
  access-token-expiration: 7200000
  refresh-token-expiration: 604800000

minio:
  endpoint: ${MINIO_ENDPOINT:http://localhost:9000}
  access-key: ${MINIO_ACCESS_KEY:minioadmin}
  secret-key: ${MINIO_SECRET_KEY:minioadmin123}
  bucket: gongkao

logging:
  level:
    com.gongkao: DEBUG
```

- [ ] **Step 4: 创建统一响应类**

```java
// backend/src/main/java/com/gongkao/common/Result.java
package com.gongkao.common;

import lombok.Data;

@Data
public class Result<T> {
    private int code;
    private boolean success;
    private String message;
    private T data;

    public static <T> Result<T> ok(T data) {
        Result<T> r = new Result<>();
        r.setCode(200);
        r.setSuccess(true);
        r.setData(data);
        return r;
    }

    public static <T> Result<T> ok() {
        return ok(null);
    }

    public static <T> Result<T> fail(String message) {
        Result<T> r = new Result<>();
        r.setCode(400);
        r.setSuccess(false);
        r.setMessage(message);
        return r;
    }

    public static <T> Result<T> fail(int code, String message) {
        Result<T> r = new Result<>();
        r.setCode(code);
        r.setSuccess(false);
        r.setMessage(message);
        return r;
    }
}
```

- [ ] **Step 5: 创建 Security 配置（含 CORS）**

```java
// backend/src/main/java/com/gongkao/config/SecurityConfig.java
package com.gongkao.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("*"));
        config.setAllowedMethods(List.of("*"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public HttpSecurity httpSecurity(HttpSecurity http) throws Exception {
        return http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
    }
}
```

- [ ] **Step 6: 创建 MyBatis-Plus 配置**

```java
// backend/src/main/java/com/gongkao/config/MyBatisPlusConfig.java
package com.gongkao.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MyBatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }
}
```

- [ ] **Step 7: 创建 Redis 配置**

```java
// backend/src/main/java/com/gongkao/config/RedisConfig.java
package com.gongkao.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);

        Jackson2JsonRedisSerializer<Object> jsonSerializer = new Jackson2JsonRedisSerializer<>(Object.class);
        ObjectMapper om = new ObjectMapper();
        om.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        om.activateDefaultTyping(LaissezFaireSubTypeValidator.instance, ObjectMapper.DefaultTyping.NON_FINAL);
        jsonSerializer.setObjectMapper(om);

        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);
        template.afterPropertiesSet();

        return template;
    }

    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory factory) {
        return new StringRedisTemplate(factory);
    }
}
```

- [ ] **Step 8: 创建 Dockerfile**

```dockerfile
# backend/Dockerfile
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests -B

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

- [ ] **Step 9: 提交**

```bash
git add backend/
git commit -m "feat(backend): init Spring Boot project with configs"
```

---

## Task 4: FastAPI 项目初始化

**Files:**
- Create: `ai-service/requirements.txt`
- Create: `ai-service/Dockerfile`
- Create: `ai-service/app/main.py`
- Create: `ai-service/app/core/config.py`
- Create: `ai-service/app/core/auth.py`
- Create: `ai-service/app/routers/health.py`

- [ ] **Step 1: 创建 requirements.txt**

```
# ai-service/requirements.txt
fastapi==0.115.12
uvicorn[standard]==0.34.2
httpx==0.28.1
python-jose[cryptography]==3.3.0
pydantic==2.10.6
pydantic-settings==2.8.2
sse-starlette==2.2.1
```

- [ ] **Step 2: 创建配置模块**

```python
# ai-service/app/core/config.py
from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    jwt_secret: str = "myDefaultJwtSecretKeyForDevOnlyPleaseReplaceInProd2026"

    llm_provider: str = "openai-compatible"
    llm_api_url: str = ""
    llm_api_key: str = ""
    llm_model: str = "gpt-4o"
    llm_max_tokens: int = 4096

    class Config:
        env_prefix = ""
        env_file = ".env"


settings = Settings()
```

- [ ] **Step 3: 创建 JWT 认证模块**

```python
# ai-service/app/core/auth.py
from jose import jwt, JWTError
from fastapi import HTTPException, Security
from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials
from .config import settings

security = HTTPBearer()


async def get_current_user(credentials: HTTPAuthorizationCredentials = Security(security)) -> dict:
    token = credentials.credentials
    try:
        payload = jwt.decode(token, settings.jwt_secret, algorithms=["HS256"])
        user_id = payload.get("user_id")
        if user_id is None:
            raise HTTPException(status_code=401, detail="Invalid token")
        return payload
    except JWTError:
        raise HTTPException(status_code=401, detail="Invalid or expired token")
```

- [ ] **Step 4: 创建健康检查路由**

```python
# ai-service/app/routers/health.py
from fastapi import APIRouter

router = APIRouter()


@router.get("/health")
async def health_check():
    return {"status": "ok", "service": "gongkao-ai"}
```

- [ ] **Step 5: 创建 FastAPI 主入口**

```python
# ai-service/app/main.py
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.routers import health

app = FastAPI(title="BALA 公考 AI 服务")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
    allow_credentials=True,
)

app.include_router(health.router, prefix="/ai")
```

- [ ] **Step 6: 创建 `__init__.py` 文件**

```bash
touch D:/CODE/gongkao/ai-service/app/__init__.py
touch D:/CODE/gongkao/ai-service/app/core/__init__.py
touch D:/CODE/gongkao/ai-service/app/routers/__init__.py
touch D:/CODE/gongkao/ai-service/app/services/__init__.py
```

- [ ] **Step 7: 创建 Dockerfile**

```dockerfile
# ai-service/Dockerfile
FROM python:3.12-slim
WORKDIR /app
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt
COPY . .
EXPOSE 8000
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]
```

- [ ] **Step 8: 提交**

```bash
git add ai-service/
git commit -m "feat(ai): init FastAPI project with health check and JWT auth"
```

---

## Task 5: VitePress 前端项目初始化

**Files:**
- Create: `frontend/package.json`
- Create: `frontend/Dockerfile`
- Create: `frontend/.vitepress/config.ts`

- [ ] **Step 1: 初始化 VitePress 项目**

```bash
cd D:/CODE/gongkao/frontend
npm init -y
npm install vitepress@2.0.0-alpha.15
```

- [ ] **Step 2: 更新 package.json scripts**

在 `frontend/package.json` 中更新 scripts 部分：

```json
{
  "scripts": {
    "dev": "vitepress dev",
    "build": "vitepress build",
    "preview": "vitepress preview"
  }
}
```

- [ ] **Step 3: 创建最小 VitePress 配置**

```typescript
// frontend/.vitepress/config.ts
import { defineConfig } from 'vitepress'

export default defineConfig({
  title: 'BALA 公考',
  description: '上岸没烦恼',
  lang: 'zh-CN',
  cleanUrls: true,
  themeConfig: {
    nav: [
      { text: '主页', link: '/' },
      { text: '行测', link: '/题库/' },
      { text: '申论', link: '/essay-bank/' },
      { text: '专项练习', link: '/practice/special/' },
      { text: '个人中心', link: '/login/' },
    ],
    sidebar: {},
    darkModeSwitchLabel: '切换主题',
    returnToTopLabel: '返回顶部',
    sidebarMenuLabel: '菜单',
    outlineLabel: '目录',
    docFooter: {
      prev: '上一章',
      next: '下一章',
    },
  },
})
```

- [ ] **Step 4: 创建首页**

```markdown
<!-- frontend/pages/index.md -->
---
layout: home
hero:
  name: "BALA 公考"
  text: "上岸没烦恼"
  tagline: "不负每一次努力，只为助你稳稳上岸、一战成公。"
  actions:
    - theme: brand
      text: 个人中心
      link: /login/
    - theme: alt
      text: 进站必读
      link: /must-read/
    - theme: alt
      text: 上岸秘籍
      link: /secrets/
---
```

注意：VitePress 的 pages 目录需要在 config 中配置 `srcDir`。更新 config.ts：

```typescript
// 在 defineConfig 中添加
srcDir: 'pages',
```

- [ ] **Step 5: 创建 Dockerfile**

```dockerfile
# frontend/Dockerfile
FROM node:20-alpine
WORKDIR /app
COPY package*.json ./
RUN npm install
COPY . .
EXPOSE 5173
CMD ["npm", "run", "dev", "--", "--host", "0.0.0.0"]
```

- [ ] **Step 6: 提交**

```bash
cd D:/CODE/gongkao
git add frontend/
git commit -m "feat(frontend): init VitePress project with basic config"
```

---

## Task 6: 数据库 Schema + 种子数据

**Files:**
- Create: `backend/src/main/resources/db/schema.sql`
- Create: `backend/src/main/resources/db/data.sql`

- [ ] **Step 1: 创建完整建表 SQL**

将 spec 第四节的全部 22 张表 DDL 写入 `schema.sql`。文件内容直接复制 spec 中 4.1~4.5 节的全部 `CREATE TABLE` 语句，按依赖顺序排列。

注意：`user` 表包含 `deleted` 列用于逻辑删除，后续编写 `User` 实体类时需在 `deleted` 字段上加 `@TableLogic` 注解。其余表不加逻辑删除。

```
1. user
2. region
3. paper
4. material_group
5. question
6. practice_session
7. user_answer
8. analysis
9. essay_review
10. ai_question
11. question_ai_analysis
12. checkin
13. checkin_task
14. idiom
15. high_freq_word
16. high_freq_idiom
17. study_plan
18. secret
19. user_favorite
20. feedback
21. stats_track
22. site_config
```

每张表的 DDL 直接使用 spec 中的完整 SQL（含索引、外键）。

- [ ] **Step 2: 创建种子数据 SQL**

```sql
-- backend/src/main/resources/db/data.sql
-- 地区种子数据
INSERT INTO region (name, category, sort_order) VALUES
('国考', 'national', 0),
('北京', 'provincial', 1),
('上海', 'provincial', 2),
('广东', 'provincial', 3),
('浙江', 'provincial', 4),
('江苏', 'provincial', 5),
('山东', 'provincial', 6),
('河南', 'provincial', 7),
('四川', 'provincial', 8),
('湖北', 'provincial', 9),
('湖南', 'provincial', 10),
('福建', 'provincial', 11),
('安徽', 'provincial', 12),
('河北', 'provincial', 13),
('江西', 'provincial', 14),
('山西', 'provincial', 15),
('陕西', 'provincial', 16),
('重庆', 'provincial', 17),
('天津', 'provincial', 18),
('辽宁', 'provincial', 19),
('吉林', 'provincial', 20),
('黑龙江', 'provincial', 21),
('广西', 'provincial', 22),
('云南', 'provincial', 23),
('贵州', 'provincial', 24),
('甘肃', 'provincial', 25),
('内蒙古', 'provincial', 26),
('新疆', 'provincial', 27),
('宁夏', 'provincial', 28),
('青海', 'provincial', 29),
('海南', 'provincial', 30),
('西藏', 'provincial', 31);

-- 站点配置种子
INSERT INTO site_config (config_key, config_value) VALUES
('must_read', '# 进站必读\n\n欢迎来到 BALA 公考！\n\n## 使用指南\n\n1. 注册账号后即可使用全部功能\n2. 行测题库收录近6年真题\n3. 申论支持AI智能批改\n4. 每日打卡记录学习进度');
```

- [ ] **Step 3: 提交**

```bash
git add backend/src/main/resources/db/
git commit -m "feat(db): add schema for 22 tables and seed data"
```

---

## Task 7: 验证 Docker Compose 全栈启动

**Files:** 无新增

- [ ] **Step 1: 启动 MySQL + Redis + MinIO（基础设施先行）**

```bash
cd D:/CODE/gongkao
docker-compose up -d mysql redis minio
```

等待 MySQL 健康检查通过后验证：

```bash
docker-compose ps
```

Expected: mysql (healthy), redis (healthy), minio (running)

- [ ] **Step 2: 验证数据库表已创建**

```bash
docker exec gongkao-mysql mysql -uroot -pgongkao123 -e "SHOW TABLES" gongkao
```

Expected: 输出 22 张表名

```bash
docker exec gongkao-mysql mysql -uroot -pgongkao123 -e "SELECT COUNT(*) FROM region" gongkao
```

Expected: 32 条地区数据

- [ ] **Step 3: 验证 Redis 连接**

```bash
docker exec gongkao-redis redis-cli ping
```

Expected: `PONG`

- [ ] **Step 4: 验证 MinIO Console 可访问**

浏览器打开 http://localhost:9001，使用 minioadmin / minioadmin123 登录。Expected: MinIO Console 正常显示。

- [ ] **Step 5: 启动全部服务**

```bash
docker-compose up -d
```

```bash
docker-compose ps
```

Expected: 所有 6 个服务 running

- [ ] **Step 6: 验证各服务健康检查**

```bash
# 后端健康检查 (需要先有一个简单的健康接口, 或检查日志)
docker-compose logs backend --tail=20

# AI服务健康检查
curl http://localhost:8000/ai/health
```

Expected: `{"status":"ok","service":"gongkao-ai"}`

```bash
# 前端
curl -s http://localhost:5173 | head -5
```

Expected: 返回 HTML

- [ ] **Step 7: 最终提交**

```bash
git add -A
git commit -m "chore: verify docker-compose full stack startup"
```

---

## 验收 Checklist

- [ ] `docker-compose up -d` 所有 6 个服务正常启动
- [ ] MySQL 中 22 张表已创建
- [ ] `region` 表有 32 条种子数据
- [ ] `site_config` 表有 must-read 数据
- [ ] Redis 可连接 (`PING` → `PONG`)
- [ ] MinIO Console 可访问 (http://localhost:9001)
- [ ] FastAPI 健康检查通过 (http://localhost:8000/ai/health)
- [ ] VitePress 前端可访问 (http://localhost:5173)
- [ ] Spring Boot 启动无报错
