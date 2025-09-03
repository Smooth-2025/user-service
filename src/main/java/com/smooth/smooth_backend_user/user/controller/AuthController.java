package com.smooth.smooth_backend_user.user.controller;

import com.smooth.smooth_backend_user.user.dto.request.LoginRequestDto;
import com.smooth.smooth_backend_user.user.dto.request.RegisterRequestDto;
import com.smooth.smooth_backend_user.user.dto.request.SendVerificationRequestDto;
import com.smooth.smooth_backend_user.user.dto.request.VerifyEmailRequestDto;
import com.smooth.smooth_backend_user.user.dto.request.AdminLoginRequestDto;
import com.smooth.smooth_backend_user.user.dto.response.*;
import com.smooth.smooth_backend_user.global.common.ApiResponse;
import com.smooth.smooth_backend_user.user.service.EmailVerificationService;
import com.smooth.smooth_backend_user.user.service.UserService;
import com.smooth.smooth_backend_user.user.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/users/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final EmailVerificationService emailVerificationService;
    private final AuthService authService;



    @PostMapping("/send-verification")
    public ResponseEntity<ApiResponse<SendVerificationResponseDto>> sendVerificationCode(
            @Validated @RequestBody SendVerificationRequestDto dto) {

        emailVerificationService.sendVerificationCode(dto.getEmail());

        SendVerificationResponseDto response = SendVerificationResponseDto.success(
                dto.getEmail(),
                180 // 3분
        );
        return ResponseEntity.ok(
                ApiResponse.success("인증코드 발송 완료", response)
        );
    }

    @PostMapping("/verify-email")
    public ResponseEntity<ApiResponse<VerifyEmailResponseDto>> verifyEmailCode(
            @Validated @RequestBody VerifyEmailRequestDto dto) {

        emailVerificationService.verifyCode(dto.getEmail(), dto.getCode());
        VerifyEmailResponseDto response = VerifyEmailResponseDto.success(dto.getEmail());

        return ResponseEntity.ok(
                ApiResponse.success("이메일 인증 완료", response)
        );
    }

    @GetMapping("/check-email")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> checkEmailDuplicate(@RequestParam String email) {
        boolean isDuplicate = userService.isEmailExists(email);
        Map<String, Boolean> response = new HashMap<>();
        response.put("isDuplicate", isDuplicate);

        return ResponseEntity.ok(
                ApiResponse.success("이메일 중복 확인 완료", response)
        );
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<RegisterResponseDto>> register(@Validated @RequestBody RegisterRequestDto dto,
                                                                     HttpServletResponse response) {
        RegisterResponseDto result = authService.register(dto, response);
        return ResponseEntity.ok(
                ApiResponse.success("회원가입이 완료되었습니다.", result)
        );
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponseDto>> login(@Validated @RequestBody LoginRequestDto dto,
                                                               HttpServletResponse response) {
        LoginResponseDto result = authService.login(dto, response);
        return ResponseEntity.ok(
                ApiResponse.success("로그인 성공", result)
        );
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletResponse response) {
        authService.logout(response);
        return ResponseEntity.ok(
                ApiResponse.success("로그아웃이 완료되었습니다.")
        );
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<RefreshTokenResponseDto>> refreshToken(
            HttpServletRequest request, HttpServletResponse response) {
        
        RefreshTokenResponseDto result = authService.refreshToken(request, response);
        return ResponseEntity.ok(
                ApiResponse.success("토큰 재발급 완료", result)
        );
    }

    @PostMapping("/admin-login")
    public ResponseEntity<ApiResponse<AdminLoginResponseDto>> adminLogin(
            @Validated @RequestBody AdminLoginRequestDto dto, HttpServletResponse response) {
        
        AdminLoginResponseDto result = authService.adminLogin(dto, response);
        return ResponseEntity.ok(
                ApiResponse.success("관리자 로그인 성공", result)
        );
    }

    @PostMapping("/admin-refresh")
    public ResponseEntity<ApiResponse<AdminRefreshTokenResponseDto>> adminRefreshToken(
            HttpServletRequest request, HttpServletResponse response) {
        
        AdminRefreshTokenResponseDto result = authService.adminRefreshToken(request, response);
        return ResponseEntity.ok(
                ApiResponse.success("관리자 토큰 재발급 완료", result)
        );
    }

    @PostMapping("/admin-logout")
    public ResponseEntity<ApiResponse<Void>> adminLogout(HttpServletResponse response) {
        authService.adminLogout(response);
        return ResponseEntity.ok(
                ApiResponse.success("관리자 로그아웃이 완료되었습니다.")
        );
    }
}