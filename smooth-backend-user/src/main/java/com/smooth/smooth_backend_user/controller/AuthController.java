package com.smooth.smooth_backend_user.controller;

import com.smooth.smooth_backend_user.config.JwtTokenProvider;
import com.smooth.smooth_backend_user.dto.LoginRequestDto;
import com.smooth.smooth_backend_user.dto.RegisterRequestDto;
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

    // 토큰 블랙리스트 확인 메서드 (다른 클래스에서 사용)
    public static boolean isTokenBlacklisted(String token) {
        return tokenBlacklist.contains(token);
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Validated @RequestBody RegisterRequestDto dto) {
        try {
            User user = userService.register(dto);

            //회원가입 후 자동로그인 ( jwt token 생성 )
            String token = jwtTokenProvider.createToken(user.getId(), user.getEmail());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "회원가입이 완료되었습니다.");
            response.put("userId", user.getId());
            response.put("name", user.getName());
            response.put("token", token);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());

            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Validated @RequestBody LoginRequestDto dto) {
        try {
            User user = userService.login(dto);

            String token = jwtTokenProvider.createToken(user.getId(), user.getEmail());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "로그인 성공");
            response.put("userId", user.getId());
            response.put("name", user.getName());
            response.put("token", token);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());

            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/logout")
    public  ResponseEntity<?> logout(HttpServletRequest request) {
        try {
            // Authorization 헤더에서 토큰 추출
            String token = getTokenFromRequest(request);

            if (token != null && jwtTokenProvider.validateToken(token)) {

                // 토큰을 블랙리스트에 추가
                tokenBlacklist.add(token);

                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "로그아웃이 완료되었습니다.");

                return ResponseEntity.ok(response);
            } else {
                throw new RuntimeException("유효하지 않은 토큰입니다.");
            }
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());

            return ResponseEntity.badRequest().body(response);
        }
    }

    @DeleteMapping("/account")
    public ResponseEntity<?> deleteAccount(HttpServletRequest request){
        try{
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String userIdStr = (String) auth.getPrincipal();
            Long userId = Long.valueOf(userIdStr);

            userService.deleteAccount(userId);

            String token = getTokenFromRequest(request);
            if (token != null) {
                tokenBlacklist.add(token);
            }
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "회원탈퇴가 완료되었습니다.");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());

            return  ResponseEntity.badRequest().body(response);
        }
    }
}