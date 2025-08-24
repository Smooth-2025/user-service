package com.smooth.smooth_backend_user.user.controller;

import com.smooth.smooth_backend_user.global.config.JwtTokenProvider;
import com.smooth.smooth_backend_user.user.dto.request.LoginRequestDto;
import com.smooth.smooth_backend_user.user.dto.request.RegisterRequestDto;
import com.smooth.smooth_backend_user.user.dto.request.SendVerificationRequestDto;
import com.smooth.smooth_backend_user.user.dto.request.VerifyEmailRequestDto;
import com.smooth.smooth_backend_user.user.dto.response.RefreshTokenResponseDto;
import com.smooth.smooth_backend_user.user.dto.response.*;
import com.smooth.smooth_backend_user.user.entity.User;
import com.smooth.smooth_backend_user.user.exception.UserErrorCode;
import com.smooth.smooth_backend_user.global.common.ApiResponse;
import com.smooth.smooth_backend_user.global.exception.BusinessException;
import com.smooth.smooth_backend_user.user.service.EmailVerificationService;
import com.smooth.smooth_backend_user.user.service.RedisService;
import com.smooth.smooth_backend_user.user.service.UserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${app.cookie.secure:false}")
    private boolean cookieSecure;

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
    public ResponseEntity<ApiResponse<RegisterResponseDto>> register(@Validated @RequestBody RegisterRequestDto dto ,
                                                                     HttpServletResponse response) {
        // 이메일 인증 완료 여부 확인
        if (!emailVerificationService.isEmailVerified(dto.getEmail())) {
            throw new BusinessException(UserErrorCode.EMAIL_NOT_VERIFIED);
        }

        // 회원가입 처리
        User user = userService.register(dto);

        // 회원가입 완료 후 이메일 인증 상태 삭제
        emailVerificationService.clearVerificationStatus(dto.getEmail());

        // access token, refresh token 생성
        String accessToken = jwtTokenProvider.createAccessToken(user.getId(), user.getEmail());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId(), user.getEmail());

        // Refresh Token을 HttpOnly 쿠키로 설정
        Cookie refreshCookie = new Cookie("refreshToken", refreshToken);
        refreshCookie.setHttpOnly(true);
        refreshCookie.setSecure(cookieSecure);
        refreshCookie.setPath("/");
        refreshCookie.setMaxAge(14 * 24 * 60 * 60); //14일
        refreshCookie.setAttribute("SameSite", "Strict");

        response.addCookie(refreshCookie);

        RegisterResponseDto registerResponse = RegisterResponseDto.success(
                user.getId(),
                user.getName(),
                accessToken  // Access Token만
        );

        return ResponseEntity.ok(
                ApiResponse.success("회원가입이 완료되었습니다.", registerResponse)
        );
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponseDto>> login(@Validated @RequestBody LoginRequestDto dto,
                                                               HttpServletResponse response) {
        User user = userService.login(dto);

        String accessToken = jwtTokenProvider.createAccessToken(user.getId(), user.getEmail());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId(), user.getEmail());

        // Refresh Token을 HttpOnly 쿠키로 설정
        Cookie refreshCookie = new Cookie("refreshToken", refreshToken);
        refreshCookie.setHttpOnly(true);        // JS 접근 불가
        refreshCookie.setSecure(cookieSecure);  // 환경별 설정
        refreshCookie.setPath("/");             // 모든 경로에서 사용
        refreshCookie.setMaxAge(14 * 24 * 60 * 60); // 14일
        refreshCookie.setAttribute("SameSite", "Strict"); // CSRF 방어

        response.addCookie(refreshCookie);

        // Access Token만 JSON으로 응답
        LoginResponseDto loginResponse = LoginResponseDto.success(
                user.getId(),
                user.getName(),
                accessToken  // Access Token만
        );

        return ResponseEntity.ok(
                ApiResponse.success("로그인 성공", loginResponse)
        );
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletRequest request, HttpServletResponse response) {

        // Access Token 블랙리스트 처리
        String accessToken = getTokenFromRequest(request);
        if (accessToken != null && jwtTokenProvider.validateToken(accessToken)) {
            String accessJti = jwtTokenProvider.getJti(accessToken);
            long remainingTime = jwtTokenProvider.getExpirationTime(accessToken) - System.currentTimeMillis();
            if (remainingTime > 0) {
                redisService.addAccessTokenToBlacklist(accessJti, remainingTime / 1000);
            }
        }

        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("refreshToken".equals(cookie.getName())) {
                    String refreshToken = cookie.getValue();
                    if (jwtTokenProvider.validateToken(refreshToken)) {
                        String refreshJti = jwtTokenProvider.getJti(refreshToken);
                        long remainingTime = jwtTokenProvider.getExpirationTime(refreshToken) - System.currentTimeMillis();
                        if (remainingTime > 0) {
                            redisService.addRefreshTokenToBlacklist(refreshJti, remainingTime / 1000);
                        }
                    }
                    break;
                }
            }
        }

        // 쿠키 삭제
        Cookie refreshCookie = new Cookie("refreshToken", "");
        refreshCookie.setHttpOnly(true);
        refreshCookie.setSecure(cookieSecure);  // 환경별 설정
        refreshCookie.setPath("/");
        refreshCookie.setMaxAge(0);
        response.addCookie(refreshCookie);

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

        // 🔧 JTI 기반 블랙리스트로 변경
        String token = getTokenFromRequest(request);
        if (token != null && jwtTokenProvider.validateToken(token)) {
            String jti = jwtTokenProvider.getJti(token);  // JTI 추출
            long remainingTime = jwtTokenProvider.getExpirationTime(token) - System.currentTimeMillis();
            if (remainingTime > 0) {
                redisService.addAccessTokenToBlacklist(jti, remainingTime / 1000);  // JTI 기반
            }
        }

        return ResponseEntity.ok(
                ApiResponse.success("회원탈퇴가 완료되었습니다.")
        );
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<RefreshTokenResponseDto>> refreshAccessToken(
            HttpServletRequest request, HttpServletResponse response) {

        // 쿠키에서 Refresh Token 추출
        String refreshToken = getRefreshTokenFromCookie(request);

        if (refreshToken == null) {
            throw new BusinessException(UserErrorCode.INVALID_TOKEN, "Refresh Token이 없습니다.");
        }

        // Refresh Token 유효성 검증
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new BusinessException(UserErrorCode.INVALID_TOKEN, "유효하지 않은 Refresh Token입니다.");
        }

        // 토큰 타입 확인
        String tokenType = jwtTokenProvider.getTokenType(refreshToken);
        if (!"refresh".equals(tokenType)) {
            throw new BusinessException(UserErrorCode.INVALID_TOKEN, "Refresh Token이 아닙니다.");
        }

        // Refresh Token 블랙리스트 확인
        String refreshJti = jwtTokenProvider.getJti(refreshToken);
        if (redisService.isRefreshTokenBlacklisted(refreshJti)) {
            throw new BusinessException(UserErrorCode.INVALID_TOKEN, "만료된 Refresh Token입니다.");
        }

        // 새로운 access token 무효화
        String oldAccessToken = getTokenFromRequest(request);
        if (oldAccessToken != null && jwtTokenProvider.validateToken(oldAccessToken)) {
            String oldAccessJti = jwtTokenProvider.getJti(oldAccessToken);
            long remainingTime = jwtTokenProvider.getExpirationTime(oldAccessToken) - System.currentTimeMillis();
            if (remainingTime > 0) {
                redisService.addAccessTokenToBlacklist(oldAccessJti, remainingTime / 1000);
            }
        }

        //기존 refresh token 무효화
        String oldRefreshJti = jwtTokenProvider.getJti(refreshToken);
        long refreshRemainingTime = jwtTokenProvider.getExpirationTime(refreshToken) - System.currentTimeMillis();
        if (refreshRemainingTime > 0) {
            redisService.addRefreshTokenToBlacklist(oldRefreshJti, refreshRemainingTime / 1000);
        }

        // 새로운 Access Token 발급
        Long userId = jwtTokenProvider.getUserId(refreshToken);
        String email = jwtTokenProvider.getEmail(refreshToken);
        String newAccessToken = jwtTokenProvider.createAccessToken(userId, email);

        // 새로운 Refresh Token 발급 (로테이션)
        String newRefreshToken = jwtTokenProvider.createRefreshToken(userId, email);

        // 새로운 refresh token 을 쿠키로 설정
        Cookie newRefreshCookie = new Cookie("refreshToken", newRefreshToken);
        newRefreshCookie.setHttpOnly(true);
        newRefreshCookie.setSecure(cookieSecure);  // 환경별 설정
        newRefreshCookie.setPath("/");
        newRefreshCookie.setMaxAge(14 * 24 * 60 * 60); // 14일
        newRefreshCookie.setAttribute("SameSite", "Strict");
        response.addCookie(newRefreshCookie);

        // 응답 데이터 생성
        long expiresIn = 15 * 60; // 15분
        RefreshTokenResponseDto refreshResponse = RefreshTokenResponseDto.success(newAccessToken, expiresIn);

        return ResponseEntity.ok(
                ApiResponse.success("토큰 갱신 완료", refreshResponse)
        );
    }

    // 쿠키에서 Refresh Token 추출하는 헬퍼 메소드
    private String getRefreshTokenFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("refreshToken".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}