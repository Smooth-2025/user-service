package com.smooth.smooth_backend_user.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class LoginResponseDto extends CommonResponseDto {
    private Long userId;
    private String name;
    private String token;

    public static LoginResponseDto success(Long userId, String name, String token) {
        LoginResponseDto response = new LoginResponseDto();
        response.setSuccess(true);
        response.setMessage("로그인 성공");
        response.setUserId(userId);
        response.setName(name);
        response.setToken(token);
        return response;
    }

    public static LoginResponseDto error(String message) {
        LoginResponseDto response = new LoginResponseDto();
        response.setSuccess(false);
        response.setMessage(message);
        return response;
    }
}