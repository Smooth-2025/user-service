package com.smooth.smooth_backend_user.service;

import com.smooth.smooth_backend_user.exception.AuthErrorCode;
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
        int count = countStr != null ? Integer.parseInt(countStr) : 0;

        if (count >= MAX_SEND_COUNT) {
            log.warn("이메일 발송 횟수 초과: {} ({}회)", email, count);
            throw new BusinessException(AuthErrorCode.EMAIL_SEND_LIMIT_EXCEEDED,
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
            throw new RuntimeException("이미 가입된 회원입니다.");
        }

        // 인증코드 생성
        String verificationCode = generateVerificationCode();

        // Redis에 인증코드 저장 (3분 만료)
        String redisKey = CODE_PREFIX + email;
        redisService.setStringValue(redisKey, verificationCode, EXPIRATION_MINUTES * 60);

        // 이메일 발송
        try {
            emailService.sendVerificationEmail(email, verificationCode);

            // 발송 횟수 증가
            incrementSendCount(email);

            log.info("인증코드 발송 완료: {}", email);
        } catch (Exception e) {
            log.error("인증코드 발송 실패: {}", email, e);
            throw new BusinessException(AuthErrorCode.EMAIL_SEND_LIMIT_EXCEEDED, "이메일 발송에 실패했습니다.");
        }
    }

    // 인증코드 검증
    public boolean verifyCode(String email, String inputCode) {
        String redisKey = CODE_PREFIX + email;
        String storedCode = redisService.getStringValue(redisKey);

        if (storedCode == null) {
            log.warn("인증코드 만료 또는 존재하지 않음: {}", email);
            throw new BusinessException(AuthErrorCode.VERIFICATION_CODE_EXPIRED);
        }

        if (!storedCode.equals(inputCode)) {
            log.warn("인증코드 불일치: {}", email);
            throw new BusinessException(AuthErrorCode.VERIFICATION_CODE_MISMATCH);
        }

        // 인증 성공 시 인증코드 삭제
        redisService.deleteValue(redisKey);

        // 인증 완료 표시 (회원가입 시까지 유지)
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