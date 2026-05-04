package com.gongkao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gongkao.entity.UserAnswer;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface UserAnswerMapper extends BaseMapper<UserAnswer> {

    @Select("SELECT * FROM user_answer " +
            "WHERE user_id = #{userId} AND paper_id = #{paperId} " +
            "AND session_id = #{sessionId}")
    List<UserAnswer> selectBySession(@Param("userId") Long userId,
                                      @Param("paperId") Long paperId,
                                      @Param("sessionId") Long sessionId);

    @Select("SELECT * FROM user_answer " +
            "WHERE user_id = #{userId} AND paper_id = #{paperId}")
    List<UserAnswer> selectByUserPaper(@Param("userId") Long userId,
                                        @Param("paperId") Long paperId);
}
