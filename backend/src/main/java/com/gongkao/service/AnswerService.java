package com.gongkao.service;

import com.gongkao.dto.*;
import com.gongkao.entity.Question;
import com.gongkao.entity.PracticeSession;
import com.gongkao.entity.UserAnswer;
import com.gongkao.mapper.PracticeSessionMapper;
import com.gongkao.mapper.QuestionMapper;
import com.gongkao.mapper.UserAnswerMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnswerService {

    private final UserAnswerMapper userAnswerMapper;
    private final QuestionMapper questionMapper;
    private final PracticeSessionMapper sessionMapper;

    @Transactional
    public BatchAnswerResultVO batchSubmit(Long userId, BatchAnswerRequest req) {
        List<BatchAnswerRequest.AnswerItem> items = req.getAnswers();
        if (items == null) items = List.of();

        Set<Long> answeredIds = items.stream()
                .map(BatchAnswerRequest.AnswerItem::getQuestionId)
                .collect(Collectors.toSet());

        Map<Long, Question> answeredQuestionMap = answeredIds.isEmpty() ? Map.of()
                : questionMapper.selectBatchIds(answeredIds).stream()
                .collect(Collectors.toMap(Question::getId, q -> q));

        int correctCount = 0;
        int wrongCount = 0;
        Map<Long, QuestionWithAnswerVO> resultMap = new LinkedHashMap<>();

        for (BatchAnswerRequest.AnswerItem item : items) {
            Question question = answeredQuestionMap.get(item.getQuestionId());
            if (question == null) continue;

            Boolean isCorrect = null;
            String correctAnswer = question.getAnswer();
            String userAns = item.getAnswer();

            if ("single_choice".equals(question.getType())
                    || "multi_choice".equals(question.getType())) {
                if (userAns != null && correctAnswer != null) {
                    isCorrect = userAns.trim().equalsIgnoreCase(correctAnswer.trim());
                }
            }

            if (isCorrect != null) {
                if (isCorrect) correctCount++;
                else wrongCount++;
            }

            UserAnswer ua = new UserAnswer();
            ua.setUserId(userId);
            ua.setPaperId(question.getPaperId());
            ua.setQuestionId(question.getId());
            ua.setSessionId(req.getSessionId());
            ua.setUserAnswer(userAns);
            ua.setIsCorrect(isCorrect);
            userAnswerMapper.insert(ua);

            resultMap.put(question.getId(), QuestionWithAnswerVO.from(question, userAns, isCorrect));
        }

        // 通过 session 获取 paperId，查出试卷所有题目（含未答题）
        Long paperId = null;
        if (req.getSessionId() != null) {
            PracticeSession session = sessionMapper.selectById(req.getSessionId());
            if (session != null) paperId = session.getPaperId();
        }

        int totalQuestions;
        if (paperId != null) {
            List<Question> allQuestions = questionMapper.selectByPaperId(paperId);
            totalQuestions = allQuestions.size();
            for (Question q : allQuestions) {
                if (!resultMap.containsKey(q.getId())) {
                    resultMap.put(q.getId(), QuestionWithAnswerVO.from(q, null, null));
                }
            }
        } else {
            totalQuestions = items.size();
        }

        int graded = correctCount + wrongCount;
        double accuracy = graded > 0 ? (double) correctCount / graded * 100 : 0;

        BatchAnswerResultVO result = new BatchAnswerResultVO();
        result.setSessionId(req.getSessionId());
        result.setTotalQuestions(totalQuestions);
        result.setCorrectCount(correctCount);
        result.setWrongCount(wrongCount);
        result.setAccuracy(Math.round(accuracy * 100.0) / 100.0);
        result.setQuestions(new ArrayList<>(resultMap.values()));
        return result;
    }

    public List<QuestionWithAnswerVO> getMyAnswers(Long userId, Long paperId) {
        List<UserAnswer> answers = userAnswerMapper.selectByUserPaper(userId, paperId);
        if (answers.isEmpty()) return List.of();

        List<Long> questionIds = answers.stream()
                .map(UserAnswer::getQuestionId)
                .collect(Collectors.toList());
        List<Question> questions = questionMapper.selectBatchIds(questionIds);
        Map<Long, Question> questionMap = questions.stream()
                .collect(Collectors.toMap(Question::getId, q -> q));

        return answers.stream()
                .map(ua -> {
                    Question q = questionMap.get(ua.getQuestionId());
                    return q != null ? QuestionWithAnswerVO.from(q, ua.getUserAnswer(), ua.getIsCorrect()) : null;
                })
                .filter(v -> v != null)
                .collect(Collectors.toList());
    }
}
