package com.smooth.smooth_backend_user.controller;

import com.smooth.smooth_backend_user.config.JwtTokenProvider;
import com.smooth.smooth_backend_user.dto.LoginRequestDto;
import com.smooth.smooth_backend_user.dto.RegisterRequestDto;
import com.smooth.smooth_backend_user.dto.response.CommonResponseDto;
import com.smooth.smooth_backend_user.dto.response.LoginResponseDto;
import com.smooth.smooth_backend_user.dto.response.RegisterResponseDto;
import com.smooth.smooth_backend_user.entity.User;
import com.smooth.smooth_backend_user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.SimpleTimeZone;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;

    //jwt토큰 블랙리스트
    private static final Set<String> tokenBlacklist = ConcurrentHashMap.newKeySet();

    // 토큰 추출
    private String getTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    // 토큰 블랙리스트 확인 (다른 클래스에서 씀)
    public static boolean isTokenBlacklisted(String token) {
        return tokenBlacklist.contains(token);
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

                // 토큰을 블랙리스트에 추가
                tokenBlacklist.add(token);
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

            String token = getTokenFromRequest(request);
            if (token != null) {
                tokenBlacklist.add(token);
            }
            return ResponseEntity.ok(CommonResponseDto.success("회원탈퇴가 완료되었습니다."));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(CommonResponseDto.error(e.getMessage()));
        }
    }
}