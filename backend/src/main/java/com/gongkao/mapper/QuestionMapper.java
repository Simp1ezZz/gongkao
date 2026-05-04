package com.gongkao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gongkao.entity.Question;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface QuestionMapper extends BaseMapper<Question> {

    @Select("SELECT * FROM question " +
            "WHERE paper_id = #{paperId} " +
            "ORDER BY sort_order ASC, id ASC")
    List<Question> selectByPaperId(@Param("paperId") Long paperId);

    @Select("<script>" +
            "SELECT id FROM question " +
            "WHERE module = #{module} " +
            "<if test='subModule != null'> AND sub_module = #{subModule} </if>" +
            "<if test='knowledgePoint != null'> AND knowledge_point = #{knowledgePoint} </if>" +
            "</script>")
    List<Long> selectIdsByKnowledge(@Param("module") String module,
                                     @Param("subModule") String subModule,
                                     @Param("knowledgePoint") String knowledgePoint);
}
