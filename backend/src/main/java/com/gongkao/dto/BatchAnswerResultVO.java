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
