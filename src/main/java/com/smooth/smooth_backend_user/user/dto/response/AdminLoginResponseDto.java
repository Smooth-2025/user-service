package com.smooth.smooth_backend_user.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;


// 관리자 로그인 응답 DTO
// 토큰과 이름 포함
 
@Getter
@AllArgsConstructor
public class AdminLoginResponseDto {
    
    private String name;
    private String token;
    
    public static AdminLoginResponseDto success(String name, String token) {
        return new AdminLoginResponseDto(name, token);
    }
}