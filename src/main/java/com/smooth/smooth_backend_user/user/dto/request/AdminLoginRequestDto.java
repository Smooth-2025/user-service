package com.smooth.smooth_backend_user.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminLoginRequestDto {
    
    @NotBlank(message = "관리자 ID를 입력해주세요.")
    private String loginId;
    
    @NotBlank(message = "비밀번호를 입력해주세요.")
    private String password;
}