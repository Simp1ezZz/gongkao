package com.gongkao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gongkao.entity.PracticeSession;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface PracticeSessionMapper extends BaseMapper<PracticeSession> {

    @Select("SELECT * FROM practice_session " +
            "WHERE user_id = #{userId} AND paper_id = #{paperId} " +
            "AND status IN ('ongoing', 'paused') " +
            "ORDER BY updated_at DESC LIMIT 1")
    PracticeSession selectActiveByUserPaper(@Param("userId") Long userId,
                                             @Param("paperId") Long paperId);

    @Select("SELECT * FROM practice_session " +
            "WHERE user_id = #{userId} " +
            "ORDER BY updated_at DESC")
    List<PracticeSession> selectByUserId(@Param("userId") Long userId);
}
