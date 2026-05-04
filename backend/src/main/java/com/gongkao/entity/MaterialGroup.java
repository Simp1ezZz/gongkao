package com.gongkao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("material_group")
public class MaterialGroup {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long paperId;
    private String title;
    private String content;
    private String images;
    private Integer sortOrder;
}
