package com.smooth.smooth_backend_user.exception;

import com.smooth.smooth_backend_user.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AuthErrorCode implements ErrorCode {
    // 회원가입 관련
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, 2001, "이미 존재하는 이메일입니다."),
    EMAIL_NOT_VERIFIED(HttpStatus.BAD_REQUEST, 2002, "이메일 인증을 완료해주세요."),
    TERMS_NOT_AGREED(HttpStatus.BAD_REQUEST, 2003, "필수 약관에 동의해주세요."),

    // 로그인 관련
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, 2004, "회원정보가 일치하지 않습니다."),

    // 비밀번호 관련
    CURRENT_PASSWORD_MISMATCH(HttpStatus.BAD_REQUEST, 2005, "현재 비밀번호가 일치하지 않습니다."),
    PASSWORD_CONFIRMATION_MISMATCH(HttpStatus.BAD_REQUEST, 2006, "새 비밀번호가 일치하지 않습니다."),

    // 이메일 인증 관련
    EMAIL_SEND_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, 2007, "이메일 발송 횟수가 초과되었습니다."),
    VERIFICATION_CODE_EXPIRED(HttpStatus.BAD_REQUEST, 2008, "인증코드가 만료되었습니다."),
    VERIFICATION_CODE_MISMATCH(HttpStatus.BAD_REQUEST, 2009, "인증코드가 일치하지 않습니다."),

    // 토큰 관련
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, 2010, "유효하지 않은 토큰입니다.");

    private final HttpStatus httpStatus;
    private final Integer code;
    private final String message;
}