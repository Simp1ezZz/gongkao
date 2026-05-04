# P3 — 行测题库 + 在线练习 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现核心做题流程——试卷浏览筛选、在线做题（暂停/恢复）、批量提交答案、自动判分、错题归集，前端可完成从选试卷到看正确率的完整闭环。

**Architecture:** Spring Boot 提供 RESTful 试卷/题目/会话/答案 API，MyBatis-Plus 操作 region/paper/material_group/question/practice_session/user_answer 六张表。MinIO 存储题目图片。前端 PaperList 组件负责试卷筛选列表，OnlinePractice 组件负责做题交互（计时、导航、提交）。Python 脚本用于导入测试数据。

**Tech Stack:** Spring Boot 3.5.12, MyBatis-Plus 3.5.12, MinIO 8.5.14, Vue 3, VitePress 2.0.0-alpha.15, Python 3.12

**Spec:** `docs/superpowers/specs/2026-05-04-bala-gongkao-design.md`

---

## File Structure

```
backend/src/main/java/com/gongkao/
├── entity/
│   ├── Region.java                    # 地区实体
│   ├── Paper.java                     # 试卷实体
│   ├── MaterialGroup.java             # 材料组实体
│   ├── Question.java                  # 题目实体
│   ├── PracticeSession.java           # 做题会话实体
│   └── UserAnswer.java                # 用户答案实体
├── mapper/
│   ├── RegionMapper.java
│   ├── PaperMapper.java
│   ├── MaterialGroupMapper.java
│   ├── QuestionMapper.java
│   ├── PracticeSessionMapper.java
│   └── UserAnswerMapper.java
├── dto/
│   ├── PaperQueryRequest.java         # 试卷筛选参数
│   ├── PaperDetailVO.java             # 试卷详情(含题目列表)
│   ├── QuestionVO.java                # 题目视图(不含答案)
│   ├── QuestionWithAnswerVO.java      # 题目视图(含答案,提交后)
│   ├── MaterialGroupVO.java           # 材料组视图
│   ├── SessionCreateRequest.java      # 创建会话请求
│   ├── SessionUpdateRequest.java      # 更新会话请求
│   ├── BatchAnswerRequest.java        # 批量提交答案
│   ├── BatchAnswerResultVO.java       # 批量提交结果
│   ├── KnowledgeQueryRequest.java     # 知识点筛选请求
│   └── PageResult.java                # 通用分页结果
├── service/
│   ├── PaperService.java              # 试卷/题目查询
│   ├── SessionService.java            # 做题会话管理
│   ├── AnswerService.java             # 答案提交+判分
│   └── FileService.java               # MinIO文件上传/读取
├── controller/
│   ├── PaperController.java           # /api/papers/**
│   ├── SessionController.java         # /api/sessions/**
│   └── FileController.java            # /api/files/**
└── config/
    └── MinioConfig.java               # MinIO客户端配置

frontend/
├── .vitepress/theme/
│   ├── utils/
│   │   └── api.js                     # (已存在, 扩展试卷API)
│   └── components/
│       ├── PaperList.vue              # 试卷列表(行测题库+专项练习复用)
│       └── OnlinePractice.vue         # 在线做题页面
├── pages/
│   ├── 题库/
│   │   └── index.md                   # 行测题库入口
│   ├── practice/
│   │   ├── special/
│   │   │   └── index.md               # 专项练习入口
│   │   └── online/
│   │       └── index.md               # 在线练习入口(路由参数传paperId)

scripts/
├── import_test_papers.py              # 导入测试试卷+题目数据
└── seed_papers.sql                    # 试卷/题目种子数据SQL

backend/src/main/resources/
└── db/
    └── data.sql                       # (已存在, 追加region种子数据)
```

---

## Task 1: Region 实体 + Mapper + 种子数据

**Files:**
- Create: `backend/src/main/java/com/gongkao/entity/Region.java`
- Create: `backend/src/main/java/com/gongkao/mapper/RegionMapper.java`
- Modify: `backend/src/main/resources/db/data.sql`

- [ ] **Step 1: 创建 Region 实体**

```java
// backend/src/main/java/com/gongkao/entity/Region.java
package com.gongkao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("region")
public class Region {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String name;
    private String category; // national, provincial
    private Integer sortOrder;
}
```

- [ ] **Step 2: 创建 RegionMapper**

```java
// backend/src/main/java/com/gongkao/mapper/RegionMapper.java
package com.gongkao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gongkao.entity.Region;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface RegionMapper extends BaseMapper<Region> {
}
```

- [ ] **Step 3: 追加 region 种子数据到 data.sql**

在 `backend/src/main/resources/db/data.sql` 末尾追加：

```sql
-- region 种子数据
INSERT IGNORE INTO region (id, name, category, sort_order) VALUES
(1, '国考', 'national', 1),
(2, '北京', 'provincial', 2),
(3, '上海', 'provincial', 3),
(4, '广东', 'provincial', 4),
(5, '浙江', 'provincial', 5),
(6, '江苏', 'provincial', 6),
(7, '山东', 'provincial', 7),
(8, '四川', 'provincial', 8),
(9, '河南', 'provincial', 9),
(10, '湖北', 'provincial', 10);
```

- [ ] **Step 4: 验证编译**

Run: `cd backend && mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/gongkao/entity/Region.java \
        backend/src/main/java/com/gongkao/mapper/RegionMapper.java \
        backend/src/main/resources/db/data.sql
git commit -m "feat(P3): add Region entity, mapper and seed data"
```

---

## Task 2: Paper 实体 + Mapper

**Files:**
- Create: `backend/src/main/java/com/gongkao/entity/Paper.java`
- Create: `backend/src/main/java/com/gongkao/mapper/PaperMapper.java`

- [ ] **Step 1: 创建 Paper 实体**

```java
// backend/src/main/java/com/gongkao/entity/Paper.java
package com.gongkao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("paper")
public class Paper {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private String category; // 行测, 申论
    private Integer regionId;
    private Integer rating;
    private Integer questionCount;
    private Integer year;
    private LocalDateTime createdAt;

    @TableField(exist = false)
    private String regionName;
}
```

- [ ] **Step 2: 创建 PaperMapper**

```java
// backend/src/main/java/com/gongkao/mapper/PaperMapper.java
package com.gongkao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gongkao.entity.Paper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PaperMapper extends BaseMapper<Paper> {

    @Select("SELECT p.*, r.name as region_name FROM paper p " +
            "LEFT JOIN region r ON p.region_id = r.id " +
            "WHERE p.category = #{category} " +
            "AND (#{regionId} IS NULL OR p.region_id = #{regionId}) " +
            "ORDER BY p.year DESC, p.id DESC " +
            "LIMIT #{offset}, #{limit}")
    List<Paper> selectPageWithRegion(@Param("category") String category,
                                      @Param("regionId") Integer regionId,
                                      @Param("offset") long offset,
                                      @Param("limit") long limit);

    @Select("SELECT COUNT(*) FROM paper " +
            "WHERE category = #{category} " +
            "AND (#{regionId} IS NULL OR region_id = #{regionId})")
    long countByFilter(@Param("category") String category,
                       @Param("regionId") Integer regionId);
}
```

- [ ] **Step 3: 验证编译**

Run: `cd backend && mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add backend/src/main/java/com/gongkao/entity/Paper.java \
        backend/src/main/java/com/gongkao/mapper/PaperMapper.java
git commit -m "feat(P3): add Paper entity and mapper with region join query"
```

---

## Task 3: MaterialGroup + Question 实体 + Mapper

**Files:**
- Create: `backend/src/main/java/com/gongkao/entity/MaterialGroup.java`
- Create: `backend/src/main/java/com/gongkao/mapper/MaterialGroupMapper.java`
- Create: `backend/src/main/java/com/gongkao/entity/Question.java`
- Create: `backend/src/main/java/com/gongkao/mapper/QuestionMapper.java`

- [ ] **Step 1: 创建 MaterialGroup 实体**

```java
// backend/src/main/java/com/gongkao/entity/MaterialGroup.java
package com.gongkao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("material_group")
public class MaterialGroup {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long paperId;
    private String title;
    private String content;
    private String images; // JSON: MinIO key列表
    private Integer sortOrder;
}
```

- [ ] **Step 2: 创建 MaterialGroupMapper**

```java
// backend/src/main/java/com/gongkao/mapper/MaterialGroupMapper.java
package com.gongkao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gongkao.entity.MaterialGroup;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MaterialGroupMapper extends BaseMapper<MaterialGroup> {
}
```

- [ ] **Step 3: 创建 Question 实体**

```java
// backend/src/main/java/com/gongkao/entity/Question.java
package com.gongkao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("question")
public class Question {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long paperId;
    private Long materialGroupId;
    private Integer sortOrder;
    private String module;
    private String subModule;
    private String knowledgePoint;
    private String type; // single_choice, multi_choice, fill_blank, essay
    private String content;
    private String options; // JSON
    private String answer;
    private String explanation;
    private String images; // JSON
    private BigDecimal score;
    private LocalDateTime createdAt;
}
```

- [ ] **Step 4: 创建 QuestionMapper**

```java
// backend/src/main/java/com/gongkao/mapper/QuestionMapper.java
package com.gongkao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gongkao.entity.Question;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface QuestionMapper extends BaseMapper<Question> {

    @Select("<script>" +
            "SELECT * FROM question " +
            "WHERE paper_id = #{paperId} " +
            "ORDER BY sort_order ASC, id ASC" +
            "</script>")
    List<Question> selectByPaperId(@Param("paperId") Long paperId);

    @Select("<script>" +
            "SELECT * FROM question " +
            "WHERE module = #{module} " +
            "<if test='subModule != null'> AND sub_module = #{subModule} </if>" +
            "<if test='knowledgePoint != null'> AND knowledge_point = #{knowledgePoint} </if>" +
            "ORDER BY RAND() " +
            "LIMIT #{limit}" +
            "</script>")
    List<Question> selectByKnowledge(@Param("module") String module,
                                      @Param("subModule") String subModule,
                                      @Param("knowledgePoint") String knowledgePoint,
                                      @Param("limit") int limit);
}
```

- [ ] **Step 5: 验证编译**

Run: `cd backend && mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 6: Commit**

```bash
git add backend/src/main/java/com/gongkao/entity/MaterialGroup.java \
        backend/src/main/java/com/gongkao/mapper/MaterialGroupMapper.java \
        backend/src/main/java/com/gongkao/entity/Question.java \
        backend/src/main/java/com/gongkao/mapper/QuestionMapper.java
