package com.gongkao.dto;

import com.gongkao.entity.Question;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class QuestionVO {
    private Long id;
    private Long materialGroupId;
    private Integer sortOrder;
    private String module;
    private String subModule;
    private String knowledgePoint;
    private String type;
    private String content;
    private String options;
    private String images;
    private BigDecimal score;

    public static QuestionVO from(Question q) {
        QuestionVO vo = new QuestionVO();
        vo.setId(q.getId());
        vo.setMaterialGroupId(q.getMaterialGroupId());
        vo.setSortOrder(q.getSortOrder());
        vo.setModule(q.getModule());
        vo.setSubModule(q.getSubModule());
        vo.setKnowledgePoint(q.getKnowledgePoint());
        vo.setType(q.getType());
        vo.setContent(q.getContent());
        vo.setOptions(q.getOptions());
        vo.setImages(q.getImages());
        vo.setScore(q.getScore());
        return vo;
    }
}
