package com.gongkao.dto;

import lombok.Data;

@Data
public class PracticeHistorySummaryVO {
    private int totalSessions;
    private int totalQuestions;
    private int correctQuestions;
    private int wrongQuestions;
    private int unansweredQuestions;
    private double avgAccuracy;
    private int totalTimeElapsed;
    private int paperSessionCount;
    private int specialSessionCount;
}
