package com.smooth.smooth_backend_user.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterResponseDto {
    private Long userId;
    private String name;
    private String token;

    public static RegisterResponseDto success(Long userId, String name, String token) {
        RegisterResponseDto response = new RegisterResponseDto();
        response.setUserId(userId);
        response.setName(name);
        response.setToken(token);
        return response;
    }
}