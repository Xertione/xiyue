package com.xiyue.auth.controller;

import com.xiyue.auth.dto.*;
import com.xiyue.auth.service.AuthService;
import com.xiyue.auth.service.SmsCodeService;
import com.xiyue.common.result.Result;
import com.xiyue.security.SecurityUserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 认证模块接口。
 *
 * <p>路径前缀 {@code /api/auth}，除 {@code /profile} 外均无需登录。
 * 接口清单见 docs/api.md。
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "认证模块", description = "发送验证码、注册、登录、找回密码、获取个人信息")
public class AuthController {

    private final AuthService authService;
    private final SmsCodeService smsCodeService;
    private final SecurityUserContext securityUserContext;

    @Operation(summary = "发送验证码", description = "把固定验证码 123456 写入 Redis（模拟短信下发），TTL 5 分钟")
    @PostMapping("/sms-code")
    public Result<Void> sendSmsCode(@Valid @RequestBody SmsCodeRequest req) {
        smsCodeService.sendCode(req.getPhone());
        return Result.success();
    }

    @Operation(summary = "注册", description = "手机号 + 密码 + 验证码 + 角色；角色仅允许 USER / AUNT，禁止 ADMIN")
    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody RegisterRequest req) {
        authService.register(req);
        return Result.success();
    }

    @Operation(summary = "密码登录")
    @PostMapping("/login/password")
    public Result<LoginResponse> loginByPassword(@Valid @RequestBody PasswordLoginRequest req) {
        return Result.success(authService.loginByPassword(req.getPhone(), req.getPassword()));
    }

    @Operation(summary = "验证码登录")
    @PostMapping("/login/code")
    public Result<LoginResponse> loginByCode(@Valid @RequestBody CodeLoginRequest req) {
        return Result.success(authService.loginByCode(req.getPhone(), req.getCode()));
    }

    @Operation(summary = "找回密码", description = "手机号 + 验证码 + 新密码")
    @PostMapping("/reset-password")
    public Result<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest req) {
        authService.resetPassword(req.getPhone(), req.getCode(), req.getNewPassword());
        return Result.success();
    }

    @Operation(summary = "获取当前登录用户信息", description = "需携带有效 JWT")
    @GetMapping("/profile")
    public Result<ProfileResponse> profile() {
        Long userId = securityUserContext.getCurrentUserId();
        return Result.success(authService.getProfile(userId));
    }
}
