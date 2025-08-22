package com.smooth.smooth_backend_user.user.exception;

import com.smooth.smooth_backend_user.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum UserErrorCode implements ErrorCode {
    // 10xx: 기본 사용자 관리
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, 1001, "사용자를 찾을 수 없습니다."),
    USER_ALREADY_EXISTS(HttpStatus.CONFLICT, 1002, "이미 존재하는 사용자입니다."),

    // 11xx: 계정 상태 관련
    ACCOUNT_LOCKED(HttpStatus.FORBIDDEN, 1101, "계정이 잠겨있습니다."),
    ACCOUNT_DISABLED(HttpStatus.FORBIDDEN, 1102, "비활성화된 계정입니다."),
    ACCOUNT_EXPIRED(HttpStatus.FORBIDDEN, 1103, "만료된 계정입니다."),

    // 12xx: 인증/권한 관련
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, 1201, "회원정보가 일치하지 않습니다."),
    CURRENT_PASSWORD_MISMATCH(HttpStatus.BAD_REQUEST, 1202, "현재 비밀번호가 일치하지 않습니다."),
    PASSWORD_CONFIRMATION_MISMATCH(HttpStatus.BAD_REQUEST, 1203, "새 비밀번호가 일치하지 않습니다."),

    // 13xx: 이메일/인증 관련
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, 1301, "이미 존재하는 이메일입니다."),
    EMAIL_NOT_VERIFIED(HttpStatus.BAD_REQUEST, 1302, "이메일 인증을 완료해주세요."),
    EMAIL_SEND_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, 1303, "이메일 발송 횟수가 초과되었습니다."),
    VERIFICATION_CODE_EXPIRED(HttpStatus.BAD_REQUEST, 1304, "인증코드가 만료되었습니다."),
    VERIFICATION_CODE_MISMATCH(HttpStatus.BAD_REQUEST, 1305, "인증코드가 일치하지 않습니다."),

    // 14xx: 약관/정책 관련
    TERMS_NOT_AGREED(HttpStatus.BAD_REQUEST, 1401, "필수 약관에 동의해주세요."),

    // 15xx: 토큰 관련
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, 1501, "유효하지 않은 토큰입니다."),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, 1502, "만료된 토큰입니다."),

    // 16xx: 차량 관련
    VEHICLE_NOT_FOUND(HttpStatus.NOT_FOUND, 1601, "연동된 차량이 없습니다."),
    VEHICLE_LINK_CONFLICT(HttpStatus.CONFLICT, 1602, "이미 연동된 차량이 있습니다."),
    VEHICLE_LINK_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, 1603, "차량 연동에 실패했습니다.");

    private final HttpStatus httpStatus;
    private final Integer code;
    private final String message;
}