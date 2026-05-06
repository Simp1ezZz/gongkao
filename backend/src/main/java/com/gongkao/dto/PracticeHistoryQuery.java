package com.gongkao.dto;

import lombok.Data;

@Data
public class PracticeHistoryQuery {
    private Integer page = 1;
    private Integer pageSize = 10;
    private String type = "all"; // all, paper, special
}
