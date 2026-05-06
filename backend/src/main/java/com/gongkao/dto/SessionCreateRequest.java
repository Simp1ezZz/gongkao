package com.gongkao.dto;

import lombok.Data;

@Data
public class SessionCreateRequest {
    private Long paperId;
    private String module;
    private Integer questionCount;
}
