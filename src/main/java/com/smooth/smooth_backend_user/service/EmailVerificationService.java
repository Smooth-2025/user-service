package com.smooth.smooth_backend_user.service;

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

    // 인증코드 생성
    private String generateVerificationCode() {
        SecureRandom random = new SecureRandom();
        StringBuilder code = new StringBuilder();

        for (int i = 0; i < CODE_LENGTH; i++) {
            code.append(random.nextInt(10));
        }

        return code.toString();
    }

    // 인증코드 발송
    public void sendVerificationCode(String email) {
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
        emailService.sendVerificationEmail(email, verificationCode);

        log.info("인증코드 발송 완료: {} -> {}", email, verificationCode);
    }

    // 인증코드 검증
    public boolean verifyCode(String email, String inputCode) {
        String redisKey = CODE_PREFIX + email;
        String storedCode = redisService.getStringValue(redisKey);

        if (storedCode == null) {
            log.warn("인증코드 만료 또는 존재하지 않음: {}", email);
            return false;
        }

        if (!storedCode.equals(inputCode)) {
            log.warn("인증코드 불일치: {} -> 입력:{}, 저장:{}", email, inputCode, storedCode);
            return false;
        }

        // 인증 성공 시 인증코드 삭제
        redisService.deleteValue(redisKey);

        // 인증 완료 표시 (회원가입 시까지 유지)
        String verifiedKey = "email_verified:" + email;
        redisService.setStringValue(verifiedKey, "true", 30 * 60); // 30분 유지

        log.info("이메일 인증 성공: {}", email);
        return true;
    }

    // 이메일 인증 여부 확인
    public boolean isEmailVerified(String email) {
        String verifiedKey = "email_verified:" + email;
        String verified = redisService.getStringValue(verifiedKey);
        return "true".equals(verified);
    }
//    이메일 인증 상태 삭제 (회원가입 완료 후 호출)
    public void clearVerificationStatus(String email) {
        String verifiedKey = VERIFIED_PREFIX + email;
        redisService.deleteValue(verifiedKey);
        log.info("이메일 인증 상태 삭제 완료: {}", email);
    }
}