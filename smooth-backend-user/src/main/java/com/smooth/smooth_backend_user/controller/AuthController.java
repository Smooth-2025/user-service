package com.smooth.smooth_backend_user.controller;

import com.smooth.smooth_backend_user.config.JwtTokenProvider;
import com.smooth.smooth_backend_user.dto.request.LoginRequestDto;
import com.smooth.smooth_backend_user.dto.request.RegisterRequestDto;
import com.smooth.smooth_backend_user.dto.response.CommonResponseDto;
import com.smooth.smooth_backend_user.dto.response.LoginResponseDto;
import com.smooth.smooth_backend_user.dto.response.RegisterResponseDto;
import com.smooth.smooth_backend_user.entity.User;
import com.smooth.smooth_backend_user.service.RedisService;
import com.smooth.smooth_backend_user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;
    private final RedisService redisService;

    // 토큰 추출
    private String getTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    // 토큰 블랙리스트 확인 (다른 클래스에서 씀)
    public boolean isTokenBlacklisted(String token) {
        return redisService.isTokenBlacklisted(token);
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterResponseDto> register(@Validated @RequestBody RegisterRequestDto dto) {
        try {
            User user = userService.register(dto);

            //회원가입 후 자동로그인 ( jwt token 생성 )
            String token = jwtTokenProvider.createToken(user.getId(), user.getEmail());

            RegisterResponseDto response = RegisterResponseDto.success(
                    user.getId(),
                    user.getName(),
                    token
            );
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(RegisterResponseDto.error(e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDto> login(@Validated @RequestBody LoginRequestDto dto) {
        try {
            User user = userService.login(dto);

            String token = jwtTokenProvider.createToken(user.getId(), user.getEmail());

            LoginResponseDto response = LoginResponseDto.success(
                    user.getId(),
                    user.getName(),
                    token
            );

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(LoginResponseDto.error(e.getMessage()));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<CommonResponseDto> logout(HttpServletRequest request) {
        try {
            // Authorization 헤더에서 토큰 추출
            String token = getTokenFromRequest(request);

            if (token != null && jwtTokenProvider.validateToken(token)) {

                // jwt 만료시간까지 redis에 블랙리스트 저장
                long remainingTime = jwtTokenProvider.getExpirationTime(token) - System.currentTimeMillis();
                if (remainingTime > 0) {
                    redisService.addToBlacklist(token, remainingTime / 1000);
                }

                return ResponseEntity.ok(CommonResponseDto.success("로그아웃이 완료되었습니다."));
            } else {
                return ResponseEntity.badRequest()
                        .body(CommonResponseDto.error("유효하지 않은 토큰입니다."));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(CommonResponseDto.error(e.getMessage()));
        }
    }

    @DeleteMapping("/account")
    public ResponseEntity<CommonResponseDto> deleteAccount(HttpServletRequest request) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String userIdStr = (String) auth.getPrincipal();
            Long userId = Long.valueOf(userIdStr);

            userService.deleteAccount(userId);

            // 사용자 관련 redis 데이터 정리
            redisService.removeUserVehicle(userId);

            String token = getTokenFromRequest(request);
            if (token != null) {
                long remainingTime = jwtTokenProvider.getExpirationTime(token) - System.currentTimeMillis();
                if (remainingTime > 0) {
                    redisService.addToBlacklist(token, remainingTime / 1000);
                }
            }
            return ResponseEntity.ok(CommonResponseDto.success("회원탈퇴가 완료되었습니다."));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(CommonResponseDto.error(e.getMessage()));
        }
    }
}