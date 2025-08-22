package com.smooth.smooth_backend_user.user.dto.request;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class LinkVehicleRequestDto {
    @NotBlank(message = "차량 정보를 다시 확인해 주세요.")
    @Size(min = 6, max = 8, message = "차량번호는 6~8자리여야 합니다.")
    @Pattern(
            regexp = "^[0-9]{2,3}[가-힣][0-9]{3,4}$",
            message = "차량번호 형식이 올바르지 않습니다."
    )
    private String plateNumber;

    @NotBlank(message = "차량 정보를 다시 확인해 주세요.")
    @Size(min = 15, max = 15)
    @Pattern(regexp = "\\d{15}", message = "IMEI는 15자리 숫자여야 합니다.")
    private String imei;
}