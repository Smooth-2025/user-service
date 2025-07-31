package com.smooth.smooth_backend_user.controller;

import com.smooth.smooth_backend_user.config.JwtTokenProvider;
import com.smooth.smooth_backend_user.dto.LoginRequestDto;
import com.smooth.smooth_backend_user.dto.RegisterRequestDto;
import com.smooth.smooth_backend_user.entity.User;
import com.smooth.smooth_backend_user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;

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
}