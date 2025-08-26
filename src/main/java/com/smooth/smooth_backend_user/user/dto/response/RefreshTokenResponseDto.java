package com.smooth.smooth_backend_user.user.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RefreshTokenResponseDto {
    private String accessToken;
    private long expiresIn;  // 초 단위

    public static RefreshTokenResponseDto success(String accessToken, long expiresIn) {
        return RefreshTokenResponseDto.builder()
                .accessToken(accessToken)
                .expiresIn(expiresIn)
                .build();
    }
}