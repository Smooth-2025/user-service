package com.smooth.smooth_backend_user.user.controller;

import
        com.smooth.smooth_backend_user.user.dto.request.LoginRequestDto;
import com.smooth.smooth_backend_user.user.dto.request.RegisterRequestDto;
import com.smooth.smooth_backend_user.user.dto.request.SendVerificationRequestDto;
import com.smooth.smooth_backend_user.user.dto.request.VerifyEmailRequestDto;
import com.smooth.smooth_backend_user.user.dto.request.AdminLoginRequestDto;
import com.smooth.smooth_backend_user.user.dto.response.*;
import com.smooth.smooth_backend_user.user.entity.User;
import com.smooth.smooth_backend_user.user.entity.UserRole;
import com.smooth.smooth_backend_user.user.exception.UserErrorCode;
import com.smooth.smooth_backend_user.global.common.ApiResponse;
import com.smooth.smooth_backend_user.global.exception.BusinessException;
import com.smooth.smooth_backend_user.user.service.EmailVerificationService;
import com.smooth.smooth_backend_user.user.service.UserService;
import com.smooth.smooth_backend_user.global.config.JwtTokenProvider;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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

    // 리프레시 토큰 쿠키 삭제하는 헬퍼 메서드
    private void clearRefreshTokenCookie(HttpServletResponse response) {
        Cookie refreshCookie = new Cookie("refreshToken", null);
        refreshCookie.setHttpOnly(true);
        refreshCookie.setSecure(cookieSecure);
        refreshCookie.setPath("/");
        refreshCookie.setMaxAge(0); // 즉시 만료
        refreshCookie.setAttribute("SameSite", "None");
        response.addCookie(refreshCookie);
        log.info("리프레시 토큰 쿠키 삭제 완료");
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
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletResponse response) {
        // 리프레시 토큰 쿠키 삭제
        clearRefreshTokenCookie(response);
        
        log.info("사용자 로그아웃 완료");
        return ResponseEntity.ok(
                ApiResponse.success("로그아웃이 완료되었습니다. 클라이언트에서 액세스 토큰을 삭제해 주세요.")
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

            // 사용자 존재 여부 확인
            User user = userService.findById(userId);
            
            // 새로운 액세스 토큰 생성
            String newAccessToken = jwtTokenProvider.createAccessToken(user.getId(), user.getEmail());
            
            // 리프레시 토큰 재발급 여부 확인 (10분 미만 남았을 때만)
            boolean needsRefreshRenewal = jwtTokenProvider.needsRefreshTokenRenewal(claims);
            
            if (needsRefreshRenewal) {
                // 리프레시 토큰도 새로 발급
                String newRefreshToken = jwtTokenProvider.createRefreshToken(user.getId(), user.getEmail());
                setRefreshTokenCookie(response, newRefreshToken);
                log.info("리프레시 토큰도 재발급 완료 - 사용자 ID: {}", userId);
            }
            // 리프레시 토큰이 충분히 남아있으면 기존 것 재사용 (쿠키 업데이트 안함)

            // 응답 생성
            RefreshTokenResponseDto refreshResponse = RefreshTokenResponseDto.success(
                    newAccessToken,
                    jwtTokenProvider.getAccessTokenExpirationTime() / 1000 // 초 단위로 변환
            );

            String message = needsRefreshRenewal ? 
                    "액세스 토큰과 리프레시 토큰이 재발급되었습니다." : 
                    "액세스 토큰이 재발급되었습니다.";
                    
            return ResponseEntity.ok(
                    ApiResponse.success(message, refreshResponse)
            );

        } catch (Exception e) {
            log.error("Token refresh failed: {}", e.getMessage());
            throw new BusinessException(UserErrorCode.INVALID_TOKEN, "유효하지 않은 refresh token입니다.");
        }
    }

    @PostMapping("/admin-login")
    public ResponseEntity<ApiResponse<LoginResponseDto>> adminLogin(
            @Validated @RequestBody AdminLoginRequestDto dto, HttpServletResponse response) {
        
        User user = userService.adminLogin(dto);

        // JWT 토큰 생성 (관리자용 - 60분 + 12시간)
        String accessToken = jwtTokenProvider.createAccessToken(user.getId(), user.getEmail(), user.getRole());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId(), user.getEmail(), user.getRole());

        // Refresh Token을 HttpOnly 쿠키로 설정 (12시간)
        Cookie refreshCookie = new Cookie("refreshToken", refreshToken);
        refreshCookie.setHttpOnly(true);
        refreshCookie.setSecure(cookieSecure);
        refreshCookie.setPath("/");
        refreshCookie.setMaxAge(12 * 60 * 60); // 12시간
        refreshCookie.setAttribute("SameSite", "None");
        response.addCookie(refreshCookie);

        // Access Token을 JSON으로 응답
        LoginResponseDto loginResponse = LoginResponseDto.success(
                user.getId(),
                user.getName(),
                accessToken
        );

        return ResponseEntity.ok(
                ApiResponse.success("관리자 로그인 성공", loginResponse)
        );
    }

    @PostMapping("/admin-refresh")
    public ResponseEntity<ApiResponse<RefreshTokenResponseDto>> adminRefreshToken(
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

            // 사용자 존재 여부 및 관리자 권한 확인
            User user = userService.findById(userId);
            if (user.getRole() != UserRole.ADMIN) {
                throw new BusinessException(UserErrorCode.ACCESS_DENIED, "관리자 권한이 필요합니다.");
            }
            
            // 새로운 관리자 액세스 토큰 생성
            String newAccessToken = jwtTokenProvider.createAccessToken(user.getId(), user.getEmail(), user.getRole());
            
            // 리프레시 토큰 재발급 여부 확인 (10분 미만 남았을 때만)
            boolean needsRefreshRenewal = jwtTokenProvider.needsRefreshTokenRenewal(claims);
            
            if (needsRefreshRenewal) {
                // 리프레시 토큰도 새로 발급 (관리자용 12시간)
                String newRefreshToken = jwtTokenProvider.createRefreshToken(user.getId(), user.getEmail(), user.getRole());
                
                // 새로운 refresh token을 쿠키에 설정 (12시간)
                Cookie refreshCookie = new Cookie("refreshToken", newRefreshToken);
                refreshCookie.setHttpOnly(true);
                refreshCookie.setSecure(cookieSecure);
                refreshCookie.setPath("/");
                refreshCookie.setMaxAge(12 * 60 * 60); // 12시간
                refreshCookie.setAttribute("SameSite", "None");
                response.addCookie(refreshCookie);
                
                log.info("관리자 리프레시 토큰도 재발급 완료 - 사용자 ID: {}", userId);
            }
            // 리프레시 토큰이 충분히 남아있으면 기존 것 재사용 (쿠키 업데이트 안함)

            // 응답 생성
            RefreshTokenResponseDto refreshResponse = RefreshTokenResponseDto.success(
                    newAccessToken,
                    3600 // 관리자 액세스 토큰 60분 = 3600초
            );

            String message = needsRefreshRenewal ? 
                    "관리자 액세스 토큰과 리프레시 토큰이 재발급되었습니다." : 
                    "관리자 액세스 토큰이 재발급되었습니다.";
                    
            return ResponseEntity.ok(
                    ApiResponse.success(message, refreshResponse)
            );

        } catch (Exception e) {
            log.error("Admin token refresh failed: {}", e.getMessage());
            throw new BusinessException(UserErrorCode.INVALID_TOKEN, "유효하지 않은 관리자 refresh token입니다.");
        }
    }
}