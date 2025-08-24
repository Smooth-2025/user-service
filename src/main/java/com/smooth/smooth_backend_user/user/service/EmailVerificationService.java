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

        // Redis에 인증코드 저장 (3분 만료)
        String redisKey = CODE_PREFIX + email;
        redisService.setStringValue(redisKey, verificationCode, EXPIRATION_MINUTES * 60);
        
        // Redis 저장 실패 시에도 이메일 발송은 진행
        // (사용자가 인증코드를 받기는 하지만, 검증 시 문제가 될 수 있음)

        // 이메일 발송
        try {
            emailService.sendVerificationEmail(email, verificationCode);

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
        String redisKey = CODE_PREFIX + email;
        String storedCode = redisService.getStringValue(redisKey);

        if (storedCode == null) {
            // Redis 장애와 실제 코드 만료를 구분하기 어려운 상황
            // 보안상 Redis 장애 시에는 인증을 허용하지 않음
            log.warn("인증코드 만료 또는 존재하지 않음 (Redis 연결 실패 가능성 포함): {}", email);
            throw new BusinessException(UserErrorCode.VERIFICATION_CODE_EXPIRED, 
                "인증코드가 만료되었거나 시스템 장애입니다. 다시 시도해주세요.");
        }

        if (!storedCode.equals(inputCode)) {
            log.warn("인증코드 불일치: {}", email);
            throw new BusinessException(UserErrorCode.VERIFICATION_CODE_MISMATCH);
        }

        // 인증 성공 시 인증코드 삭제 (Redis 장애 시에도 시도)
        redisService.deleteValue(redisKey);

        // 인증 완료 표시 (회원가입 시까지 유지) - Redis 장애 시에도 시도
        String verifiedKey = VERIFIED_PREFIX + email;
        redisService.setStringValue(verifiedKey, "true", 30 * 60); // 30분 유지

        log.info("이메일 인증 성공: {}", email);
        return true;
    }

    // 이메일 인증 여부 확인
    public boolean isEmailVerified(String email) {
        String verifiedKey = VERIFIED_PREFIX + email;
        String verified = redisService.getStringValue(verifiedKey);
        return "true".equals(verified);
    }

    // 이메일 인증 상태 삭제 (회원가입 완료 후 호출)
    public void clearVerificationStatus(String email) {
        String verifiedKey = VERIFIED_PREFIX + email;
        redisService.deleteValue(verifiedKey);
        log.info("이메일 인증 상태 삭제 완료: {}", email);
    }
}