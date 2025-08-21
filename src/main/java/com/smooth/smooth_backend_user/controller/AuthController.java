package com.smooth.smooth_backend_user.controller;


import com.smooth.smooth_backend_user.config.JwtTokenProvider;
import com.smooth.smooth_backend_user.dto.request.LoginRequestDto;
import com.smooth.smooth_backend_user.dto.request.RegisterRequestDto;
import com.smooth.smooth_backend_user.dto.request.SendVerificationRequestDto;
import com.smooth.smooth_backend_user.dto.request.VerifyEmailRequestDto;
import com.smooth.smooth_backend_user.dto.response.*;
import com.smooth.smooth_backend_user.entity.User;
import com.smooth.smooth_backend_user.exception.AuthErrorCode;
import com.smooth.smooth_backend_user.global.common.ApiResponse;
import com.smooth.smooth_backend_user.global.exception.BusinessException;
import com.smooth.smooth_backend_user.service.EmailVerificationService;
import com.smooth.smooth_backend_user.service.RedisService;
import com.smooth.smooth_backend_user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;
    private final RedisService redisService;
    private final EmailVerificationService emailVerificationService;

    // Authorization 헤더에서 토큰 추출
    private String getTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    // 토큰 블랙리스트 확인 (다른 클래스에서 사용)
    public boolean isTokenBlacklisted(String token) {
        return redisService.isTokenBlacklisted(token);
    }

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
    public ResponseEntity<ApiResponse<RegisterResponseDto>> register(@Validated @RequestBody RegisterRequestDto dto) {
        // 이메일 인증 완료 여부 확인
        if (!emailVerificationService.isEmailVerified(dto.getEmail())) {
            throw new BusinessException(AuthErrorCode.EMAIL_NOT_VERIFIED);
        }

        // 회원가입 처리
        User user = userService.register(dto);

        // 회원가입 완료 후 이메일 인증 상태 삭제
        emailVerificationService.clearVerificationStatus(dto.getEmail());

        // 회원가입 후 자동로그인 (JWT 토큰 생성)
        String token = jwtTokenProvider.createToken(user.getId(), user.getEmail());

        RegisterResponseDto response = RegisterResponseDto.success(
                user.getId(),
                user.getName(),
                token
        );

        return ResponseEntity.ok(
                ApiResponse.success("회원가입이 완료되었습니다.", response)
        );
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponseDto>> login(@Validated @RequestBody LoginRequestDto dto) {
        User user = userService.login(dto);
        String token = jwtTokenProvider.createToken(user.getId(), user.getEmail());

        LoginResponseDto response = LoginResponseDto.success(
                user.getId(),
                user.getName(),
                token
        );

        return ResponseEntity.ok(
                ApiResponse.success("로그인 성공", response)
        );
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletRequest request) {
        // Authorization 헤더에서 토큰 추출
        String token = getTokenFromRequest(request);

        if (token == null || !jwtTokenProvider.validateToken(token)) {
            throw new BusinessException(AuthErrorCode.INVALID_TOKEN);
        }

        // JWT 만료시간까지 Redis에 블랙리스트 저장
        long remainingTime = jwtTokenProvider.getExpirationTime(token) - System.currentTimeMillis();
        if (remainingTime > 0) {
            redisService.addToBlacklist(token, remainingTime / 1000);
        }

        return ResponseEntity.ok(
                ApiResponse.success("로그아웃이 완료되었습니다.")
        );
    }

    @DeleteMapping("/account")
    public ResponseEntity<ApiResponse<Void>> deleteAccount(HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String userIdStr = (String) auth.getPrincipal();
        Long userId = Long.valueOf(userIdStr);

        userService.deleteAccount(userId);

        // 현재 토큰을 블랙리스트에 추가
        String token = getTokenFromRequest(request);
        if (token != null && jwtTokenProvider.validateToken(token)) {
            long remainingTime = jwtTokenProvider.getExpirationTime(token) - System.currentTimeMillis();
            if (remainingTime > 0) {
                redisService.addToBlacklist(token, remainingTime / 1000);
            }
        }

        return ResponseEntity.ok(
                ApiResponse.success("회원탈퇴가 완료되었습니다.")
        );
    }
}