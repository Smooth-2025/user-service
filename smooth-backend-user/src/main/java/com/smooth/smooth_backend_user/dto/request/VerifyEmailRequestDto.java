package com.smooth.smooth_backend_user.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VerifyEmailRequestDto {

    @NotBlank(message = "이메일은 필수입니다.")
    @Email(message = "유효한 이메일 형식이 아닙니다.")
    private String email;

    @NotBlank(message = "인증코드는 필수입니다.")
    @Pattern(regexp = "^\\d{5}$", message = "인증코드는 5자리 숫자여야 합니다.")
    private String code;
}