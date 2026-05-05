package com.gongkao.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class PaperImportRequest {

    @Data
    public static class Metadata {
        private String title;
        private String category;
        private Integer year;
        private Integer regionId;
        private Integer rating;
    }

    @Data
    public static class OptionItem {
        private String label;
        private String text;
        private String image;
    }

    @Data
    public static class QuestionItem {
        private Integer sortOrder;
        private String content;
        private List<OptionItem> options;
        private String answer;
        private String explanation;
        private List<String> images;
        private String type;
        private String module;
        private BigDecimal score;
        private Integer materialGroupIndex;
    }

    @Data
    public static class SectionItem {
        private String module;
        private String description;
        private List<QuestionItem> questions;
    }

    @Data
    public static class MaterialGroupItem {
        private String title;
        private String content;
        private List<String> images;
        private Integer sortOrder;
        private List<Integer> questionNumbers;
    }

    private Metadata metadata;
    private List<SectionItem> sections;
    private List<MaterialGroupItem> materialGroups;
}
