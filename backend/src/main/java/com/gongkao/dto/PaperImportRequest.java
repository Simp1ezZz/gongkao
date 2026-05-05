package com.gongkao.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class PaperImportRequest {

    @Data
    public static class PaperInfo {
        private String title;
        private Integer year;
        private String category;
        private Integer regionId;
        private String regionName;
    }

    @Data
    public static class QuestionItem {
        private Integer sortOrder;
        private String module;
        private String subModule;
        private Long materialGroupId;
        private String type;
        private String content;
        private String options;  // JSON string
        private String answer;
        private String explanation;
        private String images;   // JSON string
        private BigDecimal score;
    }

    @Data
    public static class MaterialGroupItem {
        private Integer sortOrder;
        private String title;
        private String content;
        private String images;  // JSON string
    }

    private PaperInfo paper;
    private List<QuestionItem> questions;
    private List<MaterialGroupItem> materialGroups;
}
