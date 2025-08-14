package com.smooth.smooth_backend_user.dto.request;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class VehicleLinkRequestDto {
    @NotBlank(message = "차량 정보를 다시 확인해 주세요.") @Size(max = 20)
    private String plateNumber;

    @NotBlank(message = "차량 정보를 다시 확인해 주세요.") @Size(max = 15)
    private String imei;
}