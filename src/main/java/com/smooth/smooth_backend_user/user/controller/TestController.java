package com.smooth.smooth_backend_user.user.controller;

import com.smooth.smooth_backend_user.global.common.ApiResponse;
import com.smooth.smooth_backend_user.user.service.RedisService;
import com.smooth.smooth_backend_user.global.auth.GatewayUserDetails;
import com.smooth.smooth_backend_user.global.exception.BusinessException;
import com.smooth.smooth_backend_user.user.exception.UserErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/users/test")
@RequiredArgsConstructor
public class TestController {

    private final RedisService redisService;

    // 현재 인증된 사용자 ID 가져오는 헬퍼 메서드
    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof GatewayUserDetails) {
            GatewayUserDetails userDetails = (GatewayUserDetails) authentication.getPrincipal();
            return userDetails.getUserId();
        }
        throw new BusinessException(UserErrorCode.INVALID_TOKEN, "인증되지 않은 사용자입니다.");
    }

    @GetMapping("/protected")
    public ResponseEntity<?> protectedEndpoint() {
        Long userId = getCurrentUserId();

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "JWT 인증 성공!");
        response.put("userId", userId);
        response.put("timestamp", System.currentTimeMillis());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/health/redis")
    public ResponseEntity<ApiResponse<Map<String, Object>>> redisHealthCheck() {
        Map<String, Object> healthInfo = new HashMap<>();
        
        try {
            // Redis 연결 테스트
            String testKey = "health_check_" + System.currentTimeMillis();
            redisService.setStringValue(testKey, "test", 10);
            String value = redisService.getStringValue(testKey);
            redisService.deleteValue(testKey);
            
            boolean isConnected = "test".equals(value);
            
            healthInfo.put("status", isConnected ? "UP" : "DOWN");
            healthInfo.put("redis", isConnected ? "연결됨" : "연결 실패");
            healthInfo.put("timestamp", System.currentTimeMillis());
            
            if (isConnected) {
                return ResponseEntity.ok(
                    ApiResponse.success("Redis 상태 확인 완료", healthInfo)
                );
            } else {
                return ResponseEntity.status(503).body(
                    ApiResponse.error(null, "Redis 연결 실패", healthInfo)
                );
            }
            
        } catch (Exception e) {
            healthInfo.put("status", "DOWN");
            healthInfo.put("redis", "연결 실패");
            healthInfo.put("error", e.getMessage());
            healthInfo.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.status(503).body(
                ApiResponse.error(null, "Redis 헬스체크 실패", healthInfo)
            );
        }
    }
}