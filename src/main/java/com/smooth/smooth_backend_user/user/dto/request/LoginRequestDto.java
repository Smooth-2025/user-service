package com.smooth.smooth_backend_user.user.dto.request;

import lombok.Data;
import jakarta.validation.constraints.*;

@Data
public class LoginRequestDto {

    @NotBlank(message = "이메일을 입력해주세요.")
    @Email(message = "이메일 형식이 올바르지 않습니다.")
    private String email;

    @NotBlank(message = "비밀번호를 입력해주세요.")
    private String password;
}