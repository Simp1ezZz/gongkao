package com.gongkao.config;

import com.gongkao.util.JwtUtil;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final StringRedisTemplate redisTemplate;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        log.debug("JWT filter: uri={}, header={}", request.getRequestURI(), header != null ? "present" : "null");

        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            try {
                Boolean blacklisted = redisTemplate.hasKey("token:blacklist:" + token);
                if (Boolean.TRUE.equals(blacklisted)) {
                    response.setStatus(401);
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write("{\"code\":401,\"success\":false,\"message\":\"Token已失效\"}");
                    return;
                }

                Long userId = jwtUtil.getUserIdFromToken(token);
                log.debug("JWT filter: extracted userId={}", userId);
                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(userId, null, new ArrayList<>());
                SecurityContextHolder.getContext().setAuthentication(auth);
                log.debug("JWT filter: auth set, authenticated={}", auth.isAuthenticated());

                request.setAttribute("userId", userId);
            } catch (Exception e) {
                log.warn("JWT filter: token parse failed: {}", e.getMessage());
            }
        }

        filterChain.doFilter(request, response);
    }
}
