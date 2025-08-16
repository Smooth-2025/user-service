package com.smooth.smooth_backend_user.global.exception;

import org.springframework.http.HttpStatus;

public interface ErrorCode {

    Integer getCode();
    String getMessage();
    HttpStatus getHttpStatus();
}
