package com.gongkao.controller;

import com.gongkao.common.Result;
import com.gongkao.dto.BatchAnswerRequest;
import com.gongkao.dto.BatchAnswerResultVO;
import com.gongkao.dto.QuestionWithAnswerVO;
import com.gongkao.service.AnswerService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/papers")
@RequiredArgsConstructor
public class AnswerController {

    private final AnswerService answerService;

    @PostMapping("/user-answers/batch")
    public Result<BatchAnswerResultVO> batchSubmit(
            @AuthenticationPrincipal Long userId,
            @RequestBody BatchAnswerRequest req) {
        if (userId == null) return Result.fail(401, "请先登录");
        return Result.ok(answerService.batchSubmit(userId, req));
    }

    @GetMapping("/{id}/my-answers")
    public Result<List<QuestionWithAnswerVO>> getMyAnswers(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long id) {
        if (userId == null) return Result.fail(401, "请先登录");
        return Result.ok(answerService.getMyAnswers(userId, id));
    }
}
