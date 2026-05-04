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
