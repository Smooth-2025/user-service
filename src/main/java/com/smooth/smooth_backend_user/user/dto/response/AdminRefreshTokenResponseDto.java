package com.smooth.smooth_backend_user.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;


// 관리자 토큰 재발급 응답 DTO
// 토큰만 포함 (만료시간 정보 제외)

@Getter
@AllArgsConstructor
public class AdminRefreshTokenResponseDto {
    
    private String token;
    
    public static AdminRefreshTokenResponseDto success(String token) {
        return new AdminRefreshTokenResponseDto(token);
    }
}