git commit -m "feat(P3): add MaterialGroup and Question entities with mappers"
```

---

## Task 4: PracticeSession + UserAnswer 实体 + Mapper

**Files:**
- Create: `backend/src/main/java/com/gongkao/entity/PracticeSession.java`
- Create: `backend/src/main/java/com/gongkao/mapper/PracticeSessionMapper.java`
- Create: `backend/src/main/java/com/gongkao/entity/UserAnswer.java`
- Create: `backend/src/main/java/com/gongkao/mapper/UserAnswerMapper.java`

- [ ] **Step 1: 创建 PracticeSession 实体**

```java
// backend/src/main/java/com/gongkao/entity/PracticeSession.java
package com.gongkao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("practice_session")
public class PracticeSession {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long paperId;
    private String status; // ongoing, paused, submitted, abandoned
    private Integer timeElapsed; // 已用秒数
    private Integer currentIndex;
    private String answers; // JSON
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

- [ ] **Step 2: 创建 PracticeSessionMapper**

```java
// backend/src/main/java/com/gongkao/mapper/PracticeSessionMapper.java
package com.gongkao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gongkao.entity.PracticeSession;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface PracticeSessionMapper extends BaseMapper<PracticeSession> {

    @Select("SELECT * FROM practice_session " +
            "WHERE user_id = #{userId} AND paper_id = #{paperId} " +
            "AND status IN ('ongoing', 'paused') " +
            "ORDER BY updated_at DESC LIMIT 1")
    PracticeSession selectActiveByUserPaper(@Param("userId") Long userId,
                                             @Param("paperId") Long paperId);

    @Select("SELECT * FROM practice_session " +
            "WHERE user_id = #{userId} " +
            "ORDER BY updated_at DESC")
    List<PracticeSession> selectByUserId(@Param("userId") Long userId);
}
```

- [ ] **Step 3: 创建 UserAnswer 实体**

```java
// backend/src/main/java/com/gongkao/entity/UserAnswer.java
package com.gongkao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_answer")
public class UserAnswer {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long paperId;
    private Long questionId;
    private Long sessionId;
    private String userAnswer;
    private Boolean isCorrect; // null=未批改
    private LocalDateTime createdAt;
}
```

- [ ] **Step 4: 创建 UserAnswerMapper**

```java
// backend/src/main/java/com/gongkao/mapper/UserAnswerMapper.java
package com.gongkao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gongkao.entity.UserAnswer;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface UserAnswerMapper extends BaseMapper<UserAnswer> {

    @Select("SELECT * FROM user_answer " +
            "WHERE user_id = #{userId} AND paper_id = #{paperId} " +
            "AND session_id = #{sessionId}")
    List<UserAnswer> selectBySession(@Param("userId") Long userId,
                                      @Param("paperId") Long paperId,
                                      @Param("sessionId") Long sessionId);

    @Select("SELECT * FROM user_answer " +
            "WHERE user_id = #{userId} AND paper_id = #{paperId}")
    List<UserAnswer> selectByUserPaper(@Param("userId") Long userId,
                                        @Param("paperId") Long paperId);
}
```

- [ ] **Step 5: 验证编译**

Run: `cd backend && mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 6: Commit**

```bash
git add backend/src/main/java/com/gongkao/entity/PracticeSession.java \
        backend/src/main/java/com/gongkao/mapper/PracticeSessionMapper.java \
        backend/src/main/java/com/gongkao/entity/UserAnswer.java \
        backend/src/main/java/com/gongkao/mapper/UserAnswerMapper.java
git commit -m "feat(P3): add PracticeSession and UserAnswer entities with mappers"
```

---

## Task 5: DTOs（请求/响应数据传输对象）

**Files:**
- Create: `backend/src/main/java/com/gongkao/dto/PageResult.java`
- Create: `backend/src/main/java/com/gongkao/dto/PaperQueryRequest.java`
- Create: `backend/src/main/java/com/gongkao/dto/MaterialGroupVO.java`
- Create: `backend/src/main/java/com/gongkao/dto/QuestionVO.java`
- Create: `backend/src/main/java/com/gongkao/dto/QuestionWithAnswerVO.java`
- Create: `backend/src/main/java/com/gongkao/dto/PaperDetailVO.java`
- Create: `backend/src/main/java/com/gongkao/dto/SessionCreateRequest.java`
- Create: `backend/src/main/java/com/gongkao/dto/SessionUpdateRequest.java`
- Create: `backend/src/main/java/com/gongkao/dto/BatchAnswerRequest.java`
- Create: `backend/src/main/java/com/gongkao/dto/BatchAnswerResultVO.java`

- [ ] **Step 1: 创建 PageResult 通用分页**

```java
// backend/src/main/java/com/gongkao/dto/PageResult.java
package com.gongkao.dto;

import lombok.Data;

import java.util.List;

@Data
public class PageResult<T> {
    private List<T> list;
    private long total;
    private int page;
    private int pageSize;

    public static <T> PageResult<T> of(List<T> list, long total, int page, int pageSize) {
        PageResult<T> result = new PageResult<>();
        result.setList(list);
        result.setTotal(total);
        result.setPage(page);
        result.setPageSize(pageSize);
        return result;
    }
}
```

- [ ] **Step 2: 创建 PaperQueryRequest**

```java
// backend/src/main/java/com/gongkao/dto/PaperQueryRequest.java
package com.gongkao.dto;

import lombok.Data;

@Data
public class PaperQueryRequest {
    private String category; // 行测 or 申论
    private Integer regionId;
    private Integer page = 1;
    private Integer pageSize = 20;
}
```

- [ ] **Step 3: 创建 MaterialGroupVO**

```java
// backend/src/main/java/com/gongkao/dto/MaterialGroupVO.java
package com.gongkao.dto;

import lombok.Data;

@Data
public class MaterialGroupVO {
    private Long id;
    private String title;
    private String content;
    private Integer sortOrder;
}
```

- [ ] **Step 4: 创建 QuestionVO（不含答案，做题时使用）**

```java
// backend/src/main/java/com/gongkao/dto/QuestionVO.java
package com.gongkao.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class QuestionVO {
    private Long id;
    private Long materialGroupId;
    private Integer sortOrder;
    private String module;
    private String subModule;
    private String knowledgePoint;
    private String type;
    private String content;
    private String options; // JSON
    private String images; // JSON
    private BigDecimal score;
}
```

- [ ] **Step 5: 创建 QuestionWithAnswerVO（提交后查看，含答案+解析）**

```java
// backend/src/main/java/com/gongkao/dto/QuestionWithAnswerVO.java
package com.gongkao.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class QuestionWithAnswerVO {
    private Long id;
    private Long materialGroupId;
    private Integer sortOrder;
    private String module;
    private String subModule;
    private String knowledgePoint;
    private String type;
    private String content;
    private String options; // JSON
    private String answer;
    private String explanation;
    private String images; // JSON
    private BigDecimal score;
    // 用户作答信息
    private String userAnswer;
    private Boolean isCorrect;
}
```

- [ ] **Step 6: 创建 PaperDetailVO**

```java
// backend/src/main/java/com/gongkao/dto/PaperDetailVO.java
package com.gongkao.dto;

import lombok.Data;

import java.util.List;

@Data
public class PaperDetailVO {
    private Long id;
    private String title;
    private String category;
    private String regionName;
    private Integer rating;
    private Integer questionCount;
    private Integer year;
    private List<MaterialGroupVO> materials;
    private List<QuestionVO> questions;
}
```

- [ ] **Step 7: 创建 SessionCreateRequest + SessionUpdateRequest**

```java
// backend/src/main/java/com/gongkao/dto/SessionCreateRequest.java
package com.gongkao.dto;

import lombok.Data;

@Data
public class SessionCreateRequest {
    private Long paperId;
}

// backend/src/main/java/com/gongkao/dto/SessionUpdateRequest.java
package com.gongkao.dto;

import lombok.Data;

@Data
public class SessionUpdateRequest {
    private String status; // paused, ongoing, submitted, abandoned
    private Integer timeElapsed;
    private Integer currentIndex;
    private String answers; // JSON: [{"question_id":1,"answer":"A"}]
}
```

- [ ] **Step 8: 创建 BatchAnswerRequest + BatchAnswerResultVO**

```java
// backend/src/main/java/com/gongkao/dto/BatchAnswerRequest.java
package com.gongkao.dto;

import lombok.Data;

import java.util.List;

@Data
public class BatchAnswerRequest {
    private Long sessionId;
    private List<AnswerItem> answers;

