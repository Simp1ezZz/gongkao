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

import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
                var claims = jwtUtil.parseToken(token);
                String role = claims.get("role", String.class);

                List<SimpleGrantedAuthority> authorities = "admin".equals(role)
                        ? List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
                        : List.of();

                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(userId, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(auth);

                request.setAttribute("userId", userId);
                request.setAttribute("userRole", role);
            } catch (Exception e) {
                log.warn("JWT filter: token parse failed: {}", e.getMessage());
            }
        }

        filterChain.doFilter(request, response);
    }
}
