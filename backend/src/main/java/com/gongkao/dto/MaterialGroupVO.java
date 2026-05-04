package com.gongkao.dto;

import com.gongkao.entity.MaterialGroup;
import lombok.Data;

@Data
public class MaterialGroupVO {
    private Long id;
    private String title;
    private String content;
    private Integer sortOrder;

    public static MaterialGroupVO from(MaterialGroup mg) {
        MaterialGroupVO vo = new MaterialGroupVO();
        vo.setId(mg.getId());
        vo.setTitle(mg.getTitle());
        vo.setContent(mg.getContent());
        vo.setSortOrder(mg.getSortOrder());
        return vo;
    }
}
