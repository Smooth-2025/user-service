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

    private static final int CODE_LENGTH = 5;
    private static final int EXPIRATION_MINUTES = 3;
    private static final String CODE_PREFIX = "email_verification:";
    private static final String VERIFIED_PREFIX = "email_verified:";
    private static final String SEND_LIMIT_PREFIX = "email_send_limit:";
    private static final int MAX_SEND_COUNT = 3;
    private static final int SEND_LIMIT_MINUTES = 10;

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

        redisService.setStringValue(limitKey, String.valueOf(count + 1), SEND_LIMIT_MINUTES * 60);
    }

    // 인증코드 발송
    public void sendVerificationCode(String email) {

        validateSendLimit(email); // 발송 횟수 제한 체크

        // 이미 가입된 이메일인지 확인
        if (userService.isEmailExists(email)) {
            throw new BusinessException(UserErrorCode.EMAIL_ALREADY_EXISTS);
        }

        // 인증코드 생성
        String verificationCode = generateVerificationCode();
        log.info("인증코드 생성 완료: {} (5자리)", email);

        // Redis에 인증코드 저장 (3분 만료)
        String redisKey = CODE_PREFIX + email;
        try {
            redisService.setStringValue(redisKey, verificationCode, EXPIRATION_MINUTES * 60);
            log.debug("Redis에 인증코드 저장 완료: {} ({})", email, redisKey);
        } catch (Exception e) {
            log.error("Redis 연결 오류 - 인증코드 저장 실패: {}, Error: {}", email, e.getMessage());
            throw new BusinessException(UserErrorCode.EMAIL_SEND_LIMIT_EXCEEDED, 
                "시스템 오류로 인증코드 발송에 실패했습니다. 잠시 후 다시 시도해주세요.");
        }

        // 이메일 발송
        try {
            emailService.sendVerificationEmail(email, verificationCode);
            log.info("인증코드 이메일 발송 완료: {}", email);

            // 발송 횟수 증가 (Redis 장애 시에도 시도)
            incrementSendCount(email);

            log.info("인증코드 발송 완료: {}", email);
        } catch (Exception e) {
            log.error("인증코드 발송 실패: {}", email, e);
            throw new BusinessException(UserErrorCode.EMAIL_SEND_LIMIT_EXCEEDED, "이메일 발송에 실패했습니다.");
        }
    }

    // 인증코드 검증
    public boolean verifyCode(String email, String inputCode) {
        // 입력값 정규화 (공백 제거, null 체크)
        if (inputCode == null || inputCode.trim().isEmpty()) {
            log.warn("빈 인증코드 입력: {}", email);
            throw new BusinessException(UserErrorCode.VERIFICATION_CODE_MISMATCH, "인증코드를 입력해주세요.");
        }
        
        String normalizedInputCode = inputCode.trim();
        
        // 인증코드 형식 검증 (5자리 숫자)
        if (!normalizedInputCode.matches("\\d{5}")) {
            log.warn("잘못된 인증코드 형식: {} (입력: {})", email, normalizedInputCode);
            throw new BusinessException(UserErrorCode.VERIFICATION_CODE_MISMATCH, "인증코드는 5자리 숫자여야 합니다.");
        }
        
        String redisKey = CODE_PREFIX + email;
        String storedCode = null;
        
        try {
            storedCode = redisService.getStringValue(redisKey);
            log.debug("Redis에서 인증코드 조회 시도: {} (저장된 코드 존재: {})", email, storedCode != null);
        } catch (Exception e) {
            log.error("Redis 연결 오류 - 인증코드 조회 실패: {}, Error: {}", email, e.getMessage());
            throw new BusinessException(UserErrorCode.VERIFICATION_CODE_EXPIRED, 
                "시스템 오류로 인증이 실패했습니다. 잠시 후 다시 시도해주세요.");
        }

        if (storedCode == null) {
            log.warn("인증코드 만료 또는 존재하지 않음: {}", email);
            throw new BusinessException(UserErrorCode.VERIFICATION_CODE_EXPIRED, 
                "인증코드가 만료되었습니다. 새로운 인증코드를 요청해주세요.");
        }

        if (!storedCode.equals(normalizedInputCode)) {
            log.warn("인증코드 불일치: {} (입력: {}, 저장: {})", email, normalizedInputCode, storedCode);
            throw new BusinessException(UserErrorCode.VERIFICATION_CODE_MISMATCH, "인증코드가 일치하지 않습니다.");
        }

        // 인증 성공 시 인증코드 삭제
        try {
            redisService.deleteValue(redisKey);
            log.debug("인증코드 삭제 완료: {}", email);
        } catch (Exception e) {
            log.warn("Redis 연결 오류 - 인증코드 삭제 실패: {}, Error: {}", email, e.getMessage());
            // 인증은 성공했으므로 계속 진행
        }

        // 인증 완료 표시 (회원가입 시까지 유지)
        try {
            String verifiedKey = VERIFIED_PREFIX + email;
            redisService.setStringValue(verifiedKey, "true", 30 * 60); // 30분 유지
            log.debug("이메일 인증 상태 저장 완료: {}", email);
        } catch (Exception e) {
            log.error("Redis 연결 오류 - 인증 상태 저장 실패: {}, Error: {}", email, e.getMessage());
            throw new BusinessException(UserErrorCode.VERIFICATION_CODE_EXPIRED, 
                "인증은 성공했지만 상태 저장에 실패했습니다. 다시 인증해주세요.");
        }

        log.info("이메일 인증 성공: {}", email);
        return true;
    }

    // 이메일 인증 여부 확인
    public boolean isEmailVerified(String email) {
        String verifiedKey = VERIFIED_PREFIX + email;
        String verified = redisService.getStringValue(verifiedKey);
        
        // Redis 장애 시 (verified == null) 인증되지 않은 것으로 처리
        // 보안상 Redis 연결 실패 시 인증을 허용하지 않음
        if (verified == null) {
            log.warn("이메일 인증 상태 확인 실패 (Redis 연결 실패 가능성): {}", email);
            return false;
        }
        
        return "true".equals(verified);
    }

    // 이메일 인증 상태 삭제 (회원가입 완료 후 호출)
    public void clearVerificationStatus(String email) {
        String verifiedKey = VERIFIED_PREFIX + email;
        redisService.deleteValue(verifiedKey);
        log.info("이메일 인증 상태 삭제 완료: {}", email);
    }
}