package com.smooth.smooth_backend_user.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;


// 관리자 로그인 응답 DTO
// 토큰과 이름, 역할 포함
 
@Getter
@AllArgsConstructor
public class AdminLoginResponseDto {
    
    private String name;
    private String token;
    private String role;
    
    public static AdminLoginResponseDto success(String name, String token, String role) {
        return new AdminLoginResponseDto(name, token, role);
    }
}