package com.smooth.smooth_backend_user.user.service;

import com.smooth.smooth_backend_user.user.exception.UserErrorCode;
import com.smooth.smooth_backend_user.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailVerificationService {

    private final RedisService redisService;
    private final EmailService emailService;
    private final UserService userService;

    // 인증코드 관련 상수
    private static final int CODE_LENGTH = 5;           // 인증넘버 자리수
    private static final int EXPIRATION_MINUTES = 3;    // 인증 제한시간
    
    // Redis 키 프리픽스
    private static final String CODE_PREFIX = "email_verification:";
    private static final String VERIFIED_PREFIX = "email_verified:";
    private static final String SEND_LIMIT_PREFIX = "email_send_limit:";
    
    // 발송 제한 관련 상수
    private static final int MAX_SEND_COUNT = 3;
    private static final int SEND_LIMIT_MINUTES = 10;
    
    // 시스템 관련 상수
    private static final int REDIS_RETRY_DELAY_MS = 1000;
    private static final int VERIFICATION_STATUS_MINUTES = 30;
    private static final int SECONDS_PER_MINUTE = 60;

    // 인증코드 생성
    private String generateVerificationCode() {
        SecureRandom random = new SecureRandom();
        StringBuilder code = new StringBuilder();

        for (int i = 0; i < CODE_LENGTH; i++) {
            code.append(random.nextInt(10));
        }

        return code.toString();
    }

    // 발송 횟수 제한 체크
    private void validateSendLimit(String email) {
        String limitKey = SEND_LIMIT_PREFIX + email;
        String countStr = redisService.getStringValue(limitKey);
        
        // Redis 장애 시 (countStr == null이고 Redis가 정상이었다면 "0"이 반환됨)
        if (countStr == null) {
            log.warn("Redis 연결 불가 - 발송 제한 확인 불가: {}", email);
            // Redis 장애 시에도 이메일 발송은 허용 (하지만 제한 기능은 동작하지 않음)
            return;
        }
        
        int count = Integer.parseInt(countStr);
        if (count >= MAX_SEND_COUNT) {
            log.warn("이메일 발송 횟수 초과: {} ({}회)", email, count);
            throw new BusinessException(UserErrorCode.EMAIL_SEND_LIMIT_EXCEEDED,
                    String.format("%d분 후에 다시 시도해주세요.", SEND_LIMIT_MINUTES));
        }
    }

    // 발송 횟수 증가
    private void incrementSendCount(String email) {
        String limitKey = SEND_LIMIT_PREFIX + email;
        String countStr = redisService.getStringValue(limitKey);
        int count = countStr != null ? Integer.parseInt(countStr) : 0;

        redisService.setStringValue(limitKey, String.valueOf(count + 1), SEND_LIMIT_MINUTES * SECONDS_PER_MINUTE);
    }

    // 인증코드 발송
    public void sendVerificationCode(String email) {
        validateEmailForVerification(email);
        String verificationCode = generateAndStoreVerificationCode(email);
        sendVerificationEmail(email, verificationCode);
    }

    // 이메일 검증 (발송 제한 + 중복 체크)
    private void validateEmailForVerification(String email) {
        validateSendLimit(email);
        validateEmailNotExists(email);
    }

    // 이메일 중복 체크
    private void validateEmailNotExists(String email) {
        if (userService.isEmailExists(email)) {
            throw new BusinessException(UserErrorCode.EMAIL_ALREADY_EXISTS);
        }
    }

    // 인증코드 생성 및 Redis 저장
    private String generateAndStoreVerificationCode(String email) {
        String verificationCode = generateVerificationCode();
        log.info("인증코드 생성 완료: {} ({}자리)", email, CODE_LENGTH);
        
        storeVerificationCodeInRedis(email, verificationCode);
        return verificationCode;
    }

    // Redis에 인증코드 저장 (재시도 로직 포함)
    private void storeVerificationCodeInRedis(String email, String verificationCode) {
        String codeStorageKey = CODE_PREFIX + email;
        
        try {
            redisService.setStringValue(codeStorageKey, verificationCode, EXPIRATION_MINUTES * SECONDS_PER_MINUTE);
            log.debug("Redis에 인증코드 저장 완료: {} ({})", email, codeStorageKey);
        } catch (Exception e) {
            log.error("Redis 연결 오류 - 인증코드 저장 실패: {}, Error: {}", email, e.getMessage());
            retryStoreVerificationCode(email, verificationCode, codeStorageKey);
        }
    }

    // Redis 저장 재시도
    private void retryStoreVerificationCode(String email, String verificationCode, String codeStorageKey) {
        try {
            Thread.sleep(REDIS_RETRY_DELAY_MS);
            redisService.setStringValue(codeStorageKey, verificationCode, EXPIRATION_MINUTES * SECONDS_PER_MINUTE);
            log.info("Redis 재연결 후 인증코드 저장 성공: {}", email);
        } catch (Exception retryException) {
            log.error("Redis 재시도도 실패: {}", email, retryException);
            throw new BusinessException(UserErrorCode.EMAIL_SEND_LIMIT_EXCEEDED,
                    "시스템 오류로 인증코드 발송에 실패했습니다. 잠시 후 다시 시도해주세요.");
        }
    }

    // 인증코드 이메일 발송
    private void sendVerificationEmail(String email, String verificationCode) {
        try {
            emailService.sendVerificationEmail(email, verificationCode);
            log.info("인증코드 이메일 발송 완료: {}", email);
            
            incrementSendCount(email);
            log.info("인증코드 발송 완료: {}", email);
        } catch (Exception e) {
            log.error("인증코드 발송 실패: {}", email, e);
            throw new BusinessException(UserErrorCode.EMAIL_SEND_LIMIT_EXCEEDED, "이메일 발송에 실패했습니다.");
        }
    }

    // 인증코드 검증
    public boolean verifyCode(String email, String inputCode) {
        String normalizedCode = validateAndNormalizeInputCode(email, inputCode);
        String storedCode = getStoredVerificationCode(email);
        validateCodeMatch(email, normalizedCode, storedCode);
        
        cleanupAfterSuccessfulVerification(email);
        markEmailAsVerified(email);
        
        log.info("이메일 인증 성공: {}", email);
        return true;
    }

    // 입력 코드 검증 및 정규화( 공백 제거 - 사용자가 공백을 복붙할 상황 대비 ), Null 체크
    private String validateAndNormalizeInputCode(String email, String inputCode) {
        if (inputCode == null || inputCode.trim().isEmpty()) {
            log.warn("빈 인증코드 입력: {}", email);
            throw new BusinessException(UserErrorCode.VERIFICATION_CODE_MISMATCH, "인증코드를 입력해주세요.");
        }
        
        String normalizedCode = inputCode.trim();
        
        if (!normalizedCode.matches("\\d{" + CODE_LENGTH + "}")) {
            log.warn("잘못된 인증코드 형식: {} (입력: {})", email, normalizedCode);
            throw new BusinessException(UserErrorCode.VERIFICATION_CODE_MISMATCH, 
                String.format("인증코드는 %d자리 숫자여야 합니다.", CODE_LENGTH));
        }
        
        return normalizedCode;
    }

    // Redis에서 저장된 인증코드 조회
    private String getStoredVerificationCode(String email) {
        String codeStorageKey = CODE_PREFIX + email;
        
        try {
            String storedCode = redisService.getStringValue(codeStorageKey);
            log.debug("Redis에서 인증코드 조회 시도: {} (저장된 코드 존재: {})", email, storedCode != null);
            return storedCode;
        } catch (Exception e) {
            log.error("Redis 연결 오류 - 인증코드 조회 실패: {}, Error: {}", email, e.getMessage());
            throw new BusinessException(UserErrorCode.VERIFICATION_CODE_EXPIRED, 
                "시스템 오류로 인증이 실패했습니다. 잠시 후 다시 시도해주세요.");
        }
    }

    // 인증코드 일치 검증
    private void validateCodeMatch(String email, String inputCode, String storedCode) {
        if (storedCode == null) {
            log.warn("인증코드 만료 또는 존재하지 않음: {}", email);
            throw new BusinessException(UserErrorCode.VERIFICATION_CODE_EXPIRED, 
                "인증코드가 만료되었습니다. 새로운 인증코드를 요청해주세요.");
        }

        if (!storedCode.equals(inputCode)) {
            log.warn("인증코드 불일치: {} (입력: {}, 저장: {})", email, inputCode, storedCode);
            throw new BusinessException(UserErrorCode.VERIFICATION_CODE_MISMATCH, "인증코드가 일치하지 않습니다.");
        }
    }

    // 인증 성공 후 정리 작업
    private void cleanupAfterSuccessfulVerification(String email) {
        String codeStorageKey = CODE_PREFIX + email;
        
        try {
            redisService.deleteValue(codeStorageKey);
            log.debug("인증코드 삭제 완료: {}", email);
        } catch (Exception e) {
            log.warn("Redis 연결 오류 - 인증코드 삭제 실패: {}, Error: {}", email, e.getMessage());
            // 인증은 성공했으므로 계속 진행
        }
    }

    // 이메일 인증 완료 상태 저장
    private void markEmailAsVerified(String email) {
        try {
            String verificationStatusKey = VERIFIED_PREFIX + email;
            redisService.setStringValue(verificationStatusKey, "true", 
                VERIFICATION_STATUS_MINUTES * SECONDS_PER_MINUTE);
            log.debug("이메일 인증 상태 저장 완료: {}", email);
        } catch (Exception e) {
            log.error("Redis 연결 오류 - 인증 상태 저장 실패: {}, Error: {}", email, e.getMessage());
            throw new BusinessException(UserErrorCode.VERIFICATION_CODE_EXPIRED, 
                "인증은 성공했지만 상태 저장에 실패했습니다. 다시 인증해주세요.");
        }
    }

    // 이메일 인증 여부 확인
    public boolean isEmailVerified(String email) {
        String verificationStatusKey = VERIFIED_PREFIX + email;
        String verificationStatus = redisService.getStringValue(verificationStatusKey);
        
        // Redis 장애 시 인증되지 않은 것으로 처리 (보안상 안전한 방향)
        if (verificationStatus == null) {
            log.warn("이메일 인증 상태 확인 실패 (Redis 연결 실패 가능성): {}", email);
            return false;
        }
        
        return "true".equals(verificationStatus);
    }

    // 이메일 인증 상태 삭제 (회원가입 완료 후 호출)
    public void clearVerificationStatus(String email) {
        String verificationStatusKey = VERIFIED_PREFIX + email;
        redisService.deleteValue(verificationStatusKey);
        log.info("이메일 인증 상태 삭제 완료: {}", email);
    }
}