package com.smooth.smooth_backend_user.user.controller;

import com.smooth.smooth_backend_user.user.dto.request.LoginRequestDto;
import com.smooth.smooth_backend_user.user.dto.request.RegisterRequestDto;
import com.smooth.smooth_backend_user.user.dto.request.SendVerificationRequestDto;
import com.smooth.smooth_backend_user.user.dto.request.VerifyEmailRequestDto;
import com.smooth.smooth_backend_user.user.dto.response.*;
import com.smooth.smooth_backend_user.user.entity.User;
import com.smooth.smooth_backend_user.user.exception.UserErrorCode;
import com.smooth.smooth_backend_user.global.common.ApiResponse;
import com.smooth.smooth_backend_user.global.exception.BusinessException;
import com.smooth.smooth_backend_user.user.service.EmailVerificationService;
import com.smooth.smooth_backend_user.user.service.UserService;
import com.smooth.smooth_backend_user.global.config.JwtTokenProvider;
import com.smooth.smooth_backend_user.global.auth.GatewayUserDetails;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import com.smooth.smooth_backend_user.global.auth.GatewayAuthenticationHelper;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
    private final GatewayAuthenticationHelper gatewayAuthHelper;
    private final JwtTokenProvider jwtTokenProvider;

    @Value("${app.cookie.secure:false}")
    private boolean cookieSecure;

    // 쿠키에서 refresh token 추출하는 헬퍼 메서드
    private String getRefreshTokenFromCookie(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return null;
        }
        
        for (Cookie cookie : request.getCookies()) {
            if ("refreshToken".equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    // 쿠키에 refresh token 설정하는 헬퍼 메서드
    private void setRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        Cookie refreshCookie = new Cookie("refreshToken", refreshToken);
        refreshCookie.setHttpOnly(true);
        refreshCookie.setSecure(cookieSecure);
        refreshCookie.setPath("/");
        refreshCookie.setMaxAge(14 * 24 * 60 * 60); // 14일
        refreshCookie.setAttribute("SameSite", "None");
        response.addCookie(refreshCookie);
    }

    // 현재 인증된 사용자 ID 가져오는 헬퍼 메서드
    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof GatewayUserDetails) {
            GatewayUserDetails userDetails = (GatewayUserDetails) authentication.getPrincipal();
            return userDetails.getUserId();
        }
        throw new BusinessException(UserErrorCode.INVALID_TOKEN, "인증되지 않은 사용자입니다.");
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

        // JWT 토큰 생성
        String accessToken = jwtTokenProvider.createAccessToken(user.getId(), user.getEmail());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId(), user.getEmail());

        // Refresh Token을 HttpOnly 쿠키로 설정
        setRefreshTokenCookie(response, refreshToken);

        RegisterResponseDto registerResponse = RegisterResponseDto.success(
                user.getId(),
                user.getName(),
                accessToken
        );

        return ResponseEntity.ok(
                ApiResponse.success("회원가입이 완료되었습니다.", registerResponse)
        );
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponseDto>> login(@Validated @RequestBody LoginRequestDto dto,
                                                               HttpServletResponse response) {
        User user = userService.login(dto);

        // JWT 토큰 생성
        String accessToken = jwtTokenProvider.createAccessToken(user.getId(), user.getEmail());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId(), user.getEmail());

        // Refresh Token을 HttpOnly 쿠키로 설정
        setRefreshTokenCookie(response, refreshToken);

        // Access Token을 JSON으로 응답
        LoginResponseDto loginResponse = LoginResponseDto.success(
                user.getId(),
                user.getName(),
                accessToken
        );

        return ResponseEntity.ok(
                ApiResponse.success("로그인 성공", loginResponse)
        );
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletRequest request, HttpServletResponse response) {
        return ResponseEntity.ok(
                ApiResponse.success("로그아웃이 완료되었습니다.")
        );
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<RefreshTokenResponseDto>> refreshToken(
            HttpServletRequest request, HttpServletResponse response) {
        
        // 쿠키에서 refresh token 추출
        String refreshToken = getRefreshTokenFromCookie(request);
        if (refreshToken == null) {
            throw new BusinessException(UserErrorCode.INVALID_TOKEN, "Refresh token이 없습니다.");
        }

        try {
            // refresh token 검증
            Claims claims = jwtTokenProvider.validateRefreshToken(refreshToken);
            Long userId = jwtTokenProvider.getUserIdFromToken(claims);
            String email = jwtTokenProvider.getEmailFromToken(claims);

            // 사용자 존재 여부 확인
            User user = userService.findById(userId);
            
            // 새로운 토큰들 생성
            String newAccessToken = jwtTokenProvider.createAccessToken(user.getId(), user.getEmail());
            String newRefreshToken = jwtTokenProvider.createRefreshToken(user.getId(), user.getEmail());

            // 새로운 refresh token을 쿠키에 설정
            setRefreshTokenCookie(response, newRefreshToken);

            // 응답 생성
            RefreshTokenResponseDto refreshResponse = RefreshTokenResponseDto.success(
                    newAccessToken,
                    jwtTokenProvider.getAccessTokenExpirationTime() / 1000 // 초 단위로 변환
            );

            return ResponseEntity.ok(
                    ApiResponse.success("토큰이 재발급되었습니다.", refreshResponse)
            );

        } catch (Exception e) {
            log.error("Token refresh failed: {}", e.getMessage());
            throw new BusinessException(UserErrorCode.INVALID_TOKEN, "유효하지 않은 refresh token입니다.");
        }
    }

    @DeleteMapping("/account")
    public ResponseEntity<ApiResponse<Void>> deleteAccount() {
        Long userId = getCurrentUserId();
        userService.deleteAccount(userId);

        return ResponseEntity.ok(
                ApiResponse.success("회원탈퇴가 완료되었습니다.")
        );
    }
}