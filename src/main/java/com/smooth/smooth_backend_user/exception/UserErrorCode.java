package com.smooth.smooth_backend_user.exception;

import com.smooth.smooth_backend_user.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum UserErrorCode implements ErrorCode {
    // 기본 사용자 관리
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, 1001, "사용자를 찾을 수 없습니다.");

    private final HttpStatus httpStatus;
    private final Integer code;
    private final String message;
}
