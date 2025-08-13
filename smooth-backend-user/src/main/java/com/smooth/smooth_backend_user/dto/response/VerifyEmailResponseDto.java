package com.smooth.smooth_backend_user.dto.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VerifyEmailResponseDto {
    private boolean success;
    private String message;
    private String email;
    private boolean verified;

    public static VerifyEmailResponseDto success(String email) {
        VerifyEmailResponseDto dto = new VerifyEmailResponseDto();
        dto.success = true;
        dto.message = "이메일 인증이 완료되었습니다.";
        dto.email = email;
        dto.verified = true;
        return dto;
    }

    public static VerifyEmailResponseDto error(String message) {
        VerifyEmailResponseDto dto = new VerifyEmailResponseDto();
        dto.success = false;
        dto.message = message;
        dto.verified = false;
        return dto;
    }
}