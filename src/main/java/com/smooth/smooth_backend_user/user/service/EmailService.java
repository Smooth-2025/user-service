package com.smooth.smooth_backend_user.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender javaMailSender;

    public void sendVerificationEmail(String toEmail, String verificationCode) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(toEmail);
            message.setSubject("[Smooth] 이메일 인증코드");
            message.setText(
                    "안녕하세요, Smooth입니다.\n\n" +
                            "회원가입을 위한 인증코드입니다:\n\n" +
                            "인증코드: " + verificationCode + "\n\n" +
                            "인증코드는 3분간 유효합니다.\n" +
                            "만약 본인이 요청하지 않은 인증코드라면 무시해주세요.\n\n" +
                            "감사합니다."
            );

            javaMailSender.send(message);
            log.info("인증 이메일 발송 완료: {}", toEmail);

        } catch (Exception e) {
            log.error("이메일 발송 실패: {}", toEmail, e);
            throw new RuntimeException("이메일 발송에 실패했습니다.");
        }
    }
}