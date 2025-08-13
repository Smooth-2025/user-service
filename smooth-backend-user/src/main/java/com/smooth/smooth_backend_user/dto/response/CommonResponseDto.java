package com.smooth.smooth_backend_user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommonResponseDto {
    private Boolean success;
    private String message;

    public static CommonResponseDto success(String message) {
        return new CommonResponseDto(true, message);
    }

    public static CommonResponseDto error(String message) {
        return new CommonResponseDto(false, message);
    }
}