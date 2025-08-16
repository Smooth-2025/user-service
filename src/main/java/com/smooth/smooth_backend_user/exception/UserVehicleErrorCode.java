package com.smooth.smooth_backend_user.exception;

import com.smooth.smooth_backend_user.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum UserVehicleErrorCode implements ErrorCode {
    // 조회/권한

    // 연동(Link) 상태
    LINK_NOT_FOUND(HttpStatus.NOT_FOUND, 4010, "연동된 차량이 없습니다."),


    // 시스템/경합
    VEHICLE_LINK_CONFLICT(HttpStatus.CONFLICT, 4030, "이미 연동된 차량이 있습니다.");

    private final HttpStatus httpStatus;
    private final Integer code;
    private final String message;
}
