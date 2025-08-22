package com.smooth.smooth_backend_user.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class RegisterResponseDto extends CommonResponseDto {
    private Long userId;
    private String name;
    private String token;

    public static RegisterResponseDto success(Long userId, String name, String token) {
        RegisterResponseDto response = new RegisterResponseDto();
        response.setSuccess(true);
        response.setMessage("회원가입이 완료되었습니다.");
        response.setUserId(userId);
        response.setName(name);
        response.setToken(token);
        return response;
    }

    public static RegisterResponseDto error(String message) {
        RegisterResponseDto response = new RegisterResponseDto();
        response.setSuccess(false);
        response.setMessage(message);
        return response;
    }
}