package com.gongkao.dto;

import lombok.Data;

@Data
public class PaperQueryRequest {
    private String category;
    private Integer regionId;
    private Integer page = 1;
    private Integer pageSize = 20;
}
