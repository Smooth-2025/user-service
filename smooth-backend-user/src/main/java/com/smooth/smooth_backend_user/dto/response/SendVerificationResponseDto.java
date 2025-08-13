package com.smooth.smooth_backend_user.dto.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SendVerificationResponseDto {
    private boolean success;
    private String message;
    private String email;
    private int expirationTime; // 만료시간 (초)

    public static SendVerificationResponseDto success(String email, int expirationTime) {
        SendVerificationResponseDto dto = new SendVerificationResponseDto();
        dto.success = true;
        dto.message = "인증코드를 발송했습니다.";
        dto.email = email;
        dto.expirationTime = expirationTime;
        return dto;
    }

    public static SendVerificationResponseDto error(String message) {
        SendVerificationResponseDto dto = new SendVerificationResponseDto();
        dto.success = false;
        dto.message = message;
        return dto;
    }
}