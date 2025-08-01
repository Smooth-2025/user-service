package com.smooth.smooth_backend_user.controller;

import com.smooth.smooth_backend_user.dto.response.CommonResponseDto;
import com.smooth.smooth_backend_user.dto.response.QrGenerateResponseDto;
import com.smooth.smooth_backend_user.dto.response.VehicleResponseDto;
import com.smooth.smooth_backend_user.service.RedisService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/vehicle")
@RequiredArgsConstructor
public class VehicleController {

    // redis service 추가
    private final RedisService redisService;

    @PostMapping("/qr-generate")
    public ResponseEntity<QrGenerateResponseDto> generateQR(@RequestBody Map<String, Object> request) {
        try {
            String vehicleId = (String) request.get("vehicleId");
            String vehicleType = (String) request.get("vehicleType");

            // QR 세션 토큰 생성
            String sessionToken = UUID.randomUUID().toString();

            // 3분 후 만료되도록 설정
            Map<String, Object> sessionData = new ConcurrentHashMap<>();
            sessionData.put("vehicleId", vehicleId);
            sessionData.put("vehicleType", vehicleType);
            sessionData.put("createdAt", System.currentTimeMillis());
            sessionData.put("expiresAt", System.currentTimeMillis() + 180000); // 3분

            redisService.setQRSession(sessionToken, sessionData, 180);

            QrGenerateResponseDto response = QrGenerateResponseDto.success(
                    sessionToken,
                    sessionToken,
                    180
            );

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(QrGenerateResponseDto.error(e.getMessage()));
        }
    }

    @PostMapping("/connect")
    public ResponseEntity<VehicleResponseDto> connectVehicle(@RequestBody Map<String, String> request) {
        try {
            // 현재 인증된 사용자 정보
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String userIdStr = (String) auth.getPrincipal();
            Long userId = Long.valueOf(userIdStr);

            String sessionToken = request.get("sessionToken");

            // 세션 토큰 검증
            Map<String, Object> sessionData = (Map<String, Object>) redisService.getQRSession(sessionToken);
            if (sessionData == null) {
                return ResponseEntity.badRequest()
                        .body(VehicleResponseDto.error("유효하지 않은 QR 코드입니다."));
            }

            // 만료 시간 확인
            Long expiresAt = (Long) sessionData.get("expiresAt");
            if (System.currentTimeMillis() > expiresAt) {
                redisService.removeQRSession(sessionToken);
                return ResponseEntity.badRequest()
                        .body(VehicleResponseDto.error("QR 코드가 만료되었습니다."));
            }

            // 차량과 사용자 연동
            String vehicleId = (String) sessionData.get("vehicleId");
            redisService.setUserVehicle(userId, vehicleId);

            // 사용된 세션 토큰 제거
            redisService.removeQRSession(sessionToken);

            VehicleResponseDto response = VehicleResponseDto.connectSuccess(vehicleId, userId);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(VehicleResponseDto.error(e.getMessage()));
        }
    }

    @GetMapping("/status")
    public ResponseEntity<VehicleResponseDto> getVehicleStatus() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String userIdStr = (String) auth.getPrincipal();
            Long userId = Long.valueOf(userIdStr);

            String vehicleId = redisService.getUserVehicle(userId);
            boolean connected = vehicleId != null;

            VehicleResponseDto response = VehicleResponseDto.statusResponse(
                    vehicleId,
                    userId,
                    connected
            );

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(VehicleResponseDto.error(e.getMessage()));
        }
    }

    @DeleteMapping("/disconnect")
    public ResponseEntity<VehicleResponseDto> disconnectVehicle() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String userIdStr = (String) auth.getPrincipal();
            Long userId = Long.valueOf(userIdStr);

            redisService.removeUserVehicle(userId);

            VehicleResponseDto response = VehicleResponseDto.disconnectSuccess();

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(VehicleResponseDto.error(e.getMessage()));
        }
    }
}