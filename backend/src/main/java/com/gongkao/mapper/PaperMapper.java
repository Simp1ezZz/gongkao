package com.gongkao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.gongkao.entity.Paper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PaperMapper extends BaseMapper<Paper> {

    IPage<Paper> selectPageWithRegion(IPage<Paper> page,
                                       @Param("category") String category,
                                       @Param("regionName") String regionName);
}
