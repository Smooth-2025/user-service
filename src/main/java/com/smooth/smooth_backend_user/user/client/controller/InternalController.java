package com.smooth.smooth_backend_user.user.client.controller;

import com.smooth.smooth_backend_user.user.client.dto.EmergencyInfoResponse;
import com.smooth.smooth_backend_user.user.client.dto.TraitsBulkResponse;
import com.smooth.smooth_backend_user.user.client.dto.UserTraitResponse;
import com.smooth.smooth_backend_user.user.service.DriveCastService;
import com.smooth.smooth_backend_user.user.exception.UserErrorCode;
import com.smooth.smooth_backend_user.global.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/internal/v1")
@RequiredArgsConstructor
public class InternalController {

    private final DriveCastService driveCastService;

    // 운전자 성향 벌크 조회 (웹서비스-주행페이지)
    // DriveCast → User Service
    @GetMapping("/traits/bulk")
    public ResponseEntity<?> getTraitsBulk(
            @RequestParam(defaultValue = "true") Boolean hasCharacter,
            HttpServletRequest request) {

        log.info("벌크 성향 조회 API 호출: hasCharacter={}", hasCharacter);

        try {
            TraitsBulkResponse response = driveCastService.getBulkTraits(hasCharacter);
            log.info("벌크 성향 조회 API 응답 완료: 총 {}명", response.getData().size());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("벌크 성향 조회 중 오류 발생", e);
            throw new BusinessException(UserErrorCode.TRAIT_SERVICE_UNAVAILABLE);
        }
    }

    // 운전자 성향 단건 조회 (웹서비스-주행페이지)
    // DriveCast → User Service
    @GetMapping("/traits/{userId}")
    public ResponseEntity<?> getUserTrait(@PathVariable String userId, HttpServletRequest request) {
        log.info("단건 성향 조회 API 호출: userId={}", userId);

        try {
            UserTraitResponse response = driveCastService.getUserTrait(userId);
            log.info("단건 성향 조회 API 응답 완료: userId={}, character={}", userId, response.getCharacter());
            return ResponseEntity.ok(response);

        } catch (BusinessException e) {
            log.warn("단건 성향 조회 실패: userId={}, error={}", userId, e.getErrorCode());

            // 에러 응답 형식
            if (e.getErrorCode() == UserErrorCode.TRAIT_NOT_FOUND) {
                return ResponseEntity.status(404).body(createErrorResponse("TRAIT_NOT_FOUND", "No character for userId=" + userId));
            } else if (e.getErrorCode() == UserErrorCode.INVALID_USER_ID) {
                return ResponseEntity.status(400).body(createErrorResponse("INVALID_USER_ID", "잘못된 사용자 ID입니다."));
            } else {
                return ResponseEntity.status(503).body(createErrorResponse("UPSTREAM_ERROR", "Trait service unavailable"));
            }
        } catch (Exception e) {
            log.error("단건 성향 조회 중 예상치 못한 오류 발생: userId={}", userId, e);
            return ResponseEntity.status(503).body(createErrorResponse("UPSTREAM_ERROR", "Trait service unavailable"));
        }
    }

    // 운전자 정보 조회 (119 신고용)
    // DriveCast → User Service
    @GetMapping("/users/{userId}/emergency-info")
    public ResponseEntity<?> getEmergencyInfo(@PathVariable String userId, HttpServletRequest request) {

        log.info("응급정보 조회 API 호출: userId={}", userId);

        try {
            EmergencyInfoResponse response = driveCastService.getEmergencyInfo(userId);
            log.info("응급정보 조회 API 응답 완료: userId={}", userId);
            return ResponseEntity.ok(response);

        } catch (BusinessException e) {
            log.warn("응급정보 조회 실패: userId={}, error={}", userId, e.getErrorCode());

            // 에러 응답 형식
            if (e.getErrorCode() == UserErrorCode.USER_NOT_FOUND) {
                return ResponseEntity.status(404).body(createErrorResponse("USER_NOT_FOUND", "사용자를 찾을 수 없습니다."));
            } else if (e.getErrorCode() == UserErrorCode.EMERGENCY_INFO_NOT_AVAILABLE) {
                return ResponseEntity.status(404).body(createErrorResponse("EMERGENCY_INFO_NOT_AVAILABLE", "응급정보가 등록되지 않았습니다."));
            } else if (e.getErrorCode() == UserErrorCode.INVALID_USER_ID) {
                return ResponseEntity.status(400).body(createErrorResponse("INVALID_USER_ID", "잘못된 사용자 ID입니다."));
            } else {
                return ResponseEntity.status(500).body(createErrorResponse("INTERNAL_SERVER_ERROR", "서버 내부 오류가 발생했습니다."));
            }
        } catch (Exception e) {
            log.error("응급정보 조회 중 예상치 못한 오류 발생: userId={}", userId, e);
            return ResponseEntity.status(500).body(createErrorResponse("INTERNAL_SERVER_ERROR", "서버 내부 오류가 발생했습니다."));
        }
    }

    // 에러 응답 생성
    private ErrorResponse createErrorResponse(String code, String message) {
        return ErrorResponse.builder()
                .code(code)
                .message(message)
                .build();
    }

    // 에러 응답용 DTO
    @lombok.Builder
    @lombok.Getter
    @lombok.AllArgsConstructor
    private static class ErrorResponse {
        private String code;
        private String message;
    }
}