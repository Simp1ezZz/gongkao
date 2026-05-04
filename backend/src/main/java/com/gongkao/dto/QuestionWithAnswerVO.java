package com.gongkao.dto;

import com.gongkao.entity.Question;
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
    private String options;
    private String answer;
    private String explanation;
    private String images;
    private BigDecimal score;
    private String userAnswer;
    private Boolean isCorrect;

    public static QuestionWithAnswerVO from(Question q, String userAnswer, Boolean isCorrect) {
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
        vo.setUserAnswer(userAnswer);
        vo.setIsCorrect(isCorrect);
        return vo;
    }
}
