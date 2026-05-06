package com.gongkao.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gongkao.dto.*;
import com.gongkao.entity.Paper;
import com.gongkao.entity.PracticeSession;
import com.gongkao.entity.Question;
import com.gongkao.entity.UserAnswer;
import com.gongkao.mapper.PaperMapper;
import com.gongkao.mapper.PracticeSessionMapper;
import com.gongkao.mapper.QuestionMapper;
import com.gongkao.mapper.UserAnswerMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PracticeHistoryService {

    private final PracticeSessionMapper sessionMapper;
    private final UserAnswerMapper userAnswerMapper;
    private final PaperMapper paperMapper;
    private final QuestionMapper questionMapper;

    public PracticeHistorySummaryVO getSummary(Long userId) {
        List<PracticeSession> sessions = sessionMapper.selectList(
            new LambdaQueryWrapper<PracticeSession>()
                .eq(PracticeSession::getUserId, userId)
                .eq(PracticeSession::getStatus, "submitted")
        );

        PracticeHistorySummaryVO vo = new PracticeHistorySummaryVO();
        vo.setTotalSessions(sessions.size());

        int paperCount = 0, specialCount = 0;
        int totalQ = 0, correctQ = 0, wrongQ = 0, unansweredQ = 0, totalTime = 0;

        for (PracticeSession s : sessions) {
            if (s.getPaperId() != null) paperCount++;
            else specialCount++;

            totalTime += s.getTimeElapsed() != null ? s.getTimeElapsed() : 0;

            List<UserAnswer> answers = userAnswerMapper.selectList(
                new LambdaQueryWrapper<UserAnswer>()
                    .eq(UserAnswer::getSessionId, s.getId())
                    .eq(UserAnswer::getUserId, userId)
            );
            for (UserAnswer a : answers) {
                totalQ++;
                if (a.getIsCorrect() == null) unansweredQ++;
                else if (a.getIsCorrect()) correctQ++;
                else wrongQ++;
            }
        }

        vo.setPaperSessionCount(paperCount);
        vo.setSpecialSessionCount(specialCount);
        vo.setTotalQuestions(totalQ);
        vo.setCorrectQuestions(correctQ);
        vo.setWrongQuestions(wrongQ);
        vo.setUnansweredQuestions(unansweredQ);
        vo.setTotalTimeElapsed(totalTime);

        int graded = correctQ + wrongQ;
        vo.setAvgAccuracy(graded > 0 ? Math.round((double) correctQ / graded * 100 * 100.0) / 100.0 : 0);

        return vo;
    }

    public PageResult<PracticeHistoryItemVO> listHistory(Long userId, PracticeHistoryQuery query) {
        LambdaQueryWrapper<PracticeSession> wrapper = new LambdaQueryWrapper<PracticeSession>()
            .eq(PracticeSession::getUserId, userId)
            .eq(PracticeSession::getStatus, "submitted");

        if ("paper".equals(query.getType())) {
            wrapper.isNotNull(PracticeSession::getPaperId);
        } else if ("special".equals(query.getType())) {
            wrapper.isNull(PracticeSession::getPaperId);
        }

        wrapper.orderByDesc(PracticeSession::getUpdatedAt);

        IPage<PracticeSession> page = sessionMapper.selectPage(
            new Page<>(query.getPage(), query.getPageSize()), wrapper
        );

        // Batch load papers (title + questionCount)
        Set<Long> paperIds = page.getRecords().stream()
            .map(PracticeSession::getPaperId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        Map<Long, Paper> paperMap = paperIds.isEmpty() ? Map.of()
            : paperMapper.selectBatchIds(paperIds).stream()
                .collect(Collectors.toMap(Paper::getId, p -> p));

        // Batch load answer stats per session
        List<Long> sessionIds = page.getRecords().stream()
            .map(PracticeSession::getId).collect(Collectors.toList());
        Map<Long, int[]> statsMap = buildSessionStats(userId, sessionIds);

        List<PracticeHistoryItemVO> list = page.getRecords().stream().map(s -> {
            PracticeHistoryItemVO item = new PracticeHistoryItemVO();
            item.setSessionId(s.getId());
            item.setPaperId(s.getPaperId());
            item.setModule(s.getModule());
            item.setTimeElapsed(s.getTimeElapsed() != null ? s.getTimeElapsed() : 0);
            item.setSubmittedAt(s.getUpdatedAt());

            boolean isPaper = s.getPaperId() != null;
            item.setType(isPaper ? "paper" : "special");

            if (isPaper) {
                Paper paper = paperMap.get(s.getPaperId());
                item.setPaperTitle(paper != null ? paper.getTitle() : "未知试卷");
            } else {
                item.setPaperTitle(formatSpecialTitle(s));
            }

            int[] stats = statsMap.getOrDefault(s.getId(), new int[]{0, 0, 0});
            // 整卷用试卷实际题目数，专项用答题数
            if (isPaper && paperMap.containsKey(s.getPaperId())) {
                Paper paper = paperMap.get(s.getPaperId());
                item.setTotalQuestions(paper.getQuestionCount() != null ? paper.getQuestionCount() : stats[0]);
            } else {
                item.setTotalQuestions(stats[0]);
            }
            item.setCorrectCount(stats[1]);
            item.setWrongCount(stats[2]);
            item.setUnansweredCount(item.getTotalQuestions() - stats[1] - stats[2]);

            int graded = stats[1] + stats[2];
            item.setAccuracy(graded > 0 ? Math.round((double) stats[1] / graded * 100 * 100.0) / 100.0 : 0);

            return item;
        }).collect(Collectors.toList());

        return PageResult.of(list, page.getTotal(), query.getPage(), query.getPageSize());
    }

    public PracticeHistoryDetailVO getDetail(Long userId, Long sessionId) {
        PracticeSession session = sessionMapper.selectById(sessionId);
        if (session == null || !session.getUserId().equals(userId)) {
            throw new RuntimeException("记录不存在或无权访问");
        }

        boolean isPaper = session.getPaperId() != null;

        PracticeHistoryDetailVO vo = new PracticeHistoryDetailVO();
        vo.setSessionId(session.getId());
        vo.setPaperId(session.getPaperId());
        vo.setType(isPaper ? "paper" : "special");
        vo.setModule(session.getModule());
        vo.setTimeElapsed(session.getTimeElapsed() != null ? session.getTimeElapsed() : 0);
        vo.setSubmittedAt(session.getUpdatedAt());

        if (isPaper) {
            Paper paper = paperMapper.selectById(session.getPaperId());
            vo.setPaperTitle(paper != null ? paper.getTitle() : "未知试卷");
        } else {
            vo.setPaperTitle(formatSpecialTitle(session));
        }

        // Load answers (keyed by questionId)
        List<UserAnswer> answers = userAnswerMapper.selectList(
            new LambdaQueryWrapper<UserAnswer>()
                .eq(UserAnswer::getSessionId, sessionId)
                .eq(UserAnswer::getUserId, userId)
        );
        Map<Long, UserAnswer> answerMap = answers.stream()
            .collect(Collectors.toMap(UserAnswer::getQuestionId, a -> a, (a, b) -> a));

        // Load all questions: for paper sessions load from paper, for special load answered questions
        List<Question> allQuestions;
        if (isPaper && session.getPaperId() != null) {
            allQuestions = questionMapper.selectByPaperId(session.getPaperId());
        } else {
            List<Long> qIds = answers.stream().map(UserAnswer::getQuestionId).collect(Collectors.toList());
            allQuestions = qIds.isEmpty() ? List.of() : questionMapper.selectBatchIds(qIds);
        }

        int correct = 0, wrong = 0, unanswered = 0;
        List<PracticeHistoryDetailVO.QuestionDetail> questionDetails = new ArrayList<>();

        for (Question q : allQuestions) {
            PracticeHistoryDetailVO.QuestionDetail detail = new PracticeHistoryDetailVO.QuestionDetail();
            detail.setQuestionId(q.getId());
            detail.setSortOrder(q.getSortOrder());
            detail.setModule(q.getModule());
            detail.setContent(q.getContent());
            detail.setOptions(q.getOptions());
            detail.setCorrectAnswer(q.getAnswer());
            detail.setExplanation(q.getExplanation());

            UserAnswer a = answerMap.get(q.getId());
            if (a != null) {
                detail.setUserAnswer(a.getUserAnswer());
                detail.setIsCorrect(a.getIsCorrect());
                if (a.getIsCorrect() != null) {
                    if (a.getIsCorrect()) correct++;
                    else wrong++;
                } else {
                    unanswered++;
                }
            } else {
                detail.setUserAnswer(null);
                detail.setIsCorrect(null);
                unanswered++;
            }
            questionDetails.add(detail);
        }

        questionDetails.sort(Comparator.comparingInt(d -> d.getSortOrder() != null ? d.getSortOrder() : 0));

        PracticeHistoryDetailVO.Summary summary = new PracticeHistoryDetailVO.Summary();
        summary.setTotalQuestions(allQuestions.size());
        summary.setCorrectCount(correct);
        summary.setWrongCount(wrong);
        summary.setUnansweredCount(unanswered);
        int graded = correct + wrong;
        summary.setAccuracy(graded > 0 ? Math.round((double) correct / graded * 100 * 100.0) / 100.0 : 0);

        vo.setSummary(summary);
        vo.setQuestions(questionDetails);
        return vo;
    }

    private Map<Long, int[]> buildSessionStats(Long userId, List<Long> sessionIds) {
        if (sessionIds.isEmpty()) return Map.of();
        Map<Long, int[]> map = new HashMap<>();
        for (Long sid : sessionIds) {
            List<UserAnswer> answers = userAnswerMapper.selectList(
                new LambdaQueryWrapper<UserAnswer>()
                    .eq(UserAnswer::getSessionId, sid)
                    .eq(UserAnswer::getUserId, userId)
            );
            int total = answers.size();
            int correct = (int) answers.stream().filter(a -> Boolean.TRUE.equals(a.getIsCorrect())).count();
            int wrong = (int) answers.stream().filter(a -> Boolean.FALSE.equals(a.getIsCorrect())).count();
            map.put(sid, new int[]{total, correct, wrong});
        }
        return map;
    }

    private String formatSpecialTitle(PracticeSession session) {
        String date = session.getUpdatedAt() != null
            ? session.getUpdatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
            : "";
        String mod = session.getModule() != null ? session.getModule() : "";
        return date + " " + mod + " 专项练习";
    }
}
