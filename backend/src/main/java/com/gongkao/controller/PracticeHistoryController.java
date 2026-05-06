package com.gongkao.controller;

import com.gongkao.common.Result;
import com.gongkao.dto.PageResult;
import com.gongkao.dto.PracticeHistoryDetailVO;
import com.gongkao.dto.PracticeHistoryItemVO;
import com.gongkao.dto.PracticeHistoryQuery;
import com.gongkao.dto.PracticeHistorySummaryVO;
import com.gongkao.service.PracticeHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/practice/history")
@RequiredArgsConstructor
public class PracticeHistoryController {

    private final PracticeHistoryService historyService;

    @GetMapping("/summary")
    public Result<PracticeHistorySummaryVO> getSummary(
            @AuthenticationPrincipal Long userId) {
        if (userId == null) return Result.fail(401, "请先登录");
        return Result.ok(historyService.getSummary(userId));
    }

    @GetMapping
    public Result<PageResult<PracticeHistoryItemVO>> listHistory(
            @AuthenticationPrincipal Long userId,
            PracticeHistoryQuery query) {
        if (userId == null) return Result.fail(401, "请先登录");
        return Result.ok(historyService.listHistory(userId, query));
    }

    @GetMapping("/{sessionId}")
    public Result<PracticeHistoryDetailVO> getDetail(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long sessionId) {
        if (userId == null) return Result.fail(401, "请先登录");
        return Result.ok(historyService.getDetail(userId, sessionId));
    }
}
