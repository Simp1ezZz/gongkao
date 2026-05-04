package com.gongkao.controller;

import com.gongkao.common.Result;
import com.gongkao.dto.SessionCreateRequest;
import com.gongkao.dto.SessionUpdateRequest;
import com.gongkao.entity.PracticeSession;
import com.gongkao.service.SessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
public class SessionController {

    private final SessionService sessionService;

    private Result<?> requireAuth(Long userId) {
        return userId == null ? Result.fail(401, "请先登录") : null;
    }

    @PostMapping
    public Result<PracticeSession> createSession(
            @AuthenticationPrincipal Long userId,
            @RequestBody SessionCreateRequest req) {
        Result<?> err = requireAuth(userId); if (err != null) return (Result) err;
        return Result.ok(sessionService.createSession(userId, req));
    }

    @GetMapping
    public Result<List<PracticeSession>> listSessions(
            @AuthenticationPrincipal Long userId) {
        Result<?> err = requireAuth(userId); if (err != null) return (Result) err;
        return Result.ok(sessionService.getUserSessions(userId));
    }

    @GetMapping("/{id}")
    public Result<PracticeSession> getSession(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long id) {
        Result<?> err = requireAuth(userId); if (err != null) return (Result) err;
        return Result.ok(sessionService.getSession(id, userId));
    }

    @PutMapping("/{id}")
    public Result<PracticeSession> updateSession(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long id,
            @RequestBody SessionUpdateRequest req) {
        Result<?> err = requireAuth(userId); if (err != null) return (Result) err;
        return Result.ok(sessionService.updateSession(id, userId, req));
    }

    @PostMapping("/{id}/submit")
    public Result<PracticeSession> submitSession(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long id,
            @RequestBody SessionUpdateRequest req) {
        Result<?> err = requireAuth(userId); if (err != null) return (Result) err;
        return Result.ok(sessionService.submitSession(id, userId, req));
    }
}
