package com.smooth.smooth_backend_user.user.service;

import com.smooth.smooth_backend_user.global.config.JwtTokenProvider;
import com.smooth.smooth_backend_user.global.exception.BusinessException;
import com.smooth.smooth_backend_user.global.util.CookieUtils;
import com.smooth.smooth_backend_user.user.dto.request.AdminLoginRequestDto;
import com.smooth.smooth_backend_user.user.dto.request.LoginRequestDto;
import com.smooth.smooth_backend_user.user.dto.request.RegisterRequestDto;
import com.smooth.smooth_backend_user.user.dto.response.AdminLoginResponseDto;
import com.smooth.smooth_backend_user.user.dto.response.AdminRefreshTokenResponseDto;
import com.smooth.smooth_backend_user.user.dto.response.LoginResponseDto;
import com.smooth.smooth_backend_user.user.dto.response.RefreshTokenResponseDto;
import com.smooth.smooth_backend_user.user.dto.response.RegisterResponseDto;
import com.smooth.smooth_backend_user.user.entity.User;
import com.smooth.smooth_backend_user.user.entity.UserRole;
import com.smooth.smooth_backend_user.user.exception.UserErrorCode;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final UserService userService;
    private final EmailVerificationService emailVerificationService;
    private final JwtTokenProvider jwtTokenProvider;
    private final CookieUtils cookieUtils;


     // 회원가입
     public RegisterResponseDto register(RegisterRequestDto dto, HttpServletResponse response) {
        // 이메일 인증 완료 여부 확인
        if (!emailVerificationService.isEmailVerified(dto.getEmail())) {
            throw new BusinessException(UserErrorCode.EMAIL_NOT_VERIFIED);
        }
        // 회원가입 처리
        User user = userService.register(dto);

        // 회원가입 완료 후 이메일 인증 상태 삭제
        emailVerificationService.clearVerificationStatus(dto.getEmail());

        log.info("회원가입 완료 - 사용자 ID: {}, 이메일: {}", user.getId(), user.getEmail());

        // 회원가입 완료 후 토큰은 발급하지 않음 (로그인 시에만 발급)
        return RegisterResponseDto.success(
                user.getId(),
                user.getName(),
                null // 토큰 없음
        );
    }
     // 로그인
    public LoginResponseDto login(LoginRequestDto dto, HttpServletResponse response) {
        // 사용자 로그인 검증
        User user = userService.login(dto);

        // JWT 토큰 생성 및 쿠키 설정
        String accessToken = jwtTokenProvider.createAccessToken(user.getId(), user.getEmail());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId(), user.getEmail());
        cookieUtils.setRefreshTokenCookie(response, refreshToken);

        log.info("로그인 성공 - 사용자 ID: {}, 이메일: {}", user.getId(), user.getEmail());
        log.info("액세스 토큰 만료 시간: {}ms, 리프레시 토큰 만료 시간: {}ms", 
                jwtTokenProvider.getAccessTokenExpirationTime(), 
                jwtTokenProvider.getRefreshTokenExpirationTime());

        return LoginResponseDto.success(
                user.getId(),
                user.getName(),
                accessToken
        );
    }

   // 관리자 로그인
    public AdminLoginResponseDto adminLogin(AdminLoginRequestDto dto, HttpServletResponse response) {
        // 관리자 로그인 검증
        User user = userService.adminLogin(dto);

        // 관리자 JWT 토큰 생성 및 쿠키 설정
        String accessToken = jwtTokenProvider.createAccessToken(user.getId(), user.getEmail(), user.getRole());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId(), user.getEmail(), user.getRole());
        cookieUtils.setAdminRefreshTokenCookie(response, refreshToken);

        log.info("관리자 로그인 성공 - 사용자 ID: {}, 이메일: {}", user.getId(), user.getEmail());

        return AdminLoginResponseDto.success(user.getName(), accessToken, user.getRole().name());
    }

    // 로그아웃
    public void logout(HttpServletResponse response) {
        cookieUtils.clearRefreshTokenCookie(response);
        log.info("사용자 로그아웃 완료");
    }

    // 관리자 로그아웃
    public void adminLogout(HttpServletResponse response) {
        cookieUtils.clearRefreshTokenCookie(response);
        log.info("관리자 로그아웃 완료");
    }

    // 토큰 재발급
    public RefreshTokenResponseDto refreshToken(HttpServletRequest request, HttpServletResponse response) {
        // 쿠키에서 refresh token 추출
        String refreshToken = cookieUtils.getRefreshTokenFromCookie(request);
        if (refreshToken == null) {
            log.error("리프레시 토큰 재발급 실패: 쿠키에서 리프레시 토큰을 찾을 수 없음");
            throw new BusinessException(UserErrorCode.INVALID_TOKEN, "Refresh token이 없습니다.");
        }

        try {
            // refresh token 검증
            log.info("리프레시 토큰 검증 시작 - 토큰 길이: {}", refreshToken.length());
            Claims claims = jwtTokenProvider.validateRefreshToken(refreshToken);
            Long userId = jwtTokenProvider.getUserIdFromToken(claims);
            log.info("리프레시 토큰 검증 성공 - 사용자 ID: {}", userId);

            // 사용자 존재 여부 확인
            User user = userService.findById(userId);
            
            // 새로운 액세스 토큰 생성 (리프레시 토큰은 재발급하지 않음)
            String newAccessToken = jwtTokenProvider.createAccessToken(user.getId(), user.getEmail());
            
            log.info("액세스 토큰 재발급 - 사용자 ID: {}", userId);

            return RefreshTokenResponseDto.success(
                    newAccessToken,
                    jwtTokenProvider.getAccessTokenExpirationTime() / 1000 // 초 단위로 변환
            );

        } catch (Exception e) {
            log.error("Token refresh failed: {}", e.getMessage());
            throw new BusinessException(UserErrorCode.INVALID_TOKEN, "유효하지 않은 refresh token입니다.");
        }
    }

    // 관리자 토큰 재발급 처리
    public AdminRefreshTokenResponseDto adminRefreshToken(HttpServletRequest request, HttpServletResponse response) {
        // 쿠키에서 refresh token 추출
        String refreshToken = cookieUtils.getRefreshTokenFromCookie(request);
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
            
            // 새로운 관리자 액세스 토큰 생성 (리프레시 토큰은 재발급하지 않음)
            String newAccessToken = jwtTokenProvider.createAccessToken(user.getId(), user.getEmail(), user.getRole());
            
            log.info("관리자 액세스 토큰 재발급 - 사용자 ID: {}", userId);

            return AdminRefreshTokenResponseDto.success(newAccessToken);

        } catch (Exception e) {
            log.error("Admin token refresh failed: {}", e.getMessage());
            throw new BusinessException(UserErrorCode.INVALID_TOKEN, "유효하지 않은 관리자 refresh token입니다.");
        }
    }
}