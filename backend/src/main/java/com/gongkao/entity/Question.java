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
    private String type;
    private String content;
    private String options;
    private String answer;
    private String explanation;
    private String images;
    private BigDecimal score;
    private LocalDateTime createdAt;
}
