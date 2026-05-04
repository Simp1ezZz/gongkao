package com.gongkao.controller;

import com.gongkao.common.Result;
import com.gongkao.dto.*;
import com.gongkao.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/send-code")
    public Result<Void> sendCode(@Valid @RequestBody SendCodeRequest req) {
        authService.sendCode(req);
        return Result.ok();
    }

    @PostMapping("/register")
    public Result<AuthResponse> register(@Valid @RequestBody RegisterRequest req) {
        return Result.ok(authService.register(req));
    }

    @PostMapping("/login-password")
    public Result<AuthResponse> login(@Valid @RequestBody LoginRequest req) {
        return Result.ok(authService.login(req));
    }

    @PostMapping("/reset-password")
    public Result<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest req) {
        authService.resetPassword(req);
        return Result.ok();
    }

    @PostMapping("/refresh")
    public Result<AuthResponse> refresh(@Valid @RequestBody RefreshRequest req) {
        return Result.ok(authService.refresh(req));
    }

    @PostMapping("/logout")
    public Result<Void> logout(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            authService.logout(token);
        }
        return Result.ok();
    }
}
