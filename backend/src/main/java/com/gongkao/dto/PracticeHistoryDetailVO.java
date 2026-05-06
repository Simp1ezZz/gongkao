package com.gongkao.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class PracticeHistoryDetailVO {
    private Long sessionId;
    private Long paperId;
    private String paperTitle;
    private String type;
    private String module;
    private int timeElapsed;
    private LocalDateTime submittedAt;
    private Summary summary;
    private List<QuestionDetail> questions;

    @Data
    public static class Summary {
        private int totalQuestions;
        private int correctCount;
        private int wrongCount;
        private int unansweredCount;
        private double accuracy;
    }

    @Data
    public static class QuestionDetail {
        private Long questionId;
        private Integer sortOrder;
        private String module;
        private String content;
        private String options;
        private String correctAnswer;
        private String userAnswer;
        private Boolean isCorrect;
        private String explanation;
    }
}
