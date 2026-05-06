package com.gongkao.service;

import com.gongkao.dto.SessionCreateRequest;
import com.gongkao.dto.SessionUpdateRequest;
import com.gongkao.entity.PracticeSession;
import com.gongkao.mapper.PracticeSessionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SessionService {

    private final PracticeSessionMapper sessionMapper;

    public PracticeSession createSession(Long userId, SessionCreateRequest req) {
        // 整卷练习：检查是否有活跃的 session
        if (req.getPaperId() != null) {
            PracticeSession existing = sessionMapper.selectActiveByUserPaper(userId, req.getPaperId());
            if (existing != null) {
                return existing;
            }
        }

        PracticeSession session = new PracticeSession();
        session.setUserId(userId);
        session.setPaperId(req.getPaperId());
        session.setModule(req.getModule());
        session.setStatus("ongoing");
        session.setTimeElapsed(0);
        session.setCurrentIndex(0);
        session.setAnswers("[]");
        sessionMapper.insert(session);
        return session;
    }

    public PracticeSession getSession(Long sessionId, Long userId) {
        PracticeSession session = sessionMapper.selectById(sessionId);
        if (session == null || !session.getUserId().equals(userId)) {
            throw new RuntimeException("会话不存在或无权访问");
        }
        return session;
    }

    public PracticeSession updateSession(Long sessionId, Long userId,
                                          SessionUpdateRequest req) {
        PracticeSession session = getSession(sessionId, userId);

        if (req.getStatus() != null) {
            session.setStatus(req.getStatus());
        }
        if (req.getTimeElapsed() != null) {
            session.setTimeElapsed(req.getTimeElapsed());
        }
        if (req.getCurrentIndex() != null) {
            session.setCurrentIndex(req.getCurrentIndex());
        }
        if (req.getAnswers() != null) {
            session.setAnswers(req.getAnswers());
        }

        sessionMapper.updateById(session);
        return session;
    }

    public PracticeSession submitSession(Long sessionId, Long userId,
                                          SessionUpdateRequest req) {
        PracticeSession session = getSession(sessionId, userId);
        session.setStatus("submitted");
        if (req.getTimeElapsed() != null) {
            session.setTimeElapsed(req.getTimeElapsed());
        }
        if (req.getAnswers() != null) {
            session.setAnswers(req.getAnswers());
        }
        sessionMapper.updateById(session);
        return session;
    }

    public List<PracticeSession> getUserSessions(Long userId) {
        return sessionMapper.selectByUserId(userId);
    }
}
