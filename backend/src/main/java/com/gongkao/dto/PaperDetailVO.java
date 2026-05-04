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
