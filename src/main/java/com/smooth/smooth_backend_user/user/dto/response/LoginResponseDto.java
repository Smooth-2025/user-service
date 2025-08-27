package com.smooth.smooth_backend_user.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponseDto {
    private Long userId;
    private String name;
    private String token;

    public static LoginResponseDto success(Long userId, String name, String token) {
        LoginResponseDto response = new LoginResponseDto();
        response.setUserId(userId);
        response.setName(name);
        response.setToken(token);
        return response;
    }
}