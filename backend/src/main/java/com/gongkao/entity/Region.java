package com.gongkao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("region")
public class Region {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String name;
    private String category;
    private Integer sortOrder;
}
