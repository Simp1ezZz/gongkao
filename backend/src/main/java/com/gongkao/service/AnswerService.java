package com.gongkao.service;

import com.gongkao.dto.*;
import com.gongkao.entity.Question;
import com.gongkao.entity.UserAnswer;
import com.gongkao.mapper.QuestionMapper;
import com.gongkao.mapper.UserAnswerMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnswerService {

    private final UserAnswerMapper userAnswerMapper;
    private final QuestionMapper questionMapper;

    @Transactional
    public BatchAnswerResultVO batchSubmit(Long userId, BatchAnswerRequest req) {
        List<BatchAnswerRequest.AnswerItem> items = req.getAnswers();
        if (items == null || items.isEmpty()) {
            BatchAnswerResultVO result = new BatchAnswerResultVO();
            result.setSessionId(req.getSessionId());
            result.setTotalQuestions(0);
            result.setCorrectCount(0);
            result.setWrongCount(0);
            result.setAccuracy(0);
            result.setQuestions(List.of());
            return result;
        }

        List<Long> questionIds = items.stream()
                .map(BatchAnswerRequest.AnswerItem::getQuestionId)
                .collect(Collectors.toList());

        List<Question> questions = questionMapper.selectBatchIds(questionIds);
        Map<Long, Question> questionMap = questions.stream()
                .collect(Collectors.toMap(Question::getId, q -> q));

        int correctCount = 0;
        int wrongCount = 0;
        List<QuestionWithAnswerVO> resultQuestions = new ArrayList<>();

        for (BatchAnswerRequest.AnswerItem item : items) {
            Question question = questionMap.get(item.getQuestionId());
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

            resultQuestions.add(QuestionWithAnswerVO.from(question, userAns, isCorrect));
        }

        int total = items.size();
        int graded = correctCount + wrongCount;
        double accuracy = graded > 0 ? (double) correctCount / graded * 100 : 0;

        BatchAnswerResultVO result = new BatchAnswerResultVO();
        result.setSessionId(req.getSessionId());
        result.setTotalQuestions(total);
        result.setCorrectCount(correctCount);
        result.setWrongCount(wrongCount);
        result.setAccuracy(Math.round(accuracy * 100.0) / 100.0);
        result.setQuestions(resultQuestions);
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
