package com.gongkao.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PracticeHistoryItemVO {
    private Long sessionId;
    private Long paperId;
    private String paperTitle;
    private String type; // paper, special
    private String module;
    private int totalQuestions;
    private int correctCount;
    private int wrongCount;
    private int unansweredCount;
    private double accuracy;
    private int timeElapsed;
    private LocalDateTime submittedAt;
}
