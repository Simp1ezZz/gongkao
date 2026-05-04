package com.gongkao.dto;

import lombok.Data;

@Data
public class SessionUpdateRequest {
    private String status;
    private Integer timeElapsed;
    private Integer currentIndex;
    private String answers;
}
