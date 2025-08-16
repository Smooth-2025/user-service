package com.smooth.smooth_backend_user.dto.request;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class LinkVehicleRequestDto {
    @NotBlank(message = "차량 정보를 다시 확인해 주세요.")
    @Size(min = 6, max = 20)
    @Pattern(
            regexp = ".*[가-힣].*",
            message = "차량번호에는 한글이 최소 1자 이상 포함되어야 합니다."
    )
    private String plateNumber;

    @NotBlank(message = "차량 정보를 다시 확인해 주세요.")
    @Size(min = 15, max = 15)
    @Pattern(regexp = "\\d{15}", message = "IMEI는 15자리 숫자여야 합니다.")
    private String imei;
}