    @Data
    public static class AnswerItem {
        private Long questionId;
        private String answer;
    }
}

// backend/src/main/java/com/gongkao/dto/BatchAnswerResultVO.java
package com.gongkao.dto;

import lombok.Data;

import java.util.List;

@Data
public class BatchAnswerResultVO {
    private Long sessionId;
    private int totalQuestions;
    private int correctCount;
    private int wrongCount;
    private double accuracy;
    private List<QuestionWithAnswerVO> questions;
}
```

- [ ] **Step 9: 验证编译**

Run: `cd backend && mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 10: Commit**

```bash
git add backend/src/main/java/com/gongkao/dto/
git commit -m "feat(P3): add all DTOs for paper, question, session, answer"
```

---

## Task 6: PaperService — 试卷查询 + 题目查询

**Files:**
- Create: `backend/src/main/java/com/gongkao/service/PaperService.java`

- [ ] **Step 1: 创建 PaperService**

```java
// backend/src/main/java/com/gongkao/service/PaperService.java
package com.gongkao.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gongkao.dto.*;
import com.gongkao.entity.*;
import com.gongkao.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PaperService {

    private final PaperMapper paperMapper;
    private final MaterialGroupMapper materialGroupMapper;
    private final QuestionMapper questionMapper;
    private final RegionMapper regionMapper;

    /**
     * 分页查询试卷列表（含地区名称）
     */
    public PageResult<Paper> listPapers(PaperQueryRequest req) {
        String category = req.getCategory() != null ? req.getCategory() : "行测";
        long offset = (long) (req.getPage() - 1) * req.getPageSize();

        List<Paper> papers = paperMapper.selectPageWithRegion(
                category, req.getRegionId(), offset, req.getPageSize());
        long total = paperMapper.countByFilter(category, req.getRegionId());

        return PageResult.of(papers, total, req.getPage(), req.getPageSize());
    }

    /**
     * 获取试卷详情（含材料组 + 题目列表，题目不含答案）
     */
    public PaperDetailVO getPaperDetail(Long paperId) {
        Paper paper = paperMapper.selectById(paperId);
        if (paper == null) {
            throw new RuntimeException("试卷不存在: " + paperId);
        }

        // 填充地区名称
        if (paper.getRegionId() != null) {
            Region region = regionMapper.selectById(paper.getRegionId());
            if (region != null) {
                paper.setRegionName(region.getName());
            }
        }

        PaperDetailVO vo = new PaperDetailVO();
        vo.setId(paper.getId());
        vo.setTitle(paper.getTitle());
        vo.setCategory(paper.getCategory());
        vo.setRegionName(paper.getRegionName());
        vo.setRating(paper.getRating());
        vo.setQuestionCount(paper.getQuestionCount());
        vo.setYear(paper.getYear());

        // 材料组
        LambdaQueryWrapper<MaterialGroup> mgWrapper = new LambdaQueryWrapper<>();
        mgWrapper.eq(MaterialGroup::getPaperId, paperId)
                 .orderByAsc(MaterialGroup::getSortOrder);
        List<MaterialGroup> materials = materialGroupMapper.selectList(mgWrapper);
        vo.setMaterials(materials.stream().map(mg -> {
            MaterialGroupVO mgVo = new MaterialGroupVO();
            mgVo.setId(mg.getId());
            mgVo.setTitle(mg.getTitle());
            mgVo.setContent(mg.getContent());
            mgVo.setSortOrder(mg.getSortOrder());
            return mgVo;
        }).collect(Collectors.toList()));

        // 题目（不含答案）
        List<Question> questions = questionMapper.selectByPaperId(paperId);
        vo.setQuestions(questions.stream().map(q -> {
            QuestionVO qVo = new QuestionVO();
            qVo.setId(q.getId());
            qVo.setMaterialGroupId(q.getMaterialGroupId());
            qVo.setSortOrder(q.getSortOrder());
            qVo.setModule(q.getModule());
            qVo.setSubModule(q.getSubModule());
            qVo.setKnowledgePoint(q.getKnowledgePoint());
            qVo.setType(q.getType());
            qVo.setContent(q.getContent());
            qVo.setOptions(q.getOptions());
            qVo.setImages(q.getImages());
            qVo.setScore(q.getScore());
            return qVo;
        }).collect(Collectors.toList()));

        return vo;
    }

    /**
     * 获取试卷的材料组
     */
    public List<MaterialGroupVO> getMaterials(Long paperId) {
        LambdaQueryWrapper<MaterialGroup> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MaterialGroup::getPaperId, paperId)
               .orderByAsc(MaterialGroup::getSortOrder);
        List<MaterialGroup> groups = materialGroupMapper.selectList(wrapper);
        return groups.stream().map(mg -> {
            MaterialGroupVO vo = new MaterialGroupVO();
            vo.setId(mg.getId());
            vo.setTitle(mg.getTitle());
            vo.setContent(mg.getContent());
            vo.setSortOrder(mg.getSortOrder());
            return vo;
        }).collect(Collectors.toList());
    }

    /**
     * 按知识点查询题目（专项练习）
     */
    public List<QuestionVO> getQuestionsByKnowledge(String module, String subModule,
                                                     String knowledgePoint, int limit) {
        List<Question> questions = questionMapper.selectByKnowledge(
                module, subModule, knowledgePoint, limit);
        return questions.stream().map(q -> {
            QuestionVO vo = new QuestionVO();
            vo.setId(q.getId());
            vo.setMaterialGroupId(q.getMaterialGroupId());
            vo.setSortOrder(q.getSortOrder());
            vo.setModule(q.getModule());
            vo.setSubModule(q.getSubModule());
            vo.setKnowledgePoint(q.getKnowledgePoint());
            vo.setType(q.getType());
            vo.setContent(q.getContent());
            vo.setOptions(q.getOptions());
            vo.setImages(q.getImages());
            vo.setScore(q.getScore());
            return vo;
        }).collect(Collectors.toList());
    }
}
```

- [ ] **Step 2: 验证编译**

Run: `cd backend && mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/java/com/gongkao/service/PaperService.java
git commit -m "feat(P3): add PaperService with list, detail, materials, knowledge query"
```

---

## Task 7: PaperController — 试卷 REST API

**Files:**
- Create: `backend/src/main/java/com/gongkao/controller/PaperController.java`

- [ ] **Step 1: 创建 PaperController**

```java
// backend/src/main/java/com/gongkao/controller/PaperController.java
package com.gongkao.controller;

import com.gongkao.common.Result;
import com.gongkao.dto.*;
import com.gongkao.service.PaperService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/papers")
@RequiredArgsConstructor
public class PaperController {

    private final PaperService paperService;

    /**
     * GET /api/papers?category=&regionId=&page=&pageSize=
     * 试卷列表（分页 + 筛选）
     */
    @GetMapping
    public Result<PageResult<Paper>> listPapers(PaperQueryRequest req) {
        return Result.ok(paperService.listPapers(req));
    }

    /**
     * GET /api/papers/{id}
     * 试卷详情（含材料组 + 题目，题目不含答案）
     */
    @GetMapping("/{id}")
    public Result<PaperDetailVO> getPaperDetail(@PathVariable Long id) {
        return Result.ok(paperService.getPaperDetail(id));
    }

    /**
     * GET /api/papers/{id}/materials
     * 获取试卷的材料组列表
     */
    @GetMapping("/{id}/materials")
    public Result<List<MaterialGroupVO>> getMaterials(@PathVariable Long id) {
        return Result.ok(paperService.getMaterials(id));
    }

    /**
     * GET /api/papers/questions/by-knowledge?module=&sub_module=&knowledge_point=
     * 按知识点筛选题目（专项练习）
     */
    @GetMapping("/questions/by-knowledge")
    public Result<List<QuestionVO>> getQuestionsByKnowledge(
            @RequestParam String module,
            @RequestParam(required = false) String sub_module,
            @RequestParam(required = false) String knowledge_point,
            @RequestParam(defaultValue = "50") int limit) {
        return Result.ok(paperService.getQuestionsByKnowledge(
                module, sub_module, knowledge_point, limit));
    }

    /**
     * POST /api/papers/user-answers/batch
     * 批量提交答案（由 AnswerController 处理，此处仅声明路由归属）
     * → 见 Task 9: AnswerService + AnswerController
     */

    /**
     * GET /api/papers/{id}/my-answers
     * 获取我在某试卷的作答（由 AnswerController 处理）
     * → 见 Task 9
     */
}
```

- [ ] **Step 2: 验证编译**

Run: `cd backend && mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/java/com/gongkao/controller/PaperController.java
git commit -m "feat(P3): add PaperController with list, detail, materials, knowledge endpoints"
```

---

## Task 8: SessionService — 做题会话管理

**Files:**
- Create: `backend/src/main/java/com/gongkao/service/SessionService.java`

- [ ] **Step 1: 创建 SessionService**

```java
// backend/src/main/java/com/gongkao/service/SessionService.java
package com.gongkao.service;

import com.gongkao.dto.SessionCreateRequest;
import com.gongkao.dto.SessionUpdateRequest;
import com.gongkao.entity.PracticeSession;
import com.gongkao.mapper.PracticeSessionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SessionService {

    private final PracticeSessionMapper sessionMapper;

    /**
     * 创建做题会话（或恢复已有会话）
     */
    public PracticeSession createSession(Long userId, SessionCreateRequest req) {
        // 查找是否已有进行中/暂停的会话
        PracticeSession existing = sessionMapper.selectActiveByUserPaper(userId, req.getPaperId());
        if (existing != null) {
            return existing;
        }

        PracticeSession session = new PracticeSession();
        session.setUserId(userId);
        session.setPaperId(req.getPaperId());
        session.setStatus("ongoing");
        session.setTimeElapsed(0);
        session.setCurrentIndex(0);
        session.setAnswers("[]");
        sessionMapper.insert(session);
        return session;
    }

    /**
     * 获取会话详情
     */
    public PracticeSession getSession(Long sessionId, Long userId) {
        PracticeSession session = sessionMapper.selectById(sessionId);
        if (session == null || !session.getUserId().equals(userId)) {
            throw new RuntimeException("会话不存在或无权访问");
        }
        return session;
    }

    /**
     * 更新会话状态（暂停/恢复/保存进度）
     */
    public PracticeSession updateSession(Long sessionId, Long userId,
                                          SessionUpdateRequest req) {
        PracticeSession session = getSession(sessionId, userId);

        if (req.getStatus() != null) {
            session.setStatus(req.getStatus());
        }
        if (req.getTimeElapsed() != null) {
            session.setTimeElapsed(req.getTimeElapsed());
        }
        if (req.getCurrentIndex() != null) {
            session.setCurrentIndex(req.getCurrentIndex());
        }
        if (req.getAnswers() != null) {
            session.setAnswers(req.getAnswers());
        }

        sessionMapper.updateById(session);
        return session;
    }

    /**
     * 提交会话（标记为 submitted）
     */
    public PracticeSession submitSession(Long sessionId, Long userId,
                                          SessionUpdateRequest req) {
        PracticeSession session = getSession(sessionId, userId);
        session.setStatus("submitted");
        if (req.getTimeElapsed() != null) {
            session.setTimeElapsed(req.getTimeElapsed());
        }
        if (req.getAnswers() != null) {
            session.setAnswers(req.getAnswers());
        }
        sessionMapper.updateById(session);
        return session;
    }

    /**
     * 获取用户的所有会话
     */
    public List<PracticeSession> getUserSessions(Long userId) {
        return sessionMapper.selectByUserId(userId);
    }
}
```

- [ ] **Step 2: 验证编译**

Run: `cd backend && mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/java/com/gongkao/service/SessionService.java
git commit -m "feat(P3): add SessionService for practice session CRUD"
```

---

## Task 9: SessionController — 会话 REST API

**Files:**
- Create: `backend/src/main/java/com/gongkao/controller/SessionController.java`

- [ ] **Step 1: 创建 SessionController**

```java
// backend/src/main/java/com/gongkao/controller/SessionController.java
package com.gongkao.controller;

import com.gongkao.common.Result;
import com.gongkao.dto.SessionCreateRequest;
import com.gongkao.dto.SessionUpdateRequest;
import com.gongkao.entity.PracticeSession;
import com.gongkao.service.SessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
public class SessionController {

    private final SessionService sessionService;

    /**
     * POST /api/sessions
     * 创建做题会话
     */
    @PostMapping
    public Result<PracticeSession> createSession(
            @AuthenticationPrincipal Long userId,
            @RequestBody SessionCreateRequest req) {
        return Result.ok(sessionService.createSession(userId, req));
    }

    /**
     * GET /api/sessions
     * 获取我的所有会话
     */
    @GetMapping
    public Result<List<PracticeSession>> listSessions(
            @AuthenticationPrincipal Long userId) {
        return Result.ok(sessionService.getUserSessions(userId));
    }

    /**
     * GET /api/sessions/{id}
     * 获取会话详情
     */
    @GetMapping("/{id}")
    public Result<PracticeSession> getSession(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long id) {
        return Result.ok(sessionService.getSession(id, userId));
    }

    /**
     * PUT /api/sessions/{id}
     * 更新会话（暂停/恢复/保存进度）
     */
    @PutMapping("/{id}")
    public Result<PracticeSession> updateSession(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long id,
            @RequestBody SessionUpdateRequest req) {
        return Result.ok(sessionService.updateSession(id, userId, req));
    }

    /**
     * POST /api/sessions/{id}/submit
     * 提交会话
     */
    @PostMapping("/{id}/submit")
    public Result<PracticeSession> submitSession(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long id,
            @RequestBody SessionUpdateRequest req) {
        return Result.ok(sessionService.submitSession(id, userId, req));
    }
}
```

- [ ] **Step 2: 验证编译**

Run: `cd backend && mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/java/com/gongkao/controller/SessionController.java
git commit -m "feat(P3): add SessionController with create, update, submit endpoints"
```

---

## Task 10: AnswerService — 批量提交答案 + 自动判分

**Files:**
- Create: `backend/src/main/java/com/gongkao/service/AnswerService.java`

- [ ] **Step 1: 创建 AnswerService**

```java
// backend/src/main/java/com/gongkao/service/AnswerService.java
package com.gongkao.service;

import com.gongkao.dto.*;
import com.gongkao.entity.Question;
import com.gongkao.entity.UserAnswer;
import com.gongkao.mapper.QuestionMapper;
import com.gongkao.mapper.UserAnswerMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnswerService {

    private final UserAnswerMapper userAnswerMapper;
    private final QuestionMapper questionMapper;

    /**
     * 批量提交答案并自动判分
     */
    @Transactional
    public BatchAnswerResultVO batchSubmit(Long userId, BatchAnswerRequest req) {
        List<BatchAnswerRequest.AnswerItem> items = req.getAnswers();
        List<Long> questionIds = items.stream()
                .map(BatchAnswerRequest.AnswerItem::getQuestionId)
                .collect(Collectors.toList());

        // 批量查询题目
        List<Question> questions = questionMapper.selectBatchIds(questionIds);
        Map<Long, Question> questionMap = questions.stream()
                .collect(Collectors.toMap(Question::getId, q -> q));

        int correctCount = 0;
        int wrongCount = 0;
        List<QuestionWithAnswerVO> resultQuestions = new ArrayList<>();

        for (BatchAnswerRequest.AnswerItem item : items) {
            Question question = questionMap.get(item.getQuestionId());
            if (question == null) continue;

            // 判断正误（选择题：忽略大小写比较；填空/申论：暂标记为null，后续AI批改）
            Boolean isCorrect = null;
            String correctAnswer = question.getAnswer();
            String userAns = item.getAnswer();

            if ("single_choice".equals(question.getType())
                    || "multi_choice".equals(question.getType())) {
                if (userAns != null && correctAnswer != null) {
                    isCorrect = userAns.trim().equalsIgnoreCase(correctAnswer.trim());
                }
            }
            // fill_blank 和 essay 类型 isCorrect 保持 null，由 AI 批改

            if (isCorrect != null) {
                if (isCorrect) correctCount++;
                else wrongCount++;
            }

            // 保存答案
            UserAnswer ua = new UserAnswer();
            ua.setUserId(userId);
            ua.setPaperId(question.getPaperId());
            ua.setQuestionId(question.getId());
            ua.setSessionId(req.getSessionId());
            ua.setUserAnswer(userAns);
            ua.setIsCorrect(isCorrect);
            userAnswerMapper.insert(ua);

            // 构建返回结果
            QuestionWithAnswerVO vo = new QuestionWithAnswerVO();
            vo.setId(question.getId());
            vo.setMaterialGroupId(question.getMaterialGroupId());
            vo.setSortOrder(question.getSortOrder());
            vo.setModule(question.getModule());
            vo.setSubModule(question.getSubModule());
            vo.setKnowledgePoint(question.getKnowledgePoint());
            vo.setType(question.getType());
            vo.setContent(question.getContent());
            vo.setOptions(question.getOptions());
            vo.setAnswer(question.getAnswer());
            vo.setExplanation(question.getExplanation());
            vo.setImages(question.getImages());
            vo.setScore(question.getScore());
            vo.setUserAnswer(userAns);
            vo.setIsCorrect(isCorrect);
            resultQuestions.add(vo);
        }

        int total = items.size();
        int graded = correctCount + wrongCount;
        double accuracy = graded > 0 ? (double) correctCount / graded * 100 : 0;

        BatchAnswerResultVO result = new BatchAnswerResultVO();
        result.setSessionId(req.getSessionId());
        result.setTotalQuestions(total);
        result.setCorrectCount(correctCount);
        result.setWrongCount(wrongCount);
        result.setAccuracy(Math.round(accuracy * 100.0) / 100.0);
        result.setQuestions(resultQuestions);
        return result;
    }

    /**
     * 获取某试卷我的作答
     */
    public List<QuestionWithAnswerVO> getMyAnswers(Long userId, Long paperId) {
        List<UserAnswer> answers = userAnswerMapper.selectByUserPaper(userId, paperId);
        if (answers.isEmpty()) return List.of();

        List<Long> questionIds = answers.stream()
                .map(UserAnswer::getQuestionId)
                .collect(Collectors.toList());
        List<Question> questions = questionMapper.selectBatchIds(questionIds);
        Map<Long, Question> questionMap = questions.stream()
                .collect(Collectors.toMap(Question::getId, q -> q));

        return answers.stream().map(ua -> {
            Question q = questionMap.get(ua.getQuestionId());
            if (q == null) return null;

            QuestionWithAnswerVO vo = new QuestionWithAnswerVO();
            vo.setId(q.getId());
            vo.setMaterialGroupId(q.getMaterialGroupId());
            vo.setSortOrder(q.getSortOrder());
            vo.setModule(q.getModule());
            vo.setSubModule(q.getSubModule());
            vo.setKnowledgePoint(q.getKnowledgePoint());
            vo.setType(q.getType());
            vo.setContent(q.getContent());
            vo.setOptions(q.getOptions());
            vo.setAnswer(q.getAnswer());
            vo.setExplanation(q.getExplanation());
            vo.setImages(q.getImages());
            vo.setScore(q.getScore());
            vo.setUserAnswer(ua.getUserAnswer());
            vo.setIsCorrect(ua.getIsCorrect());
            return vo;
        }).filter(v -> v != null).collect(Collectors.toList());
    }
}
```

- [ ] **Step 2: 验证编译**

Run: `cd backend && mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/java/com/gongkao/service/AnswerService.java
git commit -m "feat(P3): add AnswerService with batch submit and auto-grading"
```

---

## Task 11: AnswerController — 答案提交 REST API

**Files:**
- Create: `backend/src/main/java/com/gongkao/controller/AnswerController.java`

- [ ] **Step 1: 创建 AnswerController**

```java
// backend/src/main/java/com/gongkao/controller/AnswerController.java
package com.gongkao.controller;

import com.gongkao.common.Result;
import com.gongkao.dto.BatchAnswerRequest;
import com.gongkao.dto.BatchAnswerResultVO;
import com.gongkao.dto.QuestionWithAnswerVO;
import com.gongkao.service.AnswerService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/papers")
@RequiredArgsConstructor
public class AnswerController {

    private final AnswerService answerService;

    /**
     * POST /api/papers/user-answers/batch
     * 批量提交答案 + 自动判分
     */
    @PostMapping("/user-answers/batch")
    public Result<BatchAnswerResultVO> batchSubmit(
            @AuthenticationPrincipal Long userId,
            @RequestBody BatchAnswerRequest req) {
        return Result.ok(answerService.batchSubmit(userId, req));
    }

    /**
     * GET /api/papers/{id}/my-answers
     * 获取我在某试卷的作答记录
     */
    @GetMapping("/{id}/my-answers")
    public Result<List<QuestionWithAnswerVO>> getMyAnswers(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long id) {
        return Result.ok(answerService.getMyAnswers(userId, id));
    }
}
```

- [ ] **Step 2: 验证编译**

Run: `cd backend && mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/java/com/gongkao/controller/AnswerController.java
git commit -m "feat(P3): add AnswerController for batch submit and my-answers"
```

---

## Task 12: MinIO 配置 + FileService

**Files:**
- Create: `backend/src/main/java/com/gongkao/config/MinioConfig.java`
- Create: `backend/src/main/java/com/gongkao/service/FileService.java`
- Modify: `backend/src/main/resources/application.yml` — 追加 MinIO 配置

- [ ] **Step 1: 追加 MinIO 配置到 application.yml**

在 `backend/src/main/resources/application.yml` 末尾追加：

```yaml
# MinIO 配置
minio:
  endpoint: http://localhost:9000
  access-key: minioadmin
  secret-key: minioadmin
  bucket: gongkao
```

- [ ] **Step 2: 创建 MinioConfig**

```java
// backend/src/main/java/com/gongkao/config/MinioConfig.java
package com.gongkao.config;

import io.minio.MinioClient;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "minio")
public class MinioConfig {

    private String endpoint;
    private String accessKey;
    private String secretKey;
    private String bucket;

    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
    }
}
```

- [ ] **Step 3: 创建 FileService**

```java
// backend/src/main/java/com/gongkao/service/FileService.java
package com.gongkao.service;

import com.gongkao.config.MinioConfig;
import io.minio.*;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileService {

    private final MinioClient minioClient;
    private final MinioConfig minioConfig;

    @PostConstruct
    public void initBucket() {
        try {
            boolean exists = minioClient.bucketExists(
                    BucketExistsArgs.builder()
                            .bucket(minioConfig.getBucket())
                            .build());
            if (!exists) {
                minioClient.makeBucket(
                        MakeBucketArgs.builder()
                                .bucket(minioConfig.getBucket())
                                .build());
                log.info("Created MinIO bucket: {}", minioConfig.getBucket());
            }
        } catch (Exception e) {
            log.error("Failed to init MinIO bucket", e);
        }
    }

    /**
     * 上传文件，返回 object key
     */
    public String upload(String objectKey, InputStream stream, String contentType, long size) {
        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioConfig.getBucket())
                            .object(objectKey)
                            .stream(stream, size, -1)
                            .contentType(contentType)
                            .build());
            return objectKey;
        } catch (Exception e) {
            throw new RuntimeException("文件上传失败", e);
        }
    }

    /**
     * 获取文件访问 URL（预签名，有效期 1 小时）
     */
    public String getPresignedUrl(String objectKey) {
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .bucket(minioConfig.getBucket())
                            .object(objectKey)
                            .method(Method.GET)
                            .expiry(1, TimeUnit.HOURS)
                            .build());
        } catch (Exception e) {
            throw new RuntimeException("获取文件URL失败", e);
        }
    }

    /**
     * 删除文件
     */
    public void delete(String objectKey) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(minioConfig.getBucket())
                            .object(objectKey)
                            .build());
        } catch (Exception e) {
            throw new RuntimeException("文件删除失败", e);
        }
    }
}
```

- [ ] **Step 4: 验证编译**

Run: `cd backend && mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/gongkao/config/MinioConfig.java \
        backend/src/main/java/com/gongkao/service/FileService.java \
        backend/src/main/resources/application.yml
git commit -m "feat(P3): add MinIO config and FileService for image upload/storage"
```

---

## Task 13: FileController — 文件上传 REST API

**Files:**
- Create: `backend/src/main/java/com/gongkao/controller/FileController.java`

- [ ] **Step 1: 创建 FileController**

```java
// backend/src/main/java/com/gongkao/controller/FileController.java
package com.gongkao.controller;

import com.gongkao.common.Result;
import com.gongkao.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    /**
     * POST /api/files/upload
     * 上传文件到 MinIO，返回 objectKey 和访问 URL
     */
    @PostMapping("/upload")
    public Result<Map<String, String>> upload(@RequestParam("file") MultipartFile file) {
        try {
            String originalName = file.getOriginalFilename();
            String ext = "";
            if (originalName != null && originalName.contains(".")) {
                ext = originalName.substring(originalName.lastIndexOf("."));
            }
            String objectKey = "uploads/" + UUID.randomUUID() + ext;

            String key = fileService.upload(
                    objectKey,
                    file.getInputStream(),
                    file.getContentType(),
                    file.getSize());

            String url = fileService.getPresignedUrl(key);

            Map<String, String> result = new HashMap<>();
            result.put("key", key);
            result.put("url", url);
            return Result.ok(result);
        } catch (Exception e) {
            return Result.fail("文件上传失败: " + e.getMessage());
        }
    }
}
```

- [ ] **Step 2: 验证编译**

Run: `cd backend && mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/java/com/gongkao/controller/FileController.java
git commit -m "feat(P3): add FileController for MinIO file upload"
```

---

## Task 14: 前端 — PaperList.vue 试卷列表组件

**Files:**
- Modify: `frontend/.vitepress/theme/utils/api.js` — 追加试卷相关 API
- Create: `frontend/.vitepress/theme/components/PaperList.vue`
- Modify: `frontend/pages/题库/index.md` — 挂载 PaperList
- Modify: `frontend/pages/practice/special/index.md` — 挂载 PaperList（mode=special）

- [ ] **Step 1: 扩展 api.js 追加试卷相关方法**

在 `frontend/.vitepress/theme/utils/api.js` 末尾追加：

```javascript
// 试卷相关 API
export const paperApi = {
  list(params) {
    return get('/papers', params)
  },
  getDetail(id) {
    return get(`/papers/${id}`)
  },
  getMaterials(paperId) {
    return get(`/papers/${paperId}/materials`)
  },
  getQuestionsByKnowledge(params) {
    return get('/papers/questions/by-knowledge', params)
  },
  batchSubmit(data) {
    return post('/papers/user-answers/batch', data)
  },
  getMyAnswers(paperId) {
    return get(`/papers/${paperId}/my-answers`)
  },
}

// 做题会话 API
export const sessionApi = {
  create(data) {
    return post('/sessions', data)
  },
  list() {
    return get('/sessions')
  },
  get(id) {
    return get(`/sessions/${id}`)
  },
  update(id, data) {
    return put(`/sessions/${id}`, data)
  },
  submit(id, data) {
    return post(`/sessions/${id}/submit`, data)
  },
}
```

- [ ] **Step 2: 创建 PaperList.vue**

```vue
<!-- frontend/.vitepress/theme/components/PaperList.vue -->
<template>
  <div class="paper-list">
    <!-- 筛选栏 -->
    <div class="filter-bar">
      <div class="filter-group">
        <label>地区：</label>
        <select v-model="filters.regionId" @change="loadPapers">
          <option :value="null">全部</option>
          <option v-for="r in regions" :key="r.id" :value="r.id">
            {{ r.name }}
          </option>
        </select>
      </div>
      <div class="filter-group" v-if="mode === 'special'">
        <label>模块：</label>
        <select v-model="filters.module" @change="loadPapers">
          <option value="">全部</option>
          <option value="言语理解">言语理解</option>
          <option value="数量关系">数量关系</option>
          <option value="判断推理">判断推理</option>
          <option value="资料分析">资料分析</option>
          <option value="常识判断">常识判断</option>
        </select>
      </div>
    </div>

    <!-- 加载中 -->
    <div v-if="loading" class="loading">加载中...</div>

    <!-- 试卷列表 -->
    <div v-else class="papers">
      <div v-for="paper in papers" :key="paper.id" class="paper-card"
           @click="startPractice(paper)">
        <div class="paper-title">{{ paper.title }}</div>
        <div class="paper-meta">
          <span v-if="paper.regionName" class="region">{{ paper.regionName }}</span>
          <span class="year">{{ paper.year }}年</span>
          <span class="count">{{ paper.questionCount }}题</span>
          <span class="rating">{{ '★'.repeat(paper.rating || 0) }}</span>
        </div>
      </div>
      <Empty v-if="papers.length === 0" text="暂无试卷" />
    </div>

    <!-- 分页 -->
    <div class="pagination" v-if="total > pageSize">
      <button :disabled="page <= 1" @click="page--; loadPapers()">上一页</button>
      <span>{{ page }} / {{ totalPages }}</span>
      <button :disabled="page >= totalPages" @click="page++; loadPapers()">下一页</button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { paperApi, sessionApi } from '../utils/api.js'
import Empty from './Empty.vue'

const props = defineProps({
  mode: { type: String, default: 'bank' }, // bank=题库, special=专项练习
  category: { type: String, default: '行测' }
})

const regions = ref([])
const papers = ref([])
const loading = ref(false)
const page = ref(1)
const pageSize = 20
const total = ref(0)
const totalPages = computed(() => Math.ceil(total.value / pageSize))

const filters = ref({
  regionId: null,
  module: ''
})

async function loadPapers() {
  loading.value = true
  try {
    const params = {
      category: props.category,
      regionId: filters.value.regionId,
      page: page.value,
      pageSize
    }
    const res = await paperApi.list(params)
    if (res.code === 0) {
      papers.value = res.data.list
      total.value = res.data.total
    }
  } catch (e) {
    console.error('加载试卷失败', e)
  } finally {
    loading.value = false
  }
}

async function loadRegions() {
  try {
    // 直接查 region 表，复用 paperApi（后续可独立接口）
    const res = await paperApi.list({ category: props.category, pageSize: 1 })
    // 暂时硬编码地区列表，待后端提供 /api/regions 接口后替换
    regions.value = [
      { id: 1, name: '国考' },
      { id: 2, name: '北京' },
      { id: 3, name: '上海' },
      { id: 4, name: '广东' },
      { id: 5, name: '浙江' },
      { id: 6, name: '江苏' },
      { id: 7, name: '山东' },
      { id: 8, name: '四川' },
      { id: 9, name: '河南' },
      { id: 10, name: '湖北' }
    ]
  } catch (e) {
    console.error('加载地区失败', e)
  }
}

function startPractice(paper) {
  // 跳转到在线练习页，URL 携带 paperId
  window.location.href = `/practice/online/?paperId=${paper.id}`
}

onMounted(() => {
  loadRegions()
  loadPapers()
})
</script>

<style scoped>
.paper-list { max-width: 960px; margin: 0 auto; padding: 20px; }
.filter-bar {
  display: flex; gap: 16px; margin-bottom: 20px;
  padding: 12px 16px; background: var(--vp-c-bg-soft);
  border-radius: 8px;
}
.filter-group { display: flex; align-items: center; gap: 6px; }
.filter-group label { font-size: 14px; color: var(--vp-c-text-2); }
.filter-group select {
  padding: 4px 8px; border: 1px solid var(--vp-c-divider);
  border-radius: 4px; background: var(--vp-c-bg);
  color: var(--vp-c-text-1); font-size: 14px;
}
.papers { display: flex; flex-direction: column; gap: 12px; }
.paper-card {
  padding: 16px; background: var(--vp-c-bg-soft);
  border-radius: 8px; cursor: pointer;
  transition: box-shadow 0.2s;
}
.paper-card:hover { box-shadow: 0 2px 12px rgba(0,0,0,0.1); }
.paper-title { font-size: 16px; font-weight: 600; margin-bottom: 8px; }
.paper-meta { display: flex; gap: 12px; font-size: 13px; color: var(--vp-c-text-2); }
.rating { color: #f5a623; }
.pagination {
  display: flex; align-items: center; justify-content: center;
  gap: 16px; margin-top: 20px;
}
.pagination button {
  padding: 6px 16px; border: 1px solid var(--vp-c-divider);
  border-radius: 4px; background: var(--vp-c-bg); cursor: pointer;
}
.pagination button:disabled { opacity: 0.5; cursor: not-allowed; }
.loading { text-align: center; padding: 40px; color: var(--vp-c-text-2); }
</style>
```

- [ ] **Step 3: 更新 题库/index.md**

```markdown
---
layout: page
title: 行测题库
---

<PaperList category="行测" mode="bank" />
```

- [ ] **Step 4: 更新 practice/special/index.md**

```markdown
---
layout: page
title: 专项练习
---

<PaperList category="行测" mode="special" />
```

- [ ] **Step 5: 启动前端验证页面渲染**

Run: `cd frontend && npm run dev`
Expected: 访问 `/题库/` 和 `/practice/special/` 页面正常渲染筛选栏和空列表

- [ ] **Step 6: Commit**

```bash
git add frontend/.vitepress/theme/utils/api.js \
        frontend/.vitepress/theme/components/PaperList.vue \
        frontend/pages/题库/index.md \
        frontend/pages/practice/special/index.md
git commit -m "feat(P3): add PaperList.vue component for paper browsing and filtering"
```

---

## Task 15: 前端 — OnlinePractice.vue 在线做题组件

**Files:**
- Create: `frontend/.vitepress/theme/components/OnlinePractice.vue`
- Modify: `frontend/pages/practice/online/index.md` — 挂载 OnlinePractice

- [ ] **Step 1: 创建 OnlinePractice.vue**

```vue
<!-- frontend/.vitepress/theme/components/OnlinePractice.vue -->
<template>
  <div class="online-practice" v-if="loaded">
    <!-- 顶部信息栏 -->
    <div class="top-bar">
      <div class="paper-info">
        <h2>{{ paperDetail.title }}</h2>
        <span class="meta">
          {{ paperDetail.questionCount }}题 |
          {{ paperDetail.year }}年 |
          {{ paperDetail.regionName || '通用' }}
        </span>
      </div>
      <div class="timer">
        <span class="time">{{ formatTime(timeElapsed) }}</span>
        <button v-if="session?.status === 'ongoing'" class="btn-pause"
                @click="togglePause">暂停</button>
        <button v-if="session?.status === 'paused'" class="btn-resume"
                @click="togglePause">继续</button>
      </div>
    </div>

    <!-- 材料区域（如果当前题有材料组） -->
    <div v-if="currentMaterial" class="material-panel">
      <h3>{{ currentMaterial.title }}</h3>
      <div class="material-content" v-html="currentMaterial.content"></div>
    </div>

    <!-- 题目区域 -->
    <div class="question-panel" v-if="currentQuestion">
      <div class="question-header">
        <span class="question-index">{{ currentIndex + 1 }} / {{ questions.length }}</span>
        <span class="question-module">{{ currentQuestion.module }}</span>
        <span class="question-type">{{ typeLabel(currentQuestion.type) }}</span>
      </div>

      <div class="question-content" v-html="currentQuestion.content"></div>

      <!-- 选项（选择题） -->
      <div v-if="hasOptions" class="options">
        <div v-for="opt in parsedOptions" :key="opt.label"
             class="option-item"
             :class="{ selected: answers[currentQuestion.id] === opt.label }"
             @click="selectAnswer(opt.label)">
          <span class="option-label">{{ opt.label }}.</span>
          <span class="option-text">{{ opt.text }}</span>
        </div>
      </div>

      <!-- 填空题 -->
      <div v-if="currentQuestion.type === 'fill_blank'" class="fill-blank">
        <input v-model="answers[currentQuestion.id]" placeholder="请输入答案" />
      </div>
    </div>

    <!-- 底部导航 -->
    <div class="bottom-bar">
      <button :disabled="currentIndex <= 0" @click="prevQuestion">上一题</button>
      <div class="progress">
        <div class="progress-bar" :style="{ width: progressPercent + '%' }"></div>
      </div>
      <span class="answered-count">{{ answeredCount }} / {{ questions.length }} 已答</span>
      <button v-if="currentIndex < questions.length - 1"
              @click="nextQuestion">下一题</button>
      <button v-else class="btn-submit" @click="confirmSubmit">提交试卷</button>
    </div>

    <!-- 确认提交弹窗 -->
    <Modal v-if="showSubmitModal" @close="showSubmitModal = false">
      <div class="submit-confirm">
        <h3>确认提交？</h3>
        <p>已答 {{ answeredCount }} / {{ questions.length }} 题</p>
        <p v-if="answeredCount < questions.length" class="warn">
          还有 {{ questions.length - answeredCount }} 题未作答
        </p>
        <div class="submit-actions">
          <button @click="showSubmitModal = false">取消</button>
          <button class="btn-primary" @click="submitExam">确认提交</button>
        </div>
      </div>
    </Modal>

    <!-- 提交结果 -->
    <div v-if="result" class="result-panel">
      <h3>练习结果</h3>
      <div class="result-stats">
        <div class="stat-item">
          <span class="stat-value">{{ result.correctCount }}</span>
          <span class="stat-label">正确</span>
        </div>
        <div class="stat-item wrong">
          <span class="stat-value">{{ result.wrongCount }}</span>
          <span class="stat-label">错误</span>
        </div>
        <div class="stat-item">
          <span class="stat-value">{{ result.accuracy }}%</span>
          <span class="stat-label">正确率</span>
        </div>
      </div>

      <!-- 逐题解析 -->
      <div class="result-questions">
        <div v-for="(q, idx) in result.questions" :key="q.id"
             class="result-question"
             :class="{ correct: q.isCorrect, wrong: q.isCorrect === false }">
          <div class="rq-header">
            <span>第{{ idx + 1 }}题</span>
            <span v-if="q.isCorrect === true" class="badge-correct">正确</span>
            <span v-else-if="q.isCorrect === false" class="badge-wrong">错误</span>
            <span v-else class="badge-pending">待批改</span>
          </div>
          <div class="rq-content" v-html="q.content"></div>
          <div class="rq-answer">
            <p>你的答案：<strong>{{ q.userAnswer || '未作答' }}</strong></p>
            <p>正确答案：<strong>{{ q.answer }}</strong></p>
          </div>
          <div v-if="q.explanation" class="rq-explanation">
            <h4>解析</h4>
            <div v-html="q.explanation"></div>
          </div>
        </div>
      </div>
    </div>
  </div>
  <div v-else class="loading">加载中...</div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { paperApi, sessionApi } from '../utils/api.js'
import Modal from './Modal.vue'

const loaded = ref(false)
const paperDetail = ref({})
const questions = ref([])
const materials = ref([])
const session = ref(null)
const currentIndex = ref(0)
const answers = ref({})
const timeElapsed = ref(0)
const result = ref(null)
const showSubmitModal = ref(false)

let timer = null

const currentQuestion = computed(() => questions.value[currentIndex.value])

const currentMaterial = computed(() => {
  const q = currentQuestion.value
  if (!q || !q.materialGroupId) return null
  return materials.value.find(m => m.id === q.materialGroupId)
})

const parsedOptions = computed(() => {
  const q = currentQuestion.value
  if (!q || !q.options) return []
  try {
    return JSON.parse(q.options)
  } catch { return [] }
})

const hasOptions = computed(() => {
  const q = currentQuestion.value
  return q && (q.type === 'single_choice' || q.type === 'multi_choice')
})

const answeredCount = computed(() => Object.keys(answers.value).length)

const progressPercent = computed(() =>
  questions.value.length > 0
    ? Math.round((answeredCount.value / questions.value.length) * 100)
    : 0
)

function typeLabel(type) {
  const map = {
    single_choice: '单选题',
    multi_choice: '多选题',
    fill_blank: '填空题',
    essay: '主观题'
  }
  return map[type] || type
}

function formatTime(seconds) {
  const h = Math.floor(seconds / 3600)
  const m = Math.floor((seconds % 3600) / 60)
  const s = seconds % 60
  return h > 0
    ? `${h}:${String(m).padStart(2,'0')}:${String(s).padStart(2,'0')}`
    : `${m}:${String(s).padStart(2,'0')}`
}

function selectAnswer(label) {
  if (result.value) return
  answers.value[currentQuestion.value.id] = label
  saveProgress()
}

function prevQuestion() {
  if (currentIndex.value > 0) currentIndex.value--
}

function nextQuestion() {
  if (currentIndex.value < questions.value.length - 1) currentIndex.value++
}

async function togglePause() {
  if (!session.value) return
  const newStatus = session.value.status === 'ongoing' ? 'paused' : 'ongoing'
  try {
    const res = await sessionApi.update(session.value.id, {
      status: newStatus,
      timeElapsed: timeElapsed.value,
      currentIndex: currentIndex.value,
      answers: JSON.stringify(
        Object.entries(answers.value).map(([qId, ans]) => ({
          questionId: Number(qId), answer: ans
        }))
      )
    })
    if (res.code === 0) session.value = res.data
    if (newStatus === 'paused') stopTimer()
    else startTimer()
  } catch (e) {
    console.error('暂停/恢复失败', e)
  }
}

async function saveProgress() {
  if (!session.value || session.value.status === 'submitted') return
  try {
    await sessionApi.update(session.value.id, {
      timeElapsed: timeElapsed.value,
      currentIndex: currentIndex.value,
      answers: JSON.stringify(
        Object.entries(answers.value).map(([qId, ans]) => ({
          questionId: Number(qId), answer: ans
        }))
      )
    })
  } catch (e) {
    console.error('保存进度失败', e)
  }
}

function confirmSubmit() {
  showSubmitModal.value = true
}

async function submitExam() {
  showSubmitModal.value = false
  stopTimer()

  try {
    // 更新会话为 submitted
    await sessionApi.submit(session.value.id, {
      timeElapsed: timeElapsed.value,
      answers: JSON.stringify(
        Object.entries(answers.value).map(([qId, ans]) => ({
          questionId: Number(qId), answer: ans
        }))
      )
    })

    // 批量提交答案
    const answerItems = Object.entries(answers.value).map(([qId, ans]) => ({
      questionId: Number(qId), answer: ans
    }))
    const res = await paperApi.batchSubmit({
      sessionId: session.value.id,
      answers: answerItems
    })
    if (res.code === 0) {
      result.value = res.data
    }
  } catch (e) {
    console.error('提交失败', e)
  }
}

function startTimer() {
  stopTimer()
  timer = setInterval(() => { timeElapsed.value++ }, 1000)
}

function stopTimer() {
  if (timer) { clearInterval(timer); timer = null }
}

async function init() {
  const params = new URLSearchParams(window.location.search)
  const paperId = params.get('paperId')
  if (!paperId) {
    loaded.value = true
    return
  }

  try {
    // 加载试卷详情
    const detailRes = await paperApi.getDetail(paperId)
    if (detailRes.code !== 0) return
    paperDetail.value = detailRes.data
    questions.value = detailRes.data.questions || []
    materials.value = detailRes.data.materials || []

    // 创建/恢复做题会话
    const sessionRes = await sessionApi.create({ paperId: Number(paperId) })
    if (sessionRes.code === 0) {
      session.value = sessionRes.data
      // 恢复已有进度
      if (sessionRes.data.answers) {
        try {
          const savedAnswers = JSON.parse(sessionRes.data.answers)
          savedAnswers.forEach(a => { answers.value[a.questionId] = a.answer })
        } catch {}
      }
      currentIndex.value = sessionRes.data.currentIndex || 0
      timeElapsed.value = sessionRes.data.timeElapsed || 0

      if (sessionRes.data.status === 'ongoing') startTimer()
    }

    loaded.value = true
  } catch (e) {
    console.error('加载试卷失败', e)
    loaded.value = true
  }
}

onMounted(init)
onUnmounted(stopTimer)
</script>

<style scoped>
.online-practice { max-width: 960px; margin: 0 auto; padding: 20px; }
.top-bar {
  display: flex; justify-content: space-between; align-items: center;
  padding: 12px 16px; background: var(--vp-c-bg-soft); border-radius: 8px;
  margin-bottom: 16px;
}
.paper-info h2 { font-size: 18px; margin: 0 0 4px; }
.paper-info .meta { font-size: 13px; color: var(--vp-c-text-2); }
.timer { display: flex; align-items: center; gap: 8px; }
.time { font-size: 20px; font-weight: 600; font-variant-numeric: tabular-nums; }
.btn-pause, .btn-resume {
  padding: 4px 12px; border: 1px solid var(--vp-c-divider);
  border-radius: 4px; cursor: pointer; background: var(--vp-c-bg);
}
.material-panel {
  padding: 16px; background: var(--vp-c-bg-soft);
  border-radius: 8px; margin-bottom: 16px;
  max-height: 300px; overflow-y: auto;
}
.material-panel h3 { margin: 0 0 8px; font-size: 15px; }
.question-panel {
  padding: 20px; background: var(--vp-c-bg-soft);
  border-radius: 8px; margin-bottom: 16px;
}
.question-header {
  display: flex; gap: 12px; margin-bottom: 12px;
  font-size: 13px; color: var(--vp-c-text-2);
}
.question-content { font-size: 15px; line-height: 1.8; margin-bottom: 16px; }
.options { display: flex; flex-direction: column; gap: 8px; }
.option-item {
  display: flex; align-items: flex-start; gap: 8px;
  padding: 10px 12px; border: 1px solid var(--vp-c-divider);
  border-radius: 6px; cursor: pointer; transition: all 0.2s;
}
.option-item:hover { border-color: var(--vp-c-brand); }
.option-item.selected {
  border-color: var(--vp-c-brand); background: var(--vp-c-brand-dimm);
}
.option-label { font-weight: 600; min-width: 24px; }
.fill-blank input {
  width: 100%; padding: 8px 12px; border: 1px solid var(--vp-c-divider);
  border-radius: 4px; font-size: 14px;
}
.bottom-bar {
  display: flex; align-items: center; gap: 12px;
  padding: 12px 16px; background: var(--vp-c-bg-soft);
  border-radius: 8px;
}
.bottom-bar button {
  padding: 6px 16px; border: 1px solid var(--vp-c-divider);
  border-radius: 4px; cursor: pointer; background: var(--vp-c-bg);
}
.bottom-bar button:disabled { opacity: 0.5; cursor: not-allowed; }
.btn-submit { background: var(--vp-c-brand) !important; color: #fff !important; border: none !important; }
.btn-primary { background: var(--vp-c-brand); color: #fff; border: none; padding: 8px 24px; border-radius: 4px; cursor: pointer; }
.progress { flex: 1; height: 6px; background: var(--vp-c-divider); border-radius: 3px; overflow: hidden; }
.progress-bar { height: 100%; background: var(--vp-c-brand); transition: width 0.3s; }
.answered-count { font-size: 13px; color: var(--vp-c-text-2); white-space: nowrap; }
.submit-confirm { text-align: center; }
.submit-confirm h3 { margin: 0 0 12px; }
.submit-confirm .warn { color: #e6a23c; }
.submit-actions { display: flex; gap: 12px; justify-content: center; margin-top: 16px; }
.result-panel { margin-top: 24px; }
.result-panel h3 { font-size: 18px; margin-bottom: 16px; }
.result-stats { display: flex; gap: 24px; margin-bottom: 24px; }
.stat-item { text-align: center; padding: 16px; background: var(--vp-c-bg-soft); border-radius: 8px; min-width: 100px; }
.stat-item.wrong .stat-value { color: #f56c6c; }
.stat-value { display: block; font-size: 28px; font-weight: 700; }
.stat-label { font-size: 13px; color: var(--vp-c-text-2); }
.result-questions { display: flex; flex-direction: column; gap: 16px; }
.result-question {
  padding: 16px; border-radius: 8px; border-left: 4px solid var(--vp-c-divider);
}
.result-question.correct { border-left-color: #67c23a; }
.result-question.wrong { border-left-color: #f56c6c; }
.rq-header { display: flex; gap: 8px; align-items: center; margin-bottom: 8px; }
.badge-correct { color: #67c23a; font-size: 13px; font-weight: 600; }
.badge-wrong { color: #f56c6c; font-size: 13px; font-weight: 600; }
.badge-pending { color: #e6a23c; font-size: 13px; }
.rq-content { margin-bottom: 8px; }
.rq-answer p { margin: 4px 0; font-size: 14px; }
.rq-explanation { margin-top: 8px; padding-top: 8px; border-top: 1px solid var(--vp-c-divider); }
.rq-explanation h4 { margin: 0 0 4px; font-size: 14px; }
.loading { text-align: center; padding: 40px; color: var(--vp-c-text-2); }
</style>
```

- [ ] **Step 2: 更新 practice/online/index.md**

```markdown
---
layout: page
title: 在线练习
---

<OnlinePractice />
```

- [ ] **Step 3: 启动前端验证页面渲染**

Run: `cd frontend && npm run dev`
Expected: 访问 `/practice/online/?paperId=1` 页面正常加载（显示"加载中..."后变为空列表）

- [ ] **Step 4: Commit**

```bash
git add frontend/.vitepress/theme/components/OnlinePractice.vue \
        frontend/pages/practice/online/index.md
git commit -m "feat(P3): add OnlinePractice.vue with timer, navigation, submit, and result view"
```

---

## Task 16: 测试数据导入脚本

**Files:**
- Create: `scripts/seed_papers.sql`

- [ ] **Step 1: 创建种子数据 SQL**

```sql
-- scripts/seed_papers.sql
-- 插入测试试卷和题目数据，用于开发和验收测试

-- ===== 试卷 =====
INSERT INTO paper (id, title, category, region_id, rating, question_count, year) VALUES
(1, '2024年国家公务员考试行测真题', '行测', 1, 4, 10, 2024),
(2, '2024年北京市公务员考试行测真题', '行测', 2, 3, 10, 2024),
(3, '2023年国家公务员考试行测真题', '行测', 1, 4, 10, 2023),
(4, '2024年上海市公务员考试行测真题', '行测', 3, 3, 10, 2024),
(5, '2024年广东省公务员考试行测真题', '行测', 4, 3, 10, 2024);

-- ===== 材料组 =====
INSERT INTO material_group (id, paper_id, title, content, sort_order) VALUES
(1, 1, '资料分析材料', '<p>2023年某省GDP达到8.5万亿元，同比增长6.2%。其中第一产业增加值0.4万亿元，增长3.8%；第二产业增加值3.6万亿元，增长5.8%；第三产业增加值4.5万亿元，增长7.1%。</p>', 1),
(2, 4, '言语理解材料', '<p>在当代社会，人工智能技术的飞速发展正在深刻改变着人类的生活方式和工作模式。从智能语音助手到自动驾驶汽车，从医疗诊断到金融分析，AI的应用场景日益广泛。</p>', 1);

-- ===== 题目 =====
-- 试卷1: 言语理解 (题1-2)
INSERT INTO question (id, paper_id, sort_order, module, sub_module, knowledge_point, type, content, options, answer, explanation, score) VALUES
(1, 1, 1, '言语理解', '片段阅读', '主旨概括', 'single_choice',
 '<p>人工智能技术的飞速发展正在深刻改变着人类的生活方式和工作模式。从智能语音助手到自动驾驶汽车，从医疗诊断到金融分析，AI的应用场景日益广泛。这段文字主要说明了：</p>',
 '[{"label":"A","text":"AI技术已经取代了人类大部分工作"},{"label":"B","text":"AI技术的发展对人类社会产生了广泛影响"},{"label":"C","text":"AI技术只能在少数领域应用"},{"label":"D","text":"AI技术的发展速度超出了人类的预期"}]',
 'B',
 '<p>文段首句点明主旨"人工智能技术的飞速发展正在深刻改变着人类的生活方式和工作模式"，后文列举具体应用场景来支撑这一观点。B项"AI技术的发展对人类社会产生了广泛影响"是对主旨句的同义替换。</p>',
 1.0),

(2, 1, 2, '言语理解', '逻辑填空', '实词辨析', 'single_choice',
 '<p>在国家治理体系和治理能力现代化的进程中，制度创新起着______的作用。</p>',
 '[{"label":"A","text":"举足轻重"},{"label":"B","text":"微不足道"},{"label":"C","text":"可有可无"},{"label":"D","text":"无足轻重"}]',
 'A',
 '<p>"举足轻重"比喻所处地位重要，一举一动都会影响全局，符合制度创新在国家治理中的关键地位。其他选项与语境不符。</p>',
 1.0),

-- 试卷1: 数量关系 (题3-4)
(3, 1, 3, '数量关系', '数学运算', '工程问题', 'single_choice',
 '<p>一项工程，甲单独完成需要12天，乙单独完成需要18天。甲乙两人合作，需要多少天完成？</p>',
 '[{"label":"A","text":"6"},{"label":"B","text":"7.2"},{"label":"C","text":"8"},{"label":"D","text":"9"}]',
 'B',
 '<p>甲的效率=1/12，乙的效率=1/18，合作效率=1/12+1/18=5/36。所需天数=36/5=7.2天。</p>',
 1.0),

(4, 1, 4, '数量关系', '数学运算', '行程问题', 'single_choice',
 '<p>甲乙两地相距360公里，一辆汽车从甲地出发，以每小时60公里的速度行驶。行驶了2小时后，速度提高到每小时80公里。问还需多少小时到达乙地？</p>',
 '[{"label":"A","text":"2"},{"label":"B","text":"2.5"},{"label":"C","text":"3"},{"label":"D","text":"3.5"}]',
 'C',
 '<p>已行驶距离=60×2=120公里，剩余距离=360-120=240公里。提速后所需时间=240÷80=3小时。</p>',
 1.0),

-- 试卷1: 判断推理 (题5-6)
(5, 1, 5, '判断推理', '图形推理', '位置规律', 'single_choice',
 '<p>请选择与前面图形规律最相符的一项：图形依次顺时针旋转90度。</p>',
 '[{"label":"A","text":"顺时针旋转90度"},{"label":"B","text":"逆时针旋转90度"},{"label":"C","text":"水平翻转"},{"label":"D","text":"垂直翻转"}]',
 'A',
 '<p>根据图形位置规律，图形依次顺时针旋转90度，因此下一个图形也应为顺时针旋转90度的结果。</p>',
 1.0),

(6, 1, 6, '判断推理', '定义判断', '单定义判断', 'single_choice',
 '<p>"社会懈怠"是指个体在群体中工作时，比单独工作时付出的努力更少的现象。根据上述定义，下列属于社会懈怠的是：</p>',
 '[{"label":"A","text":"小王在团队讨论中积极发言，提出了多个创新方案"},{"label":"B","text":"小李在合唱排练时，声音比独自练习时小了很多"},{"label":"C","text":"小张在比赛中为了团队荣誉拼尽全力"},{"label":"D","text":"小赵在小组作业中承担了最多的工作量"}]',
 'B',
 '<p>B项中小李在群体中（合唱排练）比单独时（独自练习）付出的努力更少（声音小了很多），符合"社会懈怠"的定义。</p>',
 1.0),

-- 试卷1: 资料分析 (题7-8, 关联材料组1)
(7, 1, 7, '资料分析', '文字资料', '增长率计算', 'single_choice',
 '<p>根据上述资料，2023年该省GDP同比增长了多少万亿元？</p>',
 '[{"label":"A","text":"约0.48"},{"label":"B","text":"约0.50"},{"label":"C","text":"约0.52"},{"label":"D","text":"约0.55"}]',
 'B',
 '<p>2023年GDP=8.5万亿，同比增长6.2%。增长量=8.5÷(1+6.2%)×6.2%≈8.5÷1.062×0.062≈0.496万亿，约0.50万亿。</p>',
 1.0),
 'single_choice'),

(8, 1, 8, '资料分析', '文字资料', '比重计算', 'single_choice',
 '<p>根据上述资料，2023年该省第三产业增加值占GDP的比重约为：</p>',
 '[{"label":"A","text":"47.1%"},{"label":"B","text":"50.2%"},{"label":"C","text":"52.9%"},{"label":"D","text":"55.3%"}]',
 'C',
 '<p>第三产业增加值=4.5万亿，GDP=8.5万亿。比重=4.5÷8.5×100%≈52.9%。</p>',
 1.0),

-- 试卷1: 常识判断 (题9-10)
(9, 1, 9, '常识判断', '政治常识', '时政热点', 'single_choice',
 '<p>党的二十大报告提出，从现在起，中国共产党的中心任务就是团结带领全国各族人民：</p>',
 '[{"label":"A","text":"实现中华民族伟大复兴"},{"label":"B","text":"全面建成社会主义现代化强国、实现第二个百年奋斗目标，以中国式现代化全面推进中华民族伟大复兴"},{"label":"C","text":"全面建设小康社会"},{"label":"D","text":"建成社会主义现代化强国"}]',
 'B',
 '<p>党的二十大报告明确指出，从现在起，中国共产党的中心任务就是团结带领全国各族人民全面建成社会主义现代化强国、实现第二个百年奋斗目标，以中国式现代化全面推进中华民族伟大复兴。</p>',
 1.0),

(10, 1, 10, '常识判断', '法律常识', '宪法', 'single_choice',
 '<p>我国宪法规定，中华人民共和国的一切权力属于：</p>',
 '[{"label":"A","text":"公民"},{"label":"B","text":"人民"},{"label":"C","text":"人民代表大会"},{"label":"D","text":"中国共产党"}]',
 'B',
 '<p>《宪法》第二条规定：中华人民共和国的一切权力属于人民。</p>',
 1.0);

-- 试卷2: 简化10题
INSERT INTO question (id, paper_id, sort_order, module, sub_module, knowledge_point, type, content, options, answer, explanation, score) VALUES
(11, 2, 1, '言语理解', '片段阅读', '主旨概括', 'single_choice',
 '<p>近年来，随着互联网技术的快速发展，电子商务已经渗透到人们生活的方方面面。这段话的主旨是：</p>',
 '[{"label":"A","text":"互联网技术发展很快"},{"label":"B","text":"电子商务已经融入人们生活"},{"label":"C","text":"人们生活离不开互联网"},{"label":"D","text":"电子商务前景广阔"}]',
 'B',
 '<p>文段强调电子商务已"渗透到人们生活的方方面面"，即融入了人们生活。</p>',
 1.0),

(12, 2, 2, '数量关系', '数学运算', '基础计算', 'single_choice',
 '<p>一个数的25%是60，这个数是多少？</p>',
 '[{"label":"A","text":"120"},{"label":"B","text":"180"},{"label":"C","text":"200"},{"label":"D","text":"240"}]',
 'D',
 '<p>设这个数为x，则x×25%=60，x=60÷0.25=240。</p>',
 1.0),

(13, 2, 3, '判断推理', '类比推理', '内涵关系', 'single_choice',
 '<p>医生：医院 对应的关系最接近：</p>',
 '[{"label":"A","text":"教师：学校"},{"label":"B","text":"司机：汽车"},{"label":"C","text":"厨师：饭店"},{"label":"D","text":"工人：工厂"}]',
 'A',
 '<p>医生在医院工作（职业：工作场所），教师在学校工作，关系最为相近。</p>',
 1.0),

(14, 2, 4, '常识判断', '科技常识', '信息技术', 'single_choice',
 '<p>5G通信技术的"G"代表什么？</p>',
 '[{"label":"A","text":"Generation"},{"label":"B","text":"Gigabyte"},{"label":"C","text":"Global"},{"label":"D","text":"Gateway"}]',
 'A',
 '<p>5G中的G是Generation（代）的缩写，表示第五代移动通信技术。</p>',
 1.0),

(15, 2, 5, '言语理解', '逻辑填空', '成语辨析', 'single_choice',
 '<p>面对困难，他始终保持______的态度，从不轻言放弃。</p>',
 '[{"label":"A","text":"积极乐观"},{"label":"B","text":"消极悲观"},{"label":"C","text":"漠不关心"},{"label":"D","text":"犹豫不决"}]',
 'A',
 '<p>根据"从不轻言放弃"可知其态度是正面的，"积极乐观"最符合语境。</p>',
 1.0),

(16, 2, 6, '数量关系', '数学运算', '概率问题', 'single_choice',
 '<p>掷两枚硬币，至少有一枚正面朝上的概率是多少？</p>',
 '[{"label":"A","text":"1/4"},{"label":"B","text":"1/2"},{"label":"C","text":"3/4"},{"label":"D","text":"1"}]',
 'C',
 '<p>总可能=4，至少一枚正面=4-全部反面=4-1=3。概率=3/4。</p>',
 1.0),

(17, 2, 7, '判断推理', '逻辑判断', '削弱论证', 'single_choice',
 '<p>某研究发现喝咖啡可以降低患心脏病的风险。以下哪项如果为真，最能削弱这一结论？</p>',
 '[{"label":"A","text":"咖啡的价格近年来持续上涨"},{"label":"B","text":"该研究只调查了100名参与者"},{"label":"C","text":"经常喝咖啡的人通常也注重运动和饮食"},{"label":"D","text":"有些人不喜欢喝咖啡"}]',
 'C',
 '<p>C项指出存在他因（运动和饮食），可能是这些因素而非咖啡降低了心脏病风险，削弱了因果关系。</p>',
 1.0),

(18, 2, 8, '资料分析', '表格资料', '简单计算', 'single_choice',
 '<p>某公司2023年Q1收入100万，Q2收入120万，Q2比Q1增长百分之几？</p>',
 '[{"label":"A","text":"15%"},{"label":"B","text":"20%"},{"label":"C","text":"25%"},{"label":"D","text":"30%"}]',
 'B',
 '<p>增长率=(120-100)÷100×100%=20%。</p>',
 1.0),

(19, 2, 9, '常识判断', '历史常识', '中国近现代史', 'single_choice',
 '<p>中华人民共和国成立于哪一年？</p>',
 '[{"label":"A","text":"1945年"},{"label":"B","text":"1947年"},{"label":"C","text":"1949年"},{"label":"D","text":"1950年"}]',
 'C',
 '<p>中华人民共和国于1949年10月1日成立。</p>',
 1.0),

(20, 2, 10, '言语理解', '语句排序', '语句衔接', 'single_choice',
 '<p>下列句子排序正确的是：①因此要珍惜时间 ②时间是宝贵的 ③它一去不复返 ④我们应该合理规划每一天</p>',
 '[{"label":"A","text":"②③①④"},{"label":"B","text":"①②③④"},{"label":"C","text":"②①③④"},{"label":"D","text":"③②①④"}]',
 'A',
 '<p>先说"时间是宝贵的"(②)，再说"一去不复返"(③)，得出结论"要珍惜时间"(①)，最后是行动建议"合理规划"(④)。</p>',
 1.0);

-- 更新试卷的 question_count
UPDATE paper SET question_count = 10 WHERE id IN (1, 2);
UPDATE paper SET question_count = 10 WHERE id IN (3, 4, 5);
```

> 注意：试卷 3/4/5 暂无题目，仅用于测试试卷列表筛选功能。后续可通过爬取脚本补充。

- [ ] **Step 2: 导入种子数据**

Run: `docker exec -i gongkao-mysql mysql -uroot -proot123 gongkao < scripts/seed_papers.sql`
Expected: 无报错，数据导入成功

- [ ] **Step 3: 验证数据**

Run: `docker exec -i gongkao-mysql mysql -uroot -proot123 gongkao -e "SELECT id, title, category, question_count FROM paper;"`
Expected: 5 行试卷数据

- [ ] **Step 4: Commit**

```bash
git add scripts/seed_papers.sql
git commit -m "feat(P3): add seed data SQL with 5 papers and 20 questions"
```

---

## Task 17: 后端集成测试

**Files:**
- 无新文件，验证已有代码

- [ ] **Step 1: 启动全部服务**

Run: `docker-compose up -d`
Expected: 6 个服务全部启动，MySQL 表已创建

- [ ] **Step 2: 启动 Spring Boot**

Run: `cd backend && mvn spring-boot:run`
Expected: 应用在 :8080 启动成功，无报错

- [ ] **Step 3: 测试试卷列表 API**

Run: `curl -s http://localhost:8080/api/papers?category=行测&page=1&pageSize=10 | python3 -m json.tool`
Expected: 返回 5 条试卷数据，含 regionName 字段

- [ ] **Step 4: 测试试卷详情 API**

Run: `curl -s http://localhost:8080/api/papers/1 | python3 -m json.tool`
Expected: 返回试卷详情，含 materials 和 questions 数组，questions 不含 answer 字段

- [ ] **Step 5: 测试材料组 API**

Run: `curl -s http://localhost:8080/api/papers/1/materials | python3 -m json.tool`
Expected: 返回材料组列表

- [ ] **Step 6: 测试知识点查询 API**

Run: `curl -s "http://localhost:8080/api/papers/questions/by-knowledge?module=言语理解&limit=5" | python3 -m json.tool`
Expected: 返回知识点为"言语理解"的题目列表

- [ ] **Step 7: 测试做题会话（需登录获取 Token）**

先登录获取 Token：
```bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login-password \
  -H "Content-Type: application/json" \
  -d '{"email":"test@test.com","password":"123456"}' | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['accessToken'])")
```

创建会话：
```bash
curl -s -X POST http://localhost:8080/api/sessions \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"paperId":1}' | python3 -m json.tool
```
Expected: 返回会话对象，status=ongoing

- [ ] **Step 8: 测试批量提交答案**

```bash
curl -s -X POST http://localhost:8080/api/papers/user-answers/batch \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"sessionId":1,"answers":[{"questionId":1,"answer":"B"},{"questionId":2,"answer":"A"},{"questionId":3,"answer":"B"}]}' | python3 -m json.tool
```
Expected: 返回判分结果，含 correctCount、wrongCount、accuracy

- [ ] **Step 9: 测试查看我的答案**

```bash
curl -s http://localhost:8080/api/papers/1/my-answers \
  -H "Authorization: Bearer $TOKEN" | python3 -m json.tool
```
Expected: 返回之前的作答记录

- [ ] **Step 10: 前端端到端测试**

1. 访问 `/题库/` → 看到试卷列表
2. 点击试卷 → 跳转到 `/practice/online/?paperId=1`
3. 看到题目和选项 → 选择答案
4. 点击"提交试卷" → 确认提交
5. 看到正确率统计和逐题解析

Expected: 完整流程无报错

---

## P3 验收清单

| # | 验收项 | 验证方式 |
|---|--------|---------|
| 1 | 试卷列表分页查询 | `GET /api/papers?category=行测&page=1&pageSize=10` 返回正确分页数据 |
| 2 | 地区筛选 | 传 `regionId=1` 只返回国考试卷 |
| 3 | 试卷详情含材料组+题目 | `GET /api/papers/1` 返回 materials 和 questions，questions 不含 answer |
| 4 | 题目不含答案（做题前） | QuestionVO 无 answer/explanation 字段 |
| 5 | 创建做题会话 | `POST /api/sessions` 返回 session，status=ongoing |
| 6 | 恢复已有会话 | 再次创建同试卷会话返回已有会话而非新建 |
| 7 | 暂停/恢复会话 | `PUT /api/sessions/{id}` 切换 status，进度不丢失 |
| 8 | 批量提交答案 | `POST /api/papers/user-answers/batch` 返回正确判分结果 |
| 9 | 自动判分（选择题） | 正确选项 isCorrect=true，错误 isCorrect=false |
| 10 | 正确率计算 | accuracy = correctCount / (correctCount + wrongCount) * 100 |
| 11 | 查看我的答案 | `GET /api/papers/1/my-answers` 返回含答案+解析的题目列表 |
| 12 | 知识点筛选题目 | `GET /api/papers/questions/by-knowledge?module=言语理解` 返回对应题目 |
| 13 | MinIO 文件上传 | `POST /api/files/upload` 返回 key 和 url |
| 14 | 前端试卷列表渲染 | `/题库/` 页面显示试卷卡片和筛选栏 |
| 15 | 前端做题页面 | `/practice/online/?paperId=1` 显示题目、选项、计时器 |
| 16 | 前端暂停/恢复 | 点击暂停→计时停止→点击继续→计时恢复 |
| 17 | 前端提交+结果 | 提交后显示正确率、逐题解析（含我的答案、正确答案、解析） |
| 18 | 错题自动归集 | 提交后查看 my-answers，isCorrect=false 的题目已记录 |

**P3 完成标志：** 以上 18 项全部通过 → 可进入 P4（申论题库 + AI 批改）
