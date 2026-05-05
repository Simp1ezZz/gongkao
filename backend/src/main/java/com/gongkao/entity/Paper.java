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
    private String category;
    private String regionName;
    private Integer rating;
    private Integer questionCount;
    private Integer year;
    private LocalDateTime createdAt;
}
