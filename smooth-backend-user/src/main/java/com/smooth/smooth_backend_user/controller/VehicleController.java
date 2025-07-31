package com.smooth.smooth_backend_user.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/vehicle")
public class VehicleController {

    // 임시 메모리 저장소 (나중에 Redis나 DB로 변경)
    private static final Map<String, Object> qrSessions = new ConcurrentHashMap<>();
    private static final Map<Long, String> userVehicleMap = new ConcurrentHashMap<>();

    @PostMapping("/qr-generate")
    public ResponseEntity<?> generateQR(@RequestBody Map<String, Object> request) {
        try {
            String vehicleId = (String) request.get("vehicleId");
            String vehicleType = (String) request.get("vehicleType");

            // QR 세션 토큰 생성
            String sessionToken = UUID.randomUUID().toString();

            // 3분 후 만료되도록 설정
            Map<String, Object> sessionData = new HashMap<>();
            sessionData.put("vehicleId", vehicleId);
            sessionData.put("vehicleType", vehicleType);
            sessionData.put("createdAt", System.currentTimeMillis());
            sessionData.put("expiresAt", System.currentTimeMillis() + 180000); // 3분

            qrSessions.put(sessionToken, sessionData);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("sessionToken", sessionToken);
            response.put("qrData", sessionToken);
            response.put("expiresIn", 300); // 5분

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/connect")
    public ResponseEntity<?> connectVehicle(@RequestBody Map<String, String> request) {
        try {
            // 현재 인증된 사용자 정보
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String userIdStr = (String) auth.getPrincipal();
            Long userId = Long.valueOf(userIdStr);

            String sessionToken = request.get("sessionToken");

            // 세션 토큰 검증
            Map<String, Object> sessionData = (Map<String, Object>) qrSessions.get(sessionToken);
            if (sessionData == null) {
                throw new RuntimeException("유효하지 않은 QR 코드입니다.");
            }

            // 만료 시간 확인
            Long expiresAt = (Long) sessionData.get("expiresAt");
            if (System.currentTimeMillis() > expiresAt) {
                qrSessions.remove(sessionToken);
                throw new RuntimeException("QR 코드가 만료되었습니다.");
            }

            // 차량과 사용자 연동
            String vehicleId = (String) sessionData.get("vehicleId");
            userVehicleMap.put(userId, vehicleId);

            // 사용된 세션 토큰 제거
            qrSessions.remove(sessionToken);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "차량 연동이 완료되었습니다.");
            response.put("vehicleId", vehicleId);
            response.put("userId", userId);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/status")
    public ResponseEntity<?> getVehicleStatus() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String userIdStr = (String) auth.getPrincipal();
            Long userId = Long.valueOf(userIdStr);

            String vehicleId = userVehicleMap.get(userId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("connected", vehicleId != null);
            response.put("vehicleId", vehicleId);
            response.put("userId", userId);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @DeleteMapping("/disconnect")
    public ResponseEntity<?> disconnectVehicle() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String userIdStr = (String) auth.getPrincipal();
            Long userId = Long.valueOf(userIdStr);

            userVehicleMap.remove(userId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "차량 연동이 해제되었습니다.");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}