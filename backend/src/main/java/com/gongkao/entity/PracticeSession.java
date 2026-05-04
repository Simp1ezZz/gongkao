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
    private String status;
    private Integer timeElapsed;
    private Integer currentIndex;
    private String answers;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
