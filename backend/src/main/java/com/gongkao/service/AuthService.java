package com.gongkao.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gongkao.dto.*;
import com.gongkao.entity.User;
import com.gongkao.mapper.UserMapper;
import com.gongkao.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final EmailService emailService;
    private final StringRedisTemplate redisTemplate;

    public void sendCode(SendCodeRequest req) {
        if ("register".equals(req.getType())) {
            User existing = userMapper.selectOne(
                    new LambdaQueryWrapper<User>().eq(User::getEmail, req.getEmail()));
            if (existing != null) {
                throw new RuntimeException("该邮箱已注册");
            }
        } else if ("reset_password".equals(req.getType())) {
            User existing = userMapper.selectOne(
                    new LambdaQueryWrapper<User>().eq(User::getEmail, req.getEmail()));
            if (existing == null) {
                throw new RuntimeException("该邮箱未注册");
            }
        }
        emailService.sendVerificationCode(req.getEmail(), req.getType());
    }

    public AuthResponse register(RegisterRequest req) {
        if (!emailService.verifyCode(req.getEmail(), "register", req.getCode())) {
            throw new RuntimeException("验证码错误或已过期");
        }

        User existing = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getEmail, req.getEmail()));
        if (existing != null) {
            throw new RuntimeException("该邮箱已注册");
        }

        User user = new User();
        user.setEmail(req.getEmail());
        user.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        user.setNickname("用户" + System.currentTimeMillis() % 100000);
        userMapper.insert(user);

        return generateAuthResponse(user);
    }

    public AuthResponse login(LoginRequest req) {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getEmail, req.getEmail()));
        if (user == null || !passwordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
            throw new RuntimeException("邮箱或密码错误");
        }

        return generateAuthResponse(user);
    }

    public void resetPassword(ResetPasswordRequest req) {
        if (!emailService.verifyCode(req.getEmail(), "reset_password", req.getCode())) {
            throw new RuntimeException("验证码错误或已过期");
        }

        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getEmail, req.getEmail()));
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        user.setPasswordHash(passwordEncoder.encode(req.getNewPassword()));
        userMapper.updateById(user);
    }

    public AuthResponse refresh(RefreshRequest req) {
        try {
            String type = jwtUtil.getTokenType(req.getRefreshToken());
            if (!"refresh".equals(type)) {
                throw new RuntimeException("无效的refresh token");
            }

            Long userId = jwtUtil.getUserIdFromToken(req.getRefreshToken());

            long remaining = jwtUtil.getTokenRemainingTime(req.getRefreshToken());
            if (remaining > 0) {
                redisTemplate.opsForValue().set(
                        "token:blacklist:" + req.getRefreshToken(), "1", remaining, TimeUnit.MILLISECONDS);
            }

            User user = userMapper.selectById(userId);
            if (user == null) {
                throw new RuntimeException("用户不存在");
            }

            return generateAuthResponse(user);
        } catch (Exception e) {
            throw new RuntimeException("refresh token无效或已过期");
        }
    }

    public void logout(String accessToken) {
        long remaining = jwtUtil.getTokenRemainingTime(accessToken);
        if (remaining > 0) {
            redisTemplate.opsForValue().set(
                    "token:blacklist:" + accessToken, "1", remaining, TimeUnit.MILLISECONDS);
        }
    }

    private AuthResponse generateAuthResponse(User user) {
        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getEmail());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId());
        return new AuthResponse(accessToken, refreshToken,
                user.getId(), user.getEmail(), user.getNickname(), user.getAvatar());
    }
